package com.mobdeve.s18.mco.utils

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.widget.Toast

class AudioPlayerUtils {

    companion object {
        private var mediaPlayer: MediaPlayer? = null

        fun playAudio(context: Context, audioUri: Uri) {
            try {
                // Stop any currently playing audio
                stopAudio()

                // Create new MediaPlayer instance
                mediaPlayer = MediaPlayer().apply {
                    setDataSource(context, audioUri)
                    prepareAsync()
                    setOnPreparedListener { mp ->
                        mp.start()
                        Toast.makeText(context, "Playing audio...", Toast.LENGTH_SHORT).show()
                    }
                    setOnCompletionListener {
                        Toast.makeText(context, "Audio playback completed", Toast.LENGTH_SHORT).show()
                    }
                    setOnErrorListener { _, what, extra ->
                        Toast.makeText(context, "Error playing audio: $what, $extra", Toast.LENGTH_SHORT).show()
                        true
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to play audio: ${e.message}", Toast.LENGTH_SHORT).show()
                e.printStackTrace()
            }
        }

        fun stopAudio() {
            mediaPlayer?.let { player ->
                try {
                    if (player.isPlaying) {
                        player.stop()
                    }
                    player.release()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                mediaPlayer = null
            }
        }

        fun pauseAudio() {
            mediaPlayer?.let { player ->
                try {
                    if (player.isPlaying) {
                        player.pause()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        fun resumeAudio() {
            mediaPlayer?.let { player ->
                try {
                    if (!player.isPlaying) {
                        player.start()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        fun isPlaying(): Boolean {
            return try {
                mediaPlayer?.isPlaying ?: false
            } catch (e: Exception) {
                false
            }
        }
    }
}
