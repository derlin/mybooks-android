package ch.derlin.mybooks.helpers

import android.app.*
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import android.webkit.URLUtil
import android.widget.Toast
import androidx.core.app.NotificationCompat
import ch.derlin.mybooks.R
import nl.komponents.kovenant.Kovenant.deferred
import nl.komponents.kovenant.Promise
import nl.komponents.kovenant.task
import timber.log.Timber
import java.net.HttpURLConnection
import java.net.URL


object ImageDownloadManager {

    private const val notificationChannelId = "derlin.mybooks"

    private val base64Compressors = mapOf(
            "png" to Bitmap.CompressFormat.PNG,
            "jpg" to Bitmap.CompressFormat.JPEG,
            "jpeg" to Bitmap.CompressFormat.JPEG,
            "webp" to Bitmap.CompressFormat.WEBP)

    fun getBase64ImageType(imageData: String): String? =
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
                Toast.makeText(this, getString(R.string.image_downloaded), Toast.LENGTH_SHORT).show()
                return true
            }
        } else {
            try {
                downloadFromUrl(applicationContext, url)
                Toast.makeText(this, getString(R.string.image_downloaded), Toast.LENGTH_SHORT).show()
            } catch (t: Throwable) {
                Timber.e(t)
            }
        }
        return false
    }

    fun downloadFromUrl(ctx: Context, imageUrl: String): Long {
        require(URLUtil.isValidUrl(imageUrl)) {
            "Not a valid URL"
        }
        val contentType = getImageUrlContentType(imageUrl).get()
        require(contentType.isNotBlank()) {
            "Could not determine content type for URL $imageUrl"
        }
        val filename = "${System.currentTimeMillis()}.${contentType.split("/").last()}"
        val request = DownloadManager.Request(Uri.parse(imageUrl))
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename)
        request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI or DownloadManager.Request.NETWORK_MOBILE)
        return (ctx.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager).enqueue(request)
    }

    fun downloadBase64Image(ctx: Context, imageUrl: String): Uri? {

        getBase64ImageType(imageUrl)?.let { type ->
            val bytes = android.util.Base64.decode(imageUrl.split(",").last(), android.util.Base64.DEFAULT)
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)

            val resolver = ctx.contentResolver
            val fileName = "${System.currentTimeMillis()}.${type}"
            val contentValues = ContentValues().apply {
                put(MediaStore.Downloads.TITLE, fileName)
                put(MediaStore.Downloads.MIME_TYPE, "image/$type")
                put(MediaStore.Downloads.IS_PENDING, 1)
            }

            resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)?.let { uri ->

                resolver.openOutputStream(uri).use {
                    bitmap.compress(base64Compressors[type], 100, it)
                    contentValues.clear()
                    contentValues.put(MediaStore.Downloads.IS_PENDING, 0)
                    resolver.update(uri, contentValues, null, null)
                }

                // Show a "download complete" notification
                val intent = Intent()
                intent.action = Intent.ACTION_VIEW
                intent.setDataAndType(uri, "image/$type")
                val pIntent = PendingIntent.getActivity(ctx, 0, intent, PendingIntent.FLAG_IMMUTABLE)

                val notificationManager = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                // for android O (8), channel mandatory
                val channel = NotificationChannel(notificationChannelId, ctx.packageName, NotificationManager.IMPORTANCE_DEFAULT)
                notificationManager.createNotificationChannel(channel)

                val notification = NotificationCompat.Builder(ctx, notificationChannelId)
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setContentText(ctx.getString(R.string.image_downloaded))
                        .setContentTitle(fileName)
                        .setContentIntent(pIntent)
                        .setChannelId(notificationChannelId)
                        .build()

                notification.flags = notification.flags or Notification.FLAG_AUTO_CANCEL
                val notificationId = 85851

                notificationManager.notify(notificationId, notification)
                return uri
            }
        }
        return null
    }

    private fun getImageUrlContentType(imageUrl: String): Promise<String, Exception> {
        val deferred = deferred<String, Exception>()
        task {
            var contentType = MimeTypeMap.getFileExtensionFromUrl(imageUrl)

            if (contentType.isNullOrBlank()) {
                (URL(imageUrl).openConnection() as HttpURLConnection).let { connection ->
                    connection.requestMethod = "HEAD"
                    connection.connect()
                    contentType = connection.contentType ?: ""
                }
            }

            deferred.resolve(contentType)
        } fail {
            deferred.resolve("")
        }

        return deferred.promise
    }
}