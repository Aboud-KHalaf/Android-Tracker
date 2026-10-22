package com.example.tracker.domain.workout

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlateCalculatorTest {

    @Test
    fun load_usesTheHeaviestPlatesFirst() {
        val load = PlateCalculator.load(totalKg = 102.5, barKg = 20.0)!!

        assertEquals(listOf(25.0, 15.0, 1.25), load.perSideKg)
        assertEquals(0.0, load.missingKg, 0.0)
        assertEquals(102.5, load.loadedKg, 0.0)
    }

    @Test
    fun load_justTheBar_hasNoPlates() {
        assertEquals(emptyList<Double>(), PlateCalculator.load(20.0, 20.0)!!.perSideKg)
    }

    @Test
    fun load_lighterThanTheBar_isNull() {
        assertNull(PlateCalculator.load(15.0, 20.0))
    }

    @Test
    fun load_weightThePlatesCantMake_reportsWhatIsMissing() {
        val load = PlateCalculator.load(totalKg = 61.0, barKg = 20.0)!!

        assertEquals(listOf(20.0), load.perSideKg)
        assertEquals(1.0, load.missingKg, 1e-9)
    }

    @Test
    fun load_otherBar() {
        assertEquals(listOf(10.0, 2.5), PlateCalculator.load(40.0, 15.0)!!.perSideKg)
    }
}
