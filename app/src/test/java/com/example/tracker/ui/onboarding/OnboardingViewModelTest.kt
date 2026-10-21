package com.example.tracker.ui.onboarding

import androidx.lifecycle.SavedStateHandle
import com.example.tracker.domain.model.CustomStat
import com.example.tracker.domain.model.StatColor
import com.example.tracker.domain.model.StatIcon
import com.example.tracker.testing.FakeCustomStatRepository
import com.example.tracker.testing.FakeProgramRepository
import com.example.tracker.testing.FakeSettingsRepository
import com.example.tracker.testing.MainDispatcherRule
import com.example.tracker.ui.program.ProgramSetup
import java.time.DayOfWeek
import java.util.Locale
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val programs = FakeProgramRepository()
    private val stats = FakeCustomStatRepository()
    private val settings = FakeSettingsRepository()
    private val savedState = SavedStateHandle()

    private fun createViewModel() = OnboardingViewModel(programs, stats, settings, savedState, locale = { Locale.US })

    private val weight = CustomStat("", "Weight", "kg", 1, null, StatIcon.SCALE, StatColor.BLUE)

    @Test
    fun goingThroughEveryStep_makesTheProgramAndStatsAndOpensTheProgram() = runTest {
        val viewModel = createViewModel()

        viewModel.onGetStarted()
        assertEquals(OnboardingStep.PROGRAM, viewModel.uiState.value.step)
        viewModel.onCreateProgram(ProgramSetup("PPL", listOf("Push", "Pull", "Legs"), setOf(DayOfWeek.MONDAY)))
        assertEquals(OnboardingStep.STATS, viewModel.uiState.value.step)
        viewModel.onFinish(listOf(weight))

        val finished = viewModel.events.first() as OnboardingEvent.Finished
        assertEquals("program-1", finished.programId)
        assertEquals("PPL", programs.created.single().first)
        assertEquals(listOf("Weight"), stats.stats.value.map { it.name })
        assertTrue(settings.onboardingDone.value)
    }

    @Test
    fun skippingTheProgram_finishesWithoutOne() = runTest {
        val viewModel = createViewModel()
        viewModel.onGetStarted()

        viewModel.onSkipProgram()
        viewModel.onFinish(emptyList())

        assertEquals(OnboardingEvent.Finished(programId = null), viewModel.events.first())
        assertTrue(settings.onboardingDone.value)
    }

    @Test
    fun skipAll_marksItDone() = runTest {
        val viewModel = createViewModel()

        viewModel.onSkipAll()

        assertEquals(OnboardingEvent.Finished(programId = null), viewModel.events.first())
        assertTrue(settings.onboardingDone.value)
    }

    @Test
    fun back_returnsToTheStepBefore_andTheStepSurvivesRecreation() = runTest {
        val viewModel = createViewModel()
        viewModel.onGetStarted()
        viewModel.onSkipProgram()

        viewModel.onBack()

        assertEquals(OnboardingStep.PROGRAM, viewModel.uiState.value.step)
        assertEquals(OnboardingStep.PROGRAM, createViewModel().uiState.value.step)
    }
}
