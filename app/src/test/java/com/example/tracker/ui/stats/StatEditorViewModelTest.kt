package com.example.tracker.ui.stats

import com.example.tracker.domain.model.CustomStat
import com.example.tracker.domain.model.StatColor
import com.example.tracker.domain.model.StatGoal
import com.example.tracker.domain.model.StatIcon
import com.example.tracker.testing.FakeCustomStatRepository
import com.example.tracker.testing.MainDispatcherRule
import com.example.tracker.ui.stats.editor.StatEditorEvent
import com.example.tracker.ui.stats.editor.StatEditorUiState
import com.example.tracker.ui.stats.editor.StatEditorViewModel
import com.example.tracker.ui.stats.editor.StatForm
import java.io.IOException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class StatEditorViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeCustomStatRepository()

    private val bodyFat = CustomStat("fat", "Body fat", "%", 1, 15.0, StatIcon.PERCENT, StatColor.ORANGE)

    /** Names a template after its enum name, so tests needn't read resources. */
    private val templateForm = { template: StatTemplate ->
        StatForm(name = template.name, decimals = template.decimals, icon = template.icon, color = template.color, goal = template.goal)
    }

    private fun StatEditorViewModel.editing() = uiState.value as StatEditorUiState.Editing

    @Test
    fun newStat_startsFromTheGivenForm_andCreatesIt() = runTest {
        val viewModel = StatEditorViewModel(null, StatForm(name = "Waist", unit = "cm"), repository, templateForm)
        assertTrue(viewModel.editing().isNew)

        viewModel.onTargetChange("80,25")
        viewModel.onColorChange(StatColor.TEAL)
        viewModel.onSave()

        val created = viewModel.events.first() as StatEditorEvent.Created
        assertEquals(
            CustomStat(created.statId, "Waist", "cm", 1, 80.2, StatIcon.CHART, StatColor.TEAL),
            repository.getStat(created.statId),
        )
    }

    @Test
    fun onSave_blankName_showsErrorAndSavesNothing() = runTest {
        val viewModel = StatEditorViewModel(null, StatForm(name = "  "), repository, templateForm)

        viewModel.onSave()

        assertTrue(viewModel.editing().nameError)
        assertTrue(repository.stats.value.isEmpty())
    }

    @Test
    fun onSave_invalidTarget_showsError() = runTest {
        val viewModel = StatEditorViewModel(null, StatForm(name = "Steps"), repository, templateForm)
        viewModel.onTargetChange("-")

        viewModel.onSave()

        assertTrue(viewModel.editing().targetError)
    }

    @Test
    fun onDecimalsChange_trimsTheTarget() = runTest {
        val viewModel = StatEditorViewModel(null, StatForm(name = "Fat", decimals = 2, target = "15.25"), repository, templateForm)

        viewModel.onDecimalsChange(1)

        assertEquals("15.2", viewModel.editing().form.target)
    }

    @Test
    fun existingStat_loadsIt_andSavesChanges() = runTest {
        repository.stats.value = listOf(bodyFat)
        val viewModel = StatEditorViewModel("fat", StatForm(), repository, templateForm)
        assertEquals(StatForm("Body fat", "%", 1, "15.0", StatIcon.PERCENT, StatColor.ORANGE), viewModel.editing().form)
        assertFalse(viewModel.editing().isNew)

        viewModel.onTargetChange("")
        viewModel.onSave()

        assertEquals(StatEditorEvent.Saved, viewModel.events.first())
        assertEquals(bodyFat.copy(target = null), repository.getStat("fat"))
    }

    @Test
    fun onGoalChange_isSavedWithTheStat() = runTest {
        repository.stats.value = listOf(bodyFat)
        val viewModel = StatEditorViewModel("fat", StatForm(), repository, templateForm)

        viewModel.onGoalChange(StatGoal.DAILY)
        viewModel.onSave()

        assertEquals(StatEditorEvent.Saved, viewModel.events.first())
        assertEquals(bodyFat.copy(goal = StatGoal.DAILY), repository.getStat("fat"))
    }

    @Test
    fun existingStat_missing_isAnError() = runTest {
        val viewModel = StatEditorViewModel("gone", StatForm(), repository, templateForm)

        assertEquals(StatEditorUiState.Error, viewModel.uiState.value)
    }

    @Test
    fun onDelete_deletesTheStat() = runTest {
        repository.stats.value = listOf(bodyFat)
        val viewModel = StatEditorViewModel("fat", StatForm(), repository, templateForm)

        viewModel.onDelete()

        assertEquals(StatEditorEvent.Deleted, viewModel.events.first())
        assertTrue(repository.stats.value.isEmpty())
    }

    @Test
    fun onSave_failure_keepsTheFormAndReports() = runTest {
        repository.writeError = IOException("disk")
        val viewModel = StatEditorViewModel(null, StatForm(name = "Fat"), repository, templateForm)

        viewModel.onSave()

        assertTrue(viewModel.events.first() is StatEditorEvent.ShowMessage)
        assertFalse(viewModel.editing().isSaving)
    }

    @Test
    fun newStat_suggestsTemplatesTheUserDoesNotHaveYet() = runTest {
        repository.stats.value = listOf(bodyFat.copy(name = " weight "))
        val viewModel = StatEditorViewModel(null, StatForm(), repository, templateForm)

        val suggestions = viewModel.editing().suggestions
        assertFalse(StatTemplate.WEIGHT in suggestions)
        assertEquals(StatTemplate.entries.size - 1, suggestions.size)
    }

    @Test
    fun onPickSuggestion_fillsTheFormFromIt() = runTest {
        val viewModel = StatEditorViewModel(null, StatForm(name = "Typed"), repository, templateForm)

        viewModel.onPickSuggestion(StatTemplate.STEPS)

        assertEquals(templateForm(StatTemplate.STEPS), viewModel.editing().form)
        assertEquals(StatTemplate.STEPS, viewModel.editing().suggestion)
        assertEquals(StatGoal.DAILY, viewModel.editing().form.goal)
    }

    @Test
    fun existingStat_hasNoSuggestions() = runTest {
        repository.stats.value = listOf(bodyFat)
        val viewModel = StatEditorViewModel("fat", StatForm(), repository, templateForm)

        assertTrue(viewModel.editing().suggestions.isEmpty())
    }
}
