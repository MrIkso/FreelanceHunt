package tech.hombre.freelancehunt.ui.contest.view

import android.content.Context
import android.content.Intent
import android.view.Menu
import android.view.MenuItem
import com.google.android.material.tabs.TabLayout
import org.koin.androidx.viewmodel.ext.android.viewModel
import tech.hombre.domain.model.ContestDetail
import tech.hombre.domain.model.Countries
import tech.hombre.freelancehunt.R
import tech.hombre.freelancehunt.common.EXTRA_1
import tech.hombre.freelancehunt.common.EXTRA_2
import tech.hombre.freelancehunt.common.extensions.currencyToChar
import tech.hombre.freelancehunt.common.extensions.getEnding
import tech.hombre.freelancehunt.common.extensions.getIconIdBySkillId
import tech.hombre.freelancehunt.common.extensions.getTimeAgo
import tech.hombre.freelancehunt.common.extensions.parseFullDate
import tech.hombre.freelancehunt.common.extensions.snackbar
import tech.hombre.freelancehunt.common.extensions.subscribe
import tech.hombre.freelancehunt.common.extensions.switch
import tech.hombre.freelancehunt.databinding.ActivityContestDetailBinding
import tech.hombre.freelancehunt.ui.base.BaseActivity
import tech.hombre.freelancehunt.ui.base.Error
import tech.hombre.freelancehunt.ui.base.Loading
import tech.hombre.freelancehunt.ui.base.NoInternetState
import tech.hombre.freelancehunt.ui.base.Success
import tech.hombre.freelancehunt.ui.base.ViewState
import tech.hombre.freelancehunt.ui.contest.presentation.ContestDetailViewModel
import tech.hombre.freelancehunt.ui.contest.presentation.ContestPublicViewModel
import tech.hombre.freelancehunt.ui.contest.view.pager.PagerContestComments
import tech.hombre.freelancehunt.ui.contest.view.pager.PagerContestOverview


class ContestDetailActivity : BaseActivity<ActivityContestDetailBinding>(ActivityContestDetailBinding::inflate) {

    private val viewModel: ContestDetailViewModel by viewModel()

    private val contestPublicViewModel: ContestPublicViewModel by viewModel()

    var contestId = 0

    var contestUrl = ""

    var countries = listOf<Countries.Data>()

    override fun viewReady() {
        setSupportActionBar(binding.toolbar)
        setTitle(R.string.contest_view)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        intent?.extras?.let {
            contestId = it.getInt(EXTRA_1, -1)
            subscribeToData()
            val locally = it.getBoolean(EXTRA_2, false)
            if (!locally) {
                val contest: ContestDetail.Data? = it.getParcelable(EXTRA_1)
                initContestDetails(contest!!)
            } else {
                viewModel.getContestDetails(it.getInt(EXTRA_1))
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_contest, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_share -> shareUrl(this, contestUrl)
            R.id.action_open -> openUrl(this, contestUrl)
        }
        return super.onOptionsItemSelected(item)
    }

    private fun subscribeToData() {
        viewModel.viewState.subscribe(this, ::handleViewState)
        viewModel.countries.subscribe(this, {
            hideLoading(binding.progressBar)
            countries = it
        })
        viewModel.setCountries()
    }

    private fun handleViewState(viewState: ViewState<ContestDetail>) {
        when (viewState) {
            is Loading -> showLoading(binding.progressBar)
            is Success -> initContestDetails(viewState.data.data)
            is Error -> handleError(viewState.error.localizedMessage)
            is NoInternetState -> showNoInternetError()
        }
    }

    private fun initContestDetails(details: ContestDetail.Data) {
        hideLoading(binding.progressBar)

        contestUrl = details.links.self.web

        binding.toolbar.subtitle = details.attributes.name


        binding.skillIcon.setUrl(
            "https://freelancehunt.com/static/images/contest/${getIconIdBySkillId(
                details.attributes.skill.id
            )}.png"
        )
        binding.skill.text = details.attributes.skill.name
        binding.name.text = details.attributes.name
        binding.status.text = details.attributes.status.name
        binding.budget.text =
            "${details.attributes.budget.amount} ${currencyToChar(details.attributes.budget!!.currency)}"

        binding.finalAt.text = details.attributes.final_started_at.parseFullDate(true).getTimeAgo()
        binding.applications.text =
            details.attributes.application_count.getEnding(this, R.array.ending_applications)

        supportFragmentManager.switch(
            R.id.fragmentContainer,
            PagerContestOverview.newInstance(details.attributes),
            PagerContestOverview.TAG,
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
                            PagerContestOverview.newInstance(details.attributes),
                            PagerContestOverview.TAG,
                            false
                        )
                        1 -> supportFragmentManager.switch(
                            R.id.fragmentContainer,
                            PagerContestComments.newInstance(details.id),
                            PagerContestComments.TAG,
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
        snackbar(getString(R.string.no_internet_error_message), binding.contestActivityContainer)
    }

    companion object {

        fun startActivity(context: Context, contextId: Int) {
            val intent = Intent(context, ContestDetailActivity::class.java)
            intent.putExtra(EXTRA_1, contextId)
            intent.putExtra(EXTRA_2, true)
            context.startActivity(intent)
        }
    }

}