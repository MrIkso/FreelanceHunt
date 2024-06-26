package tech.hombre.freelancehunt.ui.menu


import android.content.Context
import android.os.Bundle
import androidx.annotation.Keep
import tech.hombre.domain.model.MyBidsList
import tech.hombre.domain.model.WorkspaceDetail
import tech.hombre.freelancehunt.R
import tech.hombre.freelancehunt.common.*
import tech.hombre.freelancehunt.databinding.BottomMenuProposeConditionsBinding
import tech.hombre.freelancehunt.ui.base.BaseBottomDialogFragment

class ProposeConditionsBottomDialogFragment : BaseBottomDialogFragment<BottomMenuProposeConditionsBinding>(BottomMenuProposeConditionsBinding::inflate) {

    private var listener: OnConditionsListener? = null

    private var ids = -1
    private var cost = 0
    private var day = 0
    private var budget = MyBidsList.Data.Attributes.Budget()
    private var safe: SafeType? = null
    private var comm = ""

    override fun viewReady() {
        arguments?.let {
            ids = it.getInt(EXTRA_1, -1)
            val budget_con =
                it.getParcelable<WorkspaceDetail.Data.Attributes.Conditions.Budget>(EXTRA_2)!!
            val safe_type = it.getString(EXTRA_3)
            safe = SafeType.values().find { it.type == safe_type }
            budget = MyBidsList.Data.Attributes.Budget(budget_con.amount, budget_con.currency)
            day = it.getInt(EXTRA_4, -1)

            binding.costValue.setText(budget.amount.toString())
            binding.costType.setSelection(
                CurrencyType.values().find { it.currency == budget.currency }?.ordinal ?: 0
            )
            binding.safeType.setSelection(
                SafeType.values().find { it.type == safe!!.type }?.ordinal ?: 0
            )
            binding.days.setText(day.toString())

            binding.buttonAddConditions.setOnClickListener {
                if (correctInputs()) {
                    listener?.onConditionsChanged(
                        ids,
                        day,
                        budget,
                        safe!!,
                        comm
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
        if (comm.isEmpty()) {
            showError(getString(R.string.propose_comment_min))
            return false
        }
        return !(!costVerified || day < 1 || cost < 1 || comm.isEmpty() || safe == null)
    }

    interface OnConditionsListener {
        fun onConditionsChanged(
            id: Int,
            days: Int,
            budget: MyBidsList.Data.Attributes.Budget,
            safeType: SafeType,
            comment: String
        )
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (parentFragment != null && parentFragment is OnConditionsListener) {
            listener = parentFragment as OnConditionsListener
        } else if (context is OnConditionsListener) {
            listener = context
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    companion object {
        @Keep
        val TAG = ProposeConditionsBottomDialogFragment::class.java.simpleName

        fun newInstance(
            ids: Int,
            budget: WorkspaceDetail.Data.Attributes.Conditions.Budget,
            safe: String,
            days: Int
        ): ProposeConditionsBottomDialogFragment {
            val fragment = ProposeConditionsBottomDialogFragment()
            val extra = Bundle()
            extra.putInt(EXTRA_1, ids)
            extra.putParcelable(EXTRA_2, budget)
            extra.putString(EXTRA_3, safe)
            extra.putInt(EXTRA_4, days)
            fragment.arguments = extra
            return fragment
        }
    }
}