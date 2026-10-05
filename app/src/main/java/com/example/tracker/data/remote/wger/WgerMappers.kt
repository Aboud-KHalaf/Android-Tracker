package com.example.tracker.data.remote.wger

import com.example.tracker.domain.model.CatalogExercise

/** Null when the exercise has no English name, as such entries can't be shown. */
internal fun WgerExerciseInfoDto.toDomain(): CatalogExercise? {
    val english = translations.firstOrNull { it.language == WgerApi.ENGLISH && it.name.isNotBlank() }
        ?: return null
    return CatalogExercise(
        id = id,
        name = english.name.trim(),
        category = category?.name,
        primaryMuscles = muscles.map { it.displayName() },
        secondaryMuscles = musclesSecondary.map { it.displayName() },
        // wger lists bodyweight exercises as having the equipment "none (bodyweight exercise)".
        equipment = equipment.map { it.name },
        description = htmlToPlainText(english.description),
        imageUrl = (images.firstOrNull { it.isMain } ?: images.firstOrNull())?.image,
    )
}

private fun WgerMuscleDto.displayName(): String = nameEn.ifBlank { name }

private val LineBreakTags = Regex("""<br\s*/?>|</p>|</li>|</h\d>""", RegexOption.IGNORE_CASE)
private val Tags = Regex("<[^>]+>")
private val Entities = mapOf("&nbsp;" to " ", "&quot;" to "\"", "&#39;" to "'", "&lt;" to "<", "&gt;" to ">", "&amp;" to "&")

/** wger descriptions are HTML fragments; keeps the text and its paragraph breaks. */
internal fun htmlToPlainText(html: String): String {
    var text = html.replace(LineBreakTags, "\n").replace(Tags, "")
    Entities.forEach { (entity, character) -> text = text.replace(entity, character) }
    return text.lines().map { it.trim() }.filter { it.isNotEmpty() }.joinToString("\n")
}
