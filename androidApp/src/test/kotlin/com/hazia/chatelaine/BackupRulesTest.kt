package com.hazia.chatelaine

import com.hazia.chatelaine.data.DEVICE_ID_DIR
import com.hazia.chatelaine.data.app.LOCAL_DIR
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/** R59: the device-id file never reaches Android's cloud backup or a device transfer, so a restored phone is not a second copy of this one. */
class BackupRulesTest {

    private fun parse(path: String): Element =
        DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true }.newDocumentBuilder().parse(File(path)).documentElement

    private fun elements(parent: Element): List<Element> =
        (0 until parent.childNodes.length).map { parent.childNodes.item(it) }.filterIsInstance<Element>()

    private fun excluded(parent: Element): List<String> =
        elements(parent).filter { it.tagName == "exclude" && it.getAttribute("domain") == "file" }.map { it.getAttribute("path") }

    @Test
    fun deviceIdFileIsExcludedFromBackup() {
        val extraction = parse("src/main/res/xml/data_extraction_rules.xml")
        assertEquals("data-extraction-rules", extraction.tagName)
        for (section in listOf("cloud-backup", "device-transfer")) {
            val paths = excluded(elements(extraction).first { it.tagName == section })
            assertTrue("$section excludes the device id directory: $paths", paths.any { it.trimEnd('/') == DEVICE_ID_DIR })
        }

        val application = elements(parse("src/main/AndroidManifest.xml")).first { it.tagName == "application" }
        assertEquals("@xml/data_extraction_rules", application.getAttributeNS("http://schemas.android.com/apk/res/android", "dataExtractionRules"))
        assertFalse(File("src/main/res/xml/backup_rules.xml").exists())
        assertFalse(application.hasAttributeNS("http://schemas.android.com/apk/res/android", "fullBackupContent"))
    }

    /** The local settings (folders picked on this phone, the last backup's time) stay on this phone too. */
    @Test
    fun localSettingsAreExcludedFromBackup() {
        val extraction = parse("src/main/res/xml/data_extraction_rules.xml")
        for (section in listOf("cloud-backup", "device-transfer")) {
            val paths = excluded(elements(extraction).first { it.tagName == section })
            assertTrue("$section excludes the local settings directory: $paths", paths.any { it.trimEnd('/') == LOCAL_DIR })
        }
    }
}
