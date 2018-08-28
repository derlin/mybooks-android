package ch.derlin.mybooks.persistence

import android.content.Context
import ch.derlin.mybooks.App
import ch.derlin.mybooks.Books
import ch.derlin.mybooks.DbxManager
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

    abstract fun fetchBooks(): Promise<Boolean, Exception>
    abstract fun persist(): Promise<Boolean, Exception>

    fun serialize(fout: FileOutputStream = App.appContext.openFileOutput(baseFileName, Context.MODE_PRIVATE)){
        fout.use { out ->
            out.write(gson.toJson(books).toByteArray())
        }
    }

    fun deserialize(fin: FileInputStream = App.appContext.openFileInput(baseFileName)): Books? {
        return gson.fromJson<Books>(BufferedReader(InputStreamReader(fin)), object : TypeToken<Books>() {}.getType())
    }

    fun removeAppFile(filename: String = baseFileName): Boolean {
        val localFile = File(App.appContext.filesDir.getAbsolutePath(), filename)
        return localFile.delete()
    }

    companion object {
        val instance: PersistenceManager
            get() = if (Preferences().dbxAccessToken != null)
                DbxManager else LocalManager
    }
}