package ch.derlin.mybooks

import android.app.Activity
import android.widget.ImageButton
import android.widget.TextView
import com.google.android.material.bottomsheet.BottomSheetDialog

class BooksBottomSheetDialog(
    private val searchButtonCallback: (BottomSheetDialog, Book) -> Unit,
    private val editButtonCallback: (BottomSheetDialog, Book) -> Unit,
    private val showButtonCallback: (BottomSheetDialog, Book) -> Unit
) {

    fun show(activity: Activity, book: Book) {
        val bottomSheetDialog = BottomSheetDialog(activity)
        val view = activity.layoutInflater.inflate(R.layout.book_list_bottomsheet, null)

        view.findViewById<TextView>(R.id.sheetTitle).text = book.title
        view.findViewById<TextView>(R.id.notes).text = book.notes

        view.findViewById<ImageButton>(R.id.viewButton).setOnClickListener {
            showButtonCallback(bottomSheetDialog, book)
        }
        view.findViewById<ImageButton>(R.id.editButton).setOnClickListener {
            editButtonCallback(bottomSheetDialog, book)
        }
        view.findViewById<ImageButton>(R.id.searchButton)
            .apply { this.setImageResource(if (book.metas?.grId != null) R.drawable.ic_goodreads else R.drawable.ic_google) }
            .setOnClickListener {
                searchButtonCallback(bottomSheetDialog, book)
            }

        bottomSheetDialog.setContentView(view)
        bottomSheetDialog.show()
    }
}