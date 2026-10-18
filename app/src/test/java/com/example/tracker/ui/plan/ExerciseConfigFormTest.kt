package com.example.tracker.ui.plan

import com.example.tracker.domain.model.ExerciseConfig
import com.example.tracker.domain.model.ExerciseType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class ExerciseConfigFormTest {

    @Test
    fun blankTargets_meanRepeatLastTime() {
        val form = ExerciseConfigForm(sets = 3, repsText = "", weightText = " ", holdText = "")

        assertEquals(ExerciseConfig(targetSets = 3), form.toConfig(ExerciseType.WEIGHT_REPS))
    }

    @Test
    fun typedTargets_areParsed_acceptingADecimalComma() {
        val form = ExerciseConfigForm(sets = 4, repsText = "8", weightText = "47,5", holdText = "")

        assertEquals(
            ExerciseConfig(targetSets = 4, targetReps = 8, targetWeightKg = 47.5),
            form.toConfig(ExerciseType.WEIGHT_REPS),
        )
    }

    @Test
    fun onlyTheExerciseTypesTargetsAreKept() {
        val form = ExerciseConfigForm(sets = 2, repsText = "8", weightText = "60", holdText = "45")

        assertEquals(ExerciseConfig(targetSets = 2, targetDurationSeconds = 45), form.toConfig(ExerciseType.DURATION))
    }

    @Test
    fun invalidField_blocksSaving() {
        val form = ExerciseConfigForm(sets = 3, repsText = "0", weightText = "", holdText = "")

        assertFalse(form.isRepsValid)
        assertNull(form.toConfig(ExerciseType.WEIGHT_REPS))
    }

    @Test
    fun invalidFieldOfTheOtherType_doesNotBlockSaving() {
        val form = ExerciseConfigForm(sets = 3, repsText = "", weightText = "", holdText = "abc")

        assertEquals(ExerciseConfig(targetSets = 3), form.toConfig(ExerciseType.WEIGHT_REPS))
    }

    @Test
    fun sets_stayWithinLimits() {
        val form = ExerciseConfigForm.from(ExerciseConfig(targetSets = 1))

        assertEquals(1, form.withSets(0).sets)
        assertEquals(ExerciseConfig.MAX_TARGET_SETS, form.withSets(99).sets)
    }

    @Test
    fun from_roundTripsAConfig() {
        val config = ExerciseConfig(targetSets = 5, targetReps = 5, targetWeightKg = 102.5)

        assertEquals(config, ExerciseConfigForm.from(config).toConfig(ExerciseType.WEIGHT_REPS))
    }
}
