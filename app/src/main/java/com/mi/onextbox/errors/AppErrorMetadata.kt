package com.mi.onextbox.errors

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class AppErrorMetadata(
    val activity: String = "",
    val parentActivity: String = "",
    val parentProcess: String = "",
    val exceptionClass: String = "",
    val exceptionMessage: String = "",
    val throwFile: String = "",
    val throwClass: String = "",
    val throwMethod: String = "",
    val throwLine: Int = -1,
    val targetSdk: Int = 0,
    val minSdk: Int = 0,
    val systemApp: Boolean? = null,
    val device: String = "",
    val androidVersion: String = "",
    val sdk: Int = 0,
    val fingerprint: String = "",
    val abi: String = "",
    val traceStatus: String = "",
) {
    fun bounded(): AppErrorMetadata {
        fun clean(value: String, limit: Int = 512) = value.take(limit).replace('\u0000', ' ')
        return copy(
            activity = clean(activity), parentActivity = clean(parentActivity), parentProcess = clean(parentProcess),
            exceptionClass = clean(exceptionClass), exceptionMessage = clean(exceptionMessage, 2048),
            throwFile = clean(throwFile), throwClass = clean(throwClass), throwMethod = clean(throwMethod),
            throwLine = throwLine.coerceAtLeast(-1), targetSdk = targetSdk.coerceIn(0, 1000), minSdk = minSdk.coerceIn(0, 1000),
            device = clean(device), androidVersion = clean(androidVersion, 128), sdk = sdk.coerceIn(0, 1000),
            fingerprint = clean(fingerprint, 1024), abi = clean(abi, 128), traceStatus = clean(traceStatus, 32),
        )
    }

    fun location(): String = listOfNotNull(
        listOf(throwClass, throwMethod).filter { it.isNotBlank() }.joinToString(".").takeIf { it.isNotBlank() },
        throwFile.takeIf { it.isNotBlank() }?.let { file -> if (throwLine >= 0) "$file:$throwLine" else file },
    ).joinToString(" · ")

    fun reportFields(): List<Pair<String, String>> = buildList {
        fun field(key: String, value: String) { if (value.isNotBlank()) add(key to value) }
        field("Activity", activity)
        field("Parent activity", parentActivity)
        field("Parent process", parentProcess)
        field("Exception", exceptionClass)
        field("Message", exceptionMessage)
        field("Throw location", location())
        if (targetSdk > 0) add("Target SDK" to targetSdk.toString())
        if (minSdk > 0) add("Minimum SDK" to minSdk.toString())
        systemApp?.let { add("System app" to it.toString()) }
        field("Device", device)
        field("Android", androidVersion)
        if (sdk > 0) add("API" to sdk.toString())
        field("Build fingerprint", fingerprint)
        field("ABI", abi)
        field("Trace file", traceStatus)
    }
}

internal object AppErrorMetadataCodec {
    const val MAX_ENCODED = 64 * 1024
    private val json = Json { ignoreUnknownKeys = true }
    fun encode(value: AppErrorMetadata): String = json.encodeToString(value.bounded())
    fun decode(value: String?): AppErrorMetadata = if (value.isNullOrBlank() || value.length > MAX_ENCODED) AppErrorMetadata()
        else runCatching { json.decodeFromString<AppErrorMetadata>(value).bounded() }.getOrDefault(AppErrorMetadata())
}
