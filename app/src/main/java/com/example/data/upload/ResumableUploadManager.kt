package com.example.data.upload

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.min
import kotlin.math.roundToInt

enum class UploadStatus {
    IDLE,
    UPLOADING,
    PAUSED,
    RETRYING,
    TRANSCODING,
    COMPLETED,
    FAILED,
    CANCELLED
}

data class UploadProgressState(
    val status: UploadStatus = UploadStatus.IDLE,
    val fileName: String = "",
    val totalMb: Double = 0.0,
    val uploadedMb: Double = 0.0,
    val percentage: Int = 0,
    val speedMbps: Double = 0.0,
    val remainingSeconds: Int = 0,
    val completedChunks: Int = 0,
    val totalChunks: Int = 0,
    val retryCount: Int = 0,
    val statusMessage: String = "Select a video to start chunked upload",
    val simulateNetworkDropOnce: Boolean = false
)

class ResumableUploadManager(private val scope: CoroutineScope) {
    private val _state = MutableStateFlow(UploadProgressState())
    val state: StateFlow<UploadProgressState> = _state.asStateFlow()

    private var uploadJob: Job? = null
    private var onUploadFinishedCallback: (suspend () -> Result<Unit>)? = null
    private var droppedOnce = false

    fun startUpload(
        fileName: String,
        totalMb: Double,
        simulateDisconnect: Boolean = false,
        onCompletePublish: suspend () -> Result<Unit>
    ) {
        uploadJob?.cancel()
        droppedOnce = false
        onUploadFinishedCallback = onCompletePublish
        val safeTotalMb = totalMb.coerceAtLeast(12.0)
        val chunkSizeMb = 4.0
        val chunks = ((safeTotalMb / chunkSizeMb).toInt()).coerceAtLeast(4)

        _state.value = UploadProgressState(
            status = UploadStatus.UPLOADING,
            fileName = fileName,
            totalMb = safeTotalMb,
            uploadedMb = 0.0,
            percentage = 0,
            speedMbps = 6.4,
            remainingSeconds = (safeTotalMb / 6.4).roundToInt(),
            completedChunks = 0,
            totalChunks = chunks,
            retryCount = 0,
            statusMessage = "Uploading chunk 1 of $chunks (Resumable Session Active)",
            simulateNetworkDropOnce = simulateDisconnect
        )
        runChunkLoop()
    }

    fun pauseUpload() {
        if (_state.value.status == UploadStatus.UPLOADING || _state.value.status == UploadStatus.RETRYING) {
            uploadJob?.cancel()
            _state.update {
                it.copy(
                    status = UploadStatus.PAUSED,
                    speedMbps = 0.0,
                    statusMessage = "Paused at ${it.percentage}% (${String.format("%.1f", it.uploadedMb)} / ${String.format("%.1f", it.totalMb)} MB)"
                )
            }
        }
    }

    fun resumeUpload() {
        val current = _state.value
        if (current.status == UploadStatus.PAUSED || current.status == UploadStatus.FAILED) {
            _state.update {
                it.copy(
                    status = UploadStatus.UPLOADING,
                    statusMessage = "Resuming from chunk ${it.completedChunks + 1} of ${it.totalChunks}..."
                )
            }
            runChunkLoop()
        }
    }

    fun cancelUpload() {
        uploadJob?.cancel()
        onUploadFinishedCallback = null
        _state.value = UploadProgressState(
            status = UploadStatus.CANCELLED,
            statusMessage = "Upload cancelled. Chunk buffer cleared."
        )
    }

    fun reset() {
        uploadJob?.cancel()
        onUploadFinishedCallback = null
        _state.value = UploadProgressState()
    }

    private fun runChunkLoop() {
        uploadJob?.cancel()
        uploadJob = scope.launch {
            try {
                while (true) {
                    val cur = _state.value
                    if (cur.completedChunks >= cur.totalChunks) break

                    // Demonstrate automatic retry after transient internet disconnection
                    if (cur.simulateNetworkDropOnce && !droppedOnce && cur.completedChunks == cur.totalChunks / 2) {
                        droppedOnce = true
                        _state.update {
                            it.copy(
                                status = UploadStatus.RETRYING,
                                speedMbps = 0.0,
                                retryCount = it.retryCount + 1,
                                statusMessage = "Connection interrupted at ${it.percentage}%. Auto-retrying chunk ${it.completedChunks + 1}..."
                            )
                        }
                        delay(1100L)
                        _state.update {
                            it.copy(
                                status = UploadStatus.UPLOADING,
                                statusMessage = "Reconnected! Resuming chunk ${it.completedChunks + 1} of ${it.totalChunks}..."
                            )
                        }
                    }

                    delay(320L)
                    val nextChunk = cur.completedChunks + 1
                    val chunkMb = cur.totalMb / cur.totalChunks.toDouble()
                    val newUploaded = min(cur.totalMb, nextChunk * chunkMb)
                    val pct = ((newUploaded / cur.totalMb) * 100.0).roundToInt().coerceIn(0, 100)
                    val speed = 5.8 + (nextChunk % 3) * 0.9
                    val remaining = ((cur.totalMb - newUploaded) / speed).roundToInt().coerceAtLeast(1)

                    _state.update {
                        it.copy(
                            status = UploadStatus.UPLOADING,
                            completedChunks = nextChunk,
                            uploadedMb = newUploaded,
                            percentage = pct,
                            speedMbps = speed,
                            remainingSeconds = remaining,
                            statusMessage = "Uploading chunk $nextChunk/${cur.totalChunks} • ${String.format("%.1f", speed)} MB/s"
                        )
                    }
                }

                _state.update {
                    it.copy(
                        status = UploadStatus.TRANSCODING,
                        percentage = 100,
                        uploadedMb = it.totalMb,
                        speedMbps = 0.0,
                        remainingSeconds = 0,
                        statusMessage = "Finalizing adaptive HLS/MP4 transcoding & publishing..."
                    )
                }
                delay(450L)

                val result = onUploadFinishedCallback?.invoke() ?: Result.success(Unit)
                if (result.isSuccess) {
                    _state.update {
                        it.copy(
                            status = UploadStatus.COMPLETED,
                            statusMessage = "Upload complete! Video is live and indexed."
                        )
                    }
                } else {
                    _state.update {
                        it.copy(
                            status = UploadStatus.FAILED,
                            statusMessage = "Publish failed: ${result.exceptionOrNull()?.localizedMessage ?: "Retry needed"}"
                        )
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        status = UploadStatus.FAILED,
                        statusMessage = e.localizedMessage ?: "Upload failed."
                    )
                }
            }
        }
    }
}
