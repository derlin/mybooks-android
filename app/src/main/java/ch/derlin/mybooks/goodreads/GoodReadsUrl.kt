package ch.derlin.mybooks.goodreads

import java.net.URLEncoder

object GoodReadsUrl {

    /**
     * Base URL for the GoodReads front page.
     */
    const val home = "https://www.goodreads.com"

    /**
     * Get the full URL to a GoodReads book detail page from a GoodReads Book ID.
     */
    fun forBookId(id: String) = "$home/book/show/$id"

    /**
     * Get the full URL for a search query.
     *
     * The search will be in title only if author is `null`, in all fields otherwise.
     * Note that except when you have very short/generic titles, searching in title only yields better results.
     */
    fun queryFor(title: String, author: String? = null): String {
        // Usually, search in title only is better
        val searchString = cleanTitleForSearchQuery(title) + (author?.let { " " + cleanAuthorForSearchQuery(it) } ?: "")
        val searchUrl = "$home/search?&search_type=books&q=" + URLEncoder.encode(searchString, "UTF-8")

        return if (author == null) "$searchUrl&search%5Bfield%5D=title" else searchUrl
    }
}