package ch.derlin.mybooks.persistence

import android.content.Context
import ch.derlin.mybooks.App
import ch.derlin.mybooks.Books
import ch.derlin.mybooks.DbxManager
import nl.komponents.kovenant.Promise

object LocalManager : PersistenceManager() {
    override var books: Books? = null

    override val localFileExists = true

    override fun fetchBooks(): Promise<Boolean, Exception> {
        books = deserialize()
        return Promise.of(true)
    }

    override fun persist(): Promise<Boolean, Exception> {
        assert(books != null)
        try {
            App.appContext.openFileOutput(DbxManager.baseFileName, Context.MODE_PRIVATE).use { out ->
                out.write(gson.toJson(DbxManager.books).toByteArray())
            }
            return Promise.of(true)
        } catch (e: Exception) {
            return Promise.ofFail(e)
        }
    }

}