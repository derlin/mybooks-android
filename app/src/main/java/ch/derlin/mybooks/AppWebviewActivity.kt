package ch.derlin.mybooks


import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.support.v7.app.AppCompatActivity
import android.view.ContextMenu
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_webview.*
import java.io.File


class AppBrowserActivity : AppCompatActivity() {

    private var working: Boolean
        get() = progressBar.visibility == View.VISIBLE
        set(value) {
            progressBar.visibility = if (value) View.VISIBLE else View.INVISIBLE
        }

    private lateinit var url: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_webview)

        setSupportActionBar(toolbar)
        supportActionBar?.let { actionBar ->
            actionBar.setTitle("")
            actionBar.setDisplayHomeAsUpEnabled(true)
        }

        url = intent.getStringExtra("url") ?: "https://www.google.com?q=1984"

        webview.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView, progress: Int) {
                working = true
                progressBar.setProgress(progress)
                if (progress == 100) working = false
            }
        }

        WebView.setWebContentsDebuggingEnabled(true)
        webview.webViewClient = object : WebViewClient() {

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                invalidateOptionsMenu()
            }

            override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
                super.onReceivedError(view, request, error)
                progressBar!!.visibility = View.GONE
                invalidateOptionsMenu()
            }
        }
        webview.clearCache(true)
        webview.clearHistory()
        webview.settings.javaScriptEnabled = true
        webview.settings.setSupportZoom(true)
        webview.settings.builtInZoomControls = true

        webview.isHorizontalScrollBarEnabled = true

        registerForContextMenu(webview)
        webview.loadUrl(url)
    }

    override fun onCreateContextMenu(contextMenu: ContextMenu, view: View, contextMenuInfo: ContextMenu.ContextMenuInfo?) {
        super.onCreateContextMenu(contextMenu, view, contextMenuInfo)

        val webViewHitTestResult = webview.getHitTestResult()

        if (webViewHitTestResult.getType() == WebView.HitTestResult.IMAGE_TYPE
                || webViewHitTestResult.getType() == WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE) {

            contextMenu.add(0, 1, 0, "Download image")
                    .setOnMenuItemClickListener {
                        val imageUrl = webViewHitTestResult.getExtra()

                        try {
                            if (imageUrl.startsWith("data")) {
                                downloadBase64Image(imageUrl)
                            } else {
                                val request = DownloadManager.Request(Uri.parse(imageUrl))
                                request.allowScanningByMediaScanner()
                                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                                request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, imageUrl.split("/").last())
                                val downloadManager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                                downloadManager.enqueue(request)
                                Toast.makeText(applicationContext, "Image downloaded successfully.", Toast.LENGTH_LONG).show()
                            }
                        } catch (e: Throwable) {
                            Toast.makeText(applicationContext, "something went wrong...", Toast.LENGTH_SHORT).show()
                        }
                        false
                    }
        }
    }

    private fun downloadBase64Image(imageUrl: String) {
        val type = Regex("data:image/([^;]+).*").find(imageUrl)?.groups?.get(1)?.value

        if (type == null || !arrayOf("png", "jpg", "jpeg", "webp").contains(type)) {
            return
        }
        val compressor = when (type) {
            "png" -> Bitmap.CompressFormat.PNG
            "webp" -> Bitmap.CompressFormat.WEBP
            else -> Bitmap.CompressFormat.JPEG
        }

        compressor?.let {
            val bytes = android.util.Base64.decode(imageUrl.split(",").last(), android.util.Base64.DEFAULT)
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            val externalStorage = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            File(externalStorage, "${System.currentTimeMillis()}.${type}").outputStream().use {
                if(bitmap.compress(compressor, 100, it))
                    Toast.makeText(applicationContext, "Success", Toast.LENGTH_SHORT).show()
            }
        }

    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_webview, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        setMenuItemState(menu.findItem(R.id.action_back), webview.canGoBack())
        setMenuItemState(menu.findItem(R.id.action_forward), webview.canGoForward())
        return true
    }

    private fun setMenuItemState(item: MenuItem, enabled: Boolean) {
        item.isEnabled = enabled
        item.icon.alpha = if (enabled) 255 else 125
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> finish()
            R.id.action_refresh -> webview!!.loadUrl(webview!!.url)
            R.id.action_open_in_browser -> startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(webview.url)))
            R.id.action_back -> webview.goBack()
            R.id.action_forward -> webview.goForward()
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
        if (webview.canGoBack()) {
            webview.goBack()
        } else {
            super.onBackPressed()
        }
    }

}