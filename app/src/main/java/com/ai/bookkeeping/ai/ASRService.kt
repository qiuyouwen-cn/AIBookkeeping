package com.ai.bookkeeping.ai

import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.asRequestBody
import okio.ByteString.Companion.toByteString
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * ASR 语音识别服务
 * 支持两种模式：
 * 1. 音频文件转录（HTTP POST）
 * 2. 实时流式识别（WebSocket）
 */
object ASRService {

    private const val TAG = "ASRService"
    private const val PREFS_NAME = "asr_settings"
    private const val KEY_SERVER_URL = "asr_server_url"
    private const val DEFAULT_SERVER = "YOUR_SERVER:5678"

    // 录音参数
    private const val SAMPLE_RATE = 16000
    private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
    private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT

    private var serverUrl: String = DEFAULT_SERVER
    private var webSocket: WebSocket? = null
    private var audioRecord: AudioRecord? = null
    private var isRecording = false
    private var recordingThread: Thread? = null

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    // 回调接口
    var onResult: ((text: String, isFinal: Boolean) -> Unit)? = null
    var onError: ((error: String) -> Unit)? = null
    var onStateChange: ((state: RecordingState) -> Unit)? = null

    enum class RecordingState {
        IDLE, CONNECTING, RECORDING, PROCESSING, COMPLETED, ERROR
    }

