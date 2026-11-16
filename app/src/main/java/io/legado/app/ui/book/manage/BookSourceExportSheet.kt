package io.legado.app.ui.book.manage

import android.content.Context
import android.graphics.Color as AndroidColor
import android.view.View
import androidx.annotation.StringRes
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.rememberScrollableState
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import io.legado.app.R
import io.legado.app.ui.design.components.compose.NgBottomDrawerSurface
import io.legado.app.ui.design.components.compose.NgDrawerContentCardStyle
import io.legado.app.ui.design.components.compose.NgCompactDrawerHeader
import io.legado.app.ui.design.components.compose.NgCompactDrawerSelectionItem
import io.legado.app.ui.design.components.compose.NgCompactDrawerSelectionPanel
import io.legado.app.ui.design.components.compose.NgDrawerDragHandle
import io.legado.app.ui.design.components.compose.NgDrawerDragHandleVariant
import io.legado.app.ui.design.theme.NgAppTheme
import io.legado.app.utils.showWithAppNavigationBarVisibility

/** 书源与订阅源共用的分享／本地保存／上传目标抽屉。 */
class BookSourceExportSheet(
    private val context: Context,
    private val onShare: () -> Unit,
    private val onSaveLocally: () -> Unit,
    private val onUpload: () -> Unit,
    @StringRes private val titleRes: Int = R.string.export_book_source,
) {

    private val dialog by lazy { BottomSheetDialog(context) }

    fun show() {
        val contentView = ComposeView(context).apply {
            setBackgroundColor(AndroidColor.TRANSPARENT)
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            setContent {
                NgAppTheme(updateSystemBars = false) {
                    SheetContent()
                }
            }
        }
        dialog.setContentView(contentView)
        dialog.setOnShowListener {
            val sheet = dialog.findViewById<View>(
                com.google.android.material.R.id.design_bottom_sheet
            ) ?: return@setOnShowListener
            sheet.setBackgroundColor(AndroidColor.TRANSPARENT)
            BottomSheetBehavior.from(sheet).apply {
                isFitToContents = true
                skipCollapsed = true
                isDraggable = true
                isDraggableOnNestedScroll = true
                state = BottomSheetBehavior.STATE_EXPANDED
            }
        }
        dialog.showWithAppNavigationBarVisibility()
    }

    @Composable
    private fun SheetContent() {
        val items = listOf(
            NgCompactDrawerSelectionItem(
                iconRes = R.drawable.ic_share,
                title = stringResource(R.string.export_book_source_share_apps),
            ),
            NgCompactDrawerSelectionItem(
                iconRes = R.drawable.ic_folder_open,
                title = stringResource(R.string.export_book_source_save_folder),
            ),
            NgCompactDrawerSelectionItem(
                iconRes = R.drawable.ic_bookshelf_action_upload,
                title = stringResource(R.string.export_book_source_upload_link),
            ),
        )
        NgBottomDrawerSurface(
            modifier = Modifier.fillMaxWidth(),
            contentCardStyle = NgDrawerContentCardStyle.ADAPTIVE,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    // Header/handle drags must reach the existing nested-scroll bridge.
                    // The native sheet owns the offset, settling and dismissal.
                    .scrollable(
                        state = rememberScrollableState { 0f },
                        orientation = Orientation.Vertical,
                    )
                    .navigationBarsPadding()
                    .padding(start = 16.dp, top = 6.dp, end = 16.dp, bottom = 12.dp),
            ) {
                NgDrawerDragHandle(variant = NgDrawerDragHandleVariant.COMPACT)
                NgCompactDrawerHeader(title = stringResource(titleRes))
                Spacer(Modifier.height(2.dp))
                NgCompactDrawerSelectionPanel(
                    items = items,
                    onItemClick = { index ->
                        dialog.dismiss()
                        when (index) {
                            0 -> onShare()
                            1 -> onSaveLocally()
                            2 -> onUpload()
                        }
                    },
                )
            }
        }
    }
}
