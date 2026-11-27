package com.hazia.chatelaine.ui.i18n

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Every text the app shows. A screen task adds its keys here and to both [EnStrings] and [ItStrings],
 * so a missing translation does not compile.
 */
interface Strings {
    /** A language's name, written in this language. */
    fun languageName(l: AppLanguage): String
}

fun stringsFor(l: AppLanguage): Strings = when (l) {
    AppLanguage.EN -> EnStrings
    AppLanguage.IT -> ItStrings
}

val LocalStrings = staticCompositionLocalOf<Strings> { EnStrings }
