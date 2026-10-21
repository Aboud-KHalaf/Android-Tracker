package com.example.tracker.data.health

import android.content.Context
import android.os.Build
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.request.AggregateGroupByPeriodRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.example.tracker.core.TimeProvider
import com.example.tracker.domain.health.DailyValue
import com.example.tracker.domain.health.HealthAvailability
import com.example.tracker.domain.health.HealthDataSource
import com.example.tracker.domain.health.HealthMetric
import java.time.Duration
import java.time.LocalDate
import java.time.Period
import kotlin.reflect.KClass

/**
 * Reads steps, weight and sleep from Health Connect. Health Connect needs Android 9, so on
 * older phones this reports [HealthAvailability.UNSUPPORTED] and never touches its classes.
 */
class HealthConnectDataSource(context: Context, private val time: TimeProvider) : HealthDataSource {
    private val context = context.applicationContext
    private val client by lazy { HealthConnectClient.getOrCreate(this.context) }

    override fun availability(): HealthAvailability {
        if (Build.VERSION.SDK_INT < MIN_SDK) return HealthAvailability.UNSUPPORTED
        return when (HealthConnectClient.getSdkStatus(context)) {
            HealthConnectClient.SDK_AVAILABLE -> HealthAvailability.AVAILABLE
            HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> HealthAvailability.NEEDS_INSTALL
            else -> HealthAvailability.UNSUPPORTED
        }
    }

    override suspend fun grantedMetrics(): Set<HealthMetric> {
        val granted = client.permissionController.getGrantedPermissions()
        return PERMISSIONS.filterValues { it in granted }.keys
    }

    override suspend fun readDaily(metric: HealthMetric, from: LocalDate, to: LocalDate): List<DailyValue> = when (metric) {
        HealthMetric.STEPS -> readSteps(from, to)
        HealthMetric.WEIGHT -> readRecords(WeightRecord::class, from, to)
            .groupBy { it.time.atZone(time.zone()).toLocalDate() }
            // The day's last weigh-in.
            .map { (date, records) -> DailyValue(date, records.maxBy { it.time }.weight.inKilograms) }
        HealthMetric.SLEEP -> readRecords(SleepSessionRecord::class, from.minusDays(1), to)
            // A night counts toward the day it ends on.
            .groupBy { it.endTime.atZone(time.zone()).toLocalDate() }
            .filterKeys { !it.isBefore(from) && !it.isAfter(to) }
            .map { (date, sessions) ->
                val total = sessions.fold(Duration.ZERO) { sum, session -> sum + Duration.between(session.startTime, session.endTime) }
                DailyValue(date, total.toMinutes() / MINUTES_PER_HOUR)
            }
    }

    private suspend fun readSteps(from: LocalDate, to: LocalDate): List<DailyValue> =
        client.aggregateGroupByPeriod(
            AggregateGroupByPeriodRequest(
                metrics = setOf(StepsRecord.COUNT_TOTAL),
                timeRangeFilter = TimeRangeFilter.between(from.atStartOfDay(), to.plusDays(1).atStartOfDay()),
                timeRangeSlicer = Period.ofDays(1),
            ),
        ).mapNotNull { day ->
            day.result[StepsRecord.COUNT_TOTAL]?.let { DailyValue(day.startTime.toLocalDate(), it.toDouble()) }
        }

    /** Every record of [type] from the start of [from] to the end of [to], over all pages. */
    private suspend fun <T : Record> readRecords(type: KClass<T>, from: LocalDate, to: LocalDate): List<T> {
        val zone = time.zone()
        val filter = TimeRangeFilter.between(from.atStartOfDay(zone).toInstant(), to.plusDays(1).atStartOfDay(zone).toInstant())
        val records = mutableListOf<T>()
        var pageToken: String? = null
        do {
            val response = client.readRecords(ReadRecordsRequest(type, filter, pageToken = pageToken))
            records += response.records
            pageToken = response.pageToken
        } while (pageToken != null)
        return records
    }

    companion object {
        /** Health Connect runs on Android 9 (API 28) and newer. */
        const val MIN_SDK = Build.VERSION_CODES.P

        private const val MINUTES_PER_HOUR = 60.0

        /** The read permission behind each metric; the manifest declares the same ones. */
        val PERMISSIONS: Map<HealthMetric, String> by lazy {
            mapOf(
                HealthMetric.STEPS to HealthPermission.getReadPermission(StepsRecord::class),
                HealthMetric.WEIGHT to HealthPermission.getReadPermission(WeightRecord::class),
                HealthMetric.SLEEP to HealthPermission.getReadPermission(SleepSessionRecord::class),
            )
        }
    }
}
