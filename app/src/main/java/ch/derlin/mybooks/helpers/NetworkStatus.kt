package ch.derlin.mybooks.helpers

import android.content.Context
import android.net.ConnectivityManager
import ch.derlin.mybooks.App


object NetworkStatus {

    fun isInternetAvailable(context: Context = App.appContext): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        return cm.activeNetwork != null
    }
}