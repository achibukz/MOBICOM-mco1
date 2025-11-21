package com.mobdeve.s18.mco.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

class SafUtils {

    companion object {
        const val MIME_TYPE_IMAGE = "image/*"
        const val MIME_TYPE_AUDIO = "audio/*" // Changed from "audio/mp3" to support all audio formats

        fun createImagePickerLauncher(
            activity: AppCompatActivity,
            onResult: (List<Uri>) -> Unit
        ): ActivityResultLauncher<Intent> {
            return activity.registerForActivityResult(
                ActivityResultContracts.StartActivityForResult()
            ) { result ->
                if (result.resultCode == AppCompatActivity.RESULT_OK) {
                    val uris = mutableListOf<Uri>()

                    result.data?.let { data ->
                        // Multiple selection
                        data.clipData?.let { clipData ->
                            for (i in 0 until clipData.itemCount) {
                                val uri = clipData.getItemAt(i).uri
                                uris.add(uri)
                                // Use the flags returned by the intent to request persistable permission
                                takePersistablePermission(activity, uri, data.flags)
                            }
                        } ?: run {
                            // Single selection
                            data.data?.let { uri ->
                                uris.add(uri)
                                takePersistablePermission(activity, uri, data.flags)
                            }
                        }
                    }

                    onResult(uris)
                }
            }
        }

        fun createAudioPickerLauncher(
            activity: AppCompatActivity,
            onResult: (Uri?) -> Unit
        ): ActivityResultLauncher<Intent> {
            return activity.registerForActivityResult(
                ActivityResultContracts.StartActivityForResult()
            ) { result ->
                if (result.resultCode == AppCompatActivity.RESULT_OK) {
                    result.data?.data?.let { uri ->
                        // Try to take persistable permission using flags if available
                        val flags = result.data?.flags ?: Intent.FLAG_GRANT_READ_URI_PERMISSION
                        takePersistablePermission(activity, uri, flags)
                        onResult(uri)
                    }
                }
            }
        }

        fun createImagePickerIntent(): Intent {
            return Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                type = MIME_TYPE_IMAGE
                putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                addCategory(Intent.CATEGORY_OPENABLE)
            }
        }

        fun createAudioPickerIntent(): Intent {
            return Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                type = MIME_TYPE_AUDIO
                addCategory(Intent.CATEGORY_OPENABLE)
                // Add extra MIME types to ensure MP3 files are selectable
                putExtra(Intent.EXTRA_MIME_TYPES, arrayOf(
                    "audio/*",
                    "audio/mpeg",
                    "audio/mp3",
                    "audio/wav",
                    "audio/m4a"
                ))
            }
        }

        private fun takePersistablePermission(context: Context, uri: Uri, flags: Int) {
            try {
                val takeFlags = flags and (Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                context.contentResolver.takePersistableUriPermission(uri, takeFlags)
            } catch (e: SecurityException) {
                // Permission might not be available for this URI
                e.printStackTrace()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
