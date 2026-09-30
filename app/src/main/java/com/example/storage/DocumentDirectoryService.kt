package com.example.storage

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import java.io.File
import java.io.FileInputStream
import java.io.InputStream

class DocumentDirectoryService(private val context: Context) {
    companion object {
        private const val PREFS_NAME = "land_archive_saf_prefs"
        private const val KEY_TREE_URI = "selected_tree_uri"
        private const val KEY_DISPLAY_NAME = "selected_tree_display_name"
        private const val TAG = "DocumentDirService"
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getSelectedTreeUri(): Uri? {
        val uriStr = prefs.getString(KEY_TREE_URI, null) ?: return null
        return try {
            Uri.parse(uriStr)
        } catch (e: Exception) {
            null
        }
    }

    fun getSelectedDirectoryName(): String {
        return prefs.getString(KEY_DISPLAY_NAME, "Default App Storage") ?: "Default App Storage"
    }

    fun isDirectorySelected(): Boolean {
        val uri = getSelectedTreeUri() ?: return false
        return try {
            val docFile = DocumentFile.fromTreeUri(context, uri)
            docFile != null && docFile.exists() && docFile.canWrite()
        } catch (e: Exception) {
            Log.w(TAG, "Error checking tree URI validity: ${e.message}")
            false
        }
    }

    fun saveSelectedDirectory(uri: Uri): Boolean {
        return try {
            // Take persistable URI permission so permission is retained across reboots
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            try {
                context.contentResolver.takePersistableUriPermission(uri, flags)
            } catch (e: SecurityException) {
                Log.w(TAG, "Failed to take persistable URI permission: ${e.message}")
            }

            val docFile = DocumentFile.fromTreeUri(context, uri)
            val displayName = docFile?.name ?: uri.lastPathSegment ?: "Custom Storage Folder"

            prefs.edit()
                .putString(KEY_TREE_URI, uri.toString())
                .putString(KEY_DISPLAY_NAME, displayName)
                .apply()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save selected directory: ${e.message}", e)
            false
        }
    }

    fun clearSelectedDirectory() {
        getSelectedTreeUri()?.let { uri ->
            try {
                val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                context.contentResolver.releasePersistableUriPermission(uri, flags)
            } catch (e: Exception) {
                // Ignore
            }
        }
        prefs.edit().clear().apply()
    }

    fun getRootDocumentFile(): DocumentFile? {
        val uri = getSelectedTreeUri() ?: return null
        return try {
            val file = DocumentFile.fromTreeUri(context, uri)
            if (file != null && file.exists() && file.canWrite()) file else null
        } catch (e: Exception) {
            Log.e(TAG, "Error obtaining root DocumentFile: ${e.message}")
            null
        }
    }

    fun getOrCreateSubdirectory(vararg folderPath: String): DocumentFile? {
        var current = getRootDocumentFile() ?: return null
        for (segment in folderPath) {
            val cleanSegment = segment.replace(Regex("[\\\\/:*?\"<>|]"), "_").trim()
            if (cleanSegment.isEmpty()) continue
            val existing = current.findFile(cleanSegment)
            current = if (existing != null && existing.isDirectory) {
                existing
            } else {
                current.createDirectory(cleanSegment) ?: return null
            }
        }
        return current
    }

    fun saveDocumentFile(
        folderPath: List<String>,
        fileName: String,
        mimeType: String = "application/pdf",
        data: ByteArray
    ): DocumentFile? {
        return try {
            val dir = getOrCreateSubdirectory(*folderPath.toTypedArray()) ?: return null
            val cleanFileName = fileName.replace(Regex("[\\\\/:*?\"<>|]"), "_").trim()
            
            // If file already exists with same name, find or delete and recreate
            val existing = dir.findFile(cleanFileName)
            if (existing != null && existing.isFile) {
                existing.delete()
            }

            val newFile = dir.createFile(mimeType, cleanFileName) ?: return null
            context.contentResolver.openOutputStream(newFile.uri)?.use { out ->
                out.write(data)
                out.flush()
            }
            newFile
        } catch (e: Exception) {
            Log.e(TAG, "Error saving document file to DocumentDirectory: ${e.message}", e)
            null
        }
    }

    fun saveMasterZipFile(
        zipSourceFile: File,
        destinationFileName: String = zipSourceFile.name
    ): DocumentFile? {
        return try {
            val root = getRootDocumentFile() ?: return null
            val cleanName = destinationFileName.replace(Regex("[\\\\/:*?\"<>|]"), "_").trim()
            
            val existing = root.findFile(cleanName)
            if (existing != null && existing.isFile) {
                existing.delete()
            }

            val newZipDoc = root.createFile("application/zip", cleanName) ?: return null
            context.contentResolver.openOutputStream(newZipDoc.uri)?.use { outStream ->
                FileInputStream(zipSourceFile).use { inStream ->
                    inStream.copyTo(outStream)
                    outStream.flush()
                }
            }
            newZipDoc
        } catch (e: Exception) {
            Log.e(TAG, "Error saving master zip to DocumentDirectory: ${e.message}", e)
            null
        }
    }
}
