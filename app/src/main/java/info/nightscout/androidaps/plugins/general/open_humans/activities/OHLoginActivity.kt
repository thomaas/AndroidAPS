package info.nightscout.androidaps.plugins.general.open_humans.activities

import android.app.Activity
import android.app.Dialog
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.browser.customtabs.*
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import info.nightscout.androidaps.R
import info.nightscout.androidaps.databinding.Openhumans2Binding
import info.nightscout.androidaps.plugins.general.open_humans.OpenHumansUploader
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers

class OHLoginActivity : AppCompatActivity() {

    private lateinit var customTabsClient: CustomTabsClient
    private lateinit var customTabsSession: CustomTabsSession
    private lateinit var binding: Openhumans2Binding

    private val connection = object : CustomTabsServiceConnection() {
        override fun onCustomTabsServiceConnected(name: ComponentName, client: CustomTabsClient) {
            customTabsClient = client
            customTabsClient.warmup(0)
            customTabsSession = customTabsClient.newSession(customTabsCallback)!!
            customTabsSession.mayLaunchUrl(Uri.parse(OpenHumansUploader.AUTH_URL), null, null)
        }

        override fun onServiceDisconnected(name: ComponentName?) {
        }
    }

    private val customTabsCallback = object : CustomTabsCallback() {

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        CustomTabsClient.bindCustomTabsService(this, "com.android.chrome", connection)
        binding = Openhumans2Binding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.back.setOnClickListener { finish() }
        binding.login.setOnClickListener {
            if (binding.acceptTerms.isChecked) {
                CustomTabsIntent.Builder().setSession(customTabsSession).build().launchUrl(this@OHLoginActivity, Uri.parse(OpenHumansUploader.AUTH_URL))
            } else {
                binding.termsOfUseError.visibility = View.VISIBLE
            }
        }
        binding.acceptTerms.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) binding.termsOfUseError.visibility = View.GONE
        }
        binding.aboutOpenHumans.setOnClickListener { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.openhumans.org/about/"))) }
        binding.uploadOnOpenHumans.setOnClickListener { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.openhumans.org/activity/androidaps-uploader/"))) }
        binding.contactOpenHumans.setOnClickListener { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.openhumans.org/contact-us/"))) }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("touErrorVisible", binding.termsOfUseError.visibility == View.VISIBLE)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        binding.termsOfUseError.visibility = if (savedInstanceState.getBoolean("touErrorVisible")) View.VISIBLE else View.GONE
        super.onRestoreInstanceState(savedInstanceState)
    }

    override fun onDestroy() {
        super.onDestroy()
        unbindService(connection)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val code = intent.data?.getQueryParameter("code")
        if (supportFragmentManager.fragments.size == 0 && code != null) {
            ExchangeAuthTokenDialog(code).show(supportFragmentManager, "ExchangeAuthTokenDialog")
        }
    }

    class ExchangeAuthTokenDialog : DialogFragment() {

        private var disposable: Disposable? = null

        init {
            isCancelable = false
        }

        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            return MaterialAlertDialogBuilder(activity!!)
                    .setTitle(R.string.finishing_setup)
                    .setMessage(R.string.please_wait)
                    .create()
        }

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            disposable = OpenHumansUploader.login(arguments?.getString("authToken")!!).subscribeOn(Schedulers.io()).subscribe({
                dismiss()
                SetupDoneDialog().show(fragmentManager!!, "SetupDoneDialog")
            }, {
                dismiss()
                ErrorDialog(it.message).show(fragmentManager!!, "ErrorDialog")
            })
        }


        override fun onDestroy() {
            disposable?.dispose()
            super.onDestroy()
        }

        companion object {

            operator fun invoke(authToken: String): ExchangeAuthTokenDialog {
                val dialog = ExchangeAuthTokenDialog()
                val args = Bundle()
                args.putString("authToken", authToken)
                dialog.arguments = args
                return dialog
            }

        }

    }

    class ErrorDialog : DialogFragment() {

        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            val message = arguments?.getString("message")
            val shownMessage = if (message == null) getString(R.string.an_error_has_occurred)
            else "${getString(R.string.an_error_has_occurred)}\n\n$message"
            return MaterialAlertDialogBuilder(activity!!)
                    .setTitle(R.string.error)
                    .setMessage(shownMessage)
                    .setPositiveButton(R.string.close, null)
                    .create()
        }

        companion object {

            operator fun invoke(message: String?): ErrorDialog {
                val dialog = ErrorDialog()
                val args = Bundle()
                args.putString("message", message)
                dialog.arguments = args
                return dialog
            }
        }
    }

    class SetupDoneDialog : DialogFragment() {

        init {
            isCancelable = false
        }

        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            return MaterialAlertDialogBuilder(activity!!)
                    .setTitle(R.string.setup_done)
                    .setMessage(R.string.thanks_for_uploading_your_data)
                    .setCancelable(false)
                    .setPositiveButton(R.string.close) { dialog, which ->
                        activity!!.run {
                            setResult(Activity.RESULT_OK)
                            activity!!.finish()
                        }
                    }
                    .create()
        }
    }

}