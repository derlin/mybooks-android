package ch.derlin.mybooks.helpers

import androidx.appcompat.app.AppCompatDelegate
import ch.derlin.mybooks.R

/**
 * Created by Lin on 20.08.18.
 */
object ThemeHelper {

    enum class Theme {
        DEFAULT, LIGHT, DARK
    }

    private val themeToResourceId = listOf(
        Pair(Theme.DEFAULT, R.id.submenu_theme_default),
        Pair(Theme.LIGHT, R.id.submenu_theme_light),
        Pair(Theme.DARK, R.id.submenu_theme_dark)
    )

    fun Theme.toResource(): Int =
        themeToResourceId.first { it.first == this }.second

    fun Int.toTheme(): Theme =
        themeToResourceId.first { it.second == this }.first

    fun Theme.toAppCompatMode(): Int = when (this) {
        Theme.DEFAULT -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        Theme.LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
        Theme.DARK -> AppCompatDelegate.MODE_NIGHT_YES
    }

    fun applyTheme(newTheme: Theme? = null) {
        newTheme?.let { Preferences.currentTheme = newTheme }
        (newTheme ?: Preferences.currentTheme).let {
            AppCompatDelegate.setDefaultNightMode(it.toAppCompatMode())
        }
    }
}