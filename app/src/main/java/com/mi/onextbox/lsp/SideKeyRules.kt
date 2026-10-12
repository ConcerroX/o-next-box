package com.mi.onextbox.lsp

internal enum class SideKeyAction(val id: String) {
    Stock("stock"), None("none"), WeChatScan("wechat_scan"), WeChatPay("wechat_pay"),
    AlipayScan("alipay_scan"), AlipayPay("alipay_pay"), AlipayTransit("alipay_transit"),
    Flashlight("flashlight"), MediaPlayPause("media_play_pause"), MediaPrevious("media_previous"),
    MediaNext("media_next"), Notifications("notifications"), ControlCenter("control_center"),
    Screenshot("screenshot"), LockScreen("lock_screen"), DoNotDisturb("do_not_disturb"), App("app");

    companion object {
        fun parse(id: String?, fallback: SideKeyAction = None): SideKeyAction =
            entries.firstOrNull { it.id == id } ?: fallback
    }
}

internal enum class SideKeyGesture(val id: String) { Single("single"), Double("double"), Long("long") }

internal data class SideKeyBinding(val action: SideKeyAction, val packageName: String = "")

internal data class SideKeyConfig(
    val single: SideKeyBinding = SideKeyBinding(SideKeyAction.Stock),
    val double: SideKeyBinding = SideKeyBinding(SideKeyAction.None),
    val long: SideKeyBinding = SideKeyBinding(SideKeyAction.Stock),
    val longMillis: Long = 495,
    val doubleMillis: Long = 300,
) {
    fun binding(gesture: SideKeyGesture): SideKeyBinding = when (gesture) {
        SideKeyGesture.Single -> single
        SideKeyGesture.Double -> double
        SideKeyGesture.Long -> long
    }
}

internal sealed interface SideKeyCommand {
    data object StockTap : SideKeyCommand
    data object StockLongDown : SideKeyCommand
    data object StockLongUp : SideKeyCommand
    data class Run(val binding: SideKeyBinding) : SideKeyCommand
}

internal object SideKeyRules {
    const val RAW_KEY = 780
    const val TAP_KEY = 781
    const val LONG_KEY = 782
    const val BROADCAST = "com.mi.onextbox.action.SIDE_KEY_ACTION"
    const val PERMISSION = "android.permission.STATUS_BAR_SERVICE"
    val longIntervals = listOf(350, 495, 650, 800, 1000)
    val doubleIntervals = listOf(200, 300, 400, 500)

    fun longInterval(value: Int): Int = value.takeIf { it in longIntervals } ?: 495
    fun doubleInterval(value: Int): Int = value.takeIf { it in doubleIntervals } ?: 300
    fun validPackage(value: String): Boolean = value.length <= 200 &&
        value.matches(Regex("[a-zA-Z][a-zA-Z0-9_]*(\\.[a-zA-Z][a-zA-Z0-9_]*)+"))
}

internal class SideKeyGestureMachine {
    private data class Tap(val config: SideKeyConfig, val deadline: Long)
    private var pressed: SideKeyConfig? = null
    private var downAt = 0L
    private var longFired = false
    private var pending: Tap? = null
    private var previousTap: Tap? = null

    val isPressed: Boolean get() = pressed != null
    val isIdle: Boolean get() = pressed == null && pending == null && previousTap == null
    val nextDeadline: Long? get() = pressed?.takeUnless { longFired }?.let { downAt + it.longMillis }
        ?: pending?.deadline

    fun down(now: Long, config: SideKeyConfig): List<SideKeyCommand> = buildList {
        addAll(advance(now))
        if (pressed != null) return@buildList
        pending?.let { tap ->
            if (tap.config == config && now < tap.deadline) previousTap = tap
            else addAll(run(tap.config.single))
        }
        pending = null
        pressed = config
        downAt = now
        longFired = false
    }

    fun up(now: Long, cancelled: Boolean = false): List<SideKeyCommand> = buildList {
        val config = pressed ?: return@buildList
        if (cancelled) {
            if (longFired && config.long.action == SideKeyAction.Stock) add(SideKeyCommand.StockLongUp)
            previousTap?.let { pending = it }
        } else {
            addAll(advance(now))
            if (longFired) {
                if (config.long.action == SideKeyAction.Stock) add(SideKeyCommand.StockLongUp)
            } else if (previousTap != null) {
                addAll(run(config.double))
            } else if (config.double.action != SideKeyAction.None) {
                pending = Tap(config, now + config.doubleMillis)
            } else addAll(run(config.single))
        }
        pressed = null
        previousTap = null
        longFired = false
        addAll(advance(now))
    }

    fun advance(now: Long): List<SideKeyCommand> = buildList {
        val config = pressed
        if (config != null && !longFired && now >= downAt + config.longMillis) {
            previousTap?.let { addAll(run(it.config.single)) }
            previousTap = null
            longFired = true
            if (config.long.action == SideKeyAction.Stock) add(SideKeyCommand.StockLongDown)
            else addAll(run(config.long))
        }
        pending?.takeIf { now >= it.deadline }?.let { tap ->
            pending = null
            addAll(run(tap.config.single))
        }
    }

    fun reset() {
        pressed = null
        pending = null
        previousTap = null
        longFired = false
    }

    private fun run(binding: SideKeyBinding): List<SideKeyCommand> = when (binding.action) {
        SideKeyAction.Stock -> listOf(SideKeyCommand.StockTap)
        SideKeyAction.None -> emptyList()
        else -> listOf(SideKeyCommand.Run(binding))
    }
}
