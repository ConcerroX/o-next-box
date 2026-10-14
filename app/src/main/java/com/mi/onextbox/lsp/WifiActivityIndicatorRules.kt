package com.mi.onextbox.lsp

/** Resource IDs change between OEM builds; only identified activity views may be hidden. */
internal object WifiActivityIndicatorRules {
    enum class Role { OTHER, ACTIVITY, ACTIVITY_CONTAINER }

    fun role(
        resourcePackage: String?,
        resourceType: String?,
        resourceName: String?,
        isImageView: Boolean,
        isViewGroup: Boolean,
    ): Role {
        if (resourcePackage != "com.android.systemui" || resourceType != "id") return Role.OTHER
        return when {
            isImageView && resourceName in setOf("wifi_in", "wifi_out", "wifi_inout") -> Role.ACTIVITY
            isViewGroup && resourceName == "inout_container" -> Role.ACTIVITY_CONTAINER
            else -> Role.OTHER
        }
    }

    /** Never hide the binding root, a signal icon, or a container with non-activity content. */
    fun <T : Any> targets(root: T, children: (T) -> List<T>, role: (T) -> Role): List<T> {
        val targets = mutableListOf<T>()
        fun visit(node: T): Boolean {
            val nested = children(node)
            // Visit every child even when a preceding child is not an activity view.
            val activityOnlyChildren = nested.map(::visit).all { it }
            val activityOnly = when (role(node)) {
                Role.ACTIVITY -> nested.isEmpty()
                Role.ACTIVITY_CONTAINER -> nested.isNotEmpty() && activityOnlyChildren
                Role.OTHER -> false
            }
            if (node !== root && activityOnly) targets += node
            return activityOnly
        }
        visit(root)
        return targets
    }
}
