package com.example.tracker.ui.settings

import com.example.tracker.R
import com.example.tracker.domain.model.ThemeMode
import com.example.tracker.testing.FakeSettingsRepository
import com.example.tracker.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val settings = FakeSettingsRepository()

    @Test
    fun uiState_showsStoredThemeMode() = runTest {
        settings.themeMode.value = ThemeMode.LIGHT
        val viewModel = SettingsViewModel(settings)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }

        assertEquals(SettingsUiState(themeMode = ThemeMode.LIGHT), viewModel.uiState.value)
    }

    @Test
    fun onSelectThemeMode_savesIt() = runTest {
        val viewModel = SettingsViewModel(settings)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }

        viewModel.onSelectThemeMode(ThemeMode.DARK)

        assertEquals(ThemeMode.DARK, settings.themeMode.value)
        assertEquals(ThemeMode.DARK, viewModel.uiState.value.themeMode)
    }

    @Test
    fun onSelectThemeMode_failure_showsError() = runTest {
        settings.writeError = IllegalStateException("disk full")
        val viewModel = SettingsViewModel(settings)

        viewModel.onSelectThemeMode(ThemeMode.DARK)

        assertEquals(SettingsEvent.ShowMessage(R.string.settings_error_theme), viewModel.events.first())
        assertEquals(ThemeMode.SYSTEM, settings.themeMode.value)
    }

    @Test
    fun onDeleteAllData_deletesAndConfirms() = runTest {
        val viewModel = SettingsViewModel(settings)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }

        viewModel.onDeleteAllData()

        assertEquals(1, settings.deleteAllDataCalls)
        assertEquals(SettingsEvent.ShowMessage(R.string.settings_data_deleted), viewModel.events.first())
        assertFalse(viewModel.uiState.value.isDeletingData)
    }

    @Test
    fun onDeleteAllData_failure_showsErrorAndAllowsRetry() = runTest {
        settings.writeError = IllegalStateException("db locked")
        val viewModel = SettingsViewModel(settings)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }

        viewModel.onDeleteAllData()

        assertEquals(SettingsEvent.ShowMessage(R.string.settings_error_delete_data), viewModel.events.first())
        assertFalse(viewModel.uiState.value.isDeletingData)
    }
}
