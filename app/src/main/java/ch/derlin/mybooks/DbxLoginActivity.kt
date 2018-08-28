package ch.derlin.mybooks

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.TextView
import com.dropbox.core.android.Auth
import ch.derlin.mybooks.helpers.Preferences
import ch.derlin.mybooks.helpers.ThemeHelper.applyTheme
import ch.derlin.mybooks.persistence.LocalManager
import ch.derlin.mybooks.persistence.PersistenceManager
import kotlinx.android.synthetic.main.activity_start.*
import nl.komponents.kovenant.ui.alwaysUi
import timber.log.Timber

/**
 * This activity is the entry point of the application.
 * It ensures that the app is linked to dropbox and that
 * the dropbox service is instantiated before launching
 * the actual main activity.
 * @author Lucy Linder
 */
class DbxLoginActivity : AppCompatActivity() {

    private var mIsAuthenticating = false
    private val tmpBooksFile = "mybooks-tmp.json"
    private var hasLocalData = false

    private var working: Boolean
        get() = progressBar.visibility == View.VISIBLE
        set(value) {
            progressBar.visibility = if (value) View.VISIBLE else View.INVISIBLE
        }
    // ----------------------------------------------------


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applyTheme()
        setContentView(R.layout.activity_start)

        val token = Preferences(this).dbxAccessToken
        if (token == null) {
            Timber.d("Dropbox token is null")
            working = true
            mIsAuthenticating = true
            Auth.startOAuth2Authentication(this, getString(R.string.dbx_app_key))
        } else {
            Timber.d("Dropbox token is ${token}")
            finishTask()
        }
    }

    override fun onPause() {
        super.onPause()
        if (mIsAuthenticating) {
            LocalManager.books?.let { books ->
                if (books.size > 0) {
                    PersistenceManager.instance
                            .serialize(this.openFileOutput(tmpBooksFile, Context.MODE_PRIVATE))
                    hasLocalData = true
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // the dropbox linking happens in another activity.
        if (mIsAuthenticating) {
            val token = Auth.getOAuth2Token() //generate Access Token
            if (token != null) {
                Snackbar.make(findViewById(android.R.id.content), "Finishing authentication",
                        Snackbar.LENGTH_INDEFINITE).show()
                Preferences(this).dbxAccessToken = token //Store accessToken in SharedPreferences
                Timber.d("new Dropbox token is ${token}")
                mIsAuthenticating = false
                if (hasLocalData) {
                    PersistenceManager.instance.fetchBooks().alwaysUi { merge() }
                } else {
                    finishTask()
                }
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

    // ----------------------------------------------------

    private fun merge() {
        val manager = PersistenceManager.instance
        val oldBooks = manager.deserialize(this.openFileInput(tmpBooksFile))
        val newBooks = manager.books
        var dialog: AlertDialog? = null

        if (newBooks?.size == 0) {
            // we have no books in dropbox -> save local books to dropbox
            Timber.d("Saved local changes to dropbox")
            manager.books = oldBooks
            manager.persist()
        } else {
            // we have books both in local and remote
            if (oldBooks.equals(newBooks)) {
                // same content, do nothing
            } else {
                dialog = AlertDialog.Builder(this) // TODO, R.style.AppTheme_AlertDialog)
                        .setTitle("Resolve conflicts")
                        .setMessage("You have both books locally and in Dropbox. What version do you want to keep ?")
                        .setNegativeButton("Dropbox version", { d, _ ->
                            d.dismiss()
                            finishTask()
                        })
                        .setPositiveButton("Local version", { _, _ ->
                            manager.books = oldBooks
                            manager.persist()
                            finishTask()
                        })
                        .create()
                dialog.show()
            }
        }

        manager.removeAppFile(tmpBooksFile) // delete old file
        if(dialog == null) finishTask()
    }

    private fun finishTask() {
        setResult(Activity.RESULT_OK)
        this.finish()
    }

    private fun forceRestart() {
       this.finish()
    }

}
