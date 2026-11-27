package com.hazia.chatelaine.ui

import com.hazia.chatelaine.ui.i18n.AppLanguage
import kotlin.test.Test
import kotlin.test.assertEquals

/** The app's language: the owner's choice in Settings wins; with none, Italian on an Italian device and English elsewhere. */
class AppLanguageTest {

    @Test
    fun noChoiceFollowsAnItalianDevice() {
        assertEquals(AppLanguage.IT, AppLanguage.resolve(null, "it-IT"))
    }

    @Test
    fun noChoiceOtherDeviceIsEnglish() {
        assertEquals(AppLanguage.EN, AppLanguage.resolve(null, "de-DE"))
    }

    @Test
    fun choiceBeatsTheDevice() {
        assertEquals(AppLanguage.EN, AppLanguage.resolve("en", "it-IT"))
    }

    @Test
    fun unknownChoiceFollowsTheDevice() {
        assertEquals(AppLanguage.IT, AppLanguage.resolve("fr", "it"))
    }
}
