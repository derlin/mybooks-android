package ch.derlin.mybooks

/**
 * Created by Lin on 24.11.17.
 */
import android.app.Application
import android.content.Context
import ch.derlin.mybooks.helpers.ThemeHelper
import nl.komponents.kovenant.Kovenant
import nl.komponents.kovenant.android.startKovenant
import nl.komponents.kovenant.android.stopKovenant
import nl.komponents.kovenant.buildDispatcher
import timber.log.Timber
import timber.log.Timber.DebugTree


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