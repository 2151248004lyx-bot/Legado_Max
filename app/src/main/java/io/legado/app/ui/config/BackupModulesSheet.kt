package io.legado.app.ui.config

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.legado.app.help.storage.BackupModule
import io.legado.app.help.storage.BackupModules
import io.legado.app.ui.design.components.NgSettingsTrailing
import io.legado.app.ui.design.components.compose.NgBottomDrawerSurface
import io.legado.app.ui.design.components.compose.NgCompactSettingsDivider
import io.legado.app.ui.design.components.compose.NgCompactSettingsGroup
import io.legado.app.ui.design.components.compose.NgCompactSettingsItem
import io.legado.app.ui.design.theme.NgTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BackupModulesSheet(onDismiss: () -> Unit) {
    var selected by remember { mutableStateOf(BackupModules.selected()) }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color.Transparent,
        shape = RectangleShape,
        dragHandle = null,
    ) {
        NgBottomDrawerSurface(Modifier.fillMaxWidth()) {
            Column(Modifier.navigationBarsPadding().verticalScroll(rememberScrollState()).padding(20.dp)) {
                Text("备份清单", color = Color(NgTheme.colors.onSurface), fontSize = 22.sp,
                    fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 16.dp))
                NgCompactSettingsGroup {
                    BackupModule.entries.forEachIndexed { index, module ->
                        val checked = module.id in selected
                        fun change(value: Boolean) {
                            val next = if (value) selected + module.id else selected - module.id
                            BackupModules.setSelected(next)
                            selected = next
                        }
                        NgCompactSettingsItem(
                            title = module.title, summary = module.summary,
                            trailing = NgSettingsTrailing.SWITCH, checked = checked,
                            onCheckedChange = ::change, onClick = { change(!checked) },
                            summaryMaxLines = 2,
                        )
                        if (index < BackupModule.entries.lastIndex) NgCompactSettingsDivider()
                    }
                }
            }
        }
    }
}
