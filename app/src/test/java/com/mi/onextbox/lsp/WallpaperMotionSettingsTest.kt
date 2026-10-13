package com.mi.onextbox.lsp

import org.junit.Assert.*
import org.junit.Test

class WallpaperMotionSettingsTest {
    @Test fun gyroPreservesDirectionAndBoundsMultiplier() {
        assertEquals(-4f, WallpaperMotionSettings.scaleGyro(-2f, 200), 0f)
        assertEquals(1f, WallpaperMotionSettings.scaleGyro(2f, 0), 0f)
        assertEquals(2f, WallpaperMotionSettings.scaleGyro(2f, 100), 0f)
        assertEquals(0f, WallpaperMotionSettings.scaleGyro(0f, 200), 0f)
        assertTrue(WallpaperMotionSettings.scaleGyro(Float.NaN, 200).isNaN())
        assertEquals(Float.POSITIVE_INFINITY, WallpaperMotionSettings.scaleGyro(Float.POSITIVE_INFINITY, 200), 0f)
    }
    @Test fun disabledAndDefaultPreserveOriginal() {
        assertEquals(2.8f, WallpaperMotionSettings.scale(2.8f, false, 200), 0f)
        assertEquals(2.8f, WallpaperMotionSettings.scale(2.8f, true, 100), 0f)
    }
    @Test fun boundedAmplitude() {
        assertEquals(1.4f, WallpaperMotionSettings.scale(2.8f, true, -1), 0.0001f)
        assertEquals(5.6f, WallpaperMotionSettings.scale(2.8f, true, Int.MAX_VALUE), 0.0001f)
        assertEquals(4.2f, WallpaperMotionSettings.scale(2.8f, true, 150), 0.0001f)
    }
    @Test fun invalidOriginalIsNotReplaced() {
        assertTrue(WallpaperMotionSettings.scale(Float.NaN, true, 150).isNaN())
        assertEquals(0f, WallpaperMotionSettings.scale(0f, true, 150), 0f)
        assertEquals(-1f, WallpaperMotionSettings.scale(-1f, true, 150), 0f)
    }
}
