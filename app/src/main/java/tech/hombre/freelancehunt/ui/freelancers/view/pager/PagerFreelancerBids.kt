package tech.hombre.freelancehunt.ui.freelancers.view.pager

import android.os.Bundle
import androidx.annotation.Keep
import tech.hombre.domain.model.FreelancerDetail
import tech.hombre.freelancehunt.R
import tech.hombre.freelancehunt.common.EXTRA_1
import tech.hombre.freelancehunt.databinding.FragmentPagerFreelancerBidsBinding
import tech.hombre.freelancehunt.ui.base.BaseFragment

class PagerFreelancerBids : BaseFragment<FragmentPagerFreelancerBidsBinding>(FragmentPagerFreelancerBidsBinding::inflate) {

    private var profileId = 0

    override fun viewReady() {
        arguments?.let {
            profileId = it.getInt(EXTRA_1)
            if (profileId != 0) {
                //initOverview(details!!)
            }
        }
    }

    private fun initOverview(details: FreelancerDetail.Data) {

    }

    companion object {
        @Keep
        val TAG = PagerFreelancerBids::class.java.simpleName

        fun newInstance(profileId: Int): PagerFreelancerBids {
            val fragment = PagerFreelancerBids()
            val extra = Bundle()
            extra.putInt(EXTRA_1, profileId)
            fragment.arguments = extra
            return fragment
        }

    }
}