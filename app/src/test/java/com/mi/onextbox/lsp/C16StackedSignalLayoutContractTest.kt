package com.mi.onextbox.lsp

import java.io.File
import org.junit.Assert.*
import org.junit.Test

/** Protects the reflective host-Compose boundary; actual placement needs device validation. */
class C16StackedSignalLayoutContractTest {
    private val source = File("src/main/java/com/mi/onextbox/lsp/C16StackedSignalHooker.kt").readText()

    @Test fun donorPreparationIsLazyAndPrecedesCompositionWithoutDroppingStateSubscription() {
        val adapter = source.substringAfter("private class Adapter(").substringBefore("private class Drawing(")
        assertFalse(adapter.contains("InMemoryDexClassLoader("))
        assertTrue(adapter.contains("DeferredHookResource"))
        assertTrue(adapter.contains("canReload = { false }"))
        assertTrue(source.indexOf("val drawingReady =") < source.indexOf("renderer.start.invoke"))
        assertTrue(source.contains("val enabled = renderer.readState.invoke(renderer.state) == true"))
        assertTrue(source.contains("if (enabled && drawingReady) renderer.render"))
    }

    @Test fun signalOverridesBottomAlignedHostRowWithoutDiscardingCallerPadding() {
        assertTrue(source.contains("getCenterVertically"))
        assertTrue(source.contains("alignInRow.invoke(rowScope, args[4], centerVertically)"))
        assertTrue(source.contains("size.invoke(null, spaced, toDp.invoke(density, width.invoke(dims)), toDp.invoke(density, height))"))
    }

    @Test fun donorSpacingIsOutsideCanvasAndDoesNotIntroduceVerticalPadding() {
        assertTrue(source.contains("dimensions.getField(\"IconSpacingSp\").getLong(null)"))
        val padding = "horizontalPadding.invoke(null, aligned, toDp.invoke(density, iconSpacing), 0f)"
        assertTrue(source.contains(padding))
        assertTrue(source.indexOf(padding) < source.indexOf("size.invoke(null, spaced"))
        assertFalse(source.contains("getField(\"IconPaddingSp\")"))
    }

    @Test fun alignmentDoesNotReplaceOrOffsetOriginalDrawing() {
        assertTrue(source.contains("callback.newInstance(dims, bars, dual, args[2])"))
        assertFalse(source.contains("OffsetKt"))
        assertFalse(source.contains("graphicsLayer"))
        assertFalse(source.contains("translationY"))
        assertTrue(source.contains("else chain.proceed()"))
    }
}
