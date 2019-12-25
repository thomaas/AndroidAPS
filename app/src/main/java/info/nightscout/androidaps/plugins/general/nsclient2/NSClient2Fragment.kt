package info.nightscout.androidaps.plugins.general.nsclient2

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import info.nightscout.androidaps.R
import info.nightscout.androidaps.utils.BaseFragment
import kotlinx.android.synthetic.main.nsclient2_fragment.*
import javax.inject.Inject

/**
 * Created by adrian on 2019-12-25.
 */

class NSClient2Fragment : BaseFragment() {

    @Inject
    lateinit var viewModel: NSClient2Plugin

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.nsclient2_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        nsclient2_test_Button.setOnClickListener { viewModel.testConnection() }

        viewModel.testResultLiveData.observe(viewLifecycleOwner, Observer {
            nsclient2_test_textoutput.text = it
        })

    }
}