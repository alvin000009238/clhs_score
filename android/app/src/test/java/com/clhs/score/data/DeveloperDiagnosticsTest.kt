package com.clhs.score.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DeveloperDiagnosticsTest {
    @Test
    fun defaultClearableCategoriesDoNotIncludeSettings() {
        val clearableCategories = defaultClearableLocalDataCategories()

        assertFalse(LocalDataCategory.Settings in clearableCategories)
        assertTrue(LocalDataCategory.GradeCache in clearableCategories)
        assertTrue(LocalDataCategory.Session in clearableCategories)
        assertTrue(LocalDataCategory.WebView in clearableCategories)
        assertTrue(LocalDataCategory.Cache in clearableCategories)
        assertTrue(LocalDataCategory.NoBackupWebView in clearableCategories)
    }

    @Test
    fun storageEntryUsesStableCategoryMetadata() {
        val entry = StorageEntry(LocalDataCategory.Settings, bytes = 128L)

        assertEquals("settings", entry.key)
        assertEquals("設定資料", entry.label)
        assertFalse(entry.isClearable)
        assertEquals(128L, entry.bytes)
    }
}
