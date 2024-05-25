package tech.hombre.freelancehunt.ui.freelancers.view

import android.graphics.PorterDuff
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.core.content.ContextCompat
import com.google.android.material.tabs.TabLayout
import org.koin.androidx.viewmodel.ext.android.viewModel
import tech.hombre.domain.model.FreelancerDetail
import tech.hombre.freelancehunt.R
import tech.hombre.freelancehunt.common.EXTRA_1
import tech.hombre.freelancehunt.common.EXTRA_2
import tech.hombre.freelancehunt.common.FreelancerStatus
import tech.hombre.freelancehunt.common.extensions.calculateAge
import tech.hombre.freelancehunt.common.extensions.getColorByFreelancerStatus
import tech.hombre.freelancehunt.common.extensions.getEnding
import tech.hombre.freelancehunt.common.extensions.getFreelancerStatus
import tech.hombre.freelancehunt.common.extensions.getTimeAgo
import tech.hombre.freelancehunt.common.extensions.gone
import tech.hombre.freelancehunt.common.extensions.parseFullDate
import tech.hombre.freelancehunt.common.extensions.parseSimpleDate
import tech.hombre.freelancehunt.common.extensions.snackbar
import tech.hombre.freelancehunt.common.extensions.subscribe
import tech.hombre.freelancehunt.common.extensions.switch
import tech.hombre.freelancehunt.common.extensions.toVisibleState
import tech.hombre.freelancehunt.common.extensions.toast
import tech.hombre.freelancehunt.common.extensions.visible
import tech.hombre.freelancehunt.databinding.ActivityFreelancerDetailBinding
import tech.hombre.freelancehunt.ui.base.BaseActivity
import tech.hombre.freelancehunt.ui.base.Error
import tech.hombre.freelancehunt.ui.base.Loading
import tech.hombre.freelancehunt.ui.base.NoInternetState
import tech.hombre.freelancehunt.ui.base.Success
import tech.hombre.freelancehunt.ui.base.ViewState
import tech.hombre.freelancehunt.ui.freelancers.presentation.FreelancerDetailViewModel
import tech.hombre.freelancehunt.ui.freelancers.presentation.FreelancerPublicViewModel
import tech.hombre.freelancehunt.ui.freelancers.view.pager.PagerFreelancerBids
import tech.hombre.freelancehunt.ui.freelancers.view.pager.PagerFreelancerOverview
import tech.hombre.freelancehunt.ui.freelancers.view.pager.PagerFreelancerReviews
import tech.hombre.freelancehunt.ui.menu.BottomMenuBuilder
import tech.hombre.freelancehunt.ui.menu.CreateThreadBottomDialogFragment


