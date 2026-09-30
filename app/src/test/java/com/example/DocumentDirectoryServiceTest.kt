package com.example

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.example.storage.DocumentDirectoryService
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DocumentDirectoryServiceTest {

    private lateinit var context: Context
    private lateinit var service: DocumentDirectoryService

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        service = DocumentDirectoryService(context)
        service.clearSelectedDirectory()
    }

    @Test
    fun `default directory state is unselected`() {
        assertNull(service.getSelectedTreeUri())
        assertEquals("Default App Storage", service.getSelectedDirectoryName())
        assertFalse(service.isDirectorySelected())
    }

    @Test
    fun `clearSelectedDirectory resets preferences`() {
        val dummyUri = Uri.parse("content://com.android.externalstorage.documents/tree/primary%3ADownload")
        service.saveSelectedDirectory(dummyUri)
        
        service.clearSelectedDirectory()
        assertNull(service.getSelectedTreeUri())
        assertEquals("Default App Storage", service.getSelectedDirectoryName())
    }
}
