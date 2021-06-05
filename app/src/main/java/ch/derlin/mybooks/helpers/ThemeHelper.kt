package ch.derlin.mybooks.helpers

import android.app.Activity
import ch.derlin.mybooks.R

/**
 * Created by Lin on 20.08.18.
 */
object ThemeHelper {

    val availableThemes: HashMap<Int, Int> = hashMapOf(
            R.id.submenu_theme_light to R.style.AppTheme,
            R.id.submenu_theme_dark to R.style.AppTheme_Dark
    )

    fun Activity.applyTheme() {
        Preferences.currentTheme.let { themeName ->
            availableThemes.get(themeName)?.let { setTheme(it) }
        }
    }
}