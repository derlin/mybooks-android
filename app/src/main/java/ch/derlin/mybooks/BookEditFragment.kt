package ch.derlin.mybooks

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.fragment.app.Fragment
import ch.derlin.mybooks.goodreads.GoodReadsMeta
import ch.derlin.mybooks.goodreads.GoodReadsUrl
import ch.derlin.mybooks.helpers.MiscUtils.afterTextChanged
import ch.derlin.mybooks.helpers.MiscUtils.capitalize
import ch.derlin.mybooks.helpers.MiscUtils.rootView
import ch.derlin.mybooks.helpers.MiscUtils.textTrimmed
import ch.derlin.mybooks.persistence.PersistenceManager
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_book_detail.*
import kotlinx.android.synthetic.main.book_edit.*
import nl.komponents.kovenant.ui.failUi
import nl.komponents.kovenant.ui.successUi
import timber.log.Timber
import java.time.format.DateTimeFormatter.ISO_LOCAL_DATE


/**
 * A fragment representing a single Book detail screen.
 * This fragment is either contained in a [BookListActivity]
 * in two-pane mode (on tablets) or a [BookDetailActivity]
 * on handsets.
 */
class BookEditFragment : Fragment() {
    /**
     * The dummy content this fragment is presenting.
     */
    private var mItem: Book? = null
    private lateinit var manager: PersistenceManager

    private var working: Boolean
        get() = progressBar.visibility == View.VISIBLE
        set(value) {
            progressBar.visibility = if (value) View.VISIBLE else View.INVISIBLE
        }

    val searchGoodreads = registerForActivityResult(StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.let { loadGoodReadsResult(it) }
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        if (arguments?.containsKey(BookDetailActivity.BUNDLE_BOOK_KEY) == true) {
            mItem = arguments?.getParcelable(BookDetailActivity.BUNDLE_BOOK_KEY, Book::class.java)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        inflater.inflate(R.layout.book_edit, container, false)


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        working = false
        manager = PersistenceManager.instance

        // Show the dummy content as text in a TextView.
        mItem?.let {
            edit_title.setText(it.title)
            edit_author.setText(it.author)
            edit_date.setText(it.date)
            edit_notes.setText(it.notes)
        }


        mItem?.metas?.let {
            edit_pubdate.setText(it.pubDate)
            edit_pages.setText(it.pages?.toString())
            edit_isbn.setText(it.isbn)
            edit_gr_id.setText(it.grId)
        }

        button_edit_save.isEnabled = mItem?.title?.isNotBlank() ?: false

        edit_title.afterTextChanged { newName ->
            (activity as? BookDetailActivity)?.fab?.isEnabled = newName.isNotBlank()
            button_edit_save.isEnabled = newName.isNotBlank()
        }

        // save and cancel buttons
        button_edit_save.setOnClickListener { saveBook() }
        button_edit_cancel.setOnClickListener { activity?.onBackPressedDispatcher?.onBackPressed() }

        (activity as? BookDetailActivity)?.let {
            it.updateTitle(
                if (mItem != null) getString(R.string.title_edit_existing_book).format(mItem?.title)
                else getString(R.string.title_edit_new_book)
            )
            it.fab.setImageResource(R.drawable.ic_save)
            it.fab.setOnClickListener { saveBook() }
        }

        // autocomplete for authors
        manager.books?.let {
            edit_author.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, it.getAuthors()))
        }

        // date
        if (mItem == null) {
            // new item --> set a date
            edit_date.setText(Book.readNow)
        }
        edit_date.setOnFocusChangeListener { _, focus ->
            if (!focus) {
                edit_date.setText(Book.standardizedReadOn(edit_date.text.toString()))
            }
        }
        edit_date.addTextChangedListener(object : TextWatcher {
            var len = 0
            override fun afterTextChanged(editable: Editable?) {
                val date = editable.toString()
                if (date.matches("^\\d{4}(-\\d{2})?$".toRegex()) && date.length > len) {
                    edit_date.append("-")
                }
            }

            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                len = p0?.length ?: 0
            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
        })

