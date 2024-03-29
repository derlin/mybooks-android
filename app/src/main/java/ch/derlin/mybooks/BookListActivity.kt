package ch.derlin.mybooks


import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import ch.derlin.changelog.Changelog
import ch.derlin.changelog.Changelog.getAppVersion
import ch.derlin.mybooks.goodreads.GoodReadsUrl
import ch.derlin.mybooks.helpers.MiscUtils.showIntro
import ch.derlin.mybooks.helpers.NetworkStatus
import ch.derlin.mybooks.helpers.Preferences
import ch.derlin.mybooks.helpers.SwipeToDeleteCallback
import ch.derlin.mybooks.helpers.ThemeHelper.applyTheme
import ch.derlin.mybooks.helpers.ThemeHelper.toResource
import ch.derlin.mybooks.helpers.ThemeHelper.toTheme
import ch.derlin.mybooks.persistence.DbxManager
import ch.derlin.mybooks.persistence.PersistenceManager
import ch.derlin.mybooks.persistence.PersistenceManager.Companion.shareAppFile
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_book_list.*
import kotlinx.android.synthetic.main.book_list.*
import nl.komponents.kovenant.ui.alwaysUi
import nl.komponents.kovenant.ui.failUi
import nl.komponents.kovenant.ui.successUi
import timber.log.Timber
import java.net.URLEncoder
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

    companion object {
        fun googleUrlFor(book: Book): String {
            val queryParams = URLEncoder.encode("${book.title} ${book.author}", "utf-8")
            return "https://www.google.com/search?lr=lang_${Locale.getDefault().language}&q=${queryParams}&pws=0&gl=us&gws_rd=cr"
        }
    }

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

    private lateinit var bottomSheetDialog: BooksBottomSheetDialog

    private val showDetails = registerForActivityResult(StartActivityForResult()) {
        if (it.resultCode == Activity.RESULT_OK) {
            val data = it.data
            if (data?.getBooleanExtra(BookDetailActivity.RETURN_MODIFIED, false) == true) {
                // update the list in case of modification
                selectedBook = data.getParcelableExtra(BookDetailActivity.BUNDLE_BOOK_KEY, Book::class.java)
                notifyBookUpdate(selectedBook!!)
            }
        }
    }

    private val linkDropbox = registerForActivityResult(StartActivityForResult()) {
        if (it.resultCode == Activity.RESULT_OK) restart()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_book_list)

        setSupportActionBar(toolbar)
        toolbar.title = title

        fab.setImageResource(R.drawable.ic_add)
        fab.setOnClickListener {
            if (PersistenceManager.instance.canEdit()) showDetails(null, BookDetailActivity.OPERATION_NEW)
            else Snackbar.make(fab, getString(R.string.no_internet_connection), Snackbar.LENGTH_LONG).show()
        }

        bottomSheetDialog = BooksBottomSheetDialog(
            showButtonCallback = { dialog, book ->
                showDetails(book, BookDetailActivity.OPERATION_SHOW)
                dialog.dismiss()
            },
            editButtonCallback = { _, book ->
                if (PersistenceManager.instance.canEdit()) showDetails(book, BookDetailActivity.OPERATION_EDIT)
                else Snackbar.make(fab, getString(R.string.no_internet_connection), Snackbar.LENGTH_LONG).show()
            },
            searchButtonCallback = { _, book -> searchGoogle(book) }
        )

        mTwoPane = book_detail_container != null

        manager = PersistenceManager.instance
        if (manager.isInitialised) {
            setupRecyclerView()
        } else {
            fab.visibility = View.GONE
            loadBooks()
        }

        if (!Preferences.introDone) {
            showIntro()
        } else {
            displayChangelog()
        }

        registerOnBackPressed()
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

        val sort = Preferences.sortOrder
        menu.findItem(sort).isChecked = true

        val theme = Preferences.currentTheme
        menu.findItem(theme.toResource()).isChecked = true

        val linkedToDbx = Preferences.dbxAccessToken != null
        menu.findItem(R.id.action_dropbox_unlink).isVisible = linkedToDbx
        menu.findItem(R.id.action_dropbox_link).isVisible = !linkedToDbx

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.groupId) {
            R.id.group_menu_sort -> {
                Preferences.sortOrder = item.itemId
                adapter.comparator = getSortOrder(item.itemId)
                item.isChecked = true
                return true
            }
            R.id.group_menu_theme -> {
                applyTheme(item.itemId.toTheme())
                item.isChecked = true
                return true
            }
        }

        when (item.itemId) {
            R.id.action_dropbox_link ->
                linkDropbox.launch(Intent(this, DbxLoginActivity::class.java))

            R.id.action_dropbox_unlink -> {
                (manager as? DbxManager)?.unbind()?.successUi {
                    Snackbar.make(fab, getString(R.string.dropbox_unlink_success), Snackbar.LENGTH_SHORT).show()
                    PersistenceManager.invalidate()
                    restart()
                }?.failUi {
                    Snackbar.make(fab, "${getString(R.string.error)}: $it", Snackbar.LENGTH_LONG).show()
                }
            }
            R.id.action_export_file -> shareAppFile()
            R.id.action_changelog -> Changelog.createDialog(this).show()
            R.id.action_intro -> showIntro()
            else -> super.onOptionsItemSelected(item)
        }

        return true
    }

    private fun getSortOrder(itemId: Int): Comparator<Book> {
        return when (itemId) {
            R.id.submenu_sort_title_asc -> Book.nameComparatorAsc
            R.id.submenu_sort_title_desc -> Book.nameComparatorDesc
            R.id.submenu_sort_year_asc -> Book.modifiedComparatorAsc
            R.id.submenu_sort_year_desc -> Book.modifiedComparatorDesc
            else -> Book.nameComparatorAsc
        }
    }

    private fun loadBooks() {
        val showSnackbarFunc = { msg: String ->
            Snackbar.make(findViewById(android.R.id.content), msg, Snackbar.LENGTH_INDEFINITE)
                .setAction(getString(R.string.retry)) { loadBooks() }
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
            showSnackbarFunc("${getString(R.string.error)}: $it")
        }
    }

    private fun setupRecyclerView() {
        adapter = BookListAdapter(requireNotNull(manager.books), getSortOrder(Preferences.sortOrder), countText)
        recyclerView.adapter = adapter

        adapter.onClick = { book ->
            selectedBook = book
            showBottomSheet(book)
        }

        adapter.onLongClick = { book ->
            selectedBook = book
            showDetails(book, BookDetailActivity.OPERATION_SHOW)
        }

        val itemTouchHelper = ItemTouchHelper(createSwipeHandler())
        itemTouchHelper.attachToRecyclerView(recyclerView)
    }

    private fun showDetails(item: Book?, operation: String) {
        if (mTwoPane) {
            val arguments = Bundle()
            arguments.putParcelable(BookDetailActivity.BUNDLE_BOOK_KEY, item)
            mTwoPaneCurrentFragment = if (operation == BookDetailActivity.OPERATION_SHOW) BookDetailFragment() else BookEditFragment()
            mTwoPaneCurrentFragment?.let { fragment ->
                fragment.arguments = arguments
                supportFragmentManager.beginTransaction().replace(R.id.book_detail_container, fragment).commit()
            }
        } else {
            val intent = Intent(this, BookDetailActivity::class.java)
            intent.putExtra(BookDetailActivity.BUNDLE_OPERATION_KEY, operation)
            intent.putExtra(BookDetailActivity.BUNDLE_BOOK_KEY, item)
            showDetails.launch(intent)
        }
    }

    private fun registerOnBackPressed() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (mTwoPane && mTwoPaneCurrentFragment is BookEditFragment) {
                    if (selectedBook != null) {
                        showDetails(selectedBook!!, BookDetailActivity.OPERATION_SHOW)
                    } else {
                        supportFragmentManager.beginTransaction().remove(mTwoPaneCurrentFragment as BookEditFragment).commit()
                    }
                } else {
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }

    // only called in mTwoPane mode
    fun notifyBookUpdate(item: Book) {
        selectedBook = item
        adapter.replace(selectedBook!!, item)
        val idx = adapter.positionOf(item)
        if (idx >= 0) recyclerView.scrollToPosition(idx)

        if (mTwoPane) showDetails(item, BookDetailActivity.OPERATION_SHOW)
    }

    private fun showBottomSheet(item: Book) {
        selectedBook = item
        if (searchView.hasFocus()) searchView.clearFocus()
        if (mTwoPane) {
            showDetails(item, BookDetailActivity.OPERATION_SHOW)
            return
        }
        bottomSheetDialog.show(this, item)
    }

    private fun searchGoogle(book: Book) {
        // see https://stackoverflow.com/a/4800679/2667536
        val intent = Intent(this, AppBrowserActivity::class.java)
        val url = book.metas?.grId?.let { GoodReadsUrl.forBookId(it) } ?: googleUrlFor(book)
        intent.putExtra(AppBrowserActivity.BUNDLE_URL, url)
        startActivity(intent)
    }

    private fun createSwipeHandler() = object : SwipeToDeleteCallback(this, backgroundColor = getColor(R.color.colorAccent)) {
        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
            val item = adapter.removeAt(viewHolder.bindingAdapterPosition)
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
                        .setAction(getString(R.string.undo)) {
                            working = true
                            adapter.add(item)
                            manager.persist()
                                .alwaysUi { working = false }
                                .failUi {
                                    Toast.makeText(
                                        this@BookListActivity, getString(R.string.undo_failed), Toast.LENGTH_LONG
                                    ).show()
                                }
                        }
                        .show()
                }
                .failUi {
                    // undo swipe !
                    adapter.add(item)
                    Toast.makeText(this@BookListActivity, getString(R.string.save_failed), Toast.LENGTH_LONG).show()
                }
        }
    }

    private fun restart() {
        finish()
        startActivity(intent)
    }

    private fun displayChangelog() {
        val version = getAppVersion()
        if (Preferences.versionCode < version.first) {
            Preferences.versionCode = version.first
            val dialog = Changelog.createDialog(this, versionCode = version.first)
            dialog.show()
        }
    }

}
