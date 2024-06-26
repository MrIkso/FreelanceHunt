package tech.hombre.freelancehunt.ui.menu


import android.content.Context
import android.os.Bundle
import androidx.annotation.Keep
import tech.hombre.domain.model.ReviewGrades
import tech.hombre.freelancehunt.R
import tech.hombre.freelancehunt.common.EXTRA_1
import tech.hombre.freelancehunt.common.utils.Utilities
import tech.hombre.freelancehunt.databinding.BottomMenuReviewWorkspaceBinding
import tech.hombre.freelancehunt.ui.base.BaseBottomDialogFragment

class ReviewWorkspaceBottomDialogFragment : BaseBottomDialogFragment<BottomMenuReviewWorkspaceBinding>(BottomMenuReviewWorkspaceBinding::inflate) {

    private var listener: OnReviewDialogSubmitListener? = null

    private var ids = -1

    override fun viewReady() {
        arguments?.let {
            ids = it.getInt(EXTRA_1, -1)

            binding.payValue.filters = arrayOf(Utilities.InputFilterMinMax(1, 10))
            binding.requirementsValue.filters = arrayOf(Utilities.InputFilterMinMax(1, 10))
            binding.definitionValue.filters = arrayOf(Utilities.InputFilterMinMax(1, 10))
            binding.connectivityValue.filters = arrayOf(Utilities.InputFilterMinMax(1, 10))

            binding.buttonSubmit.setOnClickListener {
                if (correctInputs()) {
                    listener?.onReviewDialogSubmit(
                        ids,
                        binding.text.savedText.toString(),
                        ReviewGrades(
                            binding.payValue.text.toString().toInt(),
                            binding.definitionValue.text.toString().toInt(),
                            binding.requirementsValue.text.toString().toInt(),
                            binding.connectivityValue.text.toString().toInt()
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
                !binding.payValue.text.isNullOrEmpty() &&
                !binding.requirementsValue.text.isNullOrEmpty() &&
                !binding.definitionValue.text.isNullOrEmpty() &&
                !binding.connectivityValue.text.isNullOrEmpty()
    }

    interface OnReviewDialogSubmitListener {
        fun onReviewDialogSubmit(
            primaryId: Int,
            review: String,
            grades: ReviewGrades
        )
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (parentFragment != null && parentFragment is OnReviewDialogSubmitListener) {
            listener = parentFragment as OnReviewDialogSubmitListener
        } else if (context is OnReviewDialogSubmitListener) {
            listener = context
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    companion object {
        @Keep
        val TAG = ReviewWorkspaceBottomDialogFragment::class.java.simpleName

        fun newInstance(primaryId: Int): ReviewWorkspaceBottomDialogFragment {
            val fragment = ReviewWorkspaceBottomDialogFragment()
            val extra = Bundle()
            extra.putInt(EXTRA_1, primaryId)
            fragment.arguments = extra
            return fragment
        }
    }
}