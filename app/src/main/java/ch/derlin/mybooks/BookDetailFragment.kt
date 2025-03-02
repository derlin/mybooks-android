package ch.derlin.mybooks

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.FIND_VIEWS_WITH_CONTENT_DESCRIPTION
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import ch.derlin.mybooks.goodreads.GoodReadsUrl
import ch.derlin.mybooks.helpers.MiscUtils.rootView
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
            it.getParcelable(BookDetailActivity.BUNDLE_BOOK_KEY, Book::class.java)?.let { book ->
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

        if (mItem.metas != null) {
            mItem.metas?.let {
                details_metas_pubDate.text = it.pubDate
                details_metas_pages.text = it.pages?.toString()
                details_metas_isbn.text = it.isbn
                details_metas_url.text = it.grId?.let { id -> GoodReadsUrl.forBookId(id) }
            }
        } else {
            val metasViews = ArrayList<View>()
            requireActivity().rootView().findViewsWithText(metasViews, "metas", FIND_VIEWS_WITH_CONTENT_DESCRIPTION)
            metasViews.forEach { it.visibility = View.GONE }
        }

        (activity as? BookDetailActivity)?.let {
            it.fab.setImageResource(R.drawable.ic_edit)
            it.fab.setOnClickListener { _ ->
                it.editBook()
            }
        }
    }
}
