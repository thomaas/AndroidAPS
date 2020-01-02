package info.nightscout.androidaps.plugins.general.open_humans.views

import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.app.DialogFragment
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.preference.Preference
import android.util.AttributeSet
import info.nightscout.androidaps.MainApp
import info.nightscout.androidaps.R
import info.nightscout.androidaps.plugins.general.open_humans.OpenHumansUploader
import info.nightscout.androidaps.plugins.general.open_humans.activities.OHWelcomeActivity

class OpenHumansPreference : Preference {

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?) : super(context)

    init {
        update()
    }

    fun update() {
        if (OpenHumansUploader.projectMemberId == null) {
            setTitle(R.string.sign_in_to_open_humans)
            summary = null
        } else {
            setTitle(R.string.sign_out)
            summary = MainApp.gs(R.string.member_id, OpenHumansUploader.projectMemberId)
        }
    }

    override fun onClick() {
        super.onClick()
        if (OpenHumansUploader.projectMemberId != null) {
            ConfirmDialog().show((context as Activity).fragmentManager, "OHSignOutConfirmDialog")
        } else {
            val activity = context as Activity
            activity.startActivity(Intent(activity, OHWelcomeActivity::class.java))
        }
    }

    class ConfirmDialog : DialogFragment() {

        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog = AlertDialog.Builder(context)
                .setTitle(R.string.sign_out)
                .setMessage(R.string.do_you_really_want_to_sign_out)
                .setPositiveButton(R.string.yes) { dialog, which ->
                    OpenHumansUploader.logout()
                }
                .setNegativeButton(R.string.cancel, null)
                .create()

    }

}