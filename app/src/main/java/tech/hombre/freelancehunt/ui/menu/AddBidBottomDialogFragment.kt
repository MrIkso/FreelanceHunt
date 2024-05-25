package tech.hombre.freelancehunt.ui.menu


import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import androidx.annotation.Keep
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import org.koin.androidx.viewmodel.ext.android.viewModel
import tech.hombre.domain.model.MyBidsList
import tech.hombre.domain.model.ProjectBid
import tech.hombre.freelancehunt.R
import tech.hombre.freelancehunt.common.*
import tech.hombre.freelancehunt.common.extensions.snackbar
import tech.hombre.freelancehunt.common.extensions.subscribe
import tech.hombre.freelancehunt.databinding.BottomMenuAddBidBinding
import tech.hombre.freelancehunt.ui.base.*
import tech.hombre.freelancehunt.ui.menu.model.AddBidsViewModel


class AddBidBottomDialogFragment : BaseBottomDialogFragment<BottomMenuAddBidBinding>(BottomMenuAddBidBinding::inflate) {

    private var listener: OnBidAddedListener? = null

    private val viewModel: AddBidsViewModel by viewModel()

    private var ids = -1
    private var cost = 0
    private var day = 0
    private var budget = MyBidsList.Data.Attributes.Budget()
    private var safe: SafeType? = null
    private var comm = ""
    private var isHiddenBid = false
    private var isPlus = false

    override fun viewReady() {
        is_cancelable = false
        arguments?.let {
            isPlus = it.getBoolean(EXTRA_1)
            ids = it.getInt(EXTRA_2, -1)
            budget = it.getParcelable(EXTRA_3) ?: MyBidsList.Data.Attributes.Budget()
            binding.costValue.setText(budget.amount.toString())
            if (budget.currency.isNotBlank()) {
                try {
                    binding.costType.setSelection(
                        CurrencyType.valueOf(budget.currency).ordinal
                    )
                } catch (e: IllegalArgumentException) {
                    binding.costType.setSelection(CurrencyType.UAH.ordinal)
                }
                binding.costType.isEnabled = false
            }
            binding.hiddenBid.isEnabled = isPlus
            binding.buttonAddBid.setOnClickListener {
                if (correctInputs()) {
                    viewModel.addNewProjectBid(
                        ids,
                        day,
                        budget,
                        safe!!,
                        comm,
                        isHiddenBid
                    )
                } else {
                    showError(getString(R.string.check_inputs))
                }
            }
            viewModel.viewState.subscribe(this, ::handleAddBid)
            return
        }
        error(getString(R.string.init_error))
    }

    private fun handleAddBid(viewState: ViewState<ProjectBid.Data>) {
        hideLoading()
        when (viewState) {
            is Loading -> showLoading()
            is Success -> {
                listener?.onBidAdded(viewState.data)
                dismiss()
            }
            is Error -> handleError(viewState.error.localizedMessage)
            is NoInternetState -> handleError(getString(R.string.no_internet_error_message))
        }
    }

    private fun hideLoading() {
        binding.progressBar.hide()
    }

    private fun showLoading() {
        binding.progressBar.show()
    }

    private fun handleError(error: String) {
        hideLoading()
        showError(error)
    }

    private fun correctInputs(): Boolean {
        cost = binding.costValue.text.toString().toIntOrNull() ?: 0
        val currency = CurrencyType.values()[binding.costType.selectedItemPosition]
        budget = MyBidsList.Data.Attributes.Budget(cost, currency.currency)
        day = binding.days.text.toString().toIntOrNull() ?: 0
        safe = SafeType.values()[binding.safeType.selectedItemPosition]
        val costVerified: Boolean = when {
            currency == CurrencyType.UAH && cost < 200 -> {
                showError(getString(R.string.safe_cost_minimal))
                return false
            }
            currency == CurrencyType.RUB && cost < 600 -> {
                showError(getString(R.string.safe_cost_minimal))
                return false
            }
            else -> true
        }
        if (!costVerified) return false
        comm = binding.comment.savedText.toString()
        if (comm.length < 60) {
            showError(getString(R.string.bid_comment_min))
            return false
        }
        isHiddenBid = binding.hiddenBid.isChecked
        return !(!costVerified || day < 1 || cost < 1 || comm.isEmpty() || safe == null)
    }

    interface OnBidAddedListener {
        fun onBidAdded(
            bid: ProjectBid.Data
        )
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (parentFragment != null && parentFragment is OnBidAddedListener) {
            listener = parentFragment as OnBidAddedListener
        } else if (context is OnBidAddedListener) {
            listener = context
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = BottomSheetDialog(requireContext(), theme)
        dialog.setOnShowListener {

            val bottomSheetDialog = it as BottomSheetDialog
            val parentLayout =
                bottomSheetDialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            parentLayout?.let { it ->
                val behaviour = BottomSheetBehavior.from(it)
                behaviour.isFitToContents = true
                behaviour.state = BottomSheetBehavior.STATE_EXPANDED
                behaviour.isHideable = false
            }
        }
        dialog.setOnKeyListener { _, _, keyEvent ->
            if (keyEvent.keyCode == KeyEvent.KEYCODE_BACK) {
                dismiss()
                true
            } else false
        }
        return dialog
    }

    companion object {
        @Keep
        val TAG = AddBidBottomDialogFragment::class.java.simpleName

        fun newInstance(
            isPlus: Boolean,
            ids: Int,
            budget: MyBidsList.Data.Attributes.Budget?
        ): AddBidBottomDialogFragment {
            val fragment = AddBidBottomDialogFragment()
            val extra = Bundle()
            extra.putBoolean(EXTRA_1, isPlus)
            extra.putInt(EXTRA_2, ids)
            extra.putParcelable(EXTRA_3, budget)
            fragment.arguments = extra
            return fragment
        }
    }
}