package ch.derlin.mybooks.goodreads

import java.text.Normalizer

fun cleanTitleForSearchQuery(title: String) =
    // GoodReads doesn't like commas, semi-colon... or parenthesis in the search URL, so remove all
    title.lowercase()
        .removeContentInParentheses()
        // .removeDiacritics() this usually worsen the results
        .replaceSpecialChars()
        .trimSpaces()

fun cleanAuthorForSearchQuery(author: String) =
    // GoodReads doesn't like initials
    author.removeInitials()
        .removeSeparators()
        .trimSpaces()

internal fun String.removeDiacritics() =
    Normalizer.normalize(this, Normalizer.Form.NFD)
        .replace("\\p{Mn}+".toRegex(), "")
        .replace("Œ", "Oe")
        .replace("œ", "oe")

internal fun String.removeInitials() =
    replace("(\\b|\\W)[A-Z]{1,2}\\.(\\W|$)".toRegex(), "$1$2")

internal fun String.removeContentInParentheses() =
    replace("(\\(.*\\))".toRegex(), "")

internal fun String.replaceSpecialChars() = this
    .replace(",", "")
    .replace(":", " ")
    .replace(" & ", " and ")

internal fun String.removeSeparators() = this
    .replace("[,:;&]|(and)".toRegex(), " ")

internal fun String.trimSpaces() =
    replace("\\s+".toRegex(), " ").trim()