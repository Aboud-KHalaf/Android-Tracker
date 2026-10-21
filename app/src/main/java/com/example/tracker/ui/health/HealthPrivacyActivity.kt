package com.example.tracker.ui.health

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.tracker.R
import com.example.tracker.TrackerApplication
import com.example.tracker.ui.theme.Dimens
import com.example.tracker.ui.theme.TrackerTheme
import com.example.tracker.ui.theme.spacing

/**
 * Explains what the app does with health data. Health Connect opens it from its permission
 * screen and its settings; it has no state, so it is a screen of its own.
 */
class HealthPrivacyActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val settings = (application as TrackerApplication).container.settingsRepository
        setContent {
            val themeMode by settings.themeMode.collectAsStateWithLifecycle()
            TrackerTheme(darkTheme = themeMode.isDark(systemIsDark = isSystemInDarkTheme())) {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text(stringResource(R.string.health_privacy_title)) },
                            navigationIcon = {
                                IconButton(onClick = ::finish) {
                                    Icon(Icons.Outlined.Close, contentDescription = stringResource(R.string.action_close))
                                }
                            },
                        )
                    },
                ) { innerPadding ->
                    Column(
                        modifier = Modifier
                            .padding(innerPadding)
                            .widthIn(max = Dimens.maxContentWidth)
                            .verticalScroll(rememberScrollState())
                            .padding(MaterialTheme.spacing.lg),
                        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md),
                    ) {
                        Text(stringResource(R.string.health_privacy_body), style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }
    }
}
