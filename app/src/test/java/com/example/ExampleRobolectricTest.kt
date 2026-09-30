package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.DocumentType
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("LRmassDownload", appName)
    }

    @Test
    fun `test document type guessing`() {
        assertEquals(DocumentType.RS, DocumentType.guessFromFileName("rs_khatian_102.pdf"))
        assertEquals(DocumentType.CS, DocumentType.guessFromFileName("CS_porcha_33.jpg"))
        assertEquals(DocumentType.MOUZA_MAP, DocumentType.guessFromFileName("mouza_sheet_2.png"))
        assertEquals(DocumentType.MUTATION, DocumentType.guessFromFileName("namjari_order.pdf"))
    }
}
