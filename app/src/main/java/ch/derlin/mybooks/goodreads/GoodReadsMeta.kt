package ch.derlin.mybooks.goodreads

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.io.Serializable
import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class GoodReadsMeta(
    val url: String,
    val id: String?,
    val title: String?,
    val authors: List<String>?,
    val isbn: String?,
    val pages: Int?,
    val pubDate: LocalDate?,
) : Serializable {
    fun isValid(): Boolean {
        return listOf(url, id, title).all { it != null } && !authors.isNullOrEmpty()
    }
}

object GoodReadsParser {
    fun parse(url: String, html: String): GoodReadsMeta {
        val soup = Jsoup.parse(html)
        return GoodReadsMeta(
            url = url,
            id = Regex("/show/(\\d+)").find(url)?.groupValues?.get(1),
            title = soup.getTitle(),
            authors = soup.getAuthors(),
            isbn = soup.getIsbn(),
            pages = soup.getNumberOfPages(),
            pubDate = soup.getPublicationDate(),
        )
    }
}

//private fun Document.getUrl(): String? =
//    this.getElementsByAttributeValue("rel", "canonical").first()
//        ?.attr("href")

private fun Document.getTitle(): String? =
    this.getElementsByAttributeValue("data-testid", "bookTitle")
        .first()?.text()


private fun Document.getAuthors(): List<String>? =
    this.getElementsByClass("ContributorLinksList").first()
        ?.text()
        ?.let { getAuthorsFromString(it) }

private fun getAuthorsFromString(authors: String): List<String> =
// Authors is in the form "by First Last[, First Last]"
// Some authors have one or multiple roles, e.g. "First Last (GoodReads Author)", or "First Last (Illustrator)"
    // Here, only the main authors are returned, that no role OR GoodReads Author role
    authors.substringAfter("by ")
        .split(",")
        .mapNotNull { "([^(]+) ?(\\(.+\\))?".toRegex().find(it.trim())?.groupValues }
        .mapNotNull { (_, author, roles) ->
            author.trim().takeIf { roles.isBlank() || "(goodreads author)" in roles.lowercase() }
        }

private fun Document.getIsbn(): String? =
    getElementsByClass("EditionDetails").first()?.text()
        ?.let { Regex("\\d{13}").find(it)?.groupValues?.get(0) }
        ?.let { Regex("\\d{10}").find(it)?.groupValues?.get(0) }

private fun Document.getNumberOfPages(): Int? =
    this.getElementsByAttributeValue("data-testid", "pagesFormat").first()
        ?.text()
        ?.let { Regex("(\\d+) pages").find(it)?.groupValues?.get(1) }
        ?.toInt()

private fun Document.getPublicationDate(): LocalDate? =
    this.getElementsByAttributeValue("data-testid", "publicationInfo").first()
        ?.text()
        ?.removePrefix("Published ")?.removePrefix("First published ")
        ?.let { LocalDate.parse(it, DateTimeFormatter.ofPattern("MMMM d, yyyy")) }