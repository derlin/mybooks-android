package ch.derlin.mybooks

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import ch.derlin.mybooks.helpers.Preferences
import ch.derlin.mybooks.persistence.LocalManager
import ch.derlin.mybooks.persistence.PersistenceManager
import com.dropbox.core.android.Auth
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_login.*
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

    private val tmpBooksFile = "mybooks-tmp.json"
    private var hasLocalData = false

    private var working: Boolean
        get() = progressBar.visibility == View.VISIBLE
        set(value) {
            progressBar.visibility = if (value) View.VISIBLE else View.INVISIBLE
            button_link_dropbox.isEnabled = !value
        }
    // ----------------------------------------------------


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val token = Preferences.dbxAccessToken
        if (token == null) {
            Timber.d("Dropbox token is null")
            button_link_dropbox.setOnClickListener {
                // save local books to a tmp file
                (PersistenceManager.instance as? LocalManager)?.books?.let { books ->
                    if (books.isNotEmpty()) {
                        PersistenceManager.instance.serialize(this.openFileOutput(tmpBooksFile, Context.MODE_PRIVATE))
                        hasLocalData = true
                    }
                }
                working = true
                Auth.startOAuth2Authentication(this, getString(R.string.dbx_app_key))
            }
        } else {
            Timber.d("Dropbox token is $token")
            finishTask()
        }
    }

    override fun onBackPressed() {
        setResult(Activity.RESULT_CANCELED)
        finish()
    }

    override fun onResume() {
        super.onResume()
        // the dropbox linking happens in another activity.
        if (working) {
            val token = Auth.getOAuth2Token() //generate Access Token
            if (token != null) {
                Snackbar.make(findViewById(android.R.id.content), getString(R.string.dbx_finishing_auth), Snackbar.LENGTH_INDEFINITE).show()
                Preferences.dbxAccessToken = token //Store accessToken in SharedPreferences
                Timber.d("new Dropbox token is $token")
                PersistenceManager.invalidate()
                PersistenceManager.instance.fetchBooks().alwaysUi {
                    if (hasLocalData) merge() else finishTask()
                }
            } else {
                Snackbar.make(findViewById(android.R.id.content), getString(R.string.dbx_auth_error), Snackbar.LENGTH_LONG).show()
                Timber.d("Error authenticating")
                working = false
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
                        .setTitle(getString(R.string.resolve_conflict_title))
                        .setMessage(getString(R.string.resolve_conflict_msg))
                        .setNegativeButton(getString(R.string.resolve_conflict_option_dropbox)) { d, _ ->
                            d.dismiss()
                            finishTask()
                        }
                        .setPositiveButton(getString(R.string.resolve_conflict_option_local)) { _, _ ->
                            manager.books = oldBooks
                            manager.persist()
                            finishTask()
                        }
                        .create()
                dialog.show()
            }
        }

        manager.removeAppFile(tmpBooksFile) // delete old file
        working = false
        if (dialog == null) finishTask()
    }

    private fun finishTask() {
        setResult(Activity.RESULT_OK)
        this.finish()
    }

    private fun forceRestart() {
        this.finish()
    }

}
