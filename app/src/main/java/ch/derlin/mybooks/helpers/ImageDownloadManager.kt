package ch.derlin.mybooks.helpers

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.os.Environment
import android.support.design.widget.Snackbar
import android.support.v4.app.NotificationCompat
import android.webkit.URLUtil
import android.widget.Toast
import ch.derlin.mybooks.R
import timber.log.Timber
import java.io.File


object ImageDownloadManager {

    val notificationChannelId = "derlin.mybooks"

    val base64Compressors = mapOf<String, Bitmap.CompressFormat>(
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
            imageURI?.let { _ ->
                //                Snackbar.make(findViewById(android.R.id.content),
//                        "Image downloaded", Snackbar.LENGTH_SHORT)
//                        .setAction("view", { _ ->
//                            val intent = Intent(Intent.ACTION_VIEW)
//                            intent.setDataAndType(uri, "image/*")
//                            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
//                            if (intent.resolveActivity(packageManager) != null)
//                                startActivity(intent)
//                        })
//                        .show()
                Toast.makeText(this, "Image downloaded", Toast.LENGTH_SHORT).show()
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
                        android.support.v4.content.FileProvider.getUriForFile(ctx, ctx.getString(R.string.file_provider_authority), file)
                    else
                        Uri.fromFile(file)
            }

            //Tell the media scanner about the new file so that it is immediately available to the user.
            MediaScannerConnection.scanFile(ctx, arrayOf(file.toString()), null) { path, uri ->
                Timber.d("Scanned $path:")
                Timber.i("-> uri=$uri")
            }

            // Show a "download complete" notification
            val intent = Intent()
            intent.action = android.content.Intent.ACTION_VIEW
            intent.setDataAndType(Uri.fromFile(file), "$type/*")
            val pIntent = PendingIntent.getActivity(ctx, 0, intent, 0)

            val notificationManager = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val notificationBuilder = NotificationCompat.Builder(ctx, notificationChannelId)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentText("Downloa complete.")
                    .setContentTitle(file.name)
                    .setContentIntent(pIntent)

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                /* for android O (8), channel mandatory */
                val channelName = ctx.packageName
                val importance = NotificationManager.IMPORTANCE_DEFAULT
                val channel = NotificationChannel(notificationChannelId, channelName, importance)
                notificationManager.createNotificationChannel(channel)
                notificationBuilder.setChannelId(notificationChannelId)
            }

            val notification = notificationBuilder.build()
            notification.flags = notification.flags or Notification.FLAG_AUTO_CANCEL
            val notificationId = 85851

            notificationManager.notify(notificationId, notification)
        }
        return null
    }
}