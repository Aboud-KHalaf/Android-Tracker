package com.example.tracker.ui.workout

import androidx.lifecycle.SavedStateHandle
import java.time.Instant

/*
 * What the user is choosing, typing or timing on the Active workout screen, kept in the
 * screen's saved state. When Android ends the app in the background, it comes back as it was;
 * a running hold keeps counting, since only its start time is stored.
 */

private const val KEY_EXERCISE_INDEX = "selection.exerciseIndex"
private const val KEY_SET_ID = "selection.setId"
private const val KEY_DRAFT_SET_ID = "draft.setId"
private const val KEY_DRAFT_WEIGHT = "draft.weight"
private const val KEY_DRAFT_REPS = "draft.reps"
private const val KEY_DRAFT_BANKED_SECONDS = "draft.bankedSeconds"
private const val KEY_DRAFT_RUNNING_SINCE = "draft.runningSince"

internal fun SavedStateHandle.readSelection() =
    WorkoutSelection(exerciseIndex = get<Int>(KEY_EXERCISE_INDEX), setId = get<String>(KEY_SET_ID))

internal fun SavedStateHandle.writeSelection(selection: WorkoutSelection) {
    this[KEY_EXERCISE_INDEX] = selection.exerciseIndex
    this[KEY_SET_ID] = selection.setId
}

internal fun SavedStateHandle.readDraft(): SetDraft? {
    val setId = get<String>(KEY_DRAFT_SET_ID) ?: return null
    val weight = get<String>(KEY_DRAFT_WEIGHT)
    val reps = get<String>(KEY_DRAFT_REPS)
    if (weight != null && reps != null) return SetDraft.WeightReps(setId, weight, reps)
    val banked = get<Int>(KEY_DRAFT_BANKED_SECONDS) ?: return null
    return SetDraft.Hold(setId, banked, get<Long>(KEY_DRAFT_RUNNING_SINCE)?.let(Instant::ofEpochMilli))
}

internal fun SavedStateHandle.writeDraft(draft: SetDraft?) {
    this[KEY_DRAFT_SET_ID] = draft?.setId
    this[KEY_DRAFT_WEIGHT] = (draft as? SetDraft.WeightReps)?.weightText
    this[KEY_DRAFT_REPS] = (draft as? SetDraft.WeightReps)?.repsText
    this[KEY_DRAFT_BANKED_SECONDS] = (draft as? SetDraft.Hold)?.bankedSeconds
    this[KEY_DRAFT_RUNNING_SINCE] = (draft as? SetDraft.Hold)?.runningSince?.toEpochMilli()
}
