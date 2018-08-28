package ch.derlin.mybooks.persistence

import android.content.Context
import ch.derlin.mybooks.App
import ch.derlin.mybooks.Books
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
        try {
            val fin = fileInputStream ?: App.appContext.openFileInput(baseFileName)
            return gson.fromJson<Books>(BufferedReader(InputStreamReader(fin)), object : TypeToken<Books>() {}.getType())
        } catch (e: Exception) {
            return mutableMapOf()
        }
    }

    fun removeAppFile(filename: String = baseFileName): Boolean {
        val localFile = File(App.appContext.filesDir.getAbsolutePath(), filename)
        return localFile.delete()
    }

    companion object {
        private var _instance: PersistenceManager? = null

        val instance: PersistenceManager
            get() {
                if (_instance == null) {
                    _instance = if (Preferences().dbxAccessToken != null) DbxManager() else LocalManager()
                }
                return _instance!!
            }


        fun invalidate() {
            _instance = null
        }
    }
}