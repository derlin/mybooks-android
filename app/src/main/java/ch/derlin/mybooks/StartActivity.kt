package ch.derlin.mybooks

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import ch.derlin.mybooks.helpers.Preferences
import timber.log.Timber

/**
 * This activity is the entry point of the application.
 * It ensures that the app is linked to dropbox and that
 * the dropbox service is instantiated before launching
 * the actual main activity.
 * @author Lucy Linder
 */
class StartActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_start)

        val token = Preferences.dbxAccessToken
        Timber.d("Dropbox token is $token")
        startApp()
    }

    // ----------------------------------------------------


    private fun startApp() {
        // service up and running, start the actual app
        val intent = Intent(this, BookListActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_TASK_ON_HOME
        startActivity(intent)
        this.finish()
    }

    private fun forceRestart() {
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
        launchIntent!!.flags = Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(launchIntent)
    }

}
