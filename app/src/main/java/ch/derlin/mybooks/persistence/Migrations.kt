package ch.derlin.mybooks.persistence

import ch.derlin.mybooks.Books

object Migrations {

    fun Books.performMigrations(): Books = this
        .M001_changeNormalizedKey()


    private fun Books.M001_changeNormalizedKey(): Books {
        // We changed the normalization algorithm, so recompute the keys if needed
        val needsMigration = any { (key, book) -> key != book.normalizedKey }

        return if (needsMigration) {
            entries.associateTo(mutableMapOf()) { Pair(it.value.normalizedKey, it.value) }
        } else {
            this
        }
    }

}