package com.hazia.chatelaine.ui.i18n

/** The languages the app is written in; [tag] is what Settings stores. */
enum class AppLanguage(val tag: String) {
    EN("en"),
    IT("it");

    companion object {
        private val italianCodes = setOf("it", "ita")

        /**
         * The owner's stored [choice] wins when it names a known language (in any case); otherwise the device decides:
         * Italian when the [deviceTag]'s first subtag (split on `-` or `_`) is `it` or `ita`, English for anything else.
         */
        fun resolve(choice: String?, deviceTag: String): AppLanguage =
            entries.firstOrNull { it.tag.equals(choice, ignoreCase = true) }
                ?: if (deviceTag.split('-', '_').first().lowercase() in italianCodes) IT else EN
    }
}
