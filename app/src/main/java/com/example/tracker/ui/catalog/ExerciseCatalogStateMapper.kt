package com.example.tracker.ui.catalog

import com.example.tracker.domain.catalog.suggestedType
import com.example.tracker.domain.model.CatalogExercise

/** Names of the plan's exercises, lowercased, to mark catalog entries already in it. */
internal fun CatalogExercise.toUi(namesInPlan: Set<String>) = CatalogExerciseUi(
    id = id,
    name = name,
    category = category,
    primaryMuscles = primaryMuscles,
    secondaryMuscles = secondaryMuscles,
    equipment = equipment,
    description = description,
    imageUrl = imageUrl,
    suggestedType = suggestedType(),
    isInPlan = name.lowercase() in namesInPlan,
)
