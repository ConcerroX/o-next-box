package com.mi.onextbox.lsp

/** Only unlock capability flags; never change the user's mode or suppress hide alarms. */
internal class AodAllDaySupport(type: Class<*>, private val report: (String) -> Unit) {
    private val failedWrites = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()
    private val fields = listOf("isSupportPanoramicAllDay", "isSupportPanoramicAllDayByPanelFeature")
        .mapNotNull { name ->
            try {
                type.getDeclaredField(name).apply {
                    require(this.type == java.lang.Boolean.TYPE)
                    isAccessible = true
                }
            } catch (error: Exception) {
                report("AOD all-day field unavailable: ${type.name}#$name: $error")
                null
            }
        }

    fun apply(instance: Any?, enabled: Boolean) {
        if (!enabled || instance == null) return
        fields.forEach { field ->
            try {
                if (!field.getBoolean(instance)) field.setBoolean(instance, true)
            } catch (error: Exception) {
                if (failedWrites.add(field.name)) {
                    report("AOD all-day field update failed: ${field.name}: $error")
                }
            }
        }
    }
}
