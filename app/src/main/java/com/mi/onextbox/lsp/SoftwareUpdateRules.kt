package com.mi.onextbox.lsp

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull

internal object SoftwareUpdateRules {
    private val autoDownloadActions = setOf(
        "oplus.intent.action.OTA_AUTO_DOWNLOAD", "oplus.intent.action.OTA_PUSH_AUTO_DOWNLOAD",
    )
    private const val NIGHT_INSTALL = "oplus.intent.action.OTA_DELAY_AUTO_INSTALL"

    fun blocksAction(action: String?, disableDownload: Boolean, disableNightInstall: Boolean): Boolean =
        (disableDownload && action in autoDownloadActions) || (disableNightInstall && action == NIGHT_INSTALL)

    fun blocksMessage(what: Int, nightInstall: Boolean, disableDownload: Boolean, disableNightInstall: Boolean): Boolean =
        (disableDownload && what == 3001) ||
            (disableNightInstall && (what == 6001 || (nightInstall && what in 6002..6003)))

    fun maskRootReport(text: String): String = maskSecurityReport(text, hideRoot = true, hideBootloader = false)

    fun maskSecurityReport(text: String, hideRoot: Boolean, hideBootloader: Boolean): String {
        if (!hideRoot && !hideBootloader) return text
        var remaining = 8192
        fun status(item: JsonPrimitive, value: Boolean): JsonPrimitive = when {
            item.isString && item.content in setOf("0", "1") -> JsonPrimitive(if (value) "1" else "0")
            item.isString && item.content in setOf("true", "false") -> JsonPrimitive(value.toString())
            item.booleanOrNull != null -> JsonPrimitive(value)
            !item.isString && item.content in setOf("0", "1") -> JsonPrimitive(if (value) 1 else 0)
            else -> item
        }
        fun rewrite(value: JsonElement, depth: Int): JsonElement {
            require(depth <= 32 && --remaining >= 0)
            return when (value) {
                is JsonObject -> JsonObject(value.mapValues { (key, item) ->
                    when {
                        key in setOf("attestation", "attestationInfo", "certificate", "signature", "signedPayload") -> item
                        hideRoot && key == "isRooted" && item is JsonPrimitive -> status(item, false)
                        hideBootloader && key == "isLocked" && item is JsonPrimitive -> status(item, true)
                        hideBootloader && key in setOf("isUnlocked", "isUnlock") && item is JsonPrimitive -> status(item, false)
                        else -> rewrite(item, depth + 1)
                    }
                })
                is JsonArray -> JsonArray(value.map { rewrite(it, depth + 1) })
                else -> value
            }
        }
        return rewrite(Json.parseToJsonElement(text), 0).toString()
    }

    fun validDownloadUrl(value: String?): String? = value?.trim()?.takeIf { text ->
        runCatching {
            val uri = java.net.URI(text)
            uri.scheme?.lowercase() in setOf("http", "https") && !uri.host.isNullOrBlank() && uri.userInfo == null
        }.getOrDefault(false)
    }
}
