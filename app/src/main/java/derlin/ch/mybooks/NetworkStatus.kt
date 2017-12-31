package derlin.ch.mybooks

import android.content.Context
import android.net.ConnectivityManager


object NetworkStatus {
    var isConnected: Boolean? = null

    fun isInternetAvailable(context: Context = App.appContext): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        isConnected = cm.activeNetworkInfo != null && cm.activeNetworkInfo.isConnected
        return isConnected!!
    }
}