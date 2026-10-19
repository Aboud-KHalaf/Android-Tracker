package com.example.tracker.ui.photos

import com.example.tracker.domain.model.PhotoMeasurement
import com.example.tracker.ui.photos.measurements.MeasurementField
import com.example.tracker.ui.photos.measurements.MeasurementsForm
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MeasurementsFormTest {

    private val empty = MeasurementsForm("c1", emptyList())

    @Test
    fun toggled_addsThenRemovesIgnoringCase() {
        val added = empty.toggled("Waist", "cm")
        assertEquals(listOf(MeasurementField("Waist", "cm")), added.fields)

        assertTrue(added.toggled("waist", "cm").fields.isEmpty())
    }

    @Test
    fun added_ignoresBlankAndDuplicateNames() {
        val form = empty.added("  Biceps ", " cm ")

        assertEquals(listOf(MeasurementField("Biceps", "cm")), form.fields)
        assertEquals(form, form.added("biceps", "in"))
        assertEquals(form, form.added(" ", ""))
    }

    @Test
    fun withValue_keepsOneDecimalAndNoSign() {
        val form = empty.added("Waist", "cm").withValue(0, "-84,57")

        assertEquals("84.5", form.fields.single().value)
    }

    @Test
    fun validated_skipsEmptyFields() {
        val form = empty.added("Waist", "cm").added("Chest", "cm").withValue(1, "101")

        val (_, measurements) = form.validated()

        assertEquals(listOf(PhotoMeasurement("Chest", "cm", 101.0)), measurements)
    }

    @Test
    fun validated_marksInvalidValues() {
        val form = empty.added("Waist", "cm").withValue(0, ".")

        val (checked, measurements) = form.validated()

        assertNull(measurements)
        assertTrue(checked.fields.single().error)
    }

    @Test
    fun editing_startsFromTheSavedValues() {
        val form = MeasurementsForm.editing("c1", listOf(PhotoMeasurement("Weight", "kg", 78.4)))

        assertEquals(listOf(MeasurementField("Weight", "kg", "78.4")), form.fields)
    }
}
