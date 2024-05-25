package tech.hombre.freelancehunt.ui.menu

import android.content.Context
import android.os.Bundle
import androidx.annotation.Keep

import tech.hombre.freelancehunt.R
import tech.hombre.freelancehunt.common.EXTRA_1
import tech.hombre.freelancehunt.databinding.BottomMenuCreateThreadBinding
import tech.hombre.freelancehunt.ui.base.BaseBottomDialogFragment

class CreateThreadBottomDialogFragment : BaseBottomDialogFragment<BottomMenuCreateThreadBinding>(BottomMenuCreateThreadBinding::inflate) {

    private var listener: OnCreateThreadListener? = null

    private var profileId = -1

    override fun viewReady() {
        arguments?.let {
            profileId = it.getInt(EXTRA_1, -1)

            binding.buttonCreateThread.setOnClickListener {
                if (correctInputs()) {
                    listener?.onThreadCreated(
                        binding.subject.savedText.toString(),
                        binding.message.savedText.toString(),
                        profileId
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
        return binding.subject.savedText.isNotEmpty() && binding.message.savedText.isNotEmpty()
    }

    interface OnCreateThreadListener {
        fun onThreadCreated(
            subject: String,
            message: String,
            toProfileId: Int
        )
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (parentFragment != null && parentFragment is OnCreateThreadListener) {
            listener = parentFragment as OnCreateThreadListener
        } else if (context is OnCreateThreadListener) {
            listener = context
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    companion object {
        @Keep
        val TAG = CreateThreadBottomDialogFragment::class.java.simpleName

        fun newInstance(profileId: Int): CreateThreadBottomDialogFragment {
            val fragment = CreateThreadBottomDialogFragment()
            val extra = Bundle()
            extra.putInt(EXTRA_1, profileId)
            fragment.arguments = extra
            return fragment
        }
    }
}