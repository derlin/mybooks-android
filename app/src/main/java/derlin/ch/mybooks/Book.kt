package derlin.ch.mybooks

import android.os.Parcelable
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize

/**
 * Created by Lin on 23.12.17.
 */

typealias Books = MutableMap<String, Book>

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

    fun match(search: String): Boolean {
        return title.toLowerCase().contains(search) || //
                author.toLowerCase().contains(search) || //
                date.toLowerCase().contains(search) || //
                notes.toLowerCase().contains(search)
    }

    fun toSearchQuery(): String {
        return title.split(" +").joinToString("+") + "+" +
                author.split(" +").joinToString("+")
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

        /**
         * returns a normalized version of the book title, i.e.:
         * 1) to lower case
         * 2) accented characters replaced by their non accented counterparts
         * 3) replace not a-z or 0-9 characters by spaces
         * 4) trim + replace multiple spaces by one
         *
         * @return the normalized title
         */
        fun normalizeKey(key: String): String = key.toLowerCase() //
                .replace("é", "e")//
                .replace("è", "e")//
                .replace("ê", "e")//
                .replace("à", "a")//
                .replace("ç", "c")//
                .replace("ù", "u")//
                .replace("û", "u")
                .replace("[^a-z0-9 ]".toRegex(), " ")
                .replace(" +".toRegex(), " ")
                .trim { it <= ' ' }

    }
}
