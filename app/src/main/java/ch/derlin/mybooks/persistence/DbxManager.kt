package ch.derlin.mybooks.persistence

import android.content.Context
import ch.derlin.mybooks.App
import ch.derlin.mybooks.Books
import ch.derlin.mybooks.R
import com.dropbox.core.DbxRequestConfig
import com.dropbox.core.v2.DbxClientV2
import com.dropbox.core.v2.files.FileMetadata
import com.dropbox.core.v2.files.GetMetadataErrorException
import com.dropbox.core.v2.files.WriteMode
import ch.derlin.mybooks.helpers.Preferences
import ch.derlin.mybooks.sanitize
import nl.komponents.kovenant.Promise
import nl.komponents.kovenant.deferred
import nl.komponents.kovenant.task
import timber.log.Timber


/**
 * Created by Lin on 24.11.17.
 */

class DbxManager : PersistenceManager() {

    val remoteFilePath = "/${baseFileName}"

    var metadata: FileMetadata? = null

    override var books: Books? = null
    override val localFileExists: Boolean
        get() = prefs.revision != null

    val client: DbxClientV2 by lazy {
        val token = Preferences(App.appContext).dbxAccessToken
        Timber.d("Dropbox token ?? client created")
        val config = DbxRequestConfig.newBuilder(
                App.appContext.getString(R.string.dbx_request_config_name)).build()
        DbxClientV2(config, token)
    }

    val prefs: Preferences by lazy {
        Preferences(App.appContext)
    }

    var isInSync = false
        private set


    fun removeLocalFile(): Boolean {
        val ok = removeAppFile()
        Timber.d("""removed local file ? $ok""")
        prefs.revision = null
        return ok
    }

    override fun fetchBooks(): Promise<Boolean, Exception> {
        val deferred = deferred<Boolean, Exception>()
        task {
            try {
                metadata = client.files().getMetadata(remoteFilePath) as FileMetadata
                isInSync = metadata?.rev.equals(prefs.revision)

                if (isInSync && localFileExists) {
                    books = deserialize()
                    deferred.resolve(true)
                } else {
                    fetchRemote(deferred)
                }
            } catch (e: GetMetadataErrorException) {
                // session does not exist
                prefs.revision = null
                books = mutableMapOf()
                deferred.resolve(isInSync)
            }
        } fail {
            deferred.reject(it)
        }
        return deferred.promise
    }

    fun sanitize(): Promise<Boolean, Exception> {
        books?.let {
            books = it.sanitize()
            return persist()
        }
        return Promise.of(false)
    }

    fun unbind(): Promise<Boolean, Exception> {
        val deferred = deferred<Boolean, Exception>()
        task {
            Timber.d("revoking Dropbox token")
            val prefs = Preferences()
            prefs.dbxAccessToken = null
            prefs.revision = null
            client.auth().tokenRevoke()
            deferred.resolve(true)
        } fail {
            deferred.reject(it)
        }
        return deferred.promise
    }

    override fun persist(): Promise<Boolean, Exception> {
        assert(books != null)

        val deferred = deferred<Boolean, Exception>()
        task {
            Timber.d("begin save books %s", Thread.currentThread())

            // serialize books to private file
            serialize()
            // upload changes to dropbox
            metadata = client.files()
                    .uploadBuilder(remoteFilePath)
                    .withMode(WriteMode.OVERWRITE)
                    .uploadAndFinish(App.appContext.openFileInput(baseFileName))

            prefs.revision = metadata!!.rev
            deferred.resolve(true)
            Timber.d("end save books %s", Thread.currentThread())
        } fail {
            val ex = it
            Timber.d(it)
            deferred.reject(ex)
        }
        return deferred.promise
    }

// ----------------------------

    private fun fetchRemote(deferred: nl.komponents.kovenant.Deferred<Boolean, Exception>) {
        try {
            metadata = client.files()
                    .download(metadata!!.pathDisplay)
                    .download(App.appContext.openFileOutput(baseFileName, Context.MODE_PRIVATE))


            books = deserialize()
            prefs.revision = metadata!!.rev
            isInSync = true
            deferred.resolve(true)

        } catch (e: Exception) {
            Timber.d(e)
            deferred.reject(e)
        }
    }
}