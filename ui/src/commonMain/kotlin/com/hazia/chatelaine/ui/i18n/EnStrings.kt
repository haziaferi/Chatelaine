package com.hazia.chatelaine.ui.i18n

object EnStrings : Strings {
    override fun languageName(l: AppLanguage): String = when (l) {
        AppLanguage.EN -> "English"
        AppLanguage.IT -> "Italian"
    }
}
