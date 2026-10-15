package com.mi.onextbox.lsp

import android.content.Context
import android.net.Uri
import android.os.CancellationSignal

internal data class SoftwareUpdatePackageInfo(
    val name: String,
    val version: String,
    val type: Int,
    val bytes: Long,
    val checksum: String,
    val links: List<String>,
    val downloadLinks: List<String> = emptyList(),
    val linkFailures: List<SoftwareUpdateLinkFailure> = emptyList(),
)

internal object SoftwareUpdatePackageReader {
    fun read(context: Context, cancellation: CancellationSignal): List<SoftwareUpdatePackageInfo> {
        val columns = arrayOf("package_name", "version_name", "type", "size", "md5", "url", "active_url")
        return context.contentResolver.query(
            Uri.parse("content://com.oplus.ota/pkgList?no_notify=1"), columns, null, null, "_id ASC", cancellation,
        )?.use { cursor ->
            val result = arrayListOf<SoftwareUpdatePackageInfo>()
            while (result.size < 128 && cursor.moveToNext()) {
                cancellation.throwIfCanceled()
                val links = listOfNotNull(
                    SoftwareUpdateRules.validDownloadUrl(cursor.getString(6)),
                    SoftwareUpdateRules.validDownloadUrl(cursor.getString(5)),
                ).distinct()
                result += SoftwareUpdatePackageInfo(
                    cursor.getString(0).orEmpty(), cursor.getString(1).orEmpty(), cursor.getInt(2),
                    cursor.getLong(3).coerceAtLeast(0), cursor.getString(4).orEmpty(), links,
                )
            }
            result
        } ?: error("Updater package cache unavailable")
    }
}
