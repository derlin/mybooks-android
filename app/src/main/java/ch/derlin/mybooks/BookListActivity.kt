package ch.derlin.mybooks


import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.support.design.widget.BottomSheetBehavior
import android.support.design.widget.BottomSheetDialog
import android.support.design.widget.Snackbar
import android.support.v4.app.Fragment
import android.support.v4.widget.NestedScrollView
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.SearchView
import android.support.v7.widget.helper.ItemTouchHelper
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import ch.derlin.changelog.Changelog
import ch.derlin.changelog.Changelog.getAppVersion
import ch.derlin.mybooks.helpers.MiscUtils.showIntro
import ch.derlin.mybooks.helpers.NetworkStatus
import ch.derlin.mybooks.helpers.Preferences
import ch.derlin.mybooks.helpers.SwipeToDeleteCallback

import ch.derlin.mybooks.persistence.PersistenceManager
import ch.derlin.mybooks.helpers.ThemeHelper.applyTheme
import ch.derlin.mybooks.persistence.DbxManager
import ch.derlin.mybooks.persistence.PersistenceManager.Companion.shareAppFile
import kotlinx.android.synthetic.main.activity_book_list.*
import kotlinx.android.synthetic.main.book_list.*
import nl.komponents.kovenant.ui.alwaysUi
import nl.komponents.kovenant.ui.failUi
import nl.komponents.kovenant.ui.successUi
import timber.log.Timber
import java.util.*

/**
 * An activity representing a list of Pings. This activity
 * has different presentations for handset and tablet-size devices. On
 * handsets, the activity presents a list of items, which when touched,
 * lead to a [BookDetailActivity] representing
 * item details. On tablets, the activity presents the list of items and
 * item details side-by-side using two vertical panes.
 */
class BookListActivity : AppCompatActivity() {

    private val LINK_DROPBOX_REQUEST_CODE = 2006
    /**
     * Whether or not the activity is in two-pane mode, i.e. running on a tablet
     * device.
     */
    private var mTwoPane: Boolean = false

    private var selectedBook: Book? = null
    private var mTwoPaneCurrentFragment: Fragment? = null
    private lateinit var searchView: SearchView

    private lateinit var adapter: BookListAdapter
    private lateinit var manager: PersistenceManager

    private var working: Boolean
        get() = progressBar.visibility == View.VISIBLE
        set(value) {
            progressBar.visibility = if (value) View.VISIBLE else View.GONE
        }

    private lateinit var bottomSheetBehavior: BottomSheetBehavior<NestedScrollView>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applyTheme()
        setContentView(R.layout.activity_book_list)

        setSupportActionBar(toolbar)
        toolbar.title = title

        fab.setImageResource(R.drawable.ic_add)
        fab.setOnClickListener { _ ->
            if (NetworkStatus.isInternetAvailable(this))
                showDetails(null, BookDetailActivity.OPERATION_NEW)
            else Snackbar.make(fab, getString(R.string.no_internet_connection), Snackbar.LENGTH_LONG).show()
        }

        mTwoPane = book_detail_container != null

        manager = PersistenceManager.instance
        if (manager.isInitialised) {
            setupRecyclerView()
        } else {
            fab.visibility = View.GONE
            loadBooks()
        }

        bottomSheetBehavior = BottomSheetBehavior.from(bottom_sheet)
        bottomSheetBehavior.setPeekHeight(300)
        bottomSheetBehavior.isHideable = true
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN

