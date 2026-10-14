package com.mi.onextbox.lsp

import java.io.File
import org.junit.Assert.*
import org.junit.Test

class WifiActivityIndicatorRulesTest {
    private class Node(
        val name: String?,
        val id: Int = 0,
        val image: Boolean = false,
        val group: Boolean = false,
        val resourcePackage: String? = "com.android.systemui",
        val resourceType: String? = "id",
        vararg val children: Node,
    )

    private fun targets(root: Node) = WifiActivityIndicatorRules.targets(
        root,
        children = { it.children.toList() },
        role = { WifiActivityIndicatorRules.role(it.resourcePackage, it.resourceType, it.name, it.image, it.group) },
    )

    @Test fun legacyArrowsAndActivityOnlyContainerAreHiddenButMainSignalIsKept() {
        val incoming = Node("wifi_in", image = true)
        val outgoing = Node("wifi_out", image = true)
        val container = Node("inout_container", group = true, children = arrayOf(incoming, outgoing))
        val signal = Node("wifi_signal", image = true)
        val root = Node("wifi_group", group = true, children = arrayOf(container, signal))
        assertEquals(listOf(incoming, outgoing, container), targets(root))
    }

    @Test fun c17CombinedActivityDoesNotHideWifiComboOrVersionBadges() {
        val incoming = Node("wifi_in", image = true)
        val outgoing = Node("wifi_out", image = true)
        val container = Node("inout_container", group = true, children = arrayOf(incoming, outgoing))
        val combined = Node("wifi_inout", image = true)
        val combo = Node("wifi_combo", group = true, children = arrayOf(
            Node("wifi_signal", image = true), combined, Node("wifi_left", image = true), Node("wifi_left_tv"),
        ))
        assertEquals(listOf(incoming, outgoing, container, combined),
            targets(Node("wifi_group", group = true, children = arrayOf(container, combo))))
    }

    @Test fun oldHardcodedIdsMayBelongToWifiSignalOnAnotherFirmware() {
        val signals = listOf(0x7f0a0689, 0x7f0a10c1, 0x7f0a10c2, 0x7f0a10c8)
            .map { Node("wifi_signal", id = it, image = true) }
        val arrow = Node("wifi_in", id = 0x7f0a2222, image = true)
        val root = Node("wifi_group", group = true, children = (signals + arrow).toTypedArray())
        assertEquals(listOf(arrow), targets(root))
    }

    @Test fun activityContainerContainingMainSignalIsNeverHidden() {
        val arrow = Node("wifi_out", image = true)
        val mixed = Node("inout_container", group = true, children = arrayOf(Node("wifi_signal", image = true), arrow))
        assertEquals(listOf(arrow), targets(Node("wifi_group", group = true, children = arrayOf(mixed))))
    }

    @Test fun unknownNestedContentProtectsItsContainerButNotConfirmedArrowSiblings() {
        val arrow = Node("wifi_inout", image = true)
        val wrapper = Node(null, group = true, children = arrayOf(Node("wifi_signal", image = true)))
        val mixed = Node("inout_container", group = true, children = arrayOf(wrapper, arrow))
        assertEquals(listOf(arrow), targets(Node("wifi_group", group = true, children = arrayOf(mixed))))
    }

    @Test fun namesFromOtherPackagesAndResourceTypesCannotMatch() {
        val foreign = Node("wifi_in", image = true, resourcePackage = "other.package")
        val drawable = Node("wifi_out", image = true, resourceType = "drawable")
        assertTrue(targets(Node("wifi_group", group = true, children = arrayOf(foreign, drawable))).isEmpty())
    }

    @Test fun unrecognizedLayoutsAndWrongControlTypesAreLeftUntouched() {
        val wrongType = Node("wifi_in")
        val containerImage = Node("inout_container", image = true)
        val newIcon = Node("wifi_icon", image = true)
        assertTrue(targets(Node(null, group = true, children = arrayOf(wrongType, containerImage, newIcon))).isEmpty())
    }

    @Test fun bindingRootIsNeverHiddenEvenWhenItHasAnActivityName() {
        assertTrue(targets(Node("wifi_inout", image = true)).isEmpty())
        val arrow = Node("wifi_in", image = true)
        assertEquals(listOf(arrow), targets(Node("inout_container", group = true, children = arrayOf(arrow))))
    }

    @Test fun emptyActivityContainerAndViewGroupsNamedAsArrowsAreKept() {
        val emptyContainer = Node("inout_container", group = true)
        val namedGroup = Node("wifi_inout", group = true, children = arrayOf(Node("wifi_signal", image = true)))
        assertTrue(targets(Node("wifi_group", group = true, children = arrayOf(emptyContainer, namedGroup))).isEmpty())
    }

    @Test fun hookUsesSemanticTargetsWithoutStickyAlphaAndRestoresVisibilityOnRetirement() {
        val source = listOf(
            File("src/main/java/com/mi/onextbox/lsp/SystemUiHooker.kt"),
            File("app/src/main/java/com/mi/onextbox/lsp/SystemUiHooker.kt"),
        ).first { it.isFile }.readText()
        val guard = source.substringAfter("private fun guardWifiActivityViews(root: View)")
            .substringBefore("private fun logNetworkActivityIndicatorHidden")
        assertTrue(guard.contains("WifiActivityIndicatorRules.targets("))
        assertTrue(guard.contains("resources.getResourceEntryName(view.id)"))
        assertFalse(source.contains("WIFI_ACTIVITY_CONTAINER_VIEW_ID"))
        assertFalse(guard.contains(".alpha ="))
        assertTrue(source.contains("guard.originalVisibility.forEach"))
        assertTrue(source.contains("activity.visibility = visibility"))
    }
}
