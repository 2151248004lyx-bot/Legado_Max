package io.legado.app.ui.book.read

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FilterAltOff
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.legado.app.R
import io.legado.app.ui.book.bookmark.AllBookmarkFilter
import io.legado.app.ui.book.bookmark.BookmarkNoteFilter
import io.legado.app.ui.design.components.compose.ngDrawerContentCardColor
import io.legado.app.ui.design.theme.NgTheme
import java.util.Locale

internal enum class CatalogBookmarkToolsVariant { STANDARD, READING_BOTTOM, READING_COMPACT_SIDE }

/** Compact inline tools; filtering stays in AllBookmarkFilter. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun CatalogBookmarkFilterPanel(
    query: String,
    onQueryChange: (String) -> Unit,
    filter: AllBookmarkFilter,
    colors: List<Int>,
    onFilterChanged: (AllBookmarkFilter) -> Unit,
    onClearConditions: () -> Unit,
    autoExpandNotes: Boolean,
    onAutoExpandNotesChanged: (Boolean) -> Unit,
    variant: CatalogBookmarkToolsVariant = CatalogBookmarkToolsVariant.STANDARD,
    modifier: Modifier = Modifier,
) {
    val primary = Color(NgTheme.colors.primary)
    val muted = Color(NgTheme.colors.onSurfaceVariant)
    val cardColor = ngDrawerContentCardColor()
    val hasConditions = filter.isActive || query.isNotBlank()
    val compact = variant == CatalogBookmarkToolsVariant.READING_COMPACT_SIDE
    val readingTools = variant != CatalogBookmarkToolsVariant.STANDARD
    val labelSize = if (compact) 10.sp else 11.sp
    val noteLabel = stringResource(R.string.bookmark_note)
    val colorLabel = stringResource(R.string.all_bookmark_filter_color)
    val allLabel = stringResource(R.string.all)
    val autoExpandLabel = stringResource(R.string.bookmark_auto_expand_notes)
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = TextStyle(fontFamily = NgTheme.fontFamily, fontSize = labelSize, lineHeight = 16.sp)
    val labelWidth = with(density) {
        maxOf(textMeasurer.measure(noteLabel, labelStyle, maxLines = 1).size.width,
            textMeasurer.measure(colorLabel, labelStyle, maxLines = 1).size.width).toDp()
    }.coerceAtLeast(28.dp)
    val allWidth = (with(density) {
        textMeasurer.measure(allLabel, labelStyle.copy(fontWeight = FontWeight.SemiBold),
            maxLines = 1).size.width.toDp()
    } + if (compact) 12.dp else 16.dp).coerceAtLeast(if (compact) 36.dp else 44.dp)
    val colorTargetWidth = if (compact) 30.dp else 44.dp
    val autoExpandWidth = with(density) {
        textMeasurer.measure(autoExpandLabel, labelStyle, maxLines = 1).size.width.toDp()
    } + 30.dp // Checkbox, text gap and horizontal padding.
    val autoExpandSwitch: @Composable (Modifier) -> Unit = { actionModifier ->
        CatalogToolSwitch(autoExpandLabel, autoExpandNotes, modifier = actionModifier, compact = true,
            onCheckedChange = onAutoExpandNotesChanged)
    }
    val clearAction: @Composable () -> Unit = {
        Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) {
            if (hasConditions) {
                IconButton(onClick = onClearConditions, modifier = Modifier.size(44.dp)) {
                    Icon(Icons.Rounded.FilterAltOff,
                        stringResource(R.string.all_bookmark_filter_clear_conditions),
                        modifier = Modifier.size(20.dp), tint = primary)
                }
            }
        }
    }
    CatalogToolsPanel(query = query, onQueryChange = onQueryChange,
        hint = stringResource(R.string.read_catalog_search_bookmarks), modifier = modifier,
        variant = if (compact) CatalogToolsPanelVariant.BOOKMARK_INTEGRATED
            else CatalogToolsPanelVariant.STANDARD) {
        BoxWithConstraints(Modifier.fillMaxWidth().padding(top = 7.dp)) {
            // Reading tools show automatic expansion beside the bookmark count.
            val autoExpandInline = !readingTools && maxWidth >= 20.dp + labelWidth + 6.dp +
                allWidth + 44.dp * colors.size + autoExpandWidth
            val availableColorWidth = maxWidth - labelWidth - 6.dp - if (compact) 0.dp else 20.dp
            val colorItemsPerRow = if (readingTools &&
                allWidth + colorTargetWidth * colors.size > availableColorWidth) {
                maxOf(1, (colors.size + 2) / 2)
            } else Int.MAX_VALUE
            Column {
                Column(Modifier.fillMaxWidth().then(if (compact) Modifier else
                    Modifier.clip(RoundedCornerShape(14.dp)).background(cardColor)
                        .padding(horizontal = 10.dp, vertical = 4.dp))) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(noteLabel, color = muted, fontSize = labelSize,
                            modifier = Modifier.width(labelWidth))
                        Spacer(Modifier.width(6.dp))
                        FlowRow(Modifier.weight(1f).selectableGroup(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            BookmarkNoteFilter.entries.forEach { option ->
                                CatalogBookmarkFilterChip(
                                    label = stringResource(when (option) {
                                        BookmarkNoteFilter.ALL -> R.string.all
                                        BookmarkNoteFilter.WITH_NOTE -> R.string.all_bookmark_filter_with_note
                                        BookmarkNoteFilter.WITHOUT_NOTE -> R.string.all_bookmark_filter_without_note
                                    }),
                                    selected = filter.noteFilter == option,
                                    compact = compact,
                                    onClick = { onFilterChanged(filter.copy(noteFilter = option)) },
                                )
                            }
                        }
                        if (readingTools) clearAction()
                    }
                    HorizontalDivider(Modifier.padding(top = 4.dp), thickness = 0.5.dp,
                        color = muted.copy(alpha = 0.12f))
                    Row(Modifier.fillMaxWidth().padding(top = 4.dp),
                        verticalAlignment = if (readingTools) Alignment.Top else Alignment.CenterVertically) {
                        Box(Modifier.width(labelWidth).heightIn(min = 44.dp),
                            contentAlignment = Alignment.CenterStart) {
                            Text(colorLabel, color = muted, fontSize = labelSize)
                        }
                        Spacer(Modifier.width(6.dp))
                        FlowRow(Modifier.weight(1f).selectableGroup(),
                            horizontalArrangement = Arrangement.spacedBy(0.dp),
                            verticalArrangement = Arrangement.spacedBy(0.dp),
                            maxItemsInEachRow = colorItemsPerRow) {
                            CatalogBookmarkFilterChip(allLabel, filter.color == null,
                                modifier = Modifier.width(allWidth).align(Alignment.CenterVertically),
                                compact = compact) {
                                onFilterChanged(filter.copy(color = null))
                            }
                            colors.forEach { argb ->
                                val selected = filter.color == argb
                                val swatch = Color(argb)
                                val description = stringResource(R.string.all_bookmark_filter_color_value,
                                    String.format(Locale.ROOT, "#%08X", argb))
                                Box(Modifier.size(width = colorTargetWidth, height = 44.dp)
                                    .align(Alignment.CenterVertically).clip(RoundedCornerShape(8.dp))
                                    .selectable(selected, role = Role.RadioButton) { onFilterChanged(filter.copy(color = argb)) }
                                    .semantics { contentDescription = description },
                                    contentAlignment = Alignment.Center) {
                                    Box(Modifier.size(if (compact) 22.dp else 29.dp)
                                        .then(if (selected) Modifier.border(1.dp, primary, CircleShape) else Modifier)
                                        .padding(4.dp).background(swatch, CircleShape)
                                        .border(0.5.dp, muted.copy(alpha = 0.15f), CircleShape))
                                }
                            }
                        }
                        if (autoExpandInline) autoExpandSwitch(Modifier)
                    }
                }
                if (!readingTools && !autoExpandInline) {
                    Row(Modifier.fillMaxWidth().padding(top = 6.dp),
                        horizontalArrangement = Arrangement.End) {
                        autoExpandSwitch(Modifier)
                    }
                }
            }
        }
        if (hasConditions && !readingTools) {
            Text(stringResource(R.string.all_bookmark_filter_clear_conditions), color = primary, fontSize = 12.sp,
                modifier = Modifier.align(Alignment.End).padding(top = 4.dp)
                    .clip(RoundedCornerShape(10.dp)).clickable(onClick = onClearConditions)
                    .padding(horizontal = 8.dp, vertical = 6.dp))
        }
    }
}

@Composable
private fun CatalogBookmarkFilterChip(label: String, selected: Boolean,
    modifier: Modifier = Modifier, compact: Boolean = false, onClick: () -> Unit) {
    // Keep the touch target separate from the compact selection background.
    // An explicit line height keeps the text chip aligned with the 44dp swatch targets.
    Box(modifier.widthIn(min = if (compact) 36.dp else 44.dp)
        .heightIn(min = 44.dp).clip(RoundedCornerShape(9.dp))
        .selectable(selected, role = Role.RadioButton, onClick = onClick),
        contentAlignment = Alignment.Center) {
        Box(Modifier.widthIn(min = if (compact) 32.dp else 40.dp)
            .heightIn(min = if (compact) 24.dp else 32.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) catalogFilterSelectedColor() else Color.Transparent)
            .padding(horizontal = if (compact) 6.dp else 8.dp,
                vertical = if (compact) 4.dp else 6.dp), contentAlignment = Alignment.Center) {
            Text(label, color = Color(if (selected) NgTheme.colors.primary else NgTheme.colors.onSurfaceVariant),
                fontSize = if (compact) 10.sp else 11.sp,
                lineHeight = if (compact) 14.sp else 16.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
        }
    }
}
