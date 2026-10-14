package com.doramabloom.app

import android.app.Activity
import android.app.Application
import android.content.res.Configuration
import android.content.res.Resources
import androidx.appcompat.app.AppCompatDelegate

/** Guarda o dado do app e aplica o modo (claro / escuro / sistema) escolhido nas configurações. */
class App : Application() {
    override fun onCreate() {
        super.onCreate()
        Store.init(this)
        ThemeMode.applyNight()
    }
}

object ThemeMode {
    /** Diz ao AppCompat qual modo usar (caixas de diálogo, janelas etc. acompanham). */
    fun applyNight() {
        AppCompatDelegate.setDefaultNightMode(
            when (Store.themeMode) {
                1 -> AppCompatDelegate.MODE_NIGHT_NO
                2 -> AppCompatDelegate.MODE_NIGHT_YES
                else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            }
        )
    }

    fun isDark(): Boolean = when (Store.themeMode) {
        1 -> false
        2 -> true
        else -> (Resources.getSystem().configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES
    }

    /** Chamar no começo de cada tela, antes de montar as views. */
    fun refresh(a: Activity) {
        Palette.dark = isDark()
        a.applyBarStyle()
    }

    fun set(mode: Int) {
        Store.themeMode = mode
        applyNight()
    }
}
