package ch.derlin.mybooks

import android.content.Context
import com.dropbox.core.DbxRequestConfig
import com.dropbox.core.v2.DbxClientV2
import com.dropbox.core.v2.files.FileMetadata
import com.dropbox.core.v2.files.GetMetadataErrorException
import com.dropbox.core.v2.files.WriteMode
import com.google.gson.reflect.TypeToken
import ch.derlin.mybooks.helpers.Preferences
import ch.derlin.mybooks.persistence.PersistenceManager
import nl.komponents.kovenant.Promise
import nl.komponents.kovenant.deferred
import nl.komponents.kovenant.task
import timber.log.Timber
import java.io.*


/**
 * Created by Lin on 24.11.17.
 */

object DbxManager: PersistenceManager() {

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
        val localFile = File(App.appContext.filesDir.getAbsolutePath(), baseFileName)
        val ok = localFile.delete()
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
                    loadCachedFile()
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

    override fun persist(): Promise<Boolean, Exception> {
        assert(books != null)

        val deferred = deferred<Boolean, Exception>()
        task {
            Timber.d("begin save books %s", Thread.currentThread())

            // serialize books to private file
            val serialized = gson.toJson(books)
            App.appContext.openFileOutput(baseFileName, Context.MODE_PRIVATE).use { out ->
                out.write(serialized.toByteArray())
            }

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

            deserialize(App.appContext.openFileInput(baseFileName)) //, metadata?.pathDisplay)
            prefs.revision = metadata!!.rev
            isInSync = true
            deferred.resolve(true)

        } catch (e: Exception) {
            Timber.d(e)
            deferred.reject(e)
        }
    }

    private fun loadCachedFile() {
        deserialize(App.appContext.openFileInput(baseFileName))
        Timber.d("loaded cached file: rev=%s", prefs.revision)
    }
}