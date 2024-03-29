package ch.derlin.mybooks.persistence

import ch.derlin.mybooks.Books
import nl.komponents.kovenant.Promise

class LocalManager : PersistenceManager() {
    override var books: Books? = null

    override val localFileExists = true

    override fun fetchBooks(): Promise<Boolean, Exception> {
        books = deserialize()
        return Promise.of(true)
    }

    override fun persist(): Promise<Boolean, Exception> {
        requireNotNull(books)
        return try {
            serialize()
            Promise.of(true)
        } catch (e: Exception) {
            Promise.ofFail(e)
        }
    }

}