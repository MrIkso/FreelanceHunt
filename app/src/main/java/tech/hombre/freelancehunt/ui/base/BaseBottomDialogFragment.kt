package tech.hombre.freelancehunt.ui.base

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.viewbinding.ViewBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import org.koin.android.ext.android.inject
import org.koin.core.parameter.parametersOf
import tech.hombre.data.local.LocalProperties
import tech.hombre.freelancehunt.common.EMPTY_STRING
import tech.hombre.freelancehunt.common.extensions.toast

abstract class BaseBottomDialogFragment<VB : ViewBinding>(private val bindingInflater: (inflater: LayoutInflater) -> VB) : BottomSheetDialogFragment() {

    private var _binding: VB? = null

    val binding: VB
        get() = _binding as VB

    protected val appPreferences: LocalProperties by inject { parametersOf(this) }

    abstract fun viewReady()

    protected var is_cancelable: Boolean = true

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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewReady()
        dialog?.setCanceledOnTouchOutside(is_cancelable)
        dialog?.setCancelable(is_cancelable)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    fun showError(errorMessage: String?) =
        toast(errorMessage ?: EMPTY_STRING)
}