class FreelancerDetailActivity : BaseActivity<ActivityFreelancerDetailBinding>(ActivityFreelancerDetailBinding::inflate),
    CreateThreadBottomDialogFragment.OnCreateThreadListener {

    private val viewModel: FreelancerDetailViewModel by viewModel()

    private val freelancerPublicViewModel: FreelancerPublicViewModel by viewModel()

    var countryId = -1

    var profileId = -1

    var freelancerUrl = ""

    override fun viewReady() {
        setSupportActionBar(binding.toolbar)
        setTitle(R.string.freelancer_view)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        intent?.extras?.let {
            subscribeToData()
            val locally = it.getBoolean(EXTRA_2, false)
            if (!locally) {
                val profile: FreelancerDetail.Data? = it.getParcelable(EXTRA_1)
                initFreelancerDetails(profile!!)
            } else {
                viewModel.getFreelancerDetails(it.getInt(EXTRA_1))
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_freelancer, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_share -> shareUrl(this, freelancerUrl)
            R.id.action_open -> openUrl(this, freelancerUrl)
        }
        return super.onOptionsItemSelected(item)
    }

    private fun subscribeToData() {
        viewModel.viewState.subscribe(this, ::handleViewState)
        viewModel.action.subscribe(this, ::handleActionViewState)
        viewModel.countries.subscribe(this, {
            hideLoading(binding.progressBar)
            if (countryId != -1) {
                val country = it.find { it.id == countryId }
                if (country != null) binding.locationIcon.setUrlSVG("https://freelancehunt.com/static/images/flags/4x3/${country.iso2.toLowerCase()}.svg")
            }
        })
        viewModel.setCountries()
    }

    private fun handleViewState(viewState: ViewState<FreelancerDetail>) {
        when (viewState) {
            is Loading -> showLoading(binding.progressBar)
            is Success -> {
                initFreelancerDetails(viewState.data.data)
                viewModel.setCountries()
            }
            is Error -> handleError(viewState.error.localizedMessage)
            is NoInternetState -> showNoInternetError()
        }
    }

    private fun handleActionViewState(viewState: ViewState<String>) {
        hideLoading(binding.progressBar)
        when (viewState) {
            is Success -> {
                when (viewState.data) {
                    "message" -> toast(getString(R.string.message_sent))
                }
            }

            else -> {}
        }
    }

    private fun initFreelancerDetails(details: FreelancerDetail.Data) {
        hideLoading(binding.progressBar)

        binding.placeholderFreelancer.preview.visibility = View.INVISIBLE
        binding.content.visible()

        profileId = details.id

        freelancerUrl = details.links.self.web

        binding.toolbar.subtitle = details.attributes.login


        binding.avatar.setUrl(details.attributes.avatar.large.url, isCircle = true)
        binding.name.text = "${details.attributes.first_name} ${details.attributes.last_name}"
        binding.login.text = details.attributes.login
        binding.rating.text = details.attributes.rating.toString()
        binding.verified.visibility = details.attributes.verification.identity.toVisibleState()
        binding.isplus.visibility = details.attributes.is_plus_active.toVisibleState()
        binding.status.text = details.attributes.status.name
        binding.status.background.setColorFilter(
            ContextCompat.getColor(
                this,
                getColorByFreelancerStatus(getFreelancerStatus(details.attributes.status.id))
            ), PorterDuff.Mode.SRC_OVER
        )
        binding.location.text = details.attributes.location?.let {
            if (details.attributes.location!!.country != null && details.attributes.location!!.city != null)
                "${details.attributes.location!!.country!!.name}, ${details.attributes.location!!.city!!.name}"
            else if (details.attributes.location!!.country != null)
                details.attributes.location!!.country!!.name else ""
        }
        if (details.attributes.location != null && details.attributes.location!!.country != null) {
            countryId = details.attributes.location!!.country!!.id
        }
        binding.voteup.text = details.attributes.positive_reviews.toString()
        binding.votedown.text = details.attributes.negative_reviews.toString()
        binding.arbitrages.text = details.attributes.arbitrages.toString()
        if (details.attributes.birth_date != null) {
            binding.birthdate.text = details.attributes.birth_date!!.parseSimpleDate()
                ?.let { calculateAge(it).getEnding(this, R.array.ending_years) }
        } else binding.birthdate.gone()

        binding.visitedAt.text = details.attributes.visited_at?.parseFullDate(true).getTimeAgo()

        if (profileId != appPreferences.getCurrentUserId()) {
            binding.buttonMessage.visible()
            if (getFreelancerStatus(details.attributes.status.id) != FreelancerStatus.TEMP_NOT_WORK) {
                binding.buttonMessage.setOnClickListener {
                    BottomMenuBuilder(
                        this,
                        supportFragmentManager,
                        CreateThreadBottomDialogFragment.TAG
                    ).buildMenuForCreateThread(profileId)
                }
            } else binding.buttonMessage.isEnabled = false
        }

        supportFragmentManager.switch(
            R.id.fragmentContainer,
            PagerFreelancerOverview.newInstance(details),
            PagerFreelancerOverview.TAG,
            false
        )

        binding.tabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabReselected(tab: TabLayout.Tab?) {
                binding.containerScroller.scrollTo(0, 0)
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {
            }

            override fun onTabSelected(tab: TabLayout.Tab?) {
                binding.containerScroller.scrollTo(0, 0)
                tab?.position?.let {
                    when (it) {
                        0 -> supportFragmentManager.switch(
                            R.id.fragmentContainer,
                            PagerFreelancerOverview.newInstance(details),
                            PagerFreelancerOverview.TAG,
                            false
                        )
                        1 -> supportFragmentManager.switch(
                            R.id.fragmentContainer,
                            PagerFreelancerBids.newInstance(details.id),
                            PagerFreelancerBids.TAG,
                            false
                        )
                        2 -> supportFragmentManager.switch(
                            R.id.fragmentContainer,
                            PagerFreelancerReviews.newInstance(details.id),
                            PagerFreelancerReviews.TAG,
                            false
                        )
                    }
                }

            }

        })

    }


    private fun handleError(error: String) {
        hideLoading(binding.progressBar)
        showError(error)
    }

    private fun showNoInternetError() {
        hideLoading(binding.progressBar)
        snackbar(getString(R.string.no_internet_error_message), binding.freelancerActivityContainer)
    }

    override fun onThreadCreated(subject: String, message: String, toProfileId: Int) {
        viewModel.sendMessage(toProfileId, subject, message)
    }

}