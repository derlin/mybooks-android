package ch.derlin.mybooks

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import ch.derlin.mybooks.helpers.MiscUtils.capitalize

class BookListAdapter(var books: Books,
                      defaultComparator: Comparator<Book> = Book.nameComparatorAsc,
                      private var textviewCounter: TextView? = null) :
        RecyclerView.Adapter<BookListAdapter.BookViewHolder>() {

    var comparator: Comparator<Book> = defaultComparator
        set(value) {
            field = value; doSort(); notifyDataSetChanged()
        }

    var onClick: ((Book) -> Unit)? = null
    var onLongClick: ((Book) -> Unit)? = null

    private var lastSearch: String = ""
    private var filtered = books.values.toMutableList()

    init {
        setHasStableIds(true)
        doSort()
        updateCounter()
    }

    override fun onBindViewHolder(holder: BookViewHolder, position: Int, payloads: MutableList<Any>) {
        onBindViewHolder(holder, position)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookViewHolder {
        // create a new view
        val v: View = LayoutInflater.from(parent.context).inflate(R.layout.book_list_content, parent, false)
        return BookViewHolder(v)
    }

    override fun onBindViewHolder(holder: BookViewHolder, position: Int) {
        val item = filtered[position]

        holder.titleView.text = item.title.capitalize()
        holder.leftSubtitleView.text = item.author
        holder.rightSubtitleView.text = item.date

        holder.view.setOnClickListener { _ -> onClick?.invoke(item) }
        holder.view.setOnLongClickListener { _ -> onLongClick?.invoke(item); true }

    }

    override fun getItemCount(): Int = filtered.size

    override fun getItemId(position: Int): Long = filtered[position].uid

    fun replaceAll(newBooks: Books) {
        this.books = newBooks
        resetAndNotify()
    }

    fun itemAtPosition(position: Int): Book = filtered[position]

    fun removeAt(position: Int): Book {
        val item = filtered[position]
        books.remove(item.normalizedKey)
        resetAndNotify()
        return item
    }

    fun filter(search: String? = lastSearch) {
        lastSearch = search ?: ""
        resetAndNotify()
    }


    fun add(item: Book) {
        books[item.normalizedKey] = item
        resetAndNotify()
    }

    private fun doSort() {
        filtered.sortWith(comparator)
    }

    private fun doFilter() {
        filtered = if (lastSearch.isBlank()) books.values.toMutableList()
        else books.values.filter { i -> i.match(lastSearch) }.toMutableList()
    }

    private fun resetAndNotify() {
        doFilter()
        doSort()
        updateCounter()
        notifyDataSetChanged()
    }

    fun replace(old: Book, new: Book) {
        books.remove(old.normalizedKey)
        books[new.normalizedKey] = new
        resetAndNotify()
    }

    fun positionOf(book: Book): Int = filtered.indexOf(book)

    private fun updateCounter() {
        textviewCounter?.text = App.appContext.getString(R.string.book_list_counter_text, filtered.size)
    }
    // -----------------------------------------

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    class BookViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
        val titleView: TextView = view.findViewById(R.id.title)
        val leftSubtitleView: TextView = view.findViewById(R.id.subtitle_left)
        val rightSubtitleView: TextView = view.findViewById(R.id.subtitle_right)
    }


}