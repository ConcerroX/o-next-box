package com.mi.onextbox.lsp

internal object CarrierNameRules {
    const val MAX_CODE_POINTS = 32
    fun normalize(value: String?): String {
        val text = buildString {
            value.orEmpty().codePoints().forEach { code ->
                when {
                    code == 10 || code == 13 || code == 9 || code == 0x2028 || code == 0x2029 -> append(' ')
                    Character.isISOControl(code) || code in 0x202a..0x202e || code in 0x2066..0x2069 -> Unit
                    code in 0xd800..0xdfff -> Unit
                    else -> appendCodePoint(code)
                }
            }
        }.trim()
        return if (text.codePointCount(0, text.length) <= MAX_CODE_POINTS) text
        else text.substring(0, text.offsetByCodePoints(0, MAX_CODE_POINTS)).trimEnd()
    }

    fun replacement(enabled: Boolean, name: String?, slot: Int, simState: Int, inService: Boolean,
        airplane: Boolean): String? = normalize(name).takeIf {
        enabled && slot in 0..1 && simState == 5 && inService && !airplane && it.isNotEmpty()
    }

    data class Registration(val voice: Int, val data: Int?, val emergencyOnly: Boolean = false) {
        val inService: Boolean get() = !emergencyOnly && (voice == 0 || data == 0)
    }

    /** A missing keyguard cache entry is not proof that a non-data subscription has no service. */
    fun registration(cached: Registration?, current: Registration?): Registration? = current ?: cached

    fun finalName(original: CharSequence?, alias: String?, appliedRawAlias: String?, nativeEmergencyOnly: Boolean): CharSequence? {
        if (nativeEmergencyOnly || original.isNullOrEmpty() || alias.isNullOrEmpty()) return original
        // Keep native RAT / Wi-Fi Calling decoration if the earlier override survived it.
        return if (appliedRawAlias == alias && original.contains(alias)) original else alias
    }
}

/** A framework getter changes display text only while these native UI formatters run. */
internal class CarrierNameFormatScope {
    private class Frame(val owner: Any) {
        val aliases = mutableMapOf<Pair<Int, Int>, String?>()
        val applied = mutableMapOf<Int, String>()
    }
    private val current = ThreadLocal<Frame?>()
    fun owner(): Any? = current.get()?.owner
    fun applied(slot: Int): String? = current.get()?.applied?.get(slot)
    fun markApplied(slot: Int, alias: String) { current.get()?.applied?.set(slot, alias) }
    fun resolve(slot: Int, subId: Int, read: () -> String?): String? {
        val frame = current.get() ?: return read()
        val key = slot to subId
        if (key in frame.aliases) return frame.aliases[key]
        return read().also { frame.aliases[key] = it }
    }
    fun <T> during(owner: Any?, block: () -> T): T {
        val previous = current.get()
        if (owner == null) current.remove() else current.set(Frame(owner))
        return try { block() } finally {
            if (previous == null) current.remove() else current.set(previous)
        }
    }
}
