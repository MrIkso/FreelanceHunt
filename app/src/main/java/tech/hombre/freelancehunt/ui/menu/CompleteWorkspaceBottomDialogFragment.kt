package tech.hombre.freelancehunt.ui.menu

import android.content.Context
import android.os.Bundle
import androidx.annotation.Keep
import tech.hombre.domain.model.CompleteGrades
import tech.hombre.freelancehunt.R
import tech.hombre.freelancehunt.common.EXTRA_1
import tech.hombre.freelancehunt.common.EXTRA_2
import tech.hombre.freelancehunt.common.utils.Utilities
import tech.hombre.freelancehunt.databinding.BottomMenuCompleteWorkspaceBinding
import tech.hombre.freelancehunt.ui.base.BaseBottomDialogFragment

class CompleteWorkspaceBottomDialogFragment :
    BaseBottomDialogFragment<BottomMenuCompleteWorkspaceBinding>(BottomMenuCompleteWorkspaceBinding::inflate) {

    private var listener: OnCompleteDialogSubmitListener? = null

    private var ids = -1

    private var isComplete = false

    override fun viewReady() {
        arguments?.let {
            ids = it.getInt(EXTRA_1, -1)
            isComplete = it.getBoolean(EXTRA_2, false)

            binding.qualityValue.filters = arrayOf(Utilities.InputFilterMinMax(1, 10))
            binding.professionalismValue.filters = arrayOf(Utilities.InputFilterMinMax(1, 10))
            binding.costValue.filters = arrayOf(Utilities.InputFilterMinMax(1, 10))
            binding.connectivityValue.filters = arrayOf(Utilities.InputFilterMinMax(1, 10))
            binding.scheduleValue.filters = arrayOf(Utilities.InputFilterMinMax(1, 10))

            binding.buttonSubmit.setOnClickListener {
                if (correctInputs()) {
                    listener?.onCompleteDialogSubmit(
                        ids,
                        isComplete,
                        binding.text.savedText.toString(),
                        CompleteGrades(
                            binding.qualityValue.text.toString().toInt(),
                            binding.professionalismValue.text.toString().toInt(),
                            binding.costValue.text.toString().toInt(),
                            binding.connectivityValue.text.toString().toInt(),
                            binding.scheduleValue.text.toString().toInt()
                        )
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
        return binding.text.savedText.isNotEmpty() &&
                !binding.qualityValue.text.isNullOrEmpty() &&
                !binding.professionalismValue.text.isNullOrEmpty() &&
                !binding.costValue.text.isNullOrEmpty() &&
                !binding.connectivityValue.text.isNullOrEmpty() &&
                !binding.scheduleValue.text.isNullOrEmpty()
    }

    interface OnCompleteDialogSubmitListener {
        fun onCompleteDialogSubmit(
            primaryId: Int,
            isComplete: Boolean,
            review: String,
            grades: CompleteGrades
        )
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (parentFragment != null && parentFragment is OnCompleteDialogSubmitListener) {
            listener = parentFragment as OnCompleteDialogSubmitListener
        } else if (context is OnCompleteDialogSubmitListener) {
            listener = context
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    companion object {
        @Keep
        val TAG = CompleteWorkspaceBottomDialogFragment::class.java.simpleName

        fun newInstance(
            primaryId: Int,
            isComplete: Boolean
        ): CompleteWorkspaceBottomDialogFragment {
            val fragment = CompleteWorkspaceBottomDialogFragment()
            val extra = Bundle()
            extra.putInt(EXTRA_1, primaryId)
            extra.putBoolean(EXTRA_2, isComplete)
            fragment.arguments = extra
            return fragment
        }
    }
}