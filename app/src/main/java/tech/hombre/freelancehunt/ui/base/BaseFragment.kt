package tech.hombre.freelancehunt.ui.base

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import androidx.fragment.app.Fragment
import androidx.viewbinding.ViewBinding
import org.koin.android.ext.android.inject
import org.koin.core.parameter.parametersOf
import tech.hombre.data.local.LocalProperties
import tech.hombre.freelancehunt.R
import tech.hombre.freelancehunt.routing.AppFragmentNavigator
import tech.hombre.freelancehunt.routing.AppNavigator

abstract class BaseFragment<VB : ViewBinding>(private val bindingInflater: (inflater: LayoutInflater) -> VB) :
    Fragment() {

    private var _binding: VB? = null

    val binding: VB
        get() = _binding as VB

    protected val appFragmentNavigator: AppFragmentNavigator by inject { parametersOf(this) }

    protected val appNavigator: AppNavigator by inject { parametersOf(activity) }

    protected val appPreferences: LocalProperties by inject { parametersOf(this) }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return if (_binding == null) {
            _binding = bindingInflater.invoke(inflater)
            return _binding!!.root
        } else _binding!!.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewReady()
    }

    fun openUrl(context: Context, url: String) {
        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        context.startActivity(Intent.createChooser(browserIntent, context.getString(R.string.open)))
    }

    protected fun onBackPressed() = (activity as BaseActivity<*>).onBackPressed()

    abstract fun viewReady()

    open fun showError(errorMessage: String?, rootView: View) {
        (activity as BaseActivity<*>).showError(errorMessage)
    }

    open fun showLoading() {
        binding.root.findViewById<ProgressBar>(R.id.progressBar)
            ?.let { (activity as BaseActivity<*>).showLoading(it) }

    }

    open fun hideLoading() {
        binding.root.findViewById<ProgressBar>(R.id.progressBar)
            ?.let { (activity as BaseActivity<*>).hideLoading(it) }
    }
}