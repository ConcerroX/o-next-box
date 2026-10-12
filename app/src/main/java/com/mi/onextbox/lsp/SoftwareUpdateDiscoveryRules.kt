package com.mi.onextbox.lsp

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull

internal object SoftwareUpdateDiscoveryRules {
    const val NO_OTA_UPDATE = 2001
    const val NO_VERSION_RESULT = 5

    fun blocksOtaMessage(what: Int, enabled: Boolean): Boolean = enabled && what == 1004

    fun blocksVersionResult(query: String?, enabled: Boolean): Boolean = enabled && query == "query_version_update"

    fun blocksOpexTask(task: Int, enabled: Boolean): Boolean = enabled && task == 10

    fun hidesOpexInfo(method: String?, status: Int?, enabled: Boolean): Boolean =
        enabled && method == "getOpexUpdateInfo" && status in setOf(-1, 0)

    fun disableMergedOpex(text: String, path: String?, enabled: Boolean): String {
        if (!enabled || path != "/update/v6") return text
        val root = Json.parseToJsonElement(text) as? JsonObject ?: return text
        val opex = root["opex"] as? JsonObject ?: return text
        if ((opex["check"] as? JsonPrimitive)?.booleanOrNull == null) return text
        return JsonObject(root + ("opex" to JsonObject(opex + ("check" to JsonPrimitive(false))))).toString()
    }
}
