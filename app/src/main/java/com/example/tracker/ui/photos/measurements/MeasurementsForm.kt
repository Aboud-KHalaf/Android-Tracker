package com.example.tracker.ui.photos.measurements

import androidx.annotation.StringRes
import com.example.tracker.R
import com.example.tracker.domain.model.PhotoMeasurement
import com.example.tracker.ui.stats.parseStatInput
import com.example.tracker.ui.stats.sanitizeStatInput
import com.example.tracker.ui.stats.toStatInput

/** Measurements are typed with one decimal place, as a tape measure or scale shows them. */
private const val DECIMALS = 1

/** One measurement being entered; [value] is kept as typed, and empty means not measured. */
data class MeasurementField(
    val name: String,
    val unit: String,
    val value: String = "",
    val error: Boolean = false,
)

/** The measurements of the check-in [checkInId] being entered. */
data class MeasurementsForm(
    val checkInId: String,
    val fields: List<MeasurementField>,
    val isSaving: Boolean = false,
) {
    fun has(name: String): Boolean = fields.any { it.name.equals(name, ignoreCase = true) }

    /** Adds an empty field for [name], or removes it when there is one already. */
    fun toggled(name: String, unit: String): MeasurementsForm =
        if (has(name)) copy(fields = fields.filterNot { it.name.equals(name, ignoreCase = true) }) else added(name, unit)

    /** Adds an empty field, unless [name] is blank or already there. */
    fun added(name: String, unit: String): MeasurementsForm {
        val trimmed = name.trim().take(PhotoMeasurement.MAX_NAME_LENGTH)
        if (trimmed.isEmpty() || has(trimmed)) return this
        return copy(fields = fields + MeasurementField(trimmed, unit.trim().take(PhotoMeasurement.MAX_UNIT_LENGTH)))
    }

    fun removed(index: Int): MeasurementsForm = copy(fields = fields.filterIndexed { i, _ -> i != index })

    fun withValue(index: Int, text: String): MeasurementsForm = copy(
        fields = fields.mapIndexed { i, field ->
            if (i == index) field.copy(value = sanitizeStatInput(text, DECIMALS).removePrefix("-"), error = false) else field
        },
    )

    /**
     * The measurements to save, skipping empty fields; null, with the invalid fields marked
     * in the returned form, when any value isn't a valid measurement.
     */
    fun validated(): Pair<MeasurementsForm, List<PhotoMeasurement>?> {
        val checked = fields.map { field ->
            field to field.value.takeIf { it.isNotBlank() }?.let(::parseStatInput)
                ?.takeIf { it in PhotoMeasurement.MIN_VALUE..PhotoMeasurement.MAX_VALUE }
        }
        val marked = checked.map { (field, value) -> field.copy(error = field.value.isNotBlank() && value == null) }
        if (marked.any { it.error }) return copy(fields = marked) to null
        return this to checked.mapNotNull { (field, value) -> value?.let { PhotoMeasurement(field.name, field.unit, it) } }
    }

    companion object {
        /** A new check-in's form: the fields measured last time, still empty. */
        fun newFor(checkInId: String, previous: List<PhotoMeasurement>) =
            MeasurementsForm(checkInId, previous.map { MeasurementField(it.name, it.unit) })

        /** Editing the measurements a check-in already has. */
        fun editing(checkInId: String, measurements: List<PhotoMeasurement>) = MeasurementsForm(
            checkInId,
            measurements.map { MeasurementField(it.name, it.unit, it.value.toStatInput(DECIMALS)) },
        )
    }
}

/** Common body measurements offered as one-tap fields. */
enum class MeasurementSuggestion(@StringRes val nameRes: Int, @StringRes val unitRes: Int) {
    WEIGHT(R.string.measurement_weight, R.string.weight_unit_kg),
    BODY_FAT(R.string.stat_template_body_fat, R.string.stat_unit_percent),
    WAIST(R.string.stat_template_waist, R.string.stat_unit_cm),
    CHEST(R.string.measurement_chest, R.string.stat_unit_cm),
    HIPS(R.string.measurement_hips, R.string.stat_unit_cm),
    SHOULDERS(R.string.measurement_shoulders, R.string.stat_unit_cm),
    NECK(R.string.measurement_neck, R.string.stat_unit_cm),
    LEFT_ARM(R.string.measurement_left_arm, R.string.stat_unit_cm),
    RIGHT_ARM(R.string.measurement_right_arm, R.string.stat_unit_cm),
    LEFT_THIGH(R.string.measurement_left_thigh, R.string.stat_unit_cm),
    RIGHT_THIGH(R.string.measurement_right_thigh, R.string.stat_unit_cm),
    CALVES(R.string.measurement_calves, R.string.stat_unit_cm),
}
