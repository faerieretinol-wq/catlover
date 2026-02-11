package com.catlover.messenger.utils

import android.content.Context
import android.media.MediaRecorder
import java.io.File

class AudioRecorderManager(private val context: Context) {
    private var recorder: MediaRecorder? = null
    fun startRecording() {
        recorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setOutputFile(File(context.cacheDir, "voice.m4a").absolutePath)
            prepare()
            start()
        }
    }
    fun stopRecording(): File {
        recorder?.stop()
        recorder?.release()
        return File(context.cacheDir, "voice.m4a")
    }
}
