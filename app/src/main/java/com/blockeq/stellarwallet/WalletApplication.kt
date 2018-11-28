package com.blockeq.stellarwallet

import android.arch.lifecycle.ProcessLifecycleOwner
import android.support.multidex.MultiDexApplication
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.JsonArrayRequest
import com.android.volley.toolbox.Volley
import com.blockeq.stellarwallet.encryption.PRNGFixes
import com.blockeq.stellarwallet.helpers.Constants
import com.blockeq.stellarwallet.helpers.LocalStore
import com.blockeq.stellarwallet.helpers.WalletLifecycleListener
import com.blockeq.stellarwallet.models.ExchangeApiModel
import com.blockeq.stellarwallet.models.ExchangeMapper
import com.blockeq.stellarwallet.models.UserSession
import com.blockeq.stellarwallet.viewmodels.ExchangeRepository
import com.blockeq.stellarwallet.viewmodels.ExchangesRoomDatabase
import com.facebook.stetho.Stetho
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.squareup.leakcanary.LeakCanary
import org.bouncycastle.jce.provider.BouncyCastleProvider
import timber.log.Timber
import java.security.Provider
import java.security.Security

class WalletApplication : MultiDexApplication() {
    private val lifecycleListener: WalletLifecycleListener by lazy {
        WalletLifecycleListener()
    }

    companion object {
        private const val PRIVATE_MODE = 0
        private const val PREF_NAME = "com.blockeq.stellarwallet.PREFERENCE_FILE_KEY"

        // Use LocalStore for SharedPreferences
        lateinit var localStore: LocalStore

        var userSession = UserSession()

        var appReturnedFromBackground = false
    }

    override fun onCreate() {
        super.onCreate()

        //removing the default provider coming from Android SDK.
        Security.removeProvider("BC")
        Security.addProvider(BouncyCastleProvider() as Provider?)

        PRNGFixes.apply()

        setupLifecycleListener()

        val sharedPreferences = getSharedPreferences(PREF_NAME, PRIVATE_MODE)
        localStore = LocalStore(sharedPreferences, Gson())

        if (BuildConfig.DEBUG) {
            Stetho.initializeWithDefaults(this)
            Timber.plant(Timber.DebugTree())

            if (LeakCanary.isInAnalyzerProcess(this)) {
                // This process is dedicated to LeakCanary for heap analysis.
                // You should not init your app in this process.
                return
            }
            LeakCanary.install(this)
            // Normal app init code...
        }

        ExchangeRepository(this).getAllExchangeProviders(true)
    }

    private fun setupLifecycleListener() {
        ProcessLifecycleOwner.get().lifecycle
                .addObserver(lifecycleListener)
    }
}
