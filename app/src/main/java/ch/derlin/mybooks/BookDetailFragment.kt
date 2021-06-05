package ch.derlin.mybooks

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import ch.derlin.mybooks.R
import kotlinx.android.synthetic.main.activity_book_detail.*
import kotlinx.android.synthetic.main.book_detail.*

/**
 * A fragment representing a single Book detail screen.
 * This fragment is either contained in a [BookListActivity]
 * in two-pane mode (on tablets) or a [BookDetailActivity]
 * on handsets.
 */
class BookDetailFragment : Fragment() {

    /**
     * The dummy content this fragment is presenting.
     */
    private lateinit var mItem: Book

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
            it.getParcelable<Book>(BookDetailActivity.BUNDLE_BOOK_KEY)?.let { book ->
                mItem = book
                (activity as? BookDetailActivity)?.updateTitle(book.title)

            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
            inflater.inflate(R.layout.book_detail, container, false)


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Show the dummy content as text in a TextView.
        with(mItem) {
            details_title.text = title
            details_author.text = author
            details_date.text = date
            details_notes.text = notes
        }

        (activity as? BookDetailActivity)?.let {
            it.fab.setImageResource(R.drawable.ic_edit)
            it.fab.setOnClickListener { _ ->
                it.editBook()
            }
        }
    }
}
