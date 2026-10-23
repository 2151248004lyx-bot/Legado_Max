package io.legado.app.ui.video.player

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import io.legado.app.R
import io.legado.app.lib.theme.accentColor

class SwitchVideoAdapter<T>(
    context: Context,
    private val dataList: List<T>,
    private val titleProvider: (T) -> String = { it.toString() }
) : ArrayAdapter<T>(context, 0, dataList) {

    /** 当前选中项：倍速面板用它高亮正在生效的档位，null 表示不标注 */
    var selectedValue: T? = null
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    private val accentColor: Int by lazy { context.accentColor }
    private val cornerRadius: Float by lazy { 8 * context.resources.displayMetrics.density }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.switch_video_dialog_item, parent, false)
        val textView = view.findViewById<TextView>(R.id.text1)
        val item = dataList[position]
        textView.text = titleProvider(item)
        //选中项：主题色文字 + 主题色半透明底/描边，与播放器其它选中态同一口径。
        //背景用代码生成，避免 ..._selected.xml 里 ?attr/colorPrimary + item:alpha
        //在低版本上被忽略（minSdk 23 的部分机型不认 item 的 android:alpha）
        val selected = selectedValue != null && selectedValue == item
        if (selected) {
            textView.setTextColor(accentColor)
            //GradientDrawable 同时有 setColor(int)/setColor(ColorStateList) 两个重载，
            //Kotlin 的 color 属性会被推断成 ColorStateList 版本，必须显式调 setColor(Int)
            textView.background = GradientDrawable().apply {
                setColor((accentColor and 0x00FFFFFF) or (0x47 shl 24)) // 主题色 28% 透明度
                cornerRadius = this@SwitchVideoAdapter.cornerRadius
                setStroke(1, accentColor)
            }
        } else {
            textView.setTextColor(context.getColor(R.color.video_ctrl_text_primary))
            textView.setBackgroundResource(R.drawable.bg_video_panel_item)
        }
        return view
    }
}