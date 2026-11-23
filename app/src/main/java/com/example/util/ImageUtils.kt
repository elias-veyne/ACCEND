package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

object ImageUtils {
    /**
     * Center-crops the picked image to a 512x512 square JPEG in app filesDir
     * and returns its file:// URI string. Deletes any old file first.
     */
    fun saveProfilePhoto(context: Context, sourceUri: Uri): String? {
        return runCatching {
            val input = context.contentResolver.openInputStream(sourceUri) ?: return null
            val original = input.use { BitmapFactory.decodeStream(it) } ?: return null
            val size = minOf(original.width, original.height)
            val xOffset = (original.width - size) / 2
            val yOffset = (original.height - size) / 2
            val cropped = Bitmap.createBitmap(original, xOffset, yOffset, size, size)
            val scaled = Bitmap.createScaledBitmap(cropped, 512, 512, true)

            val outFile = File(context.filesDir, "pfp_${System.currentTimeMillis()}.jpg")
            // Clean up older pfp files
            context.filesDir.listFiles()?.filter { it.name.startsWith("pfp_") && it.name.endsWith(".jpg") }
                ?.forEach { it.delete() }

            FileOutputStream(outFile).use { fos ->
                scaled.compress(Bitmap.CompressFormat.JPEG, 90, fos)
            }
            if (cropped !== original) cropped.recycle()
            if (scaled !== cropped) scaled.recycle()
            original.recycle()
            Uri.fromFile(outFile).toString()
        }.getOrNull()
    }

    fun deleteProfilePhotos(context: Context) {
        context.filesDir.listFiles()
            ?.filter { it.name.startsWith("pfp_") && it.name.endsWith(".jpg") }
            ?.forEach { it.delete() }
    }

    fun encodeProfileThumbBase64(pfpUri: String?): String? {
        if (pfpUri.isNullOrBlank()) return null
        return runCatching {
            val file = File(Uri.parse(pfpUri).path ?: return null)
            if (!file.exists()) return null
            val bmp = BitmapFactory.decodeFile(file.absolutePath) ?: return null
            val scaled = Bitmap.createScaledBitmap(bmp, 256, 256, true)
            val baos = ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.JPEG, 82, baos)
            if (scaled !== bmp) scaled.recycle()
            bmp.recycle()
            Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)
        }.getOrNull()
    }

    fun decodeBase64ToProfilePhoto(context: Context, base64: String?): String? {
        if (base64.isNullOrBlank()) return null
        return runCatching {
            val bytes = Base64.decode(base64, Base64.DEFAULT)
            val outFile = File(context.filesDir, "pfp_${System.currentTimeMillis()}.jpg")
            deleteProfilePhotos(context)
            FileOutputStream(outFile).use { it.write(bytes) }
            Uri.fromFile(outFile).toString()
        }.getOrNull()
    }
}
