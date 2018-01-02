package derlin.ch.mybooks


import android.content.Intent
import android.os.Bundle
import android.support.design.widget.BottomSheetBehavior
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
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_book_list.*
import kotlinx.android.synthetic.main.book_list.*
import kotlinx.android.synthetic.main.book_list_bottomsheet.*
import nl.komponents.kovenant.ui.alwaysUi
import nl.komponents.kovenant.ui.failUi
import nl.komponents.kovenant.ui.successUi
import timber.log.Timber

/**
 * An activity representing a list of Pings. This activity
 * has different presentations for handset and tablet-size devices. On
 * handsets, the activity presents a list of items, which when touched,
 * lead to a [BookDetailActivity] representing
 * item details. On tablets, the activity presents the list of items and
 * item details side-by-side using two vertical panes.
 */
class BookListActivity : AppCompatActivity() {

    /**
     * Whether or not the activity is in two-pane mode, i.e. running on a tablet
     * device.
     */
    private var mTwoPane: Boolean = false
    private var selectedBook: Book? = null
    private var mTwoPaneCurrentFragment: Fragment? = null
    private lateinit var searchView: SearchView

    private lateinit var adapter: BookListAdapter

    private var working: Boolean
        get() = progressBar.visibility == View.VISIBLE
        set(value) {
            progressBar.visibility = if (value) View.VISIBLE else View.GONE
        }

    private lateinit var bottomSheetBehavior: BottomSheetBehavior<NestedScrollView>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_book_list)

        setSupportActionBar(toolbar)
        toolbar.title = title

        fab.setImageResource(R.drawable.ic_add)
        fab.setOnClickListener { view ->
            showDetails(null, BookDetailActivity.OPERATION_NEW)
        }

        if (book_detail_container != null) {
            mTwoPane = true
        }

        loadBooks()

        bottomSheetBehavior = BottomSheetBehavior.from(bottom_sheet)
        bottomSheetBehavior.setPeekHeight(300)
        bottomSheetBehavior.isHideable = true
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
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
        val sort = Preferences(this).sortOrder
        menu.findItem(sort).isChecked = true
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        if (item?.groupId == R.id.group_menu_sort) {
            Preferences(this).sortOrder = item.itemId
            adapter.comparator = getSortOrder(item.itemId)
            item.isChecked = true
            return true
        } else {
            when (item?.itemId) {
                else -> return super.onOptionsItemSelected(item)
            }
        }
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
        if (!NetworkStatus.isInternetAvailable(this)) {
            // no internet, try to load local file
            if (!DbxManager.localFileExists) {
                Snackbar.make(findViewById(android.R.id.content),
                        "Internet is not available", Snackbar.LENGTH_INDEFINITE)
                        .setAction("retry", { _ -> loadBooks() })
                        .show()
                return
            }
        }
        // internet, fetch latest rev
        working = true
        DbxManager.fetchBooks().alwaysUi {
            working = false
        } successUi {
            setupRecyclerView()
        } failUi {
            Timber.d(it)
            Snackbar.make(findViewById(android.R.id.content),
                    "Error: ${it}", Snackbar.LENGTH_INDEFINITE)
                    .setAction("retry", { _ -> loadBooks() })
                    .show()
        }
    }

    private fun setupRecyclerView() {
        adapter = BookListAdapter(DbxManager.books!!, getSortOrder(Preferences().sortOrder), countText)
        recyclerView.adapter = adapter

        adapter.onClick = { book ->
            selectedBook = book
            sheetTitle.setText(book.title)
            if (bottomSheetBehavior.state == BottomSheetBehavior.STATE_HIDDEN) {
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
            }
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

    fun createSwipeHandler() =
            object : SwipeToDeleteCallback(this, backgroundColor = getColor(R.color.colorAccent)) {
                override fun onSwiped(viewHolder: RecyclerView.ViewHolder?, direction: Int) {
                    val item = adapter.removeAt(viewHolder!!.adapterPosition)
                    working = true

                    DbxManager.upload()
                            .alwaysUi { working = false }
                            .successUi {
                                if (mTwoPane && selectedBook == item)
                                    supportFragmentManager.beginTransaction()
                                            .remove(mTwoPaneCurrentFragment!!)
                                            .commit()

                                Timber.d("removed book: %s", item)
                                Snackbar.make(fab, "Book deleted", Snackbar.LENGTH_LONG)
                                        .setAction("undo", { _ ->
                                            working = true
                                            adapter.add(item)
                                            DbxManager.upload()
                                                    .alwaysUi { working = false }
                                                    .failUi {
                                                        Toast.makeText(this@BookListActivity,
                                                                "Failed to undo changes",
                                                                Toast.LENGTH_LONG).show()
                                                    }
                                        })
                                        .show()
                            }
                            .failUi {
                                // undo swipe !
                                adapter.add(item)
                                Toast.makeText(this@BookListActivity,
                                        "Failed to save changes", Toast.LENGTH_LONG).show()

                            }
                }
            }

    companion object {
        val DETAIL_ACTIVITY_REQUEST_CODE = 1984
    }

}