        val prefs = Preferences()
        if (!prefs.introDone) {
            showIntro()
        } else {
            displayChangelog()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        searchView = (menu!!.findItem(R.id.action_search).actionView as SearchView)
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = true

            override fun onQueryTextChange(newText: String?): Boolean {
                adapter.filter(newText)
                return true
            }
        })

        val prefs = Preferences(this)

        val sort = prefs.sortOrder
        menu.findItem(sort).isChecked = true

        val theme = prefs.currentTheme
        menu.findItem(theme).isChecked = true

        val linkedToDbx = prefs.dbxAccessToken != null
        menu.findItem(R.id.action_dropbox_unlink).isVisible = linkedToDbx
        menu.findItem(R.id.action_dropbox_link).isVisible = !linkedToDbx

        return true
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            DETAIL_ACTIVITY_REQUEST_CODE ->
                if (data?.getBooleanExtra(BookDetailActivity.RETURN_MODIFIED, false) ?: false) {
                    // update the list in case of modification
                    selectedBook = data!!.getParcelableExtra(BookDetailActivity.BUNDLE_BOOK_KEY)
                    notifyBookUpdate(selectedBook!!)
                }
            LINK_DROPBOX_REQUEST_CODE -> {
                if (resultCode == Activity.RESULT_OK) restart()
            }
            else -> super.onActivityResult(requestCode, resultCode, data)
        }

    }

    override fun onOptionsItemSelected(iitem: MenuItem?): Boolean {
        iitem?.let { item ->

            when (item.groupId) {
                R.id.group_menu_sort -> {
                    Preferences(this).sortOrder = item.itemId
                    adapter.comparator = getSortOrder(item.itemId)
                    item.isChecked = true
                    return true
                }
                R.id.group_menu_theme -> {
                    Preferences(this).currentTheme = item.itemId
                    restart()
                    return true
                }
            }

            when (item.itemId) {
                R.id.action_dropbox_link ->
                    startActivityForResult(
                            Intent(this, DbxLoginActivity::class.java),
                            LINK_DROPBOX_REQUEST_CODE)
                R.id.action_dropbox_unlink -> {
                    (manager as? DbxManager)?.unbind()?.successUi {
                        Snackbar.make(fab,
                                getString(R.string.dropbox_unlink_success), Snackbar.LENGTH_SHORT).show()
                        PersistenceManager.invalidate()
                        restart()
                    }?.failUi {
                        Snackbar.make(fab, "${getString(R.string.error)}: ${it}", Snackbar.LENGTH_LONG).show()
                    }
                }
                R.id.action_export_file -> shareAppFile()
                R.id.action_changelog -> Changelog.createDialog(this).show()
                R.id.action_intro -> showIntro()
                else -> super.onOptionsItemSelected(iitem)
            }
        }
        return true
    }

    private fun getSortOrder(itemId: Int): Comparator<Book> {
        when (itemId) {
            R.id.submenu_sort_title_asc -> return Book.nameComparatorAsc
            R.id.submenu_sort_title_desc -> return Book.nameComparatorDesc
            R.id.submenu_sort_year_asc -> return Book.modifiedComparatorAsc
            R.id.submenu_sort_year_desc -> return Book.modifiedComparatorDesc
            else -> return Book.nameComparatorAsc
        }
    }

    override fun onBackPressed() {
        if (mTwoPane && mTwoPaneCurrentFragment is BookEditFragment) {
            if (selectedBook != null) {
                showDetails(selectedBook!!, BookDetailActivity.OPERATION_SHOW)
            } else {
                supportFragmentManager.beginTransaction().remove(mTwoPaneCurrentFragment).commit()
            }
        } else {
            if (bottomSheetBehavior.state != BottomSheetBehavior.STATE_HIDDEN) {
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
            } else {
                super.onBackPressed()
            }
        }
    }

    private fun loadBooks() {
        val showSnackbarFunc = { msg: String ->
            Snackbar.make(findViewById(android.R.id.content), msg, Snackbar.LENGTH_INDEFINITE)
                    .setAction(getString(R.string.retry), { _ -> loadBooks() })
                    .show()
        }
        if (!NetworkStatus.isInternetAvailable(this)) {
            // no internet, try to load local file
            if (!manager.localFileExists) {
                showSnackbarFunc(getString(R.string.no_internet_connection))
                return
            }
        }
        // internet, fetch latest rev
        working = true
        manager.fetchBooks().alwaysUi {
            working = false
        } successUi {
            setupRecyclerView()
            fab.visibility = View.VISIBLE
        } failUi {
            Timber.d(it)
            showSnackbarFunc("${getString(R.string.error)}: ${it}")
        }
    }

    private fun setupRecyclerView() {
        adapter = BookListAdapter(manager.books!!, getSortOrder(Preferences().sortOrder), countText)
        recyclerView.adapter = adapter

        adapter.onClick = { book ->
            selectedBook = book
            showBottomSheet(book)
//            sheetTitle.setText(book.title)
//            if (bottomSheetBehavior.state == BottomSheetBehavior.STATE_HIDDEN) {
//                bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
//            }
        }

        adapter.onLongClick = { book ->
            selectedBook = book
            showDetails(book, BookDetailActivity.OPERATION_EDIT)
        }

        val itemTouchHelper = ItemTouchHelper(createSwipeHandler())
        itemTouchHelper.attachToRecyclerView(recyclerView)
    }

    fun showDetails(item: Book?, operation: String): Boolean {
        if (mTwoPane) {
            val arguments = Bundle()
            arguments.putParcelable(BookDetailActivity.BUNDLE_BOOK_KEY, item)
            mTwoPaneCurrentFragment = if (operation == BookDetailActivity.OPERATION_SHOW)
                BookDetailFragment() else BookEditFragment()
            mTwoPaneCurrentFragment!!.arguments = arguments
            supportFragmentManager.beginTransaction()
                    .replace(R.id.frameLayout, mTwoPaneCurrentFragment)
                    .commit()
        } else {
            val context = this
            val intent = Intent(context, BookDetailActivity::class.java)
            intent.putExtra(BookDetailActivity.BUNDLE_OPERATION_KEY, operation)
            intent.putExtra(BookDetailActivity.BUNDLE_BOOK_KEY, item)
            context.startActivityForResult(intent, DETAIL_ACTIVITY_REQUEST_CODE)
        }
        return true
    }

    // only called in mTwoPane mode
    fun notifyBookUpdate(item: Book) {
        selectedBook = item
        adapter.replace(selectedBook!!, item)
        val idx = adapter.positionOf(item)
        if (idx >= 0) recyclerView.scrollToPosition(idx)

        if (mTwoPane) showDetails(item, BookDetailActivity.OPERATION_SHOW)
    }

    private var bottomSheetDialog: BottomSheetDialog? = null

    private fun showBottomSheet(item: Book) {

        selectedBook = item
        //hideKeyboard()
        if (searchView.hasFocus()) searchView.clearFocus()

        if (mTwoPane) {
            showDetails(selectedBook!!, BookDetailActivity.OPERATION_SHOW)
            return
        }


        bottomSheetDialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.book_list_bottomsheet, null)

        view.findViewById<TextView>(R.id.sheetTitle).text = item.title
        if (item.notes.isNotBlank()) {
            view.findViewById<TextView>(R.id.notes).text = item.notes
        }

        view.findViewById<ImageButton>(R.id.editButton)
                .setOnClickListener { _ ->
                    if (NetworkStatus.isInternetAvailable(this))
                        showDetails(selectedBook!!, BookDetailActivity.OPERATION_EDIT)
                    else Snackbar.make(fab, getString(R.string.no_internet_connection), Snackbar.LENGTH_LONG).show()
                }

        view.findViewById<ImageButton>(R.id.searchButton)
                .setOnClickListener { _ -> searchGoogle(selectedBook!!) }

        bottomSheetDialog!!.setContentView(view)
        bottomSheetDialog!!.show()
    }

    private fun searchGoogle(book: Book) {
        // see https://stackoverflow.com/a/4800679/2667536
        val intent = Intent(this, AppBrowserActivity::class.java)
        intent.putExtra("url", googleUrlFor(book.toSearchQuery()))
        startActivity(intent)
    }

    fun createSwipeHandler() =
            object : SwipeToDeleteCallback(this, backgroundColor = getColor(R.color.paleGreen)) {
                override fun onSwiped(viewHolder: RecyclerView.ViewHolder?, direction: Int) {
                    val item = adapter.removeAt(viewHolder!!.adapterPosition)
                    working = true

                    manager.persist()
                            .alwaysUi { working = false }
                            .successUi {
                                if (mTwoPane && selectedBook == item)
                                    supportFragmentManager.beginTransaction()
                                            .remove(mTwoPaneCurrentFragment!!)
                                            .commit()

                                Timber.d("removed book: %s", item)
                                Snackbar.make(fab, getString(R.string.book_deleted), Snackbar.LENGTH_LONG)
                                        .setAction(getString(R.string.undo), { _ ->
                                            working = true
                                            adapter.add(item)
                                            manager.persist()
                                                    .alwaysUi { working = false }
                                                    .failUi {
                                                        Toast.makeText(this@BookListActivity,
                                                                getString(R.string.undo_failed),
                                                                Toast.LENGTH_LONG).show()
                                                    }
                                        })
                                        .show()
                            }
                            .failUi {
                                // undo swipe !
                                adapter.add(item)
                                Toast.makeText(this@BookListActivity,
                                        getString(R.string.save_failed), Toast.LENGTH_LONG).show()

                            }
                }
            }

    private fun restart() {
        finish()
        startActivity(intent)
    }

    private fun displayChangelog() {
        val version = getAppVersion()
        val prefs = Preferences()
        if (prefs.versionCode < version.first) {
            prefs.versionCode = version.first
            val dialog = Changelog.createDialog(this,
                    versionCode = version.first)
            dialog.show()
        }
    }

    companion object {
        val DETAIL_ACTIVITY_REQUEST_CODE = 1984

        fun googleUrlFor(queryParams: String) =
                "http://www.google.com/search?lr=lang_${Locale.getDefault().language}&q=${queryParams}&pws=0&gl=us&gws_rd=cr"
    }

}
