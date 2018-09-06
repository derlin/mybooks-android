package ch.derlin.mybooks.helpers

import android.app.Activity
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.text.Editable
import android.text.Html
import android.text.Spanned
import android.text.TextWatcher
import android.util.TypedValue
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import ch.derlin.mybooks.IntroActivity


/**
 * Created by Lin on 25.11.17.
 */

object MiscUtils {

    /** Hide the soft keyboard */
    fun Activity.hideKeyboard() {
        val v = window.currentFocus
        if (v != null) {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(v.windowToken, 0)
        }
    }

    /** Get the root view of this activity */
    fun Activity.rootView(): View = findViewById(android.R.id.content)

    /** Simplify the implementation of the textChanged listener */
    fun EditText.afterTextChanged(afterTextChanged: (String) -> Unit) {
        this.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

            override fun afterTextChanged(editable: Editable?) {
                afterTextChanged.invoke(editable.toString())
            }
        })
    }

    /** Launch the introduction slides activity using the [IntroActivity.INTENT_INTRO] request code */
    fun Activity.showIntro() {
        val intent = Intent(this, IntroActivity::class.java)
        // intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_TASK_ON_HOME
        startActivityForResult(intent, IntroActivity.INTENT_INTRO)
    }

    /** Resolve an attribute color, for example [android.R.attrcolorAccent] */
    fun Activity.attrColor(resourceId: Int): Int {
        // see https://stackoverflow.com/a/27611244/2667536
        val typedValue = TypedValue()
        val a = obtainStyledAttributes(typedValue.data, intArrayOf(resourceId))
        val color = a.getColor(0, 0)
        a.recycle()
        return color
    }
}