package com.mi.onextbox.lsp

import org.luckypray.dexkit.DexKitBridge
import org.luckypray.dexkit.result.MethodData
import java.lang.reflect.Method
import java.lang.reflect.Modifier

internal class SoftwareUpdateTargets(private val dex: DexKitBridge) {
    fun resolve(loader: ClassLoader): Map<String, Array<Method>> {
        val targets = linkedMapOf<String, Array<Method>>()
        val lookups: Map<String, () -> MethodData> = linkedMapOf(
            "root" to { anchored("device is root from su binary file:", "boolean", CONTEXT) },
            "bootloaderGuard" to { anchored("verity state=", "boolean", ownerTag = " device state=") },
            "bootloaderReport" to {
                val owner = targets.getValue("root").single().declaringClass.name
                val guard = targets.getValue("bootloaderGuard").single()
                dex.findMethod {
                    matcher {
                        declaredClass = owner
                        usingStrings("ro.boot.vbmeta.device_state", "locked")
                        returnType = "boolean"
                        paramTypes(emptyList<String>())
                    }
                }.filter { it.getMethodInstance(loader) != guard }.single()
            },
            "localPicker" to {
                dex.findMethod {
                    matcher {
                        usingStrings("android.intent.action.OPEN_DOCUMENT", "android.intent.category.OPENABLE")
                        returnType = "boolean"
                        paramTypes("android.view.MenuItem")
                    }
                }.single()
            },
            "localPreflight" to { anchored("ota server grant this local update", "void", ownerTag = "ota:check_forbid_local_update") },
            "report" to {
                dex.findMethod {
                    matcher {
                        usingStrings("RequestBuilderbuildRequest Exception.")
                        paramTypes("org.json.JSONObject", "java.net.URI", CONTEXT, "boolean", "java.lang.String")
                    }
                }.single()
            },
            "downloadGate" to {
                val root = targets.getValue("root").single()
                dex.findMethod {
                    matcher {
                        declaredClass = root.declaringClass.name
                        usingStrings("auto_download_network_type")
                        returnType = "boolean"
                        paramTypes(CONTEXT)
                    }
                }.single()
            },
            "nightGate" to { anchored("isNightUpdateEnabledByUserAndServer context null", "boolean", CONTEXT) },
            "worker" to {
                dex.findMethod {
                    matcher {
                        usingStrings("MSG_OTA_START_INSTALL", "MSG_OTA_START_AUTO_DOWNLOAD , state = ")
                        returnType = "void"
                        paramTypes("android.os.Message")
                    }
                }.single()
            },
            "downloadRecommendation" to { anchored("dataType = ", "void", CONTEXT, ownerTag = "OTA_NoticeAlertDialog") },
            "nightRecommendation" to { anchored("Is manualDownloaded no need to showNoticeAutoInstallDialog.", "void", CONTEXT) },
            "updatePopup" to { anchored("sellMode has disable showNotifyDownloadDialog", "void", CONTEXT) },
            "versionQuery" to { anchored("ota has shelfOTA version!", "void", ownerTag = "query_version_update") },
            "versionResult" to {
                dex.findMethod {
                    matcher {
                        usingStrings("QueryOTAUpdateRunnableisDisableMajorvu value is true, abandon OSCrossUpgrade or AndroidCrossUpgrade")
                        returnType = "int"
                        paramCount = 2
                    }
                }.single()
            },
            "opexRequest" to { anchored("OpexRequest", "android.os.Bundle", CONTEXT, "java.lang.String", ownerTag = "client is null.") },
            "mergedOpex" to {
                dex.findMethod {
                    matcher {
                        usingStrings("lastRecordOpexIdSet ", "opexPackage")
                        returnType = "com.oplus.ota.opex.OpexPackageHelper\$OpexIdCompareResult"
                        paramTypes("com.oplus.ota.db.PackageListInfo", "java.lang.String")
                    }
                }.single()
            },
        )
        lookups.forEach { (key, lookup) ->
            runCatching {
                val data = lookup()
                val method = data.getMethodInstance(loader)
                check(if (key in setOf("worker", "downloadRecommendation", "nightRecommendation", "updatePopup", "localPicker", "localPreflight", "versionQuery", "versionResult")) {
                    !Modifier.isStatic(method.modifiers)
                } else Modifier.isStatic(method.modifiers))
                if (key == "localPicker") check(android.view.MenuItem.OnMenuItemClickListener::class.java.isAssignableFrom(method.declaringClass))
                if (key == "localPreflight") check(Runnable::class.java.isAssignableFrom(method.declaringClass))
                if (key == "versionQuery") check(Runnable::class.java.isAssignableFrom(method.declaringClass))
                if (key == "versionResult") {
                    check(Runnable::class.java.isAssignableFrom(method.declaringClass))
                    check(method.parameterTypes[1] == String::class.java)
                }
                if (key == "mergedOpex") check(method.returnType.isEnum)
                targets[key] = arrayOf(method)
                targets["callers:$key"] = (data.callers + data.callers.flatMap { it.callers })
                    .distinctBy { it.descriptor }
                    .mapNotNull { runCatching { it.getMethodInstance(loader) }.getOrNull() }.toTypedArray()
            }.onFailure { HookLog.w("SoftwareUpdate", "Target unavailable: $key, stock behavior retained", it) }
        }
        targets[CACHE_SCHEMA] = emptyArray()
        return targets
    }

    private fun anchored(text: String, result: String, vararg parameters: String, ownerTag: String? = null): MethodData =
        dex.findMethod {
            matcher {
                usingStrings(listOfNotNull(text, ownerTag))
                returnType = result
                paramTypes(parameters.toList())
            }
        }.single()

    companion object {
        const val CACHE_SCHEMA = "schema:update-discovery-v3"
        private const val CONTEXT = "android.content.Context"
    }
}
