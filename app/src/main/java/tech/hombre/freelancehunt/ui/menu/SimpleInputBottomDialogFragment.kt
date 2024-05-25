package tech.hombre.freelancehunt.ui.menu


import android.content.Context
import android.os.Bundle
import androidx.annotation.Keep
import tech.hombre.freelancehunt.R
import tech.hombre.freelancehunt.common.*
import tech.hombre.freelancehunt.common.extensions.gone
import tech.hombre.freelancehunt.common.extensions.visible
import tech.hombre.freelancehunt.common.utils.Utilities
import tech.hombre.freelancehunt.databinding.BottomMenuSimpleInputBinding
import tech.hombre.freelancehunt.ui.base.BaseBottomDialogFragment

class SimpleInputBottomDialogFragment : BaseBottomDialogFragment<BottomMenuSimpleInputBinding>(BottomMenuSimpleInputBinding::inflate) {

    private var listener: OnDialogSubmitListener? = null

    private var type = -1

    private var ids = -1

    private var title = ""

    private var comment = ""

    override fun viewReady() {
        arguments?.let {
            type = it.getInt(EXTRA_1, -1)
            ids = it.getInt(EXTRA_2, -1)
            title = it.getString(EXTRA_3, "")
            comment = it.getString(EXTRA_4, "")

            binding.dialogTitle.text = title
            binding.text.hint = comment

            if (type == SIMPLE_DIALOG_EXTEND_WORKSPACE) {
                binding.text.gone()
                binding.digitInputView.visible()
            }

            binding.digit.filters = arrayOf(Utilities.InputFilterMinMax(1, 999))

            binding.buttonChooseBid.setOnClickListener {
                if (correctInputs()) {
                    listener?.onDialogSubmit(
                        type,
                        ids,
                        binding.text.text.toString(),
                        binding.digit.text.toString().toIntOrNull()
                    )
                    dismiss()
                } else {
                    showError(getString(R.string.check_inputs))
                }
            }
            return
        }
        error(getString(R.string.init_error))
    }

    private fun correctInputs(): Boolean {
        return if (type == SIMPLE_DIALOG_EXTEND_WORKSPACE) !binding.digit.text.isNullOrEmpty() else !binding.text.text.isNullOrEmpty()
    }

    interface OnDialogSubmitListener {
        fun onDialogSubmit(
            type: Int,
            primaryId: Int,
            text: String,
            digit: Int?
        )
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (parentFragment != null && parentFragment is OnDialogSubmitListener) {
            listener = parentFragment as OnDialogSubmitListener
        } else if (context is OnDialogSubmitListener) {
            listener = context
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    companion object {
        @Keep
        val TAG = SimpleInputBottomDialogFragment::class.java.simpleName

        fun newInstance(type: Int, primaryId: Int, title: String, comment: String): SimpleInputBottomDialogFragment {
            val fragment = SimpleInputBottomDialogFragment()
            val extra = Bundle()
            extra.putInt(EXTRA_1, type)
            extra.putInt(EXTRA_2, primaryId)
            extra.putString(EXTRA_3, title)
            extra.putString(EXTRA_4, comment)
            fragment.arguments = extra
            return fragment
        }
    }
}