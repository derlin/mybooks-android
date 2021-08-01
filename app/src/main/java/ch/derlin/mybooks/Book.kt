package ch.derlin.mybooks

import android.annotation.SuppressLint
import android.os.Parcelable
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize
import java.text.Normalizer
import java.time.LocalDate
import java.time.format.DateTimeFormatter.ISO_LOCAL_DATE
import java.util.*

/**
 * Created by Lin on 23.12.17.
 */

typealias Books = MutableMap<String, Book>

@SuppressLint("ParcelCreator")
@Parcelize
data class Book(
        @Expose @SerializedName("title") val title: String,
        @Expose @SerializedName("author") val author: String,
        @Expose @SerializedName("date") val date: String = "",
        @Expose @SerializedName("notes") val notes: String = "") : Parcelable {

    private var _uid = 0L

    val uid: Long
        get() {
            if (_uid == 0L) _uid = normalizedKey.hashCode().toLong()
            return _uid
        }

    val dateNumbers: String
        get() = date.replace("[^\\d]".toRegex(), "")

    val normalizedKey: String
        get() = normalizeKey(title)

    fun match(search: String): Boolean = with(search.toLowerCase(Locale.getDefault())) {
        listOf(title, author, date, notes).any { it.toLowerCase(Locale.getDefault()).contains(this) }
    }

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
    fun normalizeKey(key: String): String = key.toLowerCase()
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
}


fun Books.getAuthors(): List<String> = map { b -> b.value.author }.distinct()

fun Books.sanitize(): Books {
    return mapValues { it.value.sanitize() }.toMutableMap()
}

