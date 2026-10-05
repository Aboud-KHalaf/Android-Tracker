package com.example.tracker.data.remote.wger

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** A page of results from the wger REST API. Only the fields the app uses are declared. */
@Serializable
internal data class WgerPageDto<T>(val results: List<T> = emptyList())

/** An entry of `/api/v2/exerciseinfo/`. */
@Serializable
internal data class WgerExerciseInfoDto(
    val id: Int,
    val category: WgerNamedDto? = null,
    val muscles: List<WgerMuscleDto> = emptyList(),
    @SerialName("muscles_secondary") val musclesSecondary: List<WgerMuscleDto> = emptyList(),
    val equipment: List<WgerNamedDto> = emptyList(),
    val images: List<WgerImageDto> = emptyList(),
    val translations: List<WgerTranslationDto> = emptyList(),
)

@Serializable
internal data class WgerNamedDto(val name: String)

@Serializable
internal data class WgerMuscleDto(
    val name: String,
    /** Common English name ("Chest"); often blank, in which case [name] is the Latin one. */
    @SerialName("name_en") val nameEn: String = "",
)

@Serializable
internal data class WgerImageDto(
    val image: String,
    @SerialName("is_main") val isMain: Boolean = false,
)

@Serializable
internal data class WgerTranslationDto(
    val name: String,
    val description: String = "",
    val language: Int,
)
