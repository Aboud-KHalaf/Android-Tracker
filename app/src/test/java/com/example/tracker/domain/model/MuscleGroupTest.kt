package com.example.tracker.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class MuscleGroupTest {

    @Test
    fun fromCatalogCategory_mapsCatalogCategories() {
        assertEquals(MuscleGroup.CHEST, MuscleGroup.fromCatalogCategory("Chest"))
        assertEquals(MuscleGroup.LEGS, MuscleGroup.fromCatalogCategory("Calves"))
        assertEquals(MuscleGroup.CORE, MuscleGroup.fromCatalogCategory(" abs "))
        assertEquals(null, MuscleGroup.fromCatalogCategory("Cardio"))
        assertEquals(null, MuscleGroup.fromCatalogCategory(null))
    }
}
