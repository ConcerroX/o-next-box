package com.mi.onextbox.lsp

import android.view.View
import com.mi.onextbox.lsp.compat.HotReloadLifecycle
import com.mi.onextbox.lsp.compat.ModernHookRegistry
import com.mi.onextbox.lsp.compat.ModernMethodHook
import dalvik.system.DexFile
import java.lang.reflect.Constructor
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.lang.reflect.Modifier

internal object KeyboardFlagEmojiHooker {
    private const val TAG = "ONextBox-KeyboardEmoji"

    fun hook(loader: ClassLoader, apkPath: String) {
        val stateKey = "$TAG:targets@${System.identityHashCode(loader)}"
        val saved = HotReloadLifecycle.restoredState(stateKey) as? Array<*>
        if (!LspConfig.isKeyboardFlagEmojiEnabledXposed() && saved == null) return
        val hookKey = "$TAG@${System.identityHashCode(loader)}:flags"
        runCatching {
            val targets = saved ?: resolve(loader, apkPath)
            val collector = targets[0] as Method
            val ownerField = (targets[1] as Field).apply { isAccessible = true }
            val category = (targets[2] as Method).apply { isAccessible = true }
            val flowValue = (targets[3] as Method).apply { isAccessible = true }
            val models = KeyboardFlagEmojiModels(
                targets[4] as Constructor<*>, targets[5] as Field, targets[6] as Field,
                targets[7] as Constructor<*>, targets[8] as Field,
                targets[9] as Constructor<*>, targets[10] as Field,
            )
            ModernHookRegistry.installCompat(hookKey, collector, object : ModernMethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    if (!LspConfig.isKeyboardFlagEmojiEnabledXposed()) return
                    val owner = ownerField.get(param.thisObject) ?: return
                    if (!category.declaringClass.isInstance(owner)) return
                    val selected = flowValue.invoke(category.invoke(owner))
                    if (!KeyboardFlagEmojiRules.shouldAugment(true, selected)) return
                    models.supplement(param.args[0])?.let { param.args[0] = it }
                }
            })
            HotReloadLifecycle.register(stateKey, capture = { targets })
            HookLog.i(TAG, "Native flag emoji panel hook installed")
        }.onFailure {
            ModernHookRegistry.unhook(hookKey)
            HookLog.w(TAG, "Flag emoji panel hook unavailable, stock behavior retained", it)
        }
    }

    @Suppress("DEPRECATION")
    private fun resolve(loader: ClassLoader, apkPath: String): Array<Any> {
        val prefixes = listOf("com.oplus.keyboard.input.toolbox.media.",
            "com.oplus.keyboard.base.data.emojis.", "com.oplus.keyboard.db.model.")
        val dex = DexFile(apkPath)
        val names = try {
            dex.entries().asSequence().filter { name -> prefixes.any(name::startsWith) }.toList()
        } finally { dex.close() }
        val classes = names.mapNotNull { name -> runCatching { Class.forName(name, false, loader) }.getOrNull() }
        val media = classes.filter { it.name.startsWith(prefixes[0]) }
        val content = media.single { type ->
            View::class.java.isAssignableFrom(type) && runCatching {
                type.getDeclaredMethod("getSubmitList").returnType == List::class.java &&
                    type.getDeclaredMethod("getEmojiList").returnType == List::class.java &&
                    type.getDeclaredMethod("getCategory").returnType.getMethod("getValue").parameterCount == 0
            }.getOrDefault(false)
        }
        val category = content.getDeclaredMethod("getCategory")
        val flowValue = category.returnType.getMethod("getValue")
        val collector = media.mapNotNull { type ->
            val owner = type.declaredFields.singleOrNull {
                !Modifier.isStatic(it.modifiers) && it.type != Any::class.java &&
                    it.type.isAssignableFrom(content)
            } ?: return@mapNotNull null
            val method = type.declaredMethods.singleOrNull {
                it.name == "emit" && !Modifier.isStatic(it.modifiers) && it.returnType == Any::class.java &&
                    it.parameterCount == 2 && it.parameterTypes[0] == Any::class.java &&
                    it.parameterTypes[1].isInterface
            } ?: return@mapNotNull null
            method to owner
        }.single()
        val emoji = classes.filter { it.name.startsWith(prefixes[2]) }.mapNotNull { type ->
            runCatching {
                val fields = type.declaredFields.filter { !Modifier.isStatic(it.modifiers) }
                if (fields.size != 3 || fields.count { it.type == String::class.java } != 2 ||
                    fields.count { it.type == Long::class.javaPrimitiveType } != 1) return@runCatching null
                val constructor = type.getDeclaredConstructor(String::class.java, String::class.java,
                    Long::class.javaPrimitiveType).apply { isAccessible = true }
                val probe = constructor.newInstance(KeyboardFlagEmojiRules.flags[0].value, "ONextBox", 0L)
                if (!probe.toString().startsWith("Emoji(value=")) return@runCatching null
                val value = fields.single {
                    it.type == String::class.java && it.apply { isAccessible = true }.get(probe) ==
                        KeyboardFlagEmojiRules.flags[0].value
                }
                constructor to value
            }.getOrNull()
        }.single()
        val group = classes.filter { it.name.startsWith(prefixes[1]) }.mapNotNull { type ->
            runCatching {
                val entries = type.declaredFields.filter { !Modifier.isStatic(it.modifiers) }
                    .singleOrNull { it.type == List::class.java } ?: return@runCatching null
                val constructor = type.getDeclaredConstructor(List::class.java).apply { isAccessible = true }
                val probe = constructor.newInstance(arrayListOf(emoji.first.newInstance("🏁", "ONextBox", 0L)))
                if (!probe.toString().startsWith("EmojiList(emojis=")) return@runCatching null
                constructor to entries
            }.getOrNull()
        }.single()
        val pair = Class.forName("kotlin.Pair", false, loader)
            .getDeclaredConstructor(Any::class.java, Any::class.java).apply { isAccessible = true }
        val pairProbe = pair.newInstance("ONextBox-first", "ONextBox-second")
        val pairFields = pair.declaringClass.declaredFields.filter { !Modifier.isStatic(it.modifiers) }
            .onEach { it.isAccessible = true }
        val first = pairFields.single { it.get(pairProbe) == "ONextBox-first" }
        val second = pairFields.single { it.get(pairProbe) == "ONextBox-second" }
        return arrayOf(collector.first, collector.second, category, flowValue, pair, first, second,
            emoji.first, emoji.second, group.first, group.second)
    }
}

