package ch.derlin.mybooks.helpers

import android.content.Context
import ch.derlin.mybooks.App


object Preferences {

    private val PREFERENCES_FILENAME = "ch.derlin.easypass.preferences"

    val sharedPrefs = App.appContext.getSharedPreferences(PREFERENCES_FILENAME, Context.MODE_PRIVATE)

    var dbxAccessToken: String?
        get() = sharedPrefs.getString("dbx_access-token", null)
        set(value) {
            sharedPrefs.edit().putString("dbx_access-token", value).commit()
        }

    var revision: String?
        get() = sharedPrefs.getString("revision", null)
        set(value) = sharedPrefs.edit().putString("revision", value).apply()

    var sortOrder: Int
        get() = App.appContext.resources.getIdentifier(
                sharedPrefs.getString("sortOrder", "submenu_sort_title_asc"),
                "id", App.appContext.packageName)
        set(value) = sharedPrefs.edit().putString("sortOrder", App.appContext.resources.getResourceName(value)).apply()

    var currentTheme: Int
        get() = App.appContext.resources.getIdentifier(
                sharedPrefs.getString("currentTheme", "submenu_theme_light"),
                "id", App.appContext.packageName)
        set(value) = sharedPrefs.edit().putString("currentTheme", App.appContext.resources.getResourceName(value)).apply()

    /** Keep track of the version to show changelog dialog on update */
    var versionCode: Int
        get() = sharedPrefs.getInt("version_code", 0)
        set(value) = sharedPrefs.edit().putInt("version_code", value).apply()

    /** Are the intro slides already displayed once ? */
    var introDone: Boolean
        get() = sharedPrefs.getBoolean("init_done", false)
        set(value) = sharedPrefs.edit().putBoolean("init_done", value).apply()
}