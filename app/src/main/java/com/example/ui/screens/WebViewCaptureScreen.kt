package com.example.ui.screens

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.net.Uri
import android.webkit.*
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.R
import com.example.data.DocumentType
import com.example.data.LocationRepository
import com.example.storage.SaveResult
import com.example.storage.StorageManager
import com.example.ui.components.CascadingLocationSelector
import com.example.ui.components.DocTypeSelector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.ByteArrayOutputStream
import java.net.URLDecoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class PortalBookmark(val name: String, val bnName: String, val url: String)

val LAND_PORTALS = listOf(
    PortalBookmark("ePorcha", "ই-পর্চা", "https://eporcha.gov.bd"),
    PortalBookmark("LD Tax", "ভূমি উন্নয়ন কর", "https://ldtax.gov.bd"),
    PortalBookmark("Mutation", "ই-নামজারি", "https://mutation.land.gov.bd"),
    PortalBookmark("DLRS", "ভূমি রেকর্ড ও জরিপ", "https://dlrs.gov.bd"),
    PortalBookmark("Land Ministry", "ভূমি মন্ত্রণালয়", "https://minland.gov.bd")
)

@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebViewCaptureScreen(
    locationRepo: LocationRepository,
    storageManager: StorageManager,
    selectedDivision: String,
    selectedDistrict: String,
    selectedUpazila: String,
    mouza: String,
    onDivisionSelected: (String) -> Unit,
    onDistrictSelected: (String) -> Unit,
    onUpazilaSelected: (String) -> Unit,
    onMouzaChanged: (String) -> Unit,
    onRecordSaved: () -> Unit,
    snackbarHostState: SnackbarHostState
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val okHttpClient = remember { OkHttpClient.Builder().followRedirects(true).build() }

    var webView: WebView? by remember { mutableStateOf(null) }
    var urlInput by remember { mutableStateOf("https://eporcha.gov.bd") }
    var currentUrl by remember { mutableStateOf("https://eporcha.gov.bd") }
    var canGoBack by remember { mutableStateOf(false) }
    var canGoForward by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var loadingProgress by remember { mutableIntStateOf(0) }

    // Save Dialog State
    var showSaveDialog by remember { mutableStateOf(false) }
    var capturedData by remember { mutableStateOf<ByteArray?>(null) }
    var capturedFileName by remember { mutableStateOf("") }
    var capturedDocType by remember { mutableStateOf(DocumentType.RS) }
    var capturedKhatianNo by remember { mutableStateOf("") }
    var capturedSourceUrl by remember { mutableStateOf("") }

    var isCapturing by remember { mutableStateOf(false) }

    BackHandler(enabled = canGoBack) {
        webView?.goBack()
    }

    // Helper to trigger save dialog from downloaded or captured data
    fun triggerSaveDialog(data: ByteArray, suggestedName: String, url: String) {
        capturedData = data
        capturedFileName = suggestedName
        capturedDocType = DocumentType.guessFromFileName(suggestedName)
        capturedSourceUrl = url
        showSaveDialog = true
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                // Bookmarks chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LAND_PORTALS.forEach { portal ->
                        AssistChip(
                            onClick = {
                                urlInput = portal.url
                                webView?.loadUrl(portal.url)
                            },
                            label = { Text("${portal.name} (${portal.bnName})") },
                            leadingIcon = {
                                Icon(Icons.Default.Public, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // URL Navigation Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    IconButton(
                        onClick = { webView?.goBack() },
                        enabled = canGoBack,
                        modifier = Modifier.testTag("browser_back")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }

                    IconButton(
                        onClick = { webView?.goForward() },
                        enabled = canGoForward,
                        modifier = Modifier.testTag("browser_forward")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Forward")
                    }

                    OutlinedTextField(
                        value = urlInput,
                        onValueChange = { urlInput = it },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("url_input"),
                        singleLine = true,
                        shape = RoundedCornerShape(24.dp),
                        trailingIcon = {
                            if (isLoading) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            } else {
                                IconButton(onClick = { webView?.reload() }) {
                                    Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                                }
                            }
                        }
                    )

                    Button(
                        onClick = {
                            var target = urlInput.trim()
                            if (!target.startsWith("http://") && !target.startsWith("https://")) {
                                target = "https://$target"
                            }
                            urlInput = target
                            webView?.loadUrl(target)
                        },
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier.testTag("go_button")
                    ) {
                        Text(stringResource(R.string.go))
                    }
                }

                if (isLoading) {
                    LinearProgressIndicator(
                        progress = { loadingProgress / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                    )
                }
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    coroutineScope.launch {
                        isCapturing = true
                        try {
                            webView?.let { view ->
                                val curUrl = view.url ?: "https://eporcha.gov.bd"
                                // Capture web page content or initiate download
                                view.evaluateJavascript(
                                    "(function() { return document.documentElement.outerHTML; })();"
                                ) { html ->
                                    val cleanedHtml = if (html != null && html.startsWith("\"") && html.endsWith("\"")) {
                                        // Unescape JS string
                                        try {
                                            com.google.gson.JsonParser.parseString(html).asString
                                        } catch (e: Exception) {
                                            html
                                        }
                                    } else html ?: "<html><body>No Content</body></html>"

                                    val time = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                                    val cleanMouza = if (mouza.isNotBlank()) "_${mouza.take(15).replace(" ", "_")}" else ""
                                    val fileName = "Khatian_${selectedUpazila}${cleanMouza}_$time.html"
                                    triggerSaveDialog(cleanedHtml.toByteArray(Charsets.UTF_8), fileName, curUrl)
                                }
                            }
                        } finally {
                            isCapturing = false
                        }
                    }
                },
                icon = { Icon(Icons.Default.Download, contentDescription = null) },
                text = { Text("Archive Page / Khatian") },
                modifier = Modifier.testTag("capture_fab")
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            loadWithOverviewMode = true
                            useWideViewPort = true
                            builtInZoomControls = true
                            displayZoomControls = false
                            allowFileAccess = true
                        }

                        webViewClient = object : WebViewClient() {
                            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                super.onPageStarted(view, url, favicon)
                                isLoading = true
                                url?.let {
                                    currentUrl = it
                                    urlInput = it
                                }
                                canGoBack = view?.canGoBack() ?: false
                                canGoForward = view?.canGoForward() ?: false
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                isLoading = false
                                url?.let {
                                    currentUrl = it
                                    urlInput = it
                                }
                                canGoBack = view?.canGoBack() ?: false
                                canGoForward = view?.canGoForward() ?: false
                            }
                        }

                        webChromeClient = object : WebChromeClient() {
                            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                super.onProgressChanged(view, newProgress)
                                loadingProgress = newProgress
                            }
                        }

                        setDownloadListener { downloadUrl, userAgent, contentDisposition, mimeType, contentLength ->
                            coroutineScope.launch(Dispatchers.IO) {
                                try {
                                    val req = Request.Builder()
                                        .url(downloadUrl)
                                        .addHeader("User-Agent", userAgent)
                                        .build()

                                    val resp = okHttpClient.newCall(req).execute()
                                    if (resp.isSuccessful) {
                                        val bytes = resp.body?.bytes()
                                        if (bytes != null && bytes.isNotEmpty()) {
                                            var guessedName = URLUtil.guessFileName(downloadUrl, contentDisposition, mimeType)
                                            if (guessedName.isBlank() || guessedName == "downloadfile") {
                                                guessedName = "LandRecord_${System.currentTimeMillis()}.${mimeType.substringAfterLast("/", "pdf")}"
                                            }
                                            withContext(Dispatchers.Main) {
                                                triggerSaveDialog(bytes, guessedName, downloadUrl)
                                            }
                                        }
                                    }
                                } catch (e: Exception) {
                                    withContext(Dispatchers.Main) {
                                        snackbarHostState.showSnackbar("Download error: ${e.localizedMessage}")
                                    }
                                }
                            }
                        }

                        loadUrl(currentUrl)
                        webView = this
                    }
                },
                update = { wv ->
                    webView = wv
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }

    // Save Confirmation Dialog
    if (showSaveDialog && capturedData != null) {
        val data = capturedData!!
        val hash = remember(data) { storageManager.calculateSha256(data) }
        val existingRecord = remember(hash) {
            storageManager.getAllRecords().firstOrNull { it.sha256.equals(hash, ignoreCase = true) }
        }

        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = {
                Text(
                    text = "Save Document to Archive",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (existingRecord != null) {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                Text(
                                    text = "Duplicate detected! Same SHA-256 already saved in ${existingRecord.upazila} (${existingRecord.docType.code}).",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = capturedFileName,
                        onValueChange = { capturedFileName = it },
                        label = { Text("File Name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = capturedKhatianNo,
                        onValueChange = { capturedKhatianNo = it },
                        label = { Text(stringResource(R.string.khatian_no)) },
                        placeholder = { Text("e.g. Khatian #142 / Plot #892") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    DocTypeSelector(
                        selectedDocType = capturedDocType,
                        onDocTypeSelected = { capturedDocType = it }
                    )

                    CascadingLocationSelector(
                        locationRepo = locationRepo,
                        selectedDivision = selectedDivision,
                        selectedDistrict = selectedDistrict,
                        selectedUpazila = selectedUpazila,
                        mouza = mouza,
                        onDivisionSelected = onDivisionSelected,
                        onDistrictSelected = onDistrictSelected,
                        onUpazilaSelected = onUpazilaSelected,
                        onMouzaChanged = onMouzaChanged
                    )

                    Text(
                        text = "SHA-256: ${hash.take(16)}... | Size: ${data.size / 1024} KB",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val result = storageManager.saveDocument(
                            division = selectedDivision,
                            district = selectedDistrict,
                            upazila = selectedUpazila,
                            mouza = mouza,
                            docType = capturedDocType,
                            khatianOrPlotNo = capturedKhatianNo,
                            rawFileName = capturedFileName,
                            data = data,
                            sourceUrl = capturedSourceUrl
                        )
                        showSaveDialog = false
                        when (result) {
                            is SaveResult.Success -> {
                                onRecordSaved()
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Saved: ${result.record.fileName} to ${result.record.upazila}")
                                }
                            }
                            is SaveResult.Duplicate -> {
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Skipped duplicate record (SHA-256 match)")
                                }
                            }
                            is SaveResult.Error -> {
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Error saving: ${result.message}")
                                }
                            }
                        }
                    },
                    modifier = Modifier.testTag("confirm_save_button")
                ) {
                    Text(if (existingRecord != null) "Save Anyway" else "Save to Archive")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
