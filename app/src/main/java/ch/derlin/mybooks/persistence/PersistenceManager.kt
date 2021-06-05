package ch.derlin.mybooks.persistence

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.core.app.ShareCompat
import androidx.core.content.FileProvider
import ch.derlin.mybooks.App
import ch.derlin.mybooks.Books
import ch.derlin.mybooks.R
import ch.derlin.mybooks.helpers.Preferences
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import nl.komponents.kovenant.Promise
import java.io.*

abstract class PersistenceManager {

    protected val gson =
            GsonBuilder()
                    .excludeFieldsWithoutExposeAnnotation()
                    .setPrettyPrinting()
                    .create()

    val baseFileName = "mybooks.json"

    abstract var books: Books?
    abstract val localFileExists: Boolean

    val isInitialised: Boolean
        get() = books != null

    abstract fun fetchBooks(): Promise<Boolean, Exception>
    abstract fun persist(): Promise<Boolean, Exception>

    fun serialize(fout: FileOutputStream = App.appContext.openFileOutput(baseFileName, Context.MODE_PRIVATE)) {
        fout.use { out ->
            out.write(gson.toJson(books).toByteArray())
        }
    }

    fun deserialize(fileInputStream: FileInputStream? = null): Books {
        return try {
            val fin = fileInputStream ?: App.appContext.openFileInput(baseFileName)
            gson.fromJson<Books>(BufferedReader(InputStreamReader(fin)), object : TypeToken<Books>() {}.type)
        } catch (e: Exception) {
            mutableMapOf()
        }
    }

    fun getAppFile(filename: String = baseFileName): File {
        return File(App.appContext.filesDir.absolutePath, filename)
    }

    fun removeAppFile(filename: String = baseFileName): Boolean {
        return getAppFile(filename).delete()
    }

    companion object {
        private var _instance: PersistenceManager? = null

        val instance: PersistenceManager
            get() {
                if (_instance == null) {
                    _instance = if (Preferences.dbxAccessToken != null) DbxManager() else LocalManager()
                }
                return _instance!!
            }


        fun invalidate() {
            _instance = null
        }

        fun Activity.shareAppFile() {
            val uri = FileProvider.getUriForFile(this, getString(R.string.file_provider_authority), instance.getAppFile())
            val shareIntent = ShareCompat.IntentBuilder.from(this)
                    .setStream(uri)
                    .intent
            // Provide read access
            shareIntent.data = uri
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            startActivity(Intent.createChooser(shareIntent, getString(R.string.chooser_title_export_file)))
        }
    }
}