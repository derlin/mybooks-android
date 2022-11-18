package ch.derlin.mybooks

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import ch.derlin.grmetafetcher.GoodReadsLookup
import ch.derlin.grmetafetcher.GoodReadsPaginatedSearchResults
import ch.derlin.grmetafetcher.GoodReadsSearchResult
import ch.derlin.mybooks.helpers.MiscUtils.rootView
import ch.derlin.mybooks.helpers.MiscUtils.textTrimmed
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_goodreads_search.*
import nl.komponents.kovenant.Promise
import nl.komponents.kovenant.deferred
import nl.komponents.kovenant.task
import nl.komponents.kovenant.ui.alwaysUi
import nl.komponents.kovenant.ui.failUi
import nl.komponents.kovenant.ui.successUi
import timber.log.Timber

class GoodreadsSearchActivity : AppCompatActivity() {

    companion object {
        const val BUNDLE_TITLE_SEARCH = "grSearchTitle"
        const val BUNDLE_AUTHOR_SEARCH = "grSearchAuthor"
        const val BUNDLE_GR_META = "result_meta"
    }

    private var working: Boolean
        get() = gr_progressbar.visibility == View.VISIBLE
        set(value) {
            gr_progressbar.visibility = if (value) View.VISIBLE else View.INVISIBLE
        }

    private lateinit var resultsListAdapter: GoodReadsResultsListAdapter

    private var currentResults: GoodReadsPaginatedSearchResults? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_goodreads_search)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        resultsListAdapter = GoodReadsResultsListAdapter(onItemClickListener = ::returnResult)
        gr_listview.adapter = resultsListAdapter

        (gr_listview.layoutManager as LinearLayoutManager).let { linearLayoutManager ->
            gr_listview.setOnScrollChangeListener { _, _, scrollY, _, oldScrollY ->
                if (currentResults?.hasNext() == true
                        && scrollY > oldScrollY // scrolling down
                        && linearLayoutManager.findLastCompletelyVisibleItemPosition() >= resultsListAdapter.itemCount - 1)
                    fetchNext()
            }
        }

        intent.extras?.let { extras ->
            gr_title_field.setText(extras.getString(BUNDLE_TITLE_SEARCH) ?: "")
            gr_author_field.setText(extras.getString(BUNDLE_AUTHOR_SEARCH) ?: "")
        }

        gr_author_switch.setOnCheckedChangeListener { _, isChecked ->
            gr_author_field.isEnabled = isChecked
        }
        gr_title_field.setOnEditorActionListener { _, actionId, _ ->
            return@setOnEditorActionListener (actionId == EditorInfo.IME_ACTION_SEARCH).also { if (it) search() }
        }
        gr_search_btn.setOnClickListener { search() }
        gr_author_field.isEnabled = gr_author_switch.isChecked
        search()
    }

    private fun updateInfoText() {
        currentResults?.let {
            gr_result_text.text = if (it.totalResults > 0)
                getString(R.string.goodreads_showing_x_out_of_y).format(resultsListAdapter.itemCount, it.totalResults)
            else
                getString(R.string.goodreads_no_results)
        } ?: resetInfoText()
    }

    private fun resetInfoText() {
        gr_result_text.text = getString(R.string.goodreads_enter_search_prompt)
    }

    private fun search() {
        val titleString = gr_title_field.textTrimmed()
        val authorString = gr_author_field.textTrimmed()
        val searchAuthorEnabled = gr_author_switch.isChecked && authorString.isNotBlank()

        if (titleString.isBlank() && !searchAuthorEnabled) {
            resetInfoText()
            return
        }

        val lookup = GoodReadsLookup(
                title = titleString,
                author = authorString,
                includeAuthorInSearch = searchAuthorEnabled
        )

        doAsync {
            lookup.getMatchesPaginated()
                    .also { currentResults = it }
                    .let { if (it.hasNext()) it.next() else null }
        } successUi {
            resultsListAdapter.setResults(it)
        } failUi {
            resultsListAdapter.setResults(null)
            Snackbar.make(rootView(), "An error occurred fetching results: $it", Snackbar.LENGTH_LONG).show()
        } alwaysUi {
            updateInfoText()
        }
    }

    private fun fetchNext() {
        currentResults?.let { results ->
            if (results.hasNext()) {
                doAsync {
                    results.next()
                } successUi {
                    resultsListAdapter.appendResults(it)
                } failUi {
                    Snackbar.make(rootView(), "An error occurred fetching more results: $it", Snackbar.LENGTH_LONG).show()
                } alwaysUi {
                    updateInfoText()
                }
            }
        }
    }

    private fun returnResult(result: GoodReadsSearchResult) {
        doAsync {
            val res = result.getMetadata()
            res
        } success { meta ->
            Timber.d("Fetched metadata $meta")
            with(Intent()) {
                putExtra(BUNDLE_GR_META, meta)
                setResult(Activity.RESULT_OK, this)
            }
            finish()
        } failUi {
            Snackbar.make(rootView(), "An error occurred while fetching metas for $result: $it", Snackbar.LENGTH_LONG)
        }
    }

    private fun <R> doAsync(block: () -> R): Promise<R, String> {
        val deferred = deferred<R, String>()
        working = true
        task {
            deferred.resolve(block())
        } fail {
            deferred.reject(it.toString())
        } alwaysUi {
            working = false
        }
        return deferred.promise
    }

    override fun onBackPressed() {
        setResult(Activity.RESULT_CANCELED)
        finish()
    }


    class GoodReadsResultsListAdapter(
            results: List<GoodReadsSearchResult> = listOf(),
            private var onItemClickListener: (GoodReadsSearchResult) -> Unit = { _ -> }) :
            RecyclerView.Adapter<GoodReadsResultsViewHolder>() {

        private val results: MutableList<GoodReadsSearchResult> = results.toMutableList()

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GoodReadsResultsViewHolder {
            val v: View = LayoutInflater.from(parent.context).inflate(R.layout.goodreads_list_content, parent, false)
            return GoodReadsResultsViewHolder(v)
        }

        override fun onBindViewHolder(holder: GoodReadsResultsViewHolder, position: Int) = results[position].let { result ->
            with(holder) {
                titleView.text = result.title
                authorView.text = result.authors.joinToString(", ")
                urlView.text = result.url
            }
            holder.view.setOnClickListener { onItemClickListener(result) }
        }

        override fun getItemCount(): Int = results.size

        fun setResults(newResults: List<GoodReadsSearchResult>?) {
            results.clear()
            newResults?.let { appendResults(it) }
        }

        fun appendResults(newResults: List<GoodReadsSearchResult>) {
            results.addAll(newResults)
            notifyDataSetChanged()
        }
    }

    class GoodReadsResultsViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
        val titleView: TextView = view.findViewById(R.id.grlist_title)
        val authorView: TextView = view.findViewById(R.id.grlist_author)
        val urlView: TextView = view.findViewById(R.id.grlist_url)
    }
}