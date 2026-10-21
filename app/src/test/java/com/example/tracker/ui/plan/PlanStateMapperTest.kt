package com.example.tracker.ui.plan

import com.example.tracker.domain.model.Exercise
import com.example.tracker.domain.model.ExerciseConfig
import com.example.tracker.domain.model.ExerciseType
import com.example.tracker.domain.model.PlanDetails
import com.example.tracker.domain.model.PlanExercise
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Test

class PlanStateMapperTest {

    private fun exercise(id: String, position: Int, group: String?) = PlanExercise(
        id = "pe-$id",
        exercise = Exercise(id, id, ExerciseType.WEIGHT_REPS),
        position = position,
        config = ExerciseConfig(),
        supersetGroup = group,
    )

    @Test
    fun supersets_areLetteredInOrder_andALoneGroupMemberIsNone() {
        val details = PlanDetails(
            id = "p",
            name = "Push",
            exercises = listOf(
                exercise("a", 0, "x"),
                exercise("b", 1, "x"),
                exercise("c", 2, null),
                exercise("d", 3, "y"),
                exercise("e", 4, "y"),
                exercise("f", 5, "z"),
            ),
        )

        val state = PlanStateMapper(ZoneOffset.UTC).map(details, null, null, emptyList(), null) as PlanUiState.Success

        assertEquals(listOf("A", "A", null, "B", "B", null), state.exercises.map { it.supersetLabel })
        assertEquals(listOf(true, true, true, true, true, false), state.exercises.map { it.canSupersetWithNext })
    }
}
