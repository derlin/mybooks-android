package ch.derlin.mybooks

import android.app.Activity
import android.widget.ImageButton
import android.widget.TextView
import androidx.core.widget.NestedScrollView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog

class BooksBottomSheetDialog(
        bottomSheetBehaviorView: NestedScrollView,
        private val searchButtonCallback: (BottomSheetDialog, Book) -> Unit,
        private val editButtonCallback: (BottomSheetDialog, Book) -> Unit,
        private val showButtonCallback: (BottomSheetDialog, Book) -> Unit
) {

    private val bottomSheetBehavior: BottomSheetBehavior<NestedScrollView> = BottomSheetBehavior.from(bottomSheetBehaviorView)

    init {
        bottomSheetBehavior.peekHeight = 300
        bottomSheetBehavior.isHideable = true
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
    }

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
        view.findViewById<ImageButton>(R.id.searchButton).setOnClickListener {
            searchButtonCallback(bottomSheetDialog, book)
        }

        bottomSheetDialog.setContentView(view)
        bottomSheetDialog.show()
    }

    fun hideAll(): Boolean {
        return if (bottomSheetBehavior.state != BottomSheetBehavior.STATE_HIDDEN) {
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
            true
        } else {
            false
        }
    }
}