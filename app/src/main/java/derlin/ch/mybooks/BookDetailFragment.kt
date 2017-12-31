package derlin.ch.mybooks

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
            if (it.containsKey(BookDetailActivity.BUNDLE_BOOK_KEY)) {
                mItem = it.getParcelable<Book>(BookDetailActivity.BUNDLE_BOOK_KEY)
                (activity as? BookDetailActivity)?.updateTitle(mItem.title)
            }
        }

    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? =
            inflater.inflate(R.layout.book_detail, container, false)


    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Show the dummy content as text in a TextView.
        details_title.setText(mItem.title)
        details_author.setText(mItem.author)
        details_date.setText(mItem.date)
        details_notes.setText(mItem.notes)

        (activity as? BookDetailActivity)?.let {
            it.fab.setImageResource(R.drawable.ic_edit)
            it.fab.setOnClickListener { _ ->
                it.editBook()
            }
        }
    }
}
