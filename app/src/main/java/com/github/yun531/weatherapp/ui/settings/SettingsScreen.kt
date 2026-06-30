package com.github.yun531.weatherapp.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.yun531.weatherapp.data.region.RegionCatalog
import com.github.yun531.weatherapp.ui.common.BellIcon
import com.github.yun531.weatherapp.ui.common.PinIcon
import com.github.yun531.weatherapp.ui.common.SectionHeader

@Composable
fun SettingsScreen(padding: PaddingValues, vm: SettingsViewModel = viewModel()) {
    val context = LocalContext.current
    val s by vm.settings.collectAsState()

    val catalog = remember { RegionCatalog.get(context) }

    val chosen = remember(s.region1, s.region2, s.region3) {
        setOf(s.region1, s.region2, s.region3).filter { it.isNotBlank() }.toSet()
    }

    var openSlot by remember { mutableStateOf<Int?>(null) }

    Column(
        modifier = Modifier
            .padding(padding)
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("설정", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

        RegionSection(
            region1 = s.region1,
            region2 = s.region2,
            region3 = s.region3,
            catalog = catalog,
            onOpenSlot = { openSlot = it }
        )

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            val hasRegion = chosen.isNotEmpty()
            val anyAlertEnabled = s.hourlyEnabled || s.dailyEnabled

            SectionHeader(BellIcon, "알림")
            AnimatedVisibility(visible = !hasRegion) {
                Text(
                    "관심 지역을 추가하면 알림을 설정할 수 있어요",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                )
            }
            HourlyAlertCard(hourlyEnabled = s.hourlyEnabled, enabled = hasRegion, vm = vm)
            DailyAlertCard(
                dailyEnabled = s.dailyEnabled,
                dailyHour = s.dailyHour,
                enabled = hasRegion,
                vm = vm
            )
            AlertKindsCard(
                anyAlertEnabled = anyAlertEnabled,
                hasRegion = hasRegion,
                enabledKinds = s.enabledKinds,
                warningKinds = s.warningKinds,
                vm = vm
            )
        }
    }

    openSlot?.let { slot ->
        val current = when (slot) {
            1 -> s.region1
            2 -> s.region2
            else -> s.region3
        }
        RegionPickerSheet(
            catalog = catalog,
            excludedIds = chosen - current,
            currentId = current,
            onSelect = { id ->
                vm.setRegion(slot, id)
                openSlot = null
            },
            onDismiss = { openSlot = null }
        )
    }
}

@Composable
private fun RegionSection(
    region1: String,
    region2: String,
    region3: String,
    catalog: RegionCatalog,
    onOpenSlot: (Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionHeader(PinIcon, "관심 지역")
        SettingsCard {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    "최대 3개까지 선택할 수 있습니다",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                RegionSlotRow(label = "지역 1", currentId = region1, catalog = catalog, onClick = { onOpenSlot(1) })
                RegionSlotRow(label = "지역 2", currentId = region2, catalog = catalog, onClick = { onOpenSlot(2) })
                RegionSlotRow(label = "지역 3", currentId = region3, catalog = catalog, onClick = { onOpenSlot(3) })
            }
        }
    }
}

@Composable
internal fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        content = content
    )
}