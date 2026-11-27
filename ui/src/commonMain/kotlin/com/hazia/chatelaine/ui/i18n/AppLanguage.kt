package com.hazia.chatelaine.ui.i18n

/** The languages the app is written in; [tag] is what Settings stores. */
enum class AppLanguage(val tag: String) {
    EN("en"),
    IT("it");

    companion object {
        /**
         * The owner's stored [choice] wins when it names a known language; otherwise the device decides:
         * a [deviceTag] starting with `it` gives Italian, anything else English.
         */
        fun resolve(choice: String?, deviceTag: String): AppLanguage =
            entries.firstOrNull { it.tag == choice }
                ?: if (deviceTag.lowercase().startsWith("it")) IT else EN
    }
}
