package com.catlover.app.utils

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import java.io.File

class AudioRecorderManager(private val context: Context) {
    private var recorder: MediaRecorder? = null
    private var audioFile: File? = null

    fun startRecording() {
        audioFile = File(context.cacheDir, "temp_voice_msg.m4a")
        recorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setOutputFile(audioFile?.absolutePath)
            prepare()
            start()
        }
    }

    fun stopRecording(): File? {
        recorder?.apply {
            stop()
            release()
        }
        recorder = null
        return audioFile
    }
}

class AudioPlayerManager {
    private var mediaPlayer: MediaPlayer? = null

    fun playAudio(filePath: String) {
        stopAudio()
        mediaPlayer = MediaPlayer().apply {
            setDataSource(filePath)
            prepare()
            start()
        }
    }

    fun stopAudio() {
        mediaPlayer?.release()
        mediaPlayer = null
    }
}
