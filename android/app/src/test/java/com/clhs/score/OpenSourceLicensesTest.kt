package com.clhs.score

import com.clhs.score.ui.buildThirdPartyLicenses
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OpenSourceLicensesTest {
    @Test
    fun everyPublishedComponentHasOneLicenseEntry() {
        val licenses = buildThirdPartyLicenses("Outfit license")

        assertEquals(27, licenses.size)
        assertEquals(licenses.size, licenses.map { it.componentName }.distinct().size)
        assertTrue(licenses.all { it.licenseName.isNotBlank() && it.licenseText.isNotBlank() })
        assertTrue(licenses.none { it.componentName == "Firebase Analytics" })
    }
}
