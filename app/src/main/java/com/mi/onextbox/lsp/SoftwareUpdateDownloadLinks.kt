package com.mi.onextbox.lsp

import java.net.URI

internal enum class SoftwareUpdateLinkProblem { Authentication, Network, Timeout, Server, InvalidResponse }
internal data class SoftwareUpdateLinkFailure(val problem: SoftwareUpdateLinkProblem, val code: Int? = null)
internal data class SoftwareUpdateResolvedLink(val url: String? = null, val failure: SoftwareUpdateLinkFailure? = null)
internal data class SoftwareUpdateLinkResponse(
    val status: Int,
    val location: String? = null,
    val mediaType: String? = null,
    val totalBytes: Long? = null,
    val prefix: ByteArray = byteArrayOf(),
    val serverCode: Int? = null,
)

internal object SoftwareUpdateDownloadLinks {
    fun isCheckUrl(value: String): Boolean = runCatching {
        URI(value).path.trimEnd('/').equals("/downloadCheck", ignoreCase = true)
    }.getOrDefault(false)

    // Device credentials stay on official check endpoints, never on CDN hops.
    fun trustedUrl(value: String): Boolean = runCatching {
        val uri = URI(value)
        val host = uri.host?.lowercase().orEmpty()
        value.length <= 65_536 && uri.scheme.equals("https", ignoreCase = true) && uri.userInfo == null &&
            uri.fragment == null && uri.port in setOf(-1, 443) &&
            listOf("allawntech.com", "allawnfs.com").any { host == it || host.endsWith(".$it") }
    }.getOrDefault(false)

    fun trustedCheckUrl(value: String): Boolean = trustedUrl(value) && isCheckUrl(value) &&
        URI(value).host.lowercase().matches(Regex("component-ota(?:-[a-z0-9]+)*\\.allawntech\\.com"))

    fun packageResponse(response: SoftwareUpdateLinkResponse, expectedBytes: Long): Boolean {
        val type = response.mediaType.orEmpty().lowercase()
        val signature = response.prefix.take(4).toByteArray()
        val binary = signature.contentEquals(byteArrayOf(0x50, 0x4b, 0x03, 0x04)) ||
            signature.contentEquals("CrAU".toByteArray(Charsets.US_ASCII))
        return response.status in setOf(200, 206) && expectedBytes > 0 && response.totalBytes == expectedBytes &&
            !type.contains("json") && !type.startsWith("text/") && binary
    }

    fun resolve(value: String, expectedBytes: Long, probe: (String, Boolean) -> SoftwareUpdateLinkResponse): SoftwareUpdateResolvedLink {
        fun failed(problem: SoftwareUpdateLinkProblem, code: Int? = null) =
            SoftwareUpdateResolvedLink(failure = SoftwareUpdateLinkFailure(problem, code))
        if (!trustedCheckUrl(value)) return failed(SoftwareUpdateLinkProblem.InvalidResponse)
        var current = value
        val visited = hashSetOf<String>()
        repeat(6) {
            if (!trustedUrl(current) || !visited.add(current)) return failed(SoftwareUpdateLinkProblem.InvalidResponse)
            val authenticate = trustedCheckUrl(current)
            if (isCheckUrl(current) && !authenticate) return failed(SoftwareUpdateLinkProblem.InvalidResponse)
            val response = probe(current, authenticate)
            if (response.status in setOf(301, 302, 303, 307, 308)) {
                val location = response.location ?: return failed(SoftwareUpdateLinkProblem.InvalidResponse)
                current = runCatching { URI(current).resolve(location).toString() }.getOrNull()
                    ?: return failed(SoftwareUpdateLinkProblem.InvalidResponse)
            } else if (packageResponse(response, expectedBytes)) {
                // A URL requiring request headers cannot be exported as a browser download link.
                if (authenticate && !packageResponse(probe(current, false), expectedBytes)) {
                    return failed(SoftwareUpdateLinkProblem.Authentication)
                }
                return SoftwareUpdateResolvedLink(url = current)
            } else {
                return failed(if (response.serverCode != null || response.status !in setOf(200, 206)) {
                    SoftwareUpdateLinkProblem.Server
                } else SoftwareUpdateLinkProblem.InvalidResponse, response.serverCode ?: response.status.takeIf { it !in setOf(200, 206) })
            }
        }
        return failed(SoftwareUpdateLinkProblem.InvalidResponse)
    }
}
