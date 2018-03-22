package ch.derlin.mybooks.helpers

import android.app.Activity
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.os.Environment
import android.support.design.widget.Snackbar
import android.webkit.URLUtil
import android.widget.Toast
import timber.log.Timber
import java.io.File


object ImageDownloadManager {

    private val base64Compressors = mapOf<String, Bitmap.CompressFormat>(
            "png" to Bitmap.CompressFormat.PNG,
            "jpg" to Bitmap.CompressFormat.JPEG,
            "jpeg" to Bitmap.CompressFormat.JPEG,
            "webp" to Bitmap.CompressFormat.WEBP)

    private fun getBase64ImageType(imageData: String): String? =
            Regex("data:image/([^;]+).*").find(imageData)?.groups?.get(1)?.value

    fun isDownloadable(url: String): Boolean =
            (url.startsWith("http") && URLUtil.isValidUrl(url)) || isBase64Image(url)

    fun isBase64Image(url: String): Boolean {
        getBase64ImageType(url)?.let {
            return base64Compressors.containsKey(it)
        }
        return false
    }

    fun Activity.downloadImage(url: String): Boolean {
        if (isBase64Image(url)) {
            val imageURI = downloadBase64Image(applicationContext, url)
            imageURI?.let { uri ->
                Snackbar.make(findViewById(android.R.id.content),
                        "Image downloaded", Snackbar.LENGTH_SHORT)
                        .setAction("view", { _ ->
                            val intent = Intent(Intent.ACTION_VIEW)
                            intent.setDataAndType(uri, "image/*")
                            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            if (intent.resolveActivity(packageManager) != null)
                                startActivity(intent)
                        })
                        .show()
                return true
            }
        } else {
            try {
                downloadFromUrl(applicationContext, url)
                Toast.makeText(this, "Image downloaded", Toast.LENGTH_SHORT).show()
            } catch (t: Throwable) {
                Timber.e(t)
            }
        }
        return false
    }

    @Throws(IllegalArgumentException::class)
    fun downloadFromUrl(ctx: Context, imageUrl: String): Long {
        if (!URLUtil.isValidUrl(imageUrl)) throw IllegalArgumentException("Not a valid URL")

        val request = DownloadManager.Request(Uri.parse(imageUrl))
        request.allowScanningByMediaScanner()
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, imageUrl.split("/").last())

        return (ctx.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager).enqueue(request)
    }

    fun downloadBase64Image(ctx: Context, imageUrl: String): Uri? {

        getBase64ImageType(imageUrl)?.let { type ->
            val bytes = android.util.Base64.decode(imageUrl.split(",").last(), android.util.Base64.DEFAULT)
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)

            val externalStorage = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val file = File(externalStorage, "${System.currentTimeMillis()}.${type}")

            file.outputStream().use {
                if (bitmap.compress(base64Compressors.get(type), 100, it))
                    // see https://proandroiddev.com/sharing-files-though-intents-are-you-ready-for-nougat-70f7e9294a0b
                    return if (VERSION.SDK_INT >= VERSION_CODES.N)
                        android.support.v4.content.FileProvider.getUriForFile(ctx, ctx.packageName + ".provider", file)
                    else
                        Uri.fromFile(file)
            }
        }
        return null
    }
}