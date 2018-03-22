package ch.derlin.mybooks


import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.view.ContextMenu
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.webkit.*
import ch.derlin.mybooks.helpers.ImageDownloadManager.downloadImage
import kotlinx.android.synthetic.main.activity_webview.*


class AppBrowserActivity : AppCompatActivity() {

    private var working: Boolean
        get() = progressBar.visibility == View.VISIBLE
        set(value) {
            progressBar.visibility = if (value) View.VISIBLE else View.INVISIBLE
        }

    private lateinit var url: String
    private var writeExternalStorageIsGranted = false

    // keep track of the last link/image/phone (i.e. <a href>) link pressed
    // needed because the link is detected in shouldOverrideUrlLoading but the context menu
    // is shown in onCreateContextMenu... So we can't pass the info as a parameter
    private var _hit: Pair<Int, String>? = null
    private val lastHit: Pair<Int, String>
        get() {
            val ret = _hit ?: Pair(webview.hitTestResult.type, webview.hitTestResult.extra)
            _hit = null
            return ret
        }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_webview)

        setSupportActionBar(toolbar)
        supportActionBar?.let { actionBar ->
            actionBar.setTitle("")
            actionBar.setDisplayHomeAsUpEnabled(true)
        }

        url = intent.getStringExtra("url") //?: "https://www.google.com?q=1984"

        checkPermissions()
        initWebview()
        registerForContextMenu(webview)
        webview.loadUrl(url)
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun initWebview() {
        webview.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView, progress: Int) {
                // show loading page progress
                working = true
                progressBar.setProgress(progress)
                if (progress == 100) working = false
            }
        }

        WebView.setWebContentsDebuggingEnabled(true)
        webview.webViewClient = object : WebViewClient() {

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                invalidateOptionsMenu()
            }

            override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
                super.onReceivedError(view, request, error)
                progressBar!!.visibility = View.GONE
                invalidateOptionsMenu()
            }

            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                request?.let {
                    val url = it.url.toString()
                    if (url.startsWith("http")) return super.shouldOverrideUrlLoading(view, request)
                    val type =
                            if (url.startsWith("tel:")) WebView.HitTestResult.PHONE_TYPE
                            else if (url.startsWith("mailto:")) WebView.HitTestResult.EMAIL_TYPE
                            else if (url.startsWith("data:image")) WebView.HitTestResult.IMAGE_TYPE
                            else WebView.HitTestResult.UNKNOWN_TYPE
                    _hit = Pair(type, url)
                    openContextMenu(view)
                    return true
                }

                return super.shouldOverrideUrlLoading(view, request)
            }
        }
        webview.clearCache(true)
        webview.clearHistory()

        webview.settings.javaScriptEnabled = true
        webview.settings.setSupportZoom(true)
        webview.settings.builtInZoomControls = true
        webview.isHorizontalScrollBarEnabled = true
    }

    private fun checkPermissions() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(this,
                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 1)
        } else {
            writeExternalStorageIsGranted = true
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        writeExternalStorageIsGranted = grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED
    }

    override fun onCreateContextMenu(contextMenu: ContextMenu, view: View, contextMenuInfo: ContextMenu.ContextMenuInfo?) {
        super.onCreateContextMenu(contextMenu, view, contextMenuInfo)
        val hit = lastHit

        if (hit.first != WebView.HitTestResult.UNKNOWN_TYPE &&
                hit.first != WebView.HitTestResult.EDIT_TEXT_TYPE) {
            contextMenu.add(0, 1, 0, "Share")
                    .setOnMenuItemClickListener {
                        val shareIntent = Intent(Intent.ACTION_SEND)
                        shareIntent.type = "text/plain"
                        shareIntent.putExtra(Intent.EXTRA_TEXT, hit.second)
                        startActivity(Intent.createChooser(shareIntent, "Share link using"))
                        true
                    }
        }

        // try to download the image under the click (if it is actually an image)
        if (hit.first == WebView.HitTestResult.IMAGE_TYPE
                || hit.first == WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE) {

            if (writeExternalStorageIsGranted) {
                contextMenu.add(0, 1, 0, "Download image")
                        .setOnMenuItemClickListener {
                            val imageUrl = hit.second
                            downloadImage(imageUrl)
                        }
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