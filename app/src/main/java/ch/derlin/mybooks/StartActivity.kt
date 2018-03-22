package ch.derlin.mybooks

import android.content.Intent
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.view.View
import com.dropbox.core.android.Auth
import ch.derlin.mybooks.helpers.Preferences
import kotlinx.android.synthetic.main.activity_start.*
import timber.log.Timber

/**
 * This activity is the entry point of the application.
 * It ensures that the app is linked to dropbox and that
 * the dropbox service is instantiated before launching
 * the actual main activity.
 * @author Lucy Linder
 */
class StartActivity : AppCompatActivity() {

    private var mIsAuthenticating = false

    private var working: Boolean
        get() = progressBar.visibility == View.VISIBLE
        set(value) {
            progressBar.visibility = if (value) View.VISIBLE else View.INVISIBLE
        }
    // ----------------------------------------------------


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_start)

        val token = Preferences(this).dbxAccessToken
        if (token == null) {
            Timber.d("Dropbox token is null")
            mIsAuthenticating = true
            Auth.startOAuth2Authentication(this, getString(R.string.dbx_app_key))
        } else {
            Timber.d("Dropbox token is ${token}")
            startApp()
        }
    }


    override fun onResume() {
        super.onResume()
        // the dropbox linking happens in another activity.
        if (mIsAuthenticating) {
            val token = Auth.getOAuth2Token() //generate Access Token
            if (token != null) {
                Preferences(this).dbxAccessToken = token //Store accessToken in SharedPreferences
                Timber.d("new Dropbox token is ${token}")
                mIsAuthenticating = false
                startApp()
            } else {
                Snackbar.make(findViewById(android.R.id.content), "Error authenticating with Dropbox",
                        Snackbar.LENGTH_INDEFINITE)
                        .setAction("retry", { _ ->
                            forceRestart()
                        })
                        .show()
                Timber.d("Error authenticating")
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
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

    /*
    private fun loadBooks() {
        if (!NetworkStatus.isInternetAvailable(this)) {
            // no internet, try to load local file
            if (!DbxManager.localFileExists) {
                Snackbar.make(findViewById(android.R.id.content),
                        "Internet is not available", Snackbar.LENGTH_INDEFINITE)
                        .setAction("retry", { _ -> loadBooks() })
                        .show()
                return
            }
        }
        // internet, fetch latest rev
        working = true
        DbxManager.fetchBooks().alwaysUi {
            working = false
        } successUi {
            startApp()
        } failUi {
            Timber.d(it)
            Snackbar.make(findViewById(android.R.id.content),
                    "Error: ${it}", Snackbar.LENGTH_INDEFINITE)
                    .setAction("retry", { _ -> loadBooks() })
                    .show()
        }
    }
    */

}
