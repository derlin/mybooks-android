package ch.derlin.mybooks

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.appcompat.app.AppCompatActivity
import android.view.MenuItem
import android.widget.Toast
import ch.derlin.mybooks.helpers.NetworkStatus
import ch.derlin.mybooks.helpers.ThemeHelper.applyTheme
import kotlinx.android.synthetic.main.activity_book_detail.*

/**
 * An activity representing a single Book detail screen. This
 * activity is only used on narrow width devices. On tablet-size devices,
 * item details are presented side-by-side with a list of items
 * in a [BookListActivity].
 */
class BookDetailActivity : AppCompatActivity() {

    var selectedBook: Book? = null
    private var selectedOperation: String? = null
    private var shouldGoBackToEditView = false
    private var accountModified = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applyTheme()
        setContentView(R.layout.activity_book_detail)
        setSupportActionBar(toolbar)

        fab.setOnClickListener { _ -> editBook() }

        // Show the Up button in the action bar.
        val actionBar = supportActionBar
        actionBar?.setDisplayHomeAsUpEnabled(true)

        val extras = intent.extras
        if (extras != null) {
            selectedOperation = extras.getString(BUNDLE_OPERATION_KEY)

            if (selectedOperation == OPERATION_EDIT || selectedOperation == OPERATION_SHOW) {
                selectedBook = intent.getParcelableExtra<Book>(BUNDLE_BOOK_KEY)
            }
        }

        app_bar.setExpanded(selectedOperation == OPERATION_SHOW, false)
        if (savedInstanceState == null) {
            // Create the detail fragment and add it to the activity
            // using a fragment transaction only the first time
            switchFragment(if (selectedOperation == OPERATION_SHOW)
                BookDetailFragment() else BookEditFragment())
        }
    }

    fun updateTitle(title: String) {
        toolbarLayout.title = title
    }

    fun editBook(): Boolean {
        return if (NetworkStatus.isInternetAvailable()) {
            switchFragment(BookEditFragment())
            shouldGoBackToEditView = true
            app_bar.setExpanded(false, true)
            true
        } else {
            Toast.makeText(this, getString(R.string.no_internet_connection), Toast.LENGTH_SHORT).show()
            false
        }
    }

    fun setUpdatedBook(book: Book) {
        selectedBook = book
        accountModified = true
        backToDetailsView()
    }

    private fun switchFragment(f: Fragment) {
        var arguments: Bundle? = null

        if (selectedBook != null) {
            arguments = Bundle()
            arguments.putParcelable(BUNDLE_BOOK_KEY, selectedBook)
        }

        f.arguments = arguments
        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.book_detail_container, f)
        transaction.commit()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            this.onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun backToDetailsView() {
        switchFragment(BookDetailFragment())
        app_bar.setExpanded(true, true)
        shouldGoBackToEditView = false
    }


    override fun onBackPressed() {
        if (shouldGoBackToEditView) {
            backToDetailsView()
        } else {
            val returnIntent = Intent()
            returnIntent.putExtra("modified", accountModified)
            returnIntent.putExtra(BUNDLE_BOOK_KEY, selectedBook)
            setResult(Activity.RESULT_OK, returnIntent)
            finish()
        }
    }


    companion object {
        const val BUNDLE_BOOK_KEY = "account"
        const val BUNDLE_OPERATION_KEY = "operation"
        const val OPERATION_SHOW = "show"
        const val OPERATION_EDIT = "edit"
        const val OPERATION_NEW = "new"
        const val RETURN_MODIFIED = "modified"
    }
}