    /**
     * 初始化服务
     */
    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        serverUrl = prefs.getString(KEY_SERVER_URL, DEFAULT_SERVER) ?: DEFAULT_SERVER
    }

    /**
     * 设置服务器地址
     */
    fun setServerUrl(context: Context, url: String) {
        serverUrl = url
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_SERVER_URL, url)
            .apply()
    }

    /**
     * 获取服务器地址
     */
    fun getServerUrl(context: Context): String {
        if (serverUrl == DEFAULT_SERVER) {
            serverUrl = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_SERVER_URL, DEFAULT_SERVER) ?: DEFAULT_SERVER
        }
        return serverUrl
    }

    /**
     * 检查是否已配置服务器
     */
    fun hasServerConfig(context: Context): Boolean {
        val url = getServerUrl(context)
        return url.isNotEmpty() && url != DEFAULT_SERVER
    }

    // ==================== 方式1：音频文件转录 ====================

    /**
     * 录制音频到文件
     */
    suspend fun recordToFile(context: Context, durationMs: Long): Result<File> = withContext(Dispatchers.IO) {
        try {
            val bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
            if (bufferSize == AudioRecord.ERROR_BAD_VALUE || bufferSize == AudioRecord.ERROR) {
                return@withContext Result.failure(Exception("无法获取录音缓冲区大小"))
            }

            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                bufferSize
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                return@withContext Result.failure(Exception("录音器初始化失败"))
            }

            val audioFile = File(context.cacheDir, "voice_${System.currentTimeMillis()}.wav")
            val outputStream = FileOutputStream(audioFile)
            val buffer = ByteArray(bufferSize)
            val audioData = mutableListOf<Byte>()

            isRecording = true
            audioRecord?.startRecording()
            onStateChange?.invoke(RecordingState.RECORDING)

            val startTime = System.currentTimeMillis()
            while (isRecording && (System.currentTimeMillis() - startTime) < durationMs) {
                val read = audioRecord?.read(buffer, 0, bufferSize) ?: 0
                if (read > 0) {
                    for (i in 0 until read) {
                        audioData.add(buffer[i])
                    }
                }
            }

            audioRecord?.stop()
            audioRecord?.release()
            audioRecord = null
            isRecording = false

            // 写入 WAV 文件
            writeWavFile(outputStream, audioData.toByteArray())
            outputStream.close()

            onStateChange?.invoke(RecordingState.PROCESSING)
            Result.success(audioFile)
        } catch (e: Exception) {
            Log.e(TAG, "录音失败", e)
            onStateChange?.invoke(RecordingState.ERROR)
            Result.failure(e)
        }
    }

    /**
     * 上传音频文件进行转录
     */
    suspend fun transcribeFile(audioFile: File): Result<String> = withContext(Dispatchers.IO) {
        try {
            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(
                    "file", audioFile.name,
                    audioFile.asRequestBody("audio/wav".toMediaType())
                )
                .addFormDataPart("language", "zh")
                .build()

            val request = Request.Builder()
                .url("http://$serverUrl/transcribe")
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val json = JSONObject(response.body?.string() ?: "{}")
                val text = json.optString("text", "")
                onStateChange?.invoke(RecordingState.COMPLETED)
                Result.success(text)
            } else {
                onStateChange?.invoke(RecordingState.ERROR)
                Result.failure(Exception("转录失败: ${response.code}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "转录失败", e)
            onStateChange?.invoke(RecordingState.ERROR)
            Result.failure(e)
        }
    }

    /**
     * 录制并转录（一步完成）
     */
    suspend fun recordAndTranscribe(context: Context, durationMs: Long = 10000): Result<String> {
        val recordResult = recordToFile(context, durationMs)
        return recordResult.fold(
            onSuccess = { file ->
                val transcribeResult = transcribeFile(file)
                file.delete() // 清理临时文件
                transcribeResult
            },
            onFailure = { Result.failure(it) }
        )
    }

    // ==================== 方式2：实时流式识别 ====================

    /**
     * 开始实时流式识别
     */
    fun startStreaming(
        onPartialResult: (text: String) -> Unit,
        onFinalResult: (text: String) -> Unit,
        onStreamError: (error: String) -> Unit
    ) {
        onStateChange?.invoke(RecordingState.CONNECTING)

        val request = Request.Builder()
            .url("ws://$serverUrl/asr/stream")
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(ws: WebSocket, response: Response) {
                Log.d(TAG, "WebSocket 连接成功")
                // 发送配置
                val config = JSONObject().apply {
                    put("type", "config")
                    put("language", "zh")
                    put("vad_enabled", true)
                    put("vad_min_silence_duration", 500)
                }
                ws.send(config.toString())

                // 开始录音并发送
                startRecordingForStream()
                onStateChange?.invoke(RecordingState.RECORDING)
            }

            override fun onMessage(ws: WebSocket, text: String) {
                try {
                    val json = JSONObject(text)
                    val resultText = json.optString("text", "")
                    val isFinal = json.optBoolean("is_final", false)

                    if (isFinal) {
                        onFinalResult(resultText)
                        onResult?.invoke(resultText, true)
                    } else {
                        onPartialResult(resultText)
                        onResult?.invoke(resultText, false)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "解析消息失败", e)
                }
            }

            override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "WebSocket 错误", t)
                stopStreaming()
                onStreamError(t.message ?: "连接失败")
                onError?.invoke(t.message ?: "连接失败")
                onStateChange?.invoke(RecordingState.ERROR)
            }

            override fun onClosed(ws: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WebSocket 关闭: $reason")
                stopRecording()
                onStateChange?.invoke(RecordingState.COMPLETED)
            }
        })
    }

    /**
     * 开始录音并发送到 WebSocket
     */
    private fun startRecordingForStream() {
        val bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)

        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                bufferSize
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                onError?.invoke("录音器初始化失败")
                return
            }

            isRecording = true
            audioRecord?.startRecording()

            recordingThread = Thread {
                val buffer = ByteArray(bufferSize)
                while (isRecording) {
                    val read = audioRecord?.read(buffer, 0, bufferSize) ?: 0
                    if (read > 0) {
                        webSocket?.send(buffer.copyOf(read).toByteString())
                    }
                }
            }
            recordingThread?.start()
        } catch (e: SecurityException) {
            onError?.invoke("没有录音权限")
        } catch (e: Exception) {
            onError?.invoke("录音启动失败: ${e.message}")
        }
    }

    /**
     * 停止流式识别
     */
    fun stopStreaming() {
        stopRecording()
        webSocket?.close(1000, "Done")
        webSocket = null
    }

    /**
     * 停止录音
     */
    fun stopRecording() {
        isRecording = false
        recordingThread?.join(1000)
        recordingThread = null

        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (e: Exception) {
            Log.e(TAG, "停止录音失败", e)
        }
        audioRecord = null
    }

    /**
     * 手动停止并等待最终结果
     */
    fun finishRecording() {
        stopRecording()
        // 发送结束信号
        val endSignal = JSONObject().apply {
            put("type", "end")
        }
        webSocket?.send(endSignal.toString())
    }

    /**
     * 写入 WAV 文件头
     */
    private fun writeWavFile(outputStream: FileOutputStream, audioData: ByteArray) {
        val totalDataLen = audioData.size + 36
        val byteRate = SAMPLE_RATE * 1 * 16 / 8

        val header = ByteArray(44)
        header[0] = 'R'.code.toByte()
        header[1] = 'I'.code.toByte()
        header[2] = 'F'.code.toByte()
        header[3] = 'F'.code.toByte()
        header[4] = (totalDataLen and 0xff).toByte()
        header[5] = (totalDataLen shr 8 and 0xff).toByte()
        header[6] = (totalDataLen shr 16 and 0xff).toByte()
        header[7] = (totalDataLen shr 24 and 0xff).toByte()
        header[8] = 'W'.code.toByte()
        header[9] = 'A'.code.toByte()
        header[10] = 'V'.code.toByte()
        header[11] = 'E'.code.toByte()
        header[12] = 'f'.code.toByte()
        header[13] = 'm'.code.toByte()
        header[14] = 't'.code.toByte()
        header[15] = ' '.code.toByte()
        header[16] = 16
        header[17] = 0
        header[18] = 0
        header[19] = 0
        header[20] = 1 // PCM
        header[21] = 0
        header[22] = 1 // 单声道
        header[23] = 0
        header[24] = (SAMPLE_RATE and 0xff).toByte()
        header[25] = (SAMPLE_RATE shr 8 and 0xff).toByte()
        header[26] = (SAMPLE_RATE shr 16 and 0xff).toByte()
        header[27] = (SAMPLE_RATE shr 24 and 0xff).toByte()
        header[28] = (byteRate and 0xff).toByte()
        header[29] = (byteRate shr 8 and 0xff).toByte()
        header[30] = (byteRate shr 16 and 0xff).toByte()
        header[31] = (byteRate shr 24 and 0xff).toByte()
        header[32] = 2 // BlockAlign
        header[33] = 0
        header[34] = 16 // BitsPerSample
        header[35] = 0
        header[36] = 'd'.code.toByte()
        header[37] = 'a'.code.toByte()
        header[38] = 't'.code.toByte()
        header[39] = 'a'.code.toByte()
        header[40] = (audioData.size and 0xff).toByte()
        header[41] = (audioData.size shr 8 and 0xff).toByte()
        header[42] = (audioData.size shr 16 and 0xff).toByte()
        header[43] = (audioData.size shr 24 and 0xff).toByte()

        outputStream.write(header)
        outputStream.write(audioData)
    }

    /**
     * 释放资源
     */
    fun release() {
        stopStreaming()
        onResult = null
        onError = null
        onStateChange = null
    }
}
