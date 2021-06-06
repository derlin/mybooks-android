package ch.derlin.mybooks

/**
 * Created by Lin on 24.11.17.
 */
import android.app.Application
import android.content.Context
import nl.komponents.kovenant.Kovenant
import nl.komponents.kovenant.android.startKovenant
import nl.komponents.kovenant.android.stopKovenant
import nl.komponents.kovenant.buildDispatcher
import timber.log.Timber
import timber.log.Timber.DebugTree
import android.content.Intent
import ch.derlin.mybooks.helpers.Preferences
import ch.derlin.mybooks.helpers.ThemeHelper


class App : Application() {

    override fun onCreate() {
        super.onCreate()
        appContext = applicationContext
        ThemeHelper.applyTheme()
        // limit background threads to one to avoid
        // concurrency on account update
        Kovenant.context {
            workerContext.dispatcher = buildDispatcher {
                name = "Kovenant worker thread"
                concurrentTasks = 1
            }
        }
        startKovenant()

        Timber.plant(DebugTree()) // TODO
    }

    override fun onTerminate() {
        super.onTerminate()
        stopKovenant()
    }

    companion object {
        lateinit var appContext: Context
            private set
    }

}