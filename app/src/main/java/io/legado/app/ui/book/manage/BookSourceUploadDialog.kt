package io.legado.app.ui.book.manage

import android.app.Application
import android.os.Bundle
import android.view.View
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.viewModels
import io.legado.app.R
import io.legado.app.base.BaseComposeDialogFragment
import io.legado.app.base.BaseViewModel
import io.legado.app.help.DirectLinkUpload
import io.legado.app.ui.design.components.compose.NgDialog
import io.legado.app.ui.design.components.compose.NgDialogTextActionButton
import io.legado.app.ui.design.theme.NgAppTheme
import io.legado.app.ui.design.theme.NgTheme
import io.legado.app.ui.widget.dialog.applyNgDialogWindow
import io.legado.app.utils.sendToClip
import java.io.File

/** 两个书源导出入口共用上传过程和只读结果，旋转时由 ViewModel 保留任务。 */
class BookSourceUploadDialog : BaseComposeDialogFragment() {
    private val viewModel by viewModels<BookSourceUploadViewModel>()

    override fun onStart() {
        super.onStart()
        applyNgDialogWindow()
    }

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        viewModel.upload(
            File(requireArguments().getString("path")!!),
            requireArguments().getString("name")!!,
        )
        (view as ComposeView).apply {
            setBackgroundColor(android.graphics.Color.TRANSPARENT)
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                NgAppTheme(updateSystemBars = false) {
                    val url = viewModel.url
                    val error = viewModel.error
                    NgDialog(
                        title = stringResource(
                            when {
                                url != null -> R.string.export_success
                                error != null -> R.string.upload_book_fail
                                else -> R.string.exporting
                            }
                        ),
                        actions = {
                            NgDialogTextActionButton(
                                text = stringResource(if (url != null) R.string.export_book_source_copy_link else R.string.cancel),
                                onClick = {
                                    if (url != null) requireContext().sendToClip(url)
                                    dismiss()
                                },
                            )
                        },
                    ) {
                        Column(Modifier.heightIn(max = 300.dp).verticalScroll(rememberScrollState())) {
                            Text(viewModel.summary, color = Color(NgTheme.colors.onSurfaceVariant), fontSize = 14.sp)
                            if (url != null || error != null) {
                                Spacer(Modifier.height(16.dp))
                                SelectionContainer {
                                    Text(
                                        text = url ?: error.orEmpty(),
                                        modifier = Modifier.fillMaxWidth(),
                                        color = Color(NgTheme.colors.onSurface),
                                        fontSize = 16.sp,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    companion object {
        fun create(file: File, name: String) = BookSourceUploadDialog().apply {
            arguments = Bundle().apply {
                putString("path", file.absolutePath)
                putString("name", name)
            }
        }
    }
}

class BookSourceUploadViewModel(application: Application) : BaseViewModel(application) {
    var url by mutableStateOf<String?>(null)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var summary by mutableStateOf("")
        private set
    private var started = false

    fun upload(file: File, name: String) {
        if (started) return
        started = true
        val rule = DirectLinkUpload.getRule().copy()
        summary = rule.summary
        execute {
            // 上传器会删除传入的文件，因此只交付独立临时副本，不消费已有导出文件。
            val copy = File.createTempFile("source-upload-", ".tmp", context.cacheDir)
            try {
                file.copyTo(copy, overwrite = true)
                DirectLinkUpload.upLoad(
                    name, copy,
                    if (name.endsWith(".js", ignoreCase = true)) "application/javascript" else "application/json",
                    rule,
                )
            } finally {
                copy.delete()
            }
        }.onSuccess {
            url = it
        }.onError {
            error = it.localizedMessage ?: context.getString(R.string.upload_book_fail)
        }
    }
}
