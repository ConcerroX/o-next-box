package com.mi.onextbox.lsp

internal object KeyboardBottomLiftRules {
    fun shouldRefresh(enabled: Boolean, raised: Boolean, gestures: Boolean,
                      portrait: Boolean, attached: Boolean, shown: Boolean): Boolean =
        enabled && raised && gestures && portrait && attached && shown

    fun containsHeightAnchors(bytes: ByteArray): Boolean = listOf(
        "registerKbPositionRaiseObserver failed", "kbPositionRaiseObserver onChange:",
    ).all { text ->
        val anchor = text.toByteArray(Charsets.UTF_8)
        (0..bytes.size - anchor.size).any { start ->
            bytes[start] == anchor[0] && anchor.indices.all { bytes[start + it] == anchor[it] }
        }
    }
}
