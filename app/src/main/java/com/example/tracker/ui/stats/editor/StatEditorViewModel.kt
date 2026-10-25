package com.example.tracker.ui.stats.editor

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.toRoute
import com.example.tracker.R
import com.example.tracker.TrackerApplication
import com.example.tracker.domain.model.CustomStat
import com.example.tracker.domain.model.StatColor
import com.example.tracker.domain.model.StatGoal
import com.example.tracker.domain.model.StatIcon
import com.example.tracker.domain.repository.CustomStatRepository
import com.example.tracker.ui.common.launchCatching
import com.example.tracker.ui.stats.StatTemplate
import com.example.tracker.ui.stats.parseStatInput
import com.example.tracker.ui.stats.roundTo
import com.example.tracker.ui.stats.sanitizeStatInput
import com.example.tracker.ui.stats.toStatInput
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update

/**
 * Creates a stat starting from [newForm], or edits the stat [statId]: its name, unit,
 * decimals, target and its goal, icon and colour. Results are sent once through [events].
 */
class StatEditorViewModel(
    private val statId: String?,
    private val newForm: StatForm,
    private val repository: CustomStatRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<StatEditorUiState>(StatEditorUiState.Loading)
    val uiState: StateFlow<StatEditorUiState> = _uiState.asStateFlow()

    private val _events = Channel<StatEditorEvent>(Channel.BUFFERED)
    val events: Flow<StatEditorEvent> = _events.receiveAsFlow()

    init {
        load()
    }

    fun onRetry() = load()

    private fun load() {
        if (statId == null) {
            _uiState.value = StatEditorUiState.Editing(newForm, isNew = true)
            return
        }
        _uiState.value = StatEditorUiState.Loading
        viewModelScope.launchCatching(onError = { _uiState.value = StatEditorUiState.Error }) {
            val stat = repository.getStat(statId)
            _uiState.value = if (stat == null) StatEditorUiState.Error else StatEditorUiState.Editing(stat.toForm(), isNew = false)
        }
    }

    fun onNameChange(name: String) = updateEditing {
        it.copy(form = it.form.copy(name = name.take(CustomStat.MAX_NAME_LENGTH)), nameError = false)
    }

    fun onUnitChange(unit: String) = updateEditing {
        it.copy(form = it.form.copy(unit = unit.take(CustomStat.MAX_UNIT_LENGTH)))
    }

    /** Also trims the target to the new number of decimals. */
    fun onDecimalsChange(decimals: Int) = updateEditing {
        if (decimals !in CustomStat.DECIMALS_RANGE) return@updateEditing it
        it.copy(form = it.form.copy(decimals = decimals, target = sanitizeStatInput(it.form.target, decimals)))
    }

    fun onTargetChange(text: String) = updateEditing {
        it.copy(form = it.form.copy(target = sanitizeStatInput(text, it.form.decimals)), targetError = false)
    }

    fun onGoalChange(goal: StatGoal) = updateEditing { it.copy(form = it.form.copy(goal = goal)) }

    fun onIconChange(icon: StatIcon) = updateEditing { it.copy(form = it.form.copy(icon = icon)) }

    fun onColorChange(color: StatColor) = updateEditing { it.copy(form = it.form.copy(color = color)) }

    fun onSave() {
        val editing = _uiState.value as? StatEditorUiState.Editing ?: return
        if (editing.isSaving) return
        val form = editing.form
        val name = form.name.trim()
        val target = form.target.takeIf { it.isNotBlank() }?.let(::parseStatInput)
        val targetInvalid = form.target.isNotBlank() && target == null
        if (name.isEmpty() || targetInvalid) {
            _uiState.value = editing.copy(nameError = name.isEmpty(), targetError = targetInvalid)
            return
        }
        val stat = CustomStat(
            id = statId.orEmpty(),
            name = name,
            unit = form.unit.trim(),
            decimals = form.decimals,
            target = target?.let { roundTo(it, form.decimals) },
            icon = form.icon,
            color = form.color,
            goal = form.goal,
        )
        _uiState.value = editing.copy(isSaving = true)
        viewModelScope.launchCatching(
            onError = {
                updateEditing { it.copy(isSaving = false) }
                showMessage(R.string.stat_error_save_stat)
            },
        ) {
            if (statId == null) {
                _events.send(StatEditorEvent.Created(repository.createStat(stat)))
            } else {
                repository.updateStat(stat)
                _events.send(StatEditorEvent.Saved)
            }
        }
    }

    /** Deletes the stat being edited, with its entries; the screen has already asked to confirm. */
    fun onDelete() {
        val editing = _uiState.value as? StatEditorUiState.Editing ?: return
        if (statId == null || editing.isSaving) return
        _uiState.value = editing.copy(isSaving = true)
        viewModelScope.launchCatching(
            onError = {
                updateEditing { it.copy(isSaving = false) }
                showMessage(R.string.stat_error_delete_stat)
            },
        ) {
            repository.deleteStat(statId)
            _events.send(StatEditorEvent.Deleted)
        }
    }

    private inline fun updateEditing(crossinline transform: (StatEditorUiState.Editing) -> StatEditorUiState.Editing) {
        _uiState.update { state -> if (state is StatEditorUiState.Editing) transform(state) else state }
    }

    private suspend fun showMessage(@StringRes messageRes: Int) {
        _events.send(StatEditorEvent.ShowMessage(messageRes))
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val application = this[APPLICATION_KEY] as TrackerApplication
                val route = createSavedStateHandle().toRoute<StatEditorDestination>()
                val template = StatTemplate.entries.firstOrNull { it.name == route.template }
                StatEditorViewModel(
                    statId = route.statId,
                    newForm = template?.let {
                        StatForm(
                            name = application.getString(it.nameRes),
                            unit = application.getString(it.unitRes),
                            decimals = it.decimals,
                            icon = it.icon,
                            color = it.color,
                            goal = it.goal,
                        )
                    } ?: StatForm(),
                    repository = application.container.customStatRepository,
                )
            }
        }
    }
}

private fun CustomStat.toForm() = StatForm(
    name = name,
    unit = unit,
    decimals = decimals,
    target = target?.toStatInput(decimals).orEmpty(),
    icon = icon,
    color = color,
    goal = goal,
)
