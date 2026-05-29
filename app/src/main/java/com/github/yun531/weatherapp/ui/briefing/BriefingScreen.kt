package com.github.yun531.weatherapp.ui.briefing

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.yun531.weatherapp.data.region.RegionCatalog
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun BriefingScreen(padding: PaddingValues, vm: BriefingViewModel = viewModel()) {
    val state by vm.state.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current
    val regionCatalog = remember { RegionCatalog.get(context) }

    LaunchedEffect(Unit) { vm.load() }

    Column(modifier = Modifier.padding(padding).fillMaxSize()) {
        Header(onRefresh = { vm.load() })

        when (val s = state) {
            is BriefingViewModel.UiState.Idle,
            is BriefingViewModel.UiState.Loading -> LoadingBlock()

            is BriefingViewModel.UiState.NoRegions -> EmptyMessage("설정에서 지역을 먼저 선택해주세요.")

            is BriefingViewModel.UiState.Failed -> ErrorBlock(message = s.message, onRetry = { vm.load() })

            is BriefingViewModel.UiState.Loaded -> {
                if (s.briefings.isEmpty()) {
                    EmptyMessage("표시할 지역이 없습니다.")
                } else {
                    BriefingList(state = s, regionCatalog = regionCatalog)
                }
            }
        }
    }
}

@Composable
private fun Header(onRefresh: () -> Unit) {
    val today = java.time.LocalDate.now()
    val dateText = today.format(
        DateTimeFormatter.ofPattern("M월 d일 (E)", Locale.KOREAN)
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "오늘의 브리핑",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            Text(dateText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        IconButton(onClick = onRefresh) {
            Icon(Icons.Default.Refresh, contentDescription = "새로고침")
        }
    }
}

@Composable
private fun BriefingList(
    state: BriefingViewModel.UiState.Loaded,
    regionCatalog: RegionCatalog
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        itemsIndexed(state.briefings, key = { _, b -> b.regionId }) { index, briefing ->
            BriefingCard(
                briefing = briefing,
                initiallyExpanded = index == 0,
                today = state.date,
                regionCatalog = regionCatalog
            )
        }
        item { Spacer(Modifier.height(8.dp)) }
    }
}

@Composable
private fun LoadingBlock() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun EmptyMessage(text: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ErrorBlock(message: String, onRetry: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("불러오기 실패", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onRetry) { Text("다시 시도") }
        }
    }
}