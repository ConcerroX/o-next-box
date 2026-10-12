package com.mi.onextbox.lsp

import com.mi.onextbox.lsp.compat.ModernReflect
import android.os.Message
import java.lang.reflect.Constructor
import java.lang.reflect.Method

internal class FluidCloudMediaTargets(val values: Array<*>) {
    val entries = values[0] as Method
    val update = values[1] as Method
    val refresh = values[2] as Method
    val packageName = values[3] as Method
    val constructors = (values[4] as Array<*>).filterIsInstance<Constructor<*>>()
    val callers = (values[5] as Array<*>).filterIsInstance<Method>()
    val seedlingType: Class<*> get() = update.declaringClass

    fun existingSeedlings(loader: ClassLoader): List<Any> = runCatching {
        val dependency = ModernReflect.findClass("com.android.systemui.Dependency", loader)
            .getDeclaredField("sDependency").apply { isAccessible = true }.get(null) ?: return@runCatching emptyList()
        val wakefulness = ModernReflect.findClass("com.android.systemui.keyguard.WakefulnessLifecycle", loader)
        val lifecycle = synchronized(dependency) {
            (ModernReflect.getObjectField(dependency, "mDependencies") as? Map<*, *>)?.get(wakefulness)
        } ?: return@runCatching emptyList()
        val observers = (ModernReflect.getObjectField(lifecycle, "mObservers") as? List<*>)?.toList().orEmpty()
        val controllerType = entries.declaringClass
        val lazyType = ModernReflect.findClass("dagger.Lazy", loader)
        val lazyField = controllerType.getDeclaredField("mediaSeedling").apply { isAccessible = true }
        observers.mapNotNull { observer ->
            runCatching {
                val field = observer?.javaClass?.declaredFields?.singleOrNull { it.type == controllerType }
                    ?: return@runCatching null
                val controller = field.apply { isAccessible = true }.get(observer) ?: return@runCatching null
                val owner = lazyType.getMethod("get").invoke(lazyField.get(controller))
                owner?.takeIf(seedlingType::isInstance)
            }.getOrNull()
        }.distinct()
    }.getOrDefault(emptyList())

    init {
        (listOf(entries, update, refresh, packageName) + constructors + callers)
            .forEach { it.isAccessible = true }
    }

    companion object {
        private const val CONTROLLER = "com.oplus.systemui.media.seedling.OplusMediaSeedlingController"
        private const val SEEDLING = "com.oplus.systemui.media.seedling.MediaSeedling"
        private const val DATA = "com.oplus.systemui.seedlingservice.mediaControl.SeedlingMediaData"

        fun resolve(loader: ClassLoader): FluidCloudMediaTargets {
            val controller = ModernReflect.findClass(CONTROLLER, loader)
            val seedling = ModernReflect.findClass(SEEDLING, loader)
            val data = ModernReflect.findClass(DATA, loader)
            val entries = controller.getDeclaredMethod("getSortedEntries")
            check(List::class.java.isAssignableFrom(entries.returnType))
            val update = seedling.getDeclaredMethod("updateSeedlings", Boolean::class.javaPrimitiveType,
                Boolean::class.javaPrimitiveType, Boolean::class.javaPrimitiveType)
            val refresh = seedling.getDeclaredMethod("postMediaDataUpdate", Int::class.javaPrimitiveType, String::class.java)
            val packageName = data.getDeclaredMethod("getPackageName")
            check(update.returnType == Void.TYPE && refresh.returnType == Void.TYPE && packageName.returnType == String::class.java)
            val handler = ModernReflect.findClass("$SEEDLING\$mainHandler\$1", loader)
            val callers = arrayOf(update, seedling.getDeclaredMethod("start"), handler.getDeclaredMethod("handleMessage", Message::class.java))
            return FluidCloudMediaTargets(arrayOf(entries, update, refresh, packageName,
                seedling.declaredConstructors, callers))
        }
    }
}
