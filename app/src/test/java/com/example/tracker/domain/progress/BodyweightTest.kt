package com.example.tracker.domain.progress

import com.example.tracker.domain.model.CustomStat
import com.example.tracker.domain.model.StatColor
import com.example.tracker.domain.model.StatEntry
import com.example.tracker.domain.model.StatIcon
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BodyweightTest {

    private val oct1 = LocalDate.of(2026, 10, 1)
    private fun entry(day: Long, kg: Double) = StatEntry("w", oct1.plusDays(day), kg)

    @Test
    fun on_usesTheLatestWeighInOnOrBeforeTheDay() {
        val log = BodyweightLog(listOf(entry(0, 80.0), entry(10, 81.0)))

        assertEquals(80.0, log.on(oct1.plusDays(9))!!, 0.0)
        assertEquals(81.0, log.on(oct1.plusDays(10))!!, 0.0)
        assertEquals(81.0, log.on(oct1.plusDays(40))!!, 0.0)
    }

    @Test
    fun on_beforeTheFirstWeighIn_looksAWeekAhead() {
        val log = BodyweightLog(listOf(entry(5, 80.0)))

        assertEquals(80.0, log.on(oct1)!!, 0.0)
        assertNull(log.on(oct1.minusDays(3)))
    }

    @Test
    fun bodyweightStat_isTheScaleStatInKilograms() {
        fun stat(id: String, unit: String, icon: StatIcon) = CustomStat(id, id, unit, 1, null, icon, StatColor.BLUE)
        val stats = listOf(stat("fat", "%", StatIcon.PERCENT), stat("lbs", "lb", StatIcon.SCALE), stat("weight", " KG ", StatIcon.SCALE))

        assertEquals("weight", stats.bodyweightStat()?.id)
    }
}
