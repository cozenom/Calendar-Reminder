package com.davidp.simpleweeklyreminders.ui.theme

import com.davidp.simpleweeklyreminders.data.settings.ThemePack
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The picker renders one row per [ThemePack] via `ThemePackPalette.getValue(pack)`, so a pack
 * added to the enum without an entry here would crash at runtime rather than fail to compile.
 */
class ThemePacksTest {

    @Test
    fun everyPackHasTones() {
        ThemePack.entries.forEach { pack ->
            assertNotNull("No tones defined for $pack", ThemePackPalette[pack])
        }
    }

    @Test
    fun everyPackHasLabelAndDescription() {
        ThemePack.entries.forEach { pack ->
            val tones = ThemePackPalette.getValue(pack)
            assertTrue("$pack has a blank label", tones.label.isNotBlank())
            assertTrue("$pack has a blank description", tones.description.isNotBlank())
        }
    }

    @Test
    fun labelsAreUnique() {
        val labels = ThemePack.entries.map { ThemePackPalette.getValue(it).label }
        assertEquals(labels.size, labels.distinct().size)
    }

    @Test
    fun tonesForFallsBackToPaper() {
        assertEquals(ThemePackPalette.getValue(ThemePack.PAPER), tonesFor(ThemePack.PAPER))
    }

    /**
     * Light accents sit on paper and dark accents on near-black, so the pair must actually
     * differ in lightness — a pack whose two tones were the same would be unreadable in one
     * theme. Uses the sRGB luma approximation rather than Compose's `luminance()`, which
     * needs the Android graphics runtime.
     */
    @Test
    fun darkToneIsLighterThanLightTone() {
        ThemePack.entries.forEach { pack ->
            val tones = ThemePackPalette.getValue(pack)
            val light = luma(tones.accentLight.value.toLong())
            val dark = luma(tones.accentDark.value.toLong())
            assertTrue(
                "$pack: dark-theme accent should be lighter than the light-theme one",
                dark > light
            )
        }
    }

    @Test
    fun deepToneIsDarkerThanPaleTone() {
        ThemePack.entries.forEach { pack ->
            val tones = ThemePackPalette.getValue(pack)
            assertTrue(
                "$pack: deep should be darker than pale",
                luma(tones.deep.value.toLong()) < luma(tones.pale.value.toLong())
            )
        }
    }

    /** Compose packs colours into the top 32 bits of a ULong as RGBA. */
    private fun luma(packed: Long): Double {
        val rgba = (packed ushr 32) and 0xFFFFFFFFL
        val r = ((rgba ushr 24) and 0xFF) / 255.0
        val g = ((rgba ushr 16) and 0xFF) / 255.0
        val b = ((rgba ushr 8) and 0xFF) / 255.0
        return 0.2126 * r + 0.7152 * g + 0.0722 * b
    }
}
