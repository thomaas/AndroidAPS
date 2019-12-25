package info.nightscout.androidaps.utils

import dagger.android.support.DaggerFragment
import io.reactivex.disposables.CompositeDisposable

/**
 * Created by adrian on 2019-12-25.
 */

open class BaseFragment : DaggerFragment() {

    protected val compositeDisposable = CompositeDisposable()

    override fun onDestroy() {
        compositeDisposable.clear()
        super.onDestroy()
    }
}