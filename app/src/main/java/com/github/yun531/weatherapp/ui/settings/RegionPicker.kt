package com.github.yun531.weatherapp.ui.settings

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.github.yun531.weatherapp.data.region.RegionCatalog

/**
 * 설정 화면의 지역 슬롯 한 개를 표시하는 행. 탭하면 picker 시트가 열린다.
 *
 * - 미선택 상태: "선택안함" 표시
 * - 선택 상태: "{시군구} · {시도}" (동명 지역 구분 위해 시/도 보조 표시)
 */
@Composable
fun RegionSlotRow(
    label: String,
    currentId: String,
    catalog: RegionCatalog,
    onClick: () -> Unit
) {
    val region = catalog.regionOf(currentId)
    val display = if (region == null) "선택안함" else "${region.province} · ${region.name}"

    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    display,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (region == null) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
            }
        }
    }
}

/**
 * 지역 선택 바텀시트.
 *
 * 화면 상태:
 * - 빈 검색창: 시/도 그룹 헤더 + 시/군/구 리스트 (행정 관습 순서 유지)
 *   상단에 "선택안함" 항목으로 슬롯 비우기 가능
 * - 입력 있음: 시/군/구 이름 접두 매칭 결과 (가나다순)
 *   동명 지역 구분 위해 각 항목에 시/도를 보조 라벨로 표시
 *
 * 다른 슬롯이 이미 점유한 항목은 회색으로 비활성화되고 "이미 선택된 지역입니다" 라벨 표시.
 *
 * @param excludedIds 다른 슬롯이 이미 점유한 cityRegionCode들 (이번 슬롯의 currentId는 제외해서 전달)
 * @param currentId 이번 슬롯의 현재 cityRegionCode. 빈 문자열이면 미선택.
 * @param onSelect 선택 시 콜백. 빈 문자열은 "선택안함" 의미.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun RegionPickerSheet(
    catalog: RegionCatalog,
    excludedIds: Set<String>,
    currentId: String,
    onSelect: (id: String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var query by remember { mutableStateOf("") }

    val all = remember { catalog.allOptions() }

    val grouped = remember(all) {
        all.groupBy { it.province }
    }

    val results = remember(query, all) {
        val q = query.trim()
        if (q.isEmpty()) emptyList()
        else all.filter { it.name.startsWith(q) }.sortedBy { it.name }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
        ) {
            Text(
                "지역 선택",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("시/군/구 이름") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = if (query.isNotEmpty()) {
                    {
                        IconButton(onClick = { query = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "지우기")
                        }
                    }
                } else null,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                singleLine = true
            )

            HorizontalDivider()

            LazyColumn(
                modifier = Modifier.weight(1f)
            ) {
                if (query.isBlank()) {
                    // 빈 입력 상태: "선택안함" + 시/도별 그룹
                    item(key = "__none__") {
                        ListItem(
                            headlineContent = {
                                Text(
                                    "선택안함",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            trailingContent = if (currentId.isBlank()) {
                                { Icon(Icons.Default.Check, contentDescription = "선택됨") }
                            } else null,
                            modifier = Modifier.clickable { onSelect("") }
                        )
                        HorizontalDivider()
                    }
                    grouped.forEach { (province, regions) ->
                        stickyHeader(key = "__h_$province") {
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    province,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(
                                        horizontal = 16.dp,
                                        vertical = 8.dp
                                    )
                                )
                            }
                        }
                        items(regions, key = { it.id }) { region ->
                            RegionItemRow(
                                name = region.name,
                                supporting = null,
                                isSelectedInThisSlot = region.id == currentId,
                                isOccupiedByOtherSlot = region.id in excludedIds,
                                onClick = { onSelect(region.id) }
                            )
                        }
                    }
                } else {
                    // 검색 모드
                    if (results.isEmpty()) {
                        item(key = "__empty__") {
                            Text(
                                "검색 결과 없음",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    } else {
                        items(results, key = { it.id }) { region ->
                            RegionItemRow(
                                name = region.name,
                                supporting = region.province,
                                isSelectedInThisSlot = region.id == currentId,
                                isOccupiedByOtherSlot = region.id in excludedIds,
                                onClick = { onSelect(region.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * picker 리스트의 단일 항목.
 *
 * 상태별 표시:
 * - 이번 슬롯에 선택됨: 일반 색상 + 체크 아이콘 (탭 가능)
 * - 다른 슬롯에 점유됨: 회색 + "이미 선택된 지역입니다" 보조 텍스트 (탭 불가)
 * - 그 외: 일반 색상 (탭 가능)
 */
@Composable
private fun RegionItemRow(
    name: String,
    supporting: String?,
    isSelectedInThisSlot: Boolean,
    isOccupiedByOtherSlot: Boolean,
    onClick: () -> Unit
) {
    val disabledColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)

    if (isOccupiedByOtherSlot) {
        // 다른 슬롯이 점유 — 회색 처리, 클릭 불가
        CompositionLocalProvider(LocalContentColor provides disabledColor) {
            ListItem(
                headlineContent = { Text(name) },
                supportingContent = { Text("이미 선택된 지역입니다") },
                colors = ListItemDefaults.colors(
                    headlineColor = disabledColor,
                    supportingColor = disabledColor,
                    trailingIconColor = disabledColor
                )
            )
        }
    } else {
        // 정상 항목
        ListItem(
            headlineContent = { Text(name) },
            supportingContent = supporting?.let { { Text(it) } },
            trailingContent = if (isSelectedInThisSlot) {
                { Icon(Icons.Default.Check, contentDescription = "선택됨") }
            } else null,
            modifier = Modifier.clickable(onClick = onClick)
        )
    }
}