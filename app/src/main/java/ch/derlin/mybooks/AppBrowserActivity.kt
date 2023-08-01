package ch.derlin.mybooks


import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.ContextMenu
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.webkit.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.menu.MenuBuilder
import ch.derlin.mybooks.goodreads.GoodReadsMeta
import ch.derlin.mybooks.goodreads.GoodReadsParser
import ch.derlin.mybooks.helpers.ImageDownloadManager.downloadImage
import com.google.gson.Gson
import kotlinx.android.synthetic.main.activity_goodreads_search.*
import kotlinx.android.synthetic.main.activity_webview.*
import kotlinx.android.synthetic.main.activity_webview.toolbar


class AppBrowserActivity : AppCompatActivity() {

    companion object {
        const val BUNDLE_URL = "url"
        const val BUNDLE_IS_GOODREADS_SEARCH = "grIsSearch"
        const val BUNDLE_GR_META = "result_meta"
    }

    // update the progressbar when the browser is working, i.e. loading a page
    private var working: Boolean
        get() = progressBar.visibility == View.VISIBLE
        set(value) {
            progressBar.visibility = if (value) View.VISIBLE else View.INVISIBLE
        }

    // keep track of the last link/image/phone (i.e. <a href>) link pressed
    // needed because the link is detected in shouldOverrideUrlLoading but the context menu
    // is shown in onCreateContextMenu... So we can't pass the info as a parameter
    private var _hit: Pair<Int, String>? = null
    private val lastHit: Pair<Int, String>
        get() {
            val ret = _hit ?: Pair(webview.hitTestResult.type, webview.hitTestResult.extra!!)
            _hit = null
            return ret
        }

    private var isGoodReadsSearch = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_webview)

        setSupportActionBar(toolbar)
        supportActionBar?.let { actionBar ->
            // remove title, but show a cross to close the webview
            actionBar.title = ""
            actionBar.setDisplayHomeAsUpEnabled(true)
            actionBar.setHomeAsUpIndicator(R.drawable.ic_close)
        }

        initWebview()
        registerForContextMenu(webview) // to show "download", "open in..." on long-press


        isGoodReadsSearch = intent.extras?.getBoolean(BUNDLE_IS_GOODREADS_SEARCH) == true
        // TODO: what if no url is provided ?
        // right now, just load the google search screen
        webview.loadUrl(intent.getStringExtra(BUNDLE_URL) ?: "https://google.com")

    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun initWebview() {
        webview.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView, progress: Int) {
                // show loading page progress
                working = true
                progressBar.progress = progress
                if (progress == 100) working = false
            }
        }

        WebView.setWebContentsDebuggingEnabled(true) // TODO
        webview.webViewClient = object : WebViewClient() {

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                invalidateOptionsMenu() // update back/forward button states in the actionbar
            }

            override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
                super.onReceivedError(view, request, error)
                // update back/forward button states in the actionbar
                // and hide progressbar whatever happened
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
        // don't keep history
        webview.clearCache(true)
        webview.clearHistory()

        webview.settings.javaScriptEnabled = true
    }

    override fun onCreateContextMenu(contextMenu: ContextMenu, view: View, contextMenuInfo: ContextMenu.ContextMenuInfo?) {
        super.onCreateContextMenu(contextMenu, view, contextMenuInfo)
        val hit = lastHit

        if (hit.first != WebView.HitTestResult.UNKNOWN_TYPE &&
            hit.first != WebView.HitTestResult.EDIT_TEXT_TYPE &&
            hit.first != WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE
        ) {
            // something like a link or a part of text --> share
            contextMenu.add(0, 1, 0, getString(R.string.share))
                .setOnMenuItemClickListener {
                    shareLink(hit.second)
                    true
                }
        }

        if (hit.first == WebView.HitTestResult.IMAGE_TYPE
            || hit.first == WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE
        ) {

            // try to download the image under the click
            contextMenu.add(0, 1, 0, getString(R.string.download_image))
                .setOnMenuItemClickListener {
                    val imageUrl = hit.second
                    downloadImage(imageUrl)
                }

        }
    }

    @SuppressLint("RestrictedApi")
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_webview, menu)
        // show the icons also in the action overflow (trick)
        (menu as? MenuBuilder)?.setOptionalIconsVisible(true)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        // update the back/forward buttons: enabled only if the action is actually possible
        setMenuItemState(menu.findItem(R.id.action_back), webview.canGoBack())
        setMenuItemState(menu.findItem(R.id.action_forward), webview.canGoForward())
        menu.findItem(R.id.action_parse_goodreads).let {
            if (isGoodReadsSearch) setMenuItemState(it, isGoodReadsDetailsPage())
            else it.isVisible = false
        }
        return true
    }

    private fun setMenuItemState(item: MenuItem, enabled: Boolean) {
        // make the icon a bit transparent if disabled
        // easier than creating a button_state with xml
        item.isEnabled = enabled
        item.icon.alpha = if (enabled) 255 else 125
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> finish()
            R.id.action_refresh -> webview.url?.let { webview!!.loadUrl(it) }
            R.id.action_open_in_browser -> startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(webview.url)))
            R.id.action_back -> webview.goBack()
            R.id.action_forward -> webview.goForward()
            R.id.action_share -> webview.url?.let { shareLink(it) }
            R.id.action_parse_goodreads -> parsePage()
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
        // go back in the browser on back button pressed
        // return to the book list view only if browser history is empty
        if (webview.canGoBack()) {
            webview.goBack()
        } else {
            super.onBackPressed()
        }
    }


    private fun shareLink(link: String) {
        val shareIntent = Intent(Intent.ACTION_SEND)
        shareIntent.type = "text/plain"
        shareIntent.putExtra(Intent.EXTRA_TEXT, link)
        startActivity(Intent.createChooser(shareIntent, getString(R.string.chooser_title_share_link)))
    }

    private fun parsePage() {
        if (!isGoodReadsSearch) return
        if (!isGoodReadsDetailsPage()) return

        webview.evaluateJavascript("(function() { return document.documentElement.outerHTML; })();") {
            val html = Gson().fromJson(it, String::class.java)
            val meta = GoodReadsParser.parse(requireNotNull(webview.url), html)
            if (!meta.isValid()) {
                someMetaMissingDialog(meta, "Title and/or author could not be parsed.")
            } else if (meta.isbn == null) {
                someMetaMissingDialog(meta, "The ISBN was not found. Try to expand the book details section (below the description).")
            } else {
                finishWithMeta(meta)
            }
        }
    }

    private fun someMetaMissingDialog(meta: GoodReadsMeta, message: String) {
        AlertDialog.Builder(this)
            .apply {
                setMessage(message)
                setPositiveButton(R.string.continue_) { _, _ -> finishWithMeta(meta) }
                setNegativeButton(R.string.cancel) { _, _ -> }
            }.create().show()
    }

    private fun isGoodReadsDetailsPage() =
        webview.url?.contains("goodreads.com/book/show") == true

    private fun finishWithMeta(meta: GoodReadsMeta) {
        with(Intent()) {
            putExtra(BUNDLE_GR_META, meta)
            setResult(Activity.RESULT_OK, this)
        }
        finish()
    }
}