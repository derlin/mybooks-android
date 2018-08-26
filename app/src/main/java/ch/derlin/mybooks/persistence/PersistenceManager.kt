package ch.derlin.mybooks.persistence

import ch.derlin.mybooks.Books
import ch.derlin.mybooks.DbxManager
import ch.derlin.mybooks.helpers.Preferences
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import nl.komponents.kovenant.Promise
import java.io.BufferedReader
import java.io.FileInputStream
import java.io.InputStreamReader

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

    protected fun deserialize(fin: FileInputStream) {
        books = gson.fromJson<Books>(BufferedReader(InputStreamReader(fin)), object : TypeToken<Books>() {}.getType())
    }

    companion object {
        val instance: PersistenceManager
            get() = if (Preferences().dbxAccessToken != null)
                DbxManager else LocalManager
    }
}