package info.nightscout.androidaps.plugins.general.open_humans.views

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import info.nightscout.androidaps.R
import info.nightscout.androidaps.databinding.OpenhumansListItemBinding
import info.nightscout.androidaps.databinding.OpenhumansListItemIndentedBinding

open class OHListItem : FrameLayout {

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init(attrs)
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        init(attrs)
    }

    private fun init(attrs: AttributeSet) {
        context.theme.obtainStyledAttributes(attrs, R.styleable.OHListItem, 0, 0).apply {
            if (getBoolean(R.styleable.OHListItem_indent, false)) {
                OpenhumansListItemIndentedBinding.inflate(LayoutInflater.from(context), this@OHListItem, true).text.text = getString(R.styleable.OHListItem_text)
            } else {
                OpenhumansListItemBinding.inflate(LayoutInflater.from(context), this@OHListItem, true).text.text = getString(R.styleable.OHListItem_text)
            }
            recycle()
        }
    }

}