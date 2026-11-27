package com.hazia.chatelaine.ui.i18n

object ItStrings : Strings {
    override fun languageName(l: AppLanguage): String = when (l) {
        AppLanguage.EN -> "Inglese"
        AppLanguage.IT -> "Italiano"
    }
}
