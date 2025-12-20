package ch.derlin.mybooks

import android.annotation.SuppressLint
import android.os.Parcelable
import ch.derlin.mybooks.helpers.MiscUtils.capitalize
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.IgnoredOnParcel
import kotlinx.android.parcel.Parcelize
import java.text.Normalizer
import java.time.LocalDate
import java.time.format.DateTimeFormatter.ISO_LOCAL_DATE
import java.util.*

/**
 * Created by Lin on 23.12.17.
 */

typealias Books = MutableMap<String, Book>

fun String.toAudiobookMinutes(): Int {
    val regex = Regex("""^(\d+)h(\d{1,2})?$""")
    val match = regex.matchEntire(this)
        ?: throw IllegalArgumentException("Expected format: 7h34")

    val hours = match.groupValues[1].toInt()
    val minutes = match.groupValues[2].ifEmpty { "0" }.toInt()

    require(minutes in 0..59) { "Minutes must be between 0 and 59" }
    return hours * 60 + minutes
}

fun Int.fromAudiobookMinutes(simple: Boolean = false): String =
    if (simple) String.format("%dh%d", this / 60, this % 60)
    else String.format("%02dh:%02dm", this / 60, this % 60)


@SuppressLint("ParcelCreator")
@Parcelize
data class BookMeta(
    @Expose @SerializedName("GoodreadsID") val grId: String? = null,
    @Expose @SerializedName("pubDate") val pubDate: String? = null,
    @Expose @SerializedName("pages") val pages: Int? = null,
    @Expose @SerializedName("duration") val audiobookMinutes: Int? = null,
    @Expose @SerializedName("ISBN") val isbn: String?,
) : Parcelable {
    fun isEmpty(): Boolean {
        return listOfNotNull(grId, pubDate, pages, audiobookMinutes, isbn).isEmpty()
    }
}

@SuppressLint("ParcelCreator")
@Parcelize
data class Book(
    @Expose @SerializedName("title") val title: String,
    @Expose @SerializedName("author") val author: String,
    @Expose @SerializedName("date") val date: String = "",
    @Expose @SerializedName("notes") val notes: String = "",
    @Expose @SerializedName("meta") val metas: BookMeta? = null,
) : Parcelable {

    @IgnoredOnParcel
    private var _uid = 0L

    val uid: Long
        get() {
            if (_uid == 0L) _uid = normalizedKey.hashCode().toLong()
            return _uid
        }

    val dateNumbers: String
        get() = date.replace("\\D".toRegex(), "")

    val normalizedKey: String
        get() = normalizeKey(title)

    fun match(search: String): Boolean = with(search.lowercase(Locale.getDefault())) {
        listOf(title, author, date, notes).any { it.lowercase(Locale.getDefault()).contains(this) }
    }

    fun isAudiobook(): Boolean = metas?.audiobookMinutes != null

    companion object {

        val nameComparatorAsc = Comparator<Book> { a1, a2 -> a1.title.compareTo(a2.title, true) }
        val nameComparatorDesc = Comparator<Book> { a1, a2 -> -nameComparatorAsc.compare(a1, a2) }
        val modifiedComparatorAsc = Comparator<Book> { a1, a2 ->
            val comp = a1.dateNumbers.compareTo(a2.dateNumbers, true)
            if (comp != 0) comp else nameComparatorAsc.compare(a1, a2)
        }
        val modifiedComparatorDesc = Comparator<Book> { a1, a2 ->
            val comp = a1.dateNumbers.compareTo(a2.dateNumbers, true)
            if (comp != 0) -comp else nameComparatorAsc.compare(a1, a2)
        }

        val readNow: String
            get() = ISO_LOCAL_DATE.format(LocalDate.now())

        /**
         * pad months.
         * "2010-1" -> "2010-01"
         * "2010-1-2" -> "2010-01-02"
         * "  2019-2" -> "  2019-02"
         * "1999-9 " -> "1999-09 "
         * "??" -> "??"
         * etc.
         */
        fun standardizedReadOn(readOn: String): String = readOn
            .replace(Regex("(19[0-9]{2}-|20[0-9]{2}-)([1-9]$|[1-9][^0-9])"), "$10$2")
            .replace(Regex("(19[0-9]{2}-|20[0-9]{2}-[0-9]{2}-)([1-9]$|[1-9][^0-9])"), "$10$2")
    }

    /**
     * returns a normalized version of the book title, i.e.:
     * 1) to lower case
     * 2) accented characters replaced by their non accented counterparts
     * 3) replace not a-z or 0-9 characters by spaces
     * 4) trim + replace multiple spaces by one
     *
     * @return the normalized title
     */
    private fun normalizeKey(key: String): String = key.lowercase()
        .removeDiacritics()
        .replace("[^a-z0-9 ]".toRegex(), " ")
        .replace(" +".toRegex(), " ")
        .trim()

    private fun String.removeDiacritics() =
        Normalizer.normalize(this, Normalizer.Form.NFD).replace("\\p{Mn}+".toRegex(), "")

    fun sanitize(): Book = Book(
        // trim + capitalize first letter of each word
        title = title.trim().split(' ').joinToString(" ") { it.capitalize() },
        author = author.trim().split(' ').joinToString(" ") { it.capitalize() },
        // just trim
        date = date.trim(),
        notes = notes.trim()
    )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Book

        if (title != other.title) return false
        if (author != other.author) return false
        if (date != other.date) return false
        if (notes != other.notes) return false
        if (metas != other.metas) return false

        return true
    }
}


fun Books.getAuthors(): List<String> = map { b -> b.value.author }.distinct()

fun Books.sanitize(): Books {
    return mapValues { it.value.sanitize() }.toMutableMap()
}

