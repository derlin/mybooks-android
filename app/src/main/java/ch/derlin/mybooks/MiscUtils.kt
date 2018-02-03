package ch.derlin.mybooks

import android.app.Activity
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.os.Build
import android.text.Editable
import android.text.Html
import android.text.Spanned
import android.text.TextWatcher
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText


/**
 * Created by Lin on 25.11.17.
 */

object MiscUtils {

    fun toSpannable(str: String, vararg args: String): Spanned? {
        val content = if (args.size > 0) str.format(args) else str
        if (Build.VERSION.SDK_INT >= 24) {
            return Html.fromHtml(content, Html.FROM_HTML_MODE_LEGACY)
        } else {
            return Html.fromHtml(content)
        }
    }

    fun Activity.hideKeyboard() {
        val v = window.currentFocus
        if (v != null) {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(v.windowToken, 0)
        }
    }

    fun Activity.restartApp() {
        // see https://stackoverflow.com/questions/17795189/how-to-programmatically-force-a-full-app-restart-e-g-kill-then-start
        val startIntent = packageManager.getLaunchIntentForPackage(packageName)
        val pendingIntent = PendingIntent.getActivity(this, 123456, startIntent,
                PendingIntent.FLAG_CANCEL_CURRENT)

        (getSystemService(Context.ALARM_SERVICE) as AlarmManager)
                .set(AlarmManager.RTC, System.currentTimeMillis() + 1, pendingIntent)

        System.exit(0)
        //android.os.Process.killProcess(android.os.Process.myPid())
    }

    fun Activity.rootView(): View = findViewById(android.R.id.content)

    fun EditText.afterTextChanged(afterTextChanged: (String) -> Unit) {
        this.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

            override fun afterTextChanged(editable: Editable?) {
                afterTextChanged.invoke(editable.toString())
            }
        })
    }
}