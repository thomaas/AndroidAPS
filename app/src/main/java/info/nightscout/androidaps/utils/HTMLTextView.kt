package info.nightscout.androidaps.utils

import android.content.Context
import android.text.Html
import android.util.AttributeSet
import android.widget.TextView

class HTMLTextView : TextView {

    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes)

    init {
        var level = -1
        text = Html.fromHtml(text.toString(), null, { opening, tag, output, xmlReader ->
            when (tag) {
                "ohul" -> if (opening) level++ else level--
                "ohli" -> {
                    if (opening) {
                        for (i in 1..level) output.append("\t")
                        output.append("• ")
                    } else {
                        output.append("\n")
                    }
                }
            }
        })
    }

}