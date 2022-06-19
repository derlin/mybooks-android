package ch.derlin.mybooks.persistence

import android.content.Context
import ch.derlin.mybooks.App
import ch.derlin.mybooks.Books
import ch.derlin.mybooks.R
import ch.derlin.mybooks.helpers.NetworkStatus
import ch.derlin.mybooks.helpers.Preferences
import ch.derlin.mybooks.sanitize
import com.dropbox.core.DbxRequestConfig
import com.dropbox.core.oauth.DbxCredential
import com.dropbox.core.v2.DbxClientV2
import com.dropbox.core.v2.files.FileMetadata
import com.dropbox.core.v2.files.GetMetadataErrorException
import com.dropbox.core.v2.files.WriteMode
import nl.komponents.kovenant.Promise
import nl.komponents.kovenant.deferred
import nl.komponents.kovenant.task
import timber.log.Timber


/**
 * Created by Lin on 24.11.17.
 */

class DbxManager : PersistenceManager() {

    companion object {
        fun requestConfig(): DbxRequestConfig = DbxRequestConfig
                .newBuilder(App.appContext.getString(R.string.dbx_request_config_name)).build()
    }

    private val remoteFilePath = "/${baseFileName}"

    private var metadata: FileMetadata? = null

    override var books: Books? = null
    override val localFileExists: Boolean
        get() = Preferences.revision != null

    private val client: DbxClientV2 by lazy {
        val token = Preferences.dbxAccessToken
        Timber.d("Dropbox token ?? client created")
        DbxClientV2(requestConfig(), DbxCredential.Reader.readFully(token))
    }

    var isInSync = false
        private set

    override fun canEdit() = NetworkStatus.isInternetAvailable()

    fun removeLocalFile(): Boolean {
        val ok = removeAppFile()
        Timber.d("""removed local file ? $ok""")
        Preferences.revision = null
        return ok
    }

    override fun fetchBooks(): Promise<Boolean, Exception> {
        val deferred = deferred<Boolean, Exception>()
        task {
            try {
                metadata = client.files().getMetadata(remoteFilePath) as FileMetadata
                isInSync = metadata?.rev.equals(Preferences.revision)

                if (isInSync && localFileExists) {
                    books = deserialize()
                    deferred.resolve(true)
                } else {
                    fetchRemote(deferred)
                }
            } catch (e: GetMetadataErrorException) {
                // session does not exist
                Preferences.revision = null
                books = mutableMapOf()
                deferred.resolve(isInSync)
            } // TODO: catch InvalidAccessTokenException and set token to null in prefs
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
            Preferences.dbxAccessToken = null
            Preferences.revision = null
            client.auth().tokenRevoke()
            deferred.resolve(true)
        } fail {
            deferred.reject(it)
        }
        return deferred.promise
    }

    override fun persist(): Promise<Boolean, Exception> {
        requireNotNull(books)

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

            Preferences.revision = metadata!!.rev
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
            Preferences.revision = metadata!!.rev
            isInSync = true
            deferred.resolve(true)

        } catch (e: Exception) {
            Timber.d(e)
            deferred.reject(e)
        }
    }
}