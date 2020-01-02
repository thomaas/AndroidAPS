package info.nightscout.androidaps.plugins.general.open_humans.views

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import info.nightscout.androidaps.R
import info.nightscout.androidaps.databinding.OpenhumansTutorialItemBinding

class OHTutorialItem : FrameLayout {

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init(attrs)
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        init(attrs)
    }

    private fun init(attrs: AttributeSet) {
        val binding = OpenhumansTutorialItemBinding.inflate(LayoutInflater.from(context), this, true)
        context.theme.obtainStyledAttributes(attrs, R.styleable.OHTutorialItem, 0, 0).apply {
            binding.index.text = getString(R.styleable.OHTutorialItem_index)
            binding.title.text = getString(R.styleable.OHTutorialItem_title)
            binding.description.text = getString(R.styleable.OHTutorialItem_description)
            recycle()
        }
    }

}