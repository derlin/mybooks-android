package derlin.ch.mybooks

import android.content.Context


class Preferences(context: Context = App.appContext) {

    private val PREFERENCES_FILENAME = "ch.derlin.easypass.preferences"

    val sharedPrefs = context.getSharedPreferences(PREFERENCES_FILENAME, Context.MODE_PRIVATE)

    var dbxAccessToken: String?
        get() = sharedPrefs.getString("dbx_access-token", null)
        set(value) { sharedPrefs.edit().putString("dbx_access-token", value).commit() }

    var revision: String?
        get() = sharedPrefs.getString("revision", null)
        set(value) = sharedPrefs.edit().putString("revision", value).apply()

    var sortOrder: Int
        get() = App.appContext.resources.getIdentifier(
                sharedPrefs.getString("sortOrder", "submenu_sort_title_asc"),
                "id", App.appContext.packageName)
        set(value) = sharedPrefs.edit().putString("sortOrder", App.appContext.resources.getResourceName(value)).apply()

}