internal class KeyboardFlagEmojiModels(
    private val pair: Constructor<*>,
    private val first: Field,
    private val second: Field,
    private val emoji: Constructor<*>,
    private val value: Field,
    private val group: Constructor<*>,
    private val entries: Field,
) {
    init {
        listOf(pair, emoji, group).forEach { it.isAccessible = true }
        listOf(first, second, value, entries).forEach { it.isAccessible = true }
    }

    fun supplement(input: Any?): Any? {
        if (input == null || !pair.declaringClass.isInstance(input)) return null
        val originals = listOf(first.get(input), second.get(input)).map { raw ->
            val list = raw as? List<*> ?: return null
            if (list.any { !group.declaringClass.isInstance(it) }) return null
            list.filterNotNull()
        }
        val updated = originals.map { original ->
            KeyboardFlagEmojiRules.supplement(original, value = { item ->
                val values = entries.get(item) as? List<*> ?: error("Emoji group changed")
                val entry = values.firstOrNull() ?: error("Empty emoji group")
                check(emoji.declaringClass.isInstance(entry))
                value.get(entry) as String
            }, create = { flag ->
                group.newInstance(arrayListOf(emoji.newInstance(flag.value, flag.name, 0L)))
            })
        }
        return pair.newInstance(updated[0], updated[1])
    }
}
