package com.mi.onextbox.lsp

import org.junit.Assert.*
import org.junit.Test

class AospVolumeGraphTest {
    private class Graph {
        @JvmField var volumeDialogControllerImplProvider: Any = Any()
        @JvmField var factoryProvider118: Any = Any()
        @JvmField var volumeDialogComponentProvider: Any = Any()
        @JvmField var provideVolumeDialogProvider: Any = Any()
        @JvmField var applicationScopeProvider: Any = Any()
    }

    @Test fun reusesControllerAndScopeButKeepsNativeFactoriesAndDialogGraph() {
        val host = Graph()
        val native = Graph()
        val factory = native.factoryProvider118
        val component = native.volumeDialogComponentProvider
        val dialog = native.provideVolumeDialogProvider
        AospVolumeGraph.shareProviders(host, native)
        assertSame(host.volumeDialogControllerImplProvider, native.volumeDialogControllerImplProvider)
        assertSame(host.applicationScopeProvider, native.applicationScopeProvider)
        assertSame(factory, native.factoryProvider118)
        assertSame(component, native.volumeDialogComponentProvider)
        assertSame(dialog, native.provideVolumeDialogProvider)
    }

    @Test fun rejectsNumberedFactoriesAndNonProviders() {
        assertFalse(AospVolumeGraph.shareableProvider("factoryProvider118"))
        assertFalse(AospVolumeGraph.shareableProvider("factoryProvider"))
        assertFalse(AospVolumeGraph.shareableProvider("context"))
        assertTrue(AospVolumeGraph.shareableProvider("provideAudioManagerProvider"))
    }
}
