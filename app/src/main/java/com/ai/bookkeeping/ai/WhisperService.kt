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
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

/**
 * Whisper 语音识别服务
 * 使用硅基流动 (SiliconFlow) 提供的 Whisper 模型进行语音转文字
 *
 * 硅基流动国内可访问，提供免费额度，支持中文识别
 * 获取API Key: https://cloud.siliconflow.cn/account/ak
 */
object WhisperService {

    private const val TAG = "WhisperService"
    private const val API_URL = "https://api.siliconflow.cn/v1/audio/transcriptions"
    private const val PREFS_NAME = "whisper_settings"
    private const val KEY_API_KEY = "siliconflow_api_key"

    // 录音参数
    private const val SAMPLE_RATE = 16000
    private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
    private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT

    private var apiKey: String? = null
    private var audioRecord: AudioRecord? = null
    private var isRecording = false

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    // 回调
    var onStateChange: ((RecordingState) -> Unit)? = null

    enum class RecordingState {
        IDLE, RECORDING, PROCESSING, COMPLETED, ERROR
    }

    /**
     * 初始化服务
     */
    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        apiKey = prefs.getString(KEY_API_KEY, null)
    }

    /**
     * 设置 API Key
     */
    fun setApiKey(context: Context, key: String) {
        apiKey = key
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_API_KEY, key)
            .apply()
    }

    /**
     * 获取 API Key
     */
    fun getApiKey(context: Context): String? {
        if (apiKey == null) {
            apiKey = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_API_KEY, null)
        }
        return apiKey
    }

    /**
     * 检查是否已配置 API Key
     */
    fun hasApiKey(context: Context): Boolean {
        return !getApiKey(context).isNullOrEmpty()
    }

    /**
     * 录音并转录 - 主要入口方法
     * @param context 上下文
     * @param maxDurationMs 最大录音时长（毫秒）
     * @param onAmplitude 音量回调（可选，用于显示波形）
     */
    suspend fun recordAndTranscribe(
        context: Context,
        maxDurationMs: Long = 15000,
        onAmplitude: ((Int) -> Unit)? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        val key = getApiKey(context)
        if (key.isNullOrEmpty()) {
            return@withContext Result.failure(Exception("请先设置 Groq API Key"))
        }

        // 1. 录音
        val recordResult = recordToFile(context, maxDurationMs, onAmplitude)
        if (recordResult.isFailure) {
            return@withContext Result.failure(recordResult.exceptionOrNull() ?: Exception("录音失败"))
        }

        val audioFile = recordResult.getOrNull()!!

        // 2. 转录
        val transcribeResult = transcribe(audioFile, key)

        // 3. 清理临时文件
        audioFile.delete()

        transcribeResult
    }

    /**
     * 录制音频到文件
     */
    private suspend fun recordToFile(
        context: Context,
        maxDurationMs: Long,
        onAmplitude: ((Int) -> Unit)?
    ): Result<File> = withContext(Dispatchers.IO) {
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

            val audioFile = File(context.cacheDir, "whisper_${System.currentTimeMillis()}.wav")
            val buffer = ByteArray(bufferSize)
            val audioData = mutableListOf<Byte>()

            isRecording = true
            audioRecord?.startRecording()
            onStateChange?.invoke(RecordingState.RECORDING)

            val startTime = System.currentTimeMillis()
            while (isRecording && (System.currentTimeMillis() - startTime) < maxDurationMs) {
                val read = audioRecord?.read(buffer, 0, bufferSize) ?: 0
                if (read > 0) {
                    // 计算音量
                    var sum = 0L
                    for (i in 0 until read step 2) {
                        if (i + 1 < read) {
                            val sample = (buffer[i].toInt() and 0xFF) or (buffer[i + 1].toInt() shl 8)
                            sum += sample * sample
                        }
                    }
                    val amplitude = kotlin.math.sqrt(sum.toDouble() / (read / 2)).toInt()
                    onAmplitude?.invoke(amplitude)

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
            FileOutputStream(audioFile).use { outputStream ->
                writeWavHeader(outputStream, audioData.size)
                outputStream.write(audioData.toByteArray())
            }

            onStateChange?.invoke(RecordingState.PROCESSING)
            Result.success(audioFile)
        } catch (e: SecurityException) {
            onStateChange?.invoke(RecordingState.ERROR)
            Result.failure(Exception("没有录音权限"))
        } catch (e: Exception) {
            Log.e(TAG, "录音失败", e)
            onStateChange?.invoke(RecordingState.ERROR)
            Result.failure(e)
        }
    }

    /**
     * 使用硅基流动 Whisper API 进行转录
     */
    private suspend fun transcribe(audioFile: File, apiKey: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(
                    "file",
                    audioFile.name,
                    audioFile.asRequestBody("audio/wav".toMediaType())
                )
                .addFormDataPart("model", "FunAudioLLM/SenseVoiceSmall")
                .build()

            val request = Request.Builder()
                .url(API_URL)
                .addHeader("Authorization", "Bearer $apiKey")
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()

            if (response.isSuccessful) {
                val responseBody = response.body?.string() ?: "{}"
                val json = JSONObject(responseBody)
                val text = json.optString("text", "").trim()

                if (text.isEmpty()) {
                    onStateChange?.invoke(RecordingState.ERROR)
                    Result.failure(Exception("未识别到语音内容"))
                } else {
                    onStateChange?.invoke(RecordingState.COMPLETED)
                    Result.success(text)
                }
            } else {
                val errorBody = response.body?.string() ?: ""
                Log.e(TAG, "Whisper API 错误: ${response.code} - $errorBody")
                onStateChange?.invoke(RecordingState.ERROR)

                val errorMessage = try {
                    val errorJson = JSONObject(errorBody)
                    errorJson.optJSONObject("error")?.optString("message") ?: "转录失败"
                } catch (e: Exception) {
                    "转录失败: ${response.code}"
                }
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Log.e(TAG, "转录失败", e)
            onStateChange?.invoke(RecordingState.ERROR)
            Result.failure(e)
        }
    }

    /**
     * 停止录音
     */
    fun stopRecording() {
        isRecording = false
    }

    /**
     * 检查是否正在录音
     */
    fun isCurrentlyRecording(): Boolean = isRecording

    /**
     * 写入 WAV 文件头
     */
    private fun writeWavHeader(outputStream: FileOutputStream, audioDataLength: Int) {
        val totalDataLen = audioDataLength + 36
        val byteRate = SAMPLE_RATE * 1 * 16 / 8

        val header = ByteArray(44)
        // RIFF header
        header[0] = 'R'.code.toByte()
        header[1] = 'I'.code.toByte()
        header[2] = 'F'.code.toByte()
        header[3] = 'F'.code.toByte()
        // File size - 8
        header[4] = (totalDataLen and 0xff).toByte()
        header[5] = (totalDataLen shr 8 and 0xff).toByte()
        header[6] = (totalDataLen shr 16 and 0xff).toByte()
        header[7] = (totalDataLen shr 24 and 0xff).toByte()
        // WAVE
        header[8] = 'W'.code.toByte()
        header[9] = 'A'.code.toByte()
        header[10] = 'V'.code.toByte()
        header[11] = 'E'.code.toByte()
        // fmt chunk
        header[12] = 'f'.code.toByte()
        header[13] = 'm'.code.toByte()
        header[14] = 't'.code.toByte()
        header[15] = ' '.code.toByte()
        // Subchunk1Size (16 for PCM)
        header[16] = 16
        header[17] = 0
        header[18] = 0
        header[19] = 0
        // AudioFormat (1 = PCM)
        header[20] = 1
        header[21] = 0
        // NumChannels (1 = Mono)
        header[22] = 1
        header[23] = 0
        // SampleRate
        header[24] = (SAMPLE_RATE and 0xff).toByte()
        header[25] = (SAMPLE_RATE shr 8 and 0xff).toByte()
        header[26] = (SAMPLE_RATE shr 16 and 0xff).toByte()
        header[27] = (SAMPLE_RATE shr 24 and 0xff).toByte()
        // ByteRate
        header[28] = (byteRate and 0xff).toByte()
        header[29] = (byteRate shr 8 and 0xff).toByte()
        header[30] = (byteRate shr 16 and 0xff).toByte()
        header[31] = (byteRate shr 24 and 0xff).toByte()
        // BlockAlign
        header[32] = 2
        header[33] = 0
        // BitsPerSample
        header[34] = 16
        header[35] = 0
        // data chunk
        header[36] = 'd'.code.toByte()
        header[37] = 'a'.code.toByte()
        header[38] = 't'.code.toByte()
        header[39] = 'a'.code.toByte()
        // Data size
        header[40] = (audioDataLength and 0xff).toByte()
        header[41] = (audioDataLength shr 8 and 0xff).toByte()
        header[42] = (audioDataLength shr 16 and 0xff).toByte()
        header[43] = (audioDataLength shr 24 and 0xff).toByte()

        outputStream.write(header)
    }

    /**
     * 释放资源
     */
    fun release() {
        stopRecording()
        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (e: Exception) {
            Log.e(TAG, "释放录音资源失败", e)
        }
        audioRecord = null
        onStateChange = null
    }
}