        button_search_goodreads.setOnClickListener { searchGoodReads() }
    }

    private fun searchGoodReads() {
        with(Intent(requireContext(), AppBrowserActivity::class.java)) {
            val grId = edit_gr_id.textTrimmed()
            val url = if (grId.isNotBlank()) GoodReadsUrl.forBookId(grId) else {
                // author search sucks on GoodReads
                GoodReadsUrl.queryFor(edit_title.textTrimmed(), null)
            }
            putExtra(AppBrowserActivity.BUNDLE_URL, url)
            putExtra(AppBrowserActivity.BUNDLE_IS_GOODREADS_SEARCH, true)
            searchGoodreads.launch(this)
        }
    }

    private fun loadGoodReadsResult(data: Intent) {
        data.extras?.getSerializable(AppBrowserActivity.BUNDLE_GR_META, GoodReadsMeta::class.java)?.let { meta ->
            Timber.d("Received metadata results from GoodReadsActivity $meta")
            meta.title?.let { edit_title.setText(it.capitalizeWords()) }
            meta.authors?.let { edit_author.setText(it.joinToString(" & ")) }
            meta.pubDate?.let { edit_pubdate.setText(ISO_LOCAL_DATE.format(it)) }
            meta.pages?.let { edit_pages.setText(it.toString()) }
            meta.isbn?.let { edit_isbn.setText(it) }
            meta.id?.let { edit_gr_id.setText(it) }
        }

    }

    private fun saveBook() {

        val newBook = getBook()
        val books = requireNotNull(manager.books)

        // check that something has indeed changed
        if (mItem != null && newBook == mItem) {
            Toast.makeText(activity, getString(R.string.nothing_to_save), Toast.LENGTH_SHORT).show()
            return
        }

        // ensure there are no duplicate names in the account list
        if (newBook.normalizedKey != mItem?.normalizedKey && books.containsKey(newBook.normalizedKey)) {
            Toast.makeText(activity, getString(R.string.title_exists), Toast.LENGTH_LONG).show()
            return
        }

        // ok, now it become critical
        if (working) return
        working = true

        mItem?.let {
            // remove old book
            books.remove(it.normalizedKey)
        }
        books[newBook.normalizedKey] = newBook


        // try save
        manager.persist().successUi {
            // saved ok, end the edit activity
            Toast.makeText(activity, getString(R.string.saved), Toast.LENGTH_SHORT).show()
            (activity as? BookDetailActivity)?.setUpdatedBook(newBook)
            (activity as? BookListActivity)?.notifyBookUpdate(newBook)

        } failUi {
            // failed ... oops
            working = false
            // undo !
            undo(newBook)
            // show error
            activity?.let {
                Snackbar.make(it.rootView(), "${getString(R.string.error)} $it", Snackbar.LENGTH_LONG).show()
            }
        }
    }

    private fun undo(newAccount: Book) {
        requireNotNull(manager.books).let { books ->
            mItem?.let { books[it.normalizedKey] = it }
            books.remove(newAccount.normalizedKey)
        }
    }

    private fun getBook(): Book {
        return Book(
            title = edit_title.textTrimmed(),
            author = edit_author.textTrimmed(),
            date = Book.standardizedReadOn(edit_date.textTrimmed()),
            notes = edit_notes.textTrimmed(),
            metas = BookMeta(
                grId = edit_gr_id.textOrNull(),
                pubDate = edit_pubdate.textOrNull(),
                pages = edit_pages.textOrNull()?.toInt(),
                isbn = edit_isbn.textOrNull()
            ).takeUnless { it.isEmpty() })
    }

    private fun EditText.textOrNull() = textTrimmed().let { it.ifBlank { null } }

    private fun String.capitalizeWords(): String = split(" ").joinToString(" ") { it.capitalize() }
}
