package com.example.tracker.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.Button
import androidx.glance.ButtonDefaults
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalContext
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.material3.ColorProviders
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.example.tracker.MainActivity
import com.example.tracker.R
import com.example.tracker.TrackerApplication
import com.example.tracker.ui.AppShortcuts
import com.example.tracker.ui.theme.DarkColorScheme
import com.example.tracker.ui.theme.LightColorScheme
import java.time.LocalDate
import java.time.format.TextStyle as DateTextStyle
import java.util.Locale
import kotlinx.coroutines.flow.first

/**
 * The home screen widget: the workout to do next (or the one in progress) with a button that
 * starts or resumes it, as the launcher shortcut does, and this week's workout count. The data
 * is read when the widget updates; [TrackerApplication] updates it when workouts or programs
 * change, and Android does every hour so "today" stays right.
 */
class UpNextWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val state = loadState(context)
        provideContent {
            GlanceTheme(colors = WidgetColors) { Content(state) }
        }
    }

    private suspend fun loadState(context: Context): UpNextWidgetState? {
        val container = (context.applicationContext as TrackerApplication).container
        return try {
            val zone = container.time.zone()
            UpNextWidgetState.of(
                today = container.time.now().atZone(zone).toLocalDate(),
                zone = zone,
                activeWorkout = container.workoutRepository.observeActiveWorkout().first(),
                programs = container.programRepository.observePrograms().first(),
                history = container.workoutRepository.observeHistory().first(),
            )
        } catch (e: Exception) {
            // The widget then just opens the app, which shows the error properly.
            null
        }
    }

    @Composable
    private fun Content(state: UpNextWidgetState?) {
        val context = LocalContext.current
        val colors = GlanceTheme.colors
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(colors.widgetBackground)
                .cornerRadius(CornerRadius)
                .padding(Padding)
                .clickable(actionStartActivity<MainActivity>()),
        ) {
            Text(
                text = context.getString(R.string.home_up_next),
                style = TextStyle(color = colors.onSurfaceVariant, fontSize = LabelSize),
            )
            val upNext = state?.upNext
            Text(
                text = when (upNext) {
                    is UpNextWidgetState.UpNext.Resume -> upNext.name
                    is UpNextWidgetState.UpNext.Start -> upNext.name
                    UpNextWidgetState.UpNext.NothingToStart -> context.getString(R.string.widget_nothing_to_start)
                    null -> context.getString(R.string.app_name)
                },
                style = TextStyle(color = colors.onSurface, fontSize = TitleSize, fontWeight = FontWeight.Medium),
                maxLines = 1,
            )
            Text(
                text = when (upNext) {
                    is UpNextWidgetState.UpNext.Resume -> context.getString(R.string.home_in_progress)
                    is UpNextWidgetState.UpNext.Start -> startDetail(context, upNext, state.today)
                    UpNextWidgetState.UpNext.NothingToStart -> context.getString(R.string.widget_nothing_to_start_body)
                    null -> context.getString(R.string.widget_error)
                },
                style = TextStyle(color = colors.onSurfaceVariant, fontSize = BodySize),
                maxLines = 2,
            )
            Spacer(GlanceModifier.defaultWeight())
            Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = state?.let { workoutsThisWeek(context, it.workoutsThisWeek) }.orEmpty(),
                    style = TextStyle(color = colors.onSurfaceVariant, fontSize = LabelSize),
                    modifier = GlanceModifier.defaultWeight(),
                )
                if (upNext is UpNextWidgetState.UpNext.Resume || upNext is UpNextWidgetState.UpNext.Start) {
                    Button(
                        text = context.getString(
                            if (upNext is UpNextWidgetState.UpNext.Resume) R.string.widget_resume else R.string.widget_start,
                        ),
                        onClick = actionStartActivity(startWorkoutIntent(context)),
                        colors = ButtonDefaults.buttonColors(backgroundColor = colors.primary, contentColor = colors.onPrimary),
                    )
                }
            }
        }
    }

    /** "5 exercises · Today", "5 exercises · Wednesday", or just the count for an unscheduled day. */
    private fun startDetail(context: Context, start: UpNextWidgetState.UpNext.Start, today: LocalDate): String {
        val exercises = context.resources.getQuantityString(R.plurals.exercise_count, start.exerciseCount, start.exerciseCount)
        val date = start.scheduledOn ?: return exercises
        val day = when (start.daysAhead(today)) {
            0L -> context.getString(R.string.home_today)
            1L -> context.getString(R.string.widget_tomorrow)
            else -> date.dayOfWeek.getDisplayName(DateTextStyle.FULL, context.resources.configuration.locales[0] ?: Locale.getDefault())
        }
        return exercises + context.getString(R.string.separator_dot) + day
    }

    private fun workoutsThisWeek(context: Context, count: Int): String =
        context.resources.getQuantityString(R.plurals.widget_workouts_this_week, count, count)

    private fun startWorkoutIntent(context: Context) =
        Intent(context, MainActivity::class.java)
            .setAction(AppShortcuts.ACTION_START_WORKOUT)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)

    private companion object {
        val WidgetColors = ColorProviders(light = LightColorScheme, dark = DarkColorScheme)
        val CornerRadius = 24.dp
        val Padding = 16.dp
        val LabelSize = 12.sp
        val BodySize = 14.sp
        val TitleSize = 20.sp
    }
}

/** Lets Android place and update [UpNextWidget]; declared in the manifest. */
class UpNextWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = UpNextWidget()
}
