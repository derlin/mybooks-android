package ch.derlin.mybooks.helpers

import android.content.Context
import android.content.SharedPreferences
import ch.derlin.mybooks.App
import ch.derlin.mybooks.helpers.ThemeHelper.Theme


object Preferences {

    private const val PREFERENCES_FILENAME = "ch.derlin.easypass.preferences"
    private val sharedPrefs: SharedPreferences = App.appContext.getSharedPreferences(PREFERENCES_FILENAME, Context.MODE_PRIVATE)

    var dbxAccessToken: String?
        get() = sharedPrefs.getString("dbx_access-token", null)
        set(value) {
            sharedPrefs.edit().putString("dbx_access-token", value).apply()
        }

    var revision: String?
        get() = sharedPrefs.getString("revision", null)
        set(value) = sharedPrefs.edit().putString("revision", value).apply()

    var sortOrder: Int
        get() = App.appContext.resources.getIdentifier(
            sharedPrefs.getString("sortOrder", "submenu_sort_title_asc"),
            "id", App.appContext.packageName
        )
        set(value) = sharedPrefs.edit().putString("sortOrder", App.appContext.resources.getResourceName(value)).apply()

    var currentTheme: Theme
        get() = sharedPrefs.getString("currentTheme", null)?.let { Theme.valueOf(it) } ?: Theme.DEFAULT
        set(value) = sharedPrefs.edit().putString("currentTheme", value.name).apply()

    /** Keep track of the version to show changelog dialog on update */
    var versionCode: Int
        get() = sharedPrefs.getInt("version_code", 0)
        set(value) = sharedPrefs.edit().putInt("version_code", value).apply()

    /** Are the intro slides already displayed once ? */
    var introDone: Boolean
        get() = sharedPrefs.getBoolean("init_done", false)
        set(value) = sharedPrefs.edit().putBoolean("init_done", value).apply()
}