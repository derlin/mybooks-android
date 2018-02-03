package ch.derlin.mybooks

import android.content.Context
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v4.app.Fragment
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.ArrayAdapter
import android.widget.Toast
import kotlinx.android.synthetic.main.book_edit.*
import nl.komponents.kovenant.ui.failUi
import nl.komponents.kovenant.ui.successUi
import kotlinx.android.synthetic.main.activity_book_detail.*
import ch.derlin.mybooks.MiscUtils.rootView
import ch.derlin.mybooks.MiscUtils.hideKeyboard
import ch.derlin.mybooks.MiscUtils.afterTextChanged

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

    private var working: Boolean
        get() = progressBar.visibility == View.VISIBLE
        set(value) = progressBar.setVisibility(if (value) View.VISIBLE else View.INVISIBLE)

    override fun onAttach(context: Context?) {
        super.onAttach(context)

        if (arguments?.containsKey(BookDetailActivity.BUNDLE_BOOK_KEY) ?: false) {
            mItem = arguments.getParcelable(BookDetailActivity.BUNDLE_BOOK_KEY)
        }
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? =
            inflater!!.inflate(R.layout.book_edit, container, false)


    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        working = false

        // Show the dummy content as text in a TextView.
        mItem?.let {
            edit_title.setText(it.title)
            edit_author.setText(it.author)
            edit_date.setText(it.date)
            edit_notes.setText(it.notes)
        }

        button_edit_save.isEnabled = mItem?.title?.isNotBlank() ?: false

        edit_title.afterTextChanged { newName ->
            (activity as? BookDetailActivity)?.fab?.isEnabled = newName.isNotBlank()
            button_edit_save.isEnabled = newName.isNotBlank()
        }


        // see https://stackoverflow.com/a/39770984/2667536
        edit_notes.setHorizontallyScrolling(false)
        edit_notes.maxLines = 5
        edit_notes.setOnEditorActionListener { textView, actionId, keyEvent ->
            if (actionId == EditorInfo.IME_ACTION_GO) {
                activity.hideKeyboard()
                true
            } else {
                false
            }
        }

        // save and cancel buttons
        button_edit_save.setOnClickListener { saveBook() }
        button_edit_cancel.setOnClickListener { activity.onBackPressed() }

        (activity as? BookDetailActivity)?.let {
            it.updateTitle(if (mItem != null) "Editing ${mItem?.title}" else "New account")
            it.fab.setImageResource(R.drawable.ic_save)
            it.fab.setOnClickListener { _ ->
                saveBook()
            }
        }

        // autocomplete for authors
        DbxManager.books?.let {
            edit_author.setAdapter(ArrayAdapter<String>(activity, android.R.layout.simple_list_item_1, it.getAuthors()))
        }

        // date
        if(mItem == null){
            // new item --> set a date
            edit_date.setText(Book.readNow)
        }
        edit_date.setOnFocusChangeListener { view, focus -> if(!focus){
            edit_date.setText(Book.standardizedReadOn(edit_date.text.toString()))
        } }
        edit_date.addTextChangedListener(object : TextWatcher {
            var len = 0
            override fun afterTextChanged(p0: Editable?) {
                var date = p0.toString()
                if(date.length == 4 && date.length > len){
                    edit_date.append("-")
                }
            }

            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                len = p0?.length ?: 0
            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
        })
    }


    private fun saveBook() {

        val newBook = getBook()

        // check that something has indeed changed
        if (mItem != null && newBook.equals(mItem)) {
            Toast.makeText(activity, "nothing to save", Toast.LENGTH_SHORT).show()
            return
        }

        // ensure there are no duplicate names in the account list
        if (newBook.normalizedKey != mItem?.normalizedKey &&
                DbxManager.books!!.containsKey(newBook.normalizedKey)) {
            Toast.makeText(activity, "an account with this name already exists", Toast.LENGTH_LONG).show()
            return
        }

        // ok, now it become critical
        if (working) return
        working = true

        mItem?.let {
            // remove old book
            DbxManager.books!!.remove(it.normalizedKey)
        }
        DbxManager.books!![newBook.normalizedKey] = newBook


        // try save
        DbxManager.upload().successUi {
            // saved ok, end the edit activity
            Toast.makeText(activity, "Saved!", Toast.LENGTH_SHORT).show()
            (activity as? BookDetailActivity)?.setUpdatedBook(newBook)
            (activity as? BookListActivity)?.notifyBookUpdate(newBook)

        } failUi {
            // failed ... oups
            working = false
            // undo !
            undo(newBook)
            // show error
            Snackbar.make(activity.rootView(),
                    "Error " + it, Snackbar.LENGTH_LONG)
                    .show()
        }
    }

    private fun undo(newAccount: Book) {
        mItem?.let {
            DbxManager.books!![it.normalizedKey] = it
        }
        DbxManager.books!!.remove(newAccount.normalizedKey)
    }

    private fun getBook(): Book = Book(
            title = edit_title.text.toString().trim(),
            author = edit_author.text.toString().trim(),
            date = Book.standardizedReadOn(edit_date.text.toString().trim()),
            notes = edit_notes.text.toString().trim())
}
