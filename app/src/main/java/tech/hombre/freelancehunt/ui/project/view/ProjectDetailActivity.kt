package tech.hombre.freelancehunt.ui.project.view

import android.content.Context
import android.content.Intent
import android.view.Menu
import android.view.MenuItem
import android.view.View
import com.google.android.material.tabs.TabLayout
import org.koin.androidx.viewmodel.ext.android.viewModel
import tech.hombre.domain.model.Countries
import tech.hombre.domain.model.MyBidsList
import tech.hombre.domain.model.ProjectBid
import tech.hombre.domain.model.ProjectDetail
import tech.hombre.freelancehunt.R
import tech.hombre.freelancehunt.common.*
import tech.hombre.freelancehunt.common.extensions.*
import tech.hombre.freelancehunt.databinding.ActivityProjectDetailBinding
import tech.hombre.freelancehunt.ui.base.*
import tech.hombre.freelancehunt.ui.menu.AddBidBottomDialogFragment
import tech.hombre.freelancehunt.ui.menu.BottomMenuBuilder
import tech.hombre.freelancehunt.ui.project.presentation.ProjectDetailViewModel
import tech.hombre.freelancehunt.ui.project.presentation.ProjectPublicViewModel
import tech.hombre.freelancehunt.ui.project.view.pager.PagerProjectBids
import tech.hombre.freelancehunt.ui.project.view.pager.PagerProjectComments
import tech.hombre.freelancehunt.ui.project.view.pager.PagerProjectOverview

class ProjectDetailActivity : BaseActivity<ActivityProjectDetailBinding>(ActivityProjectDetailBinding::inflate), AddBidBottomDialogFragment.OnBidAddedListener {

    private val viewModel: ProjectDetailViewModel by viewModel()

    private val projectPublicViewModel: ProjectPublicViewModel by viewModel()

    var countries = listOf<Countries.Data>()

    private var projectUrl = ""

    private var statusId = 0

    private var isOnlyForPlus = false

    override fun viewReady() {
        setSupportActionBar(binding.toolbar)
        setTitle(R.string.project_view)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        intent?.extras?.let {
            subscribeToData()
            val locally = it.getBoolean(EXTRA_2, false)
            if (!locally) {
                val project: ProjectDetail.Data? = it.getParcelable(EXTRA_1)
                initProjectDetails(project!!)
            } else {
                viewModel.getProjectDetails("projects/${it.getInt(EXTRA_1)}")
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_project, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_share -> shareUrl(this, projectUrl)
            R.id.action_open -> openUrl(this, projectUrl)
        }
        return super.onOptionsItemSelected(item)
    }

    private fun subscribeToData() {
        viewModel.viewState.subscribe(this, ::handleViewState)
        viewModel.countries.subscribe(this, {
            countries = it
            hideLoading(binding.progressBar)
        })
        viewModel.setCountries()
    }

    private fun handleViewState(viewState: ViewState<ProjectDetail>) {
        when (viewState) {
            is Loading -> showLoading(binding.progressBar)
            is Success -> initProjectDetails(viewState.data.data)
            is Error -> {
                handleError(viewState.error.localizedMessage)
                finish()
            }
            is NoInternetState -> showNoInternetError()
        }
    }

    private fun hideShowFab(position: Int) {
        when (position) {
            0 -> binding.fab.hide()
            1 -> {
                if (isOnlyForPlus && appPreferences.getCurrentUserProfile()?.is_plus_active != true) {
                    binding.fab.hide()
                    return
                }
                val status =
                    ProjectStatus.values().find { it.id == statusId }
                if (appPreferences.getCurrentUserType() == UserType.FREELANCER.type && status == ProjectStatus.OPEN_FOR_PROPOSALS) {
                    binding.fab.setImageResource(R.drawable.bid)
                    binding.fab.show()

                } else binding.fab.hide()
            }
            2 -> binding.fab.hide()
        }
    }

    private fun initProjectDetails(details: ProjectDetail.Data) {
        hideLoading(binding.progressBar)

        binding.placeholderProject.preview.visibility = View.INVISIBLE
        binding.content.visible()

        projectUrl = details.links.self.web

        isOnlyForPlus = details.attributes.is_only_for_plus
        statusId = details.attributes.status.id

        binding.toolbar.subtitle = details.attributes.name

        binding.isplus.visibility = details.attributes.is_only_for_plus.toVisibleState()
        binding.premium.visibility = details.attributes.is_premium.toVisibleState()

        binding.safe.text = getTitleBySafeType(
            this,
            SafeType.values().find { it.type == details.attributes.safe_type }
                ?: SafeType.EMPLOYER)

        binding.isremote.visibility = details.attributes.is_remote_job.toVisibleState()
        binding.name.text = details.attributes.name
        binding.status.text = details.attributes.status.name

        if (details.attributes.budget != null) {
            binding.budget.text =
                "${details.attributes.budget!!.amount} ${currencyToChar(details.attributes.budget!!.currency)}"
        } else binding.budget.text = getString(R.string.budget_nan)

        binding.expiredAt.text = details.attributes.expired_at.parseFullDate(true).getTimeAgo()

        binding.expiredAt.setOnClickListener {
            toast(details.attributes.expired_at.parseFullDate(true).toString())
        }

        binding.fab.setOnClickListener {
            when (binding.tabs.selectedTabPosition) {
                1 -> {
                    if (details.attributes.is_only_for_plus && appPreferences.getCurrentUserProfile()?.is_plus_active == true)
                        BottomMenuBuilder(
                            this,
                            supportFragmentManager,
                            AddBidBottomDialogFragment.TAG
                        ).buildMenuForAddBid(
                            appPreferences.getCurrentUserProfile()?.is_plus_active ?: false,
                            details.id,
                            if (details.attributes.budget != null) details.attributes.budget.let { budget ->
                                MyBidsList.Data.Attributes.Budget(budget!!.amount, budget.currency)
                            } else null
                        )
                    else if (!details.attributes.is_only_for_plus) {
                        BottomMenuBuilder(
                            this,
                            supportFragmentManager,
                            AddBidBottomDialogFragment.TAG
                        ).buildMenuForAddBid(
                            appPreferences.getCurrentUserProfile()?.is_plus_active ?: false,
                            details.id,
                            if (details.attributes.budget != null) details.attributes.budget.let { budget ->
                                MyBidsList.Data.Attributes.Budget(budget!!.amount, budget.currency)
                            } else null
                        )
                    } else {
                        handleError(getString(R.string.only_for_plus))
                    }
                }
            }
        }

        supportFragmentManager.switch(
            R.id.fragmentContainer,
            PagerProjectOverview.newInstance(details.attributes),
            PagerProjectOverview.TAG,
            false
        )

        binding.tabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabReselected(tab: TabLayout.Tab?) {
                binding. containerScroller.scrollTo(0, 0)
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {
            }

            override fun onTabSelected(tab: TabLayout.Tab?) {
                binding.containerScroller.scrollTo(0, 0)
                tab?.position?.let {
                    hideShowFab(it)
                    when (it) {
                        0 -> supportFragmentManager.switch(
                            R.id.fragmentContainer,
                            PagerProjectOverview.newInstance(details.attributes),
                            PagerProjectOverview.TAG,
                            false
                        )
                        1 -> supportFragmentManager.switch(
                            R.id.fragmentContainer,
                            PagerProjectBids.newInstance(
                                details.id,
                                details.attributes.employer?.id ?: 0
                            ),
                            PagerProjectBids.TAG,
                            false
                        )
                        2 -> supportFragmentManager.switch(
                            R.id.fragmentContainer,
                            PagerProjectComments.newInstance(details.id),
                            PagerProjectComments.TAG,
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
        snackbar(getString(R.string.no_internet_error_message), binding.projectActivityContainer)
    }

    override fun onBidAdded(
        bid: ProjectBid.Data
    ) {
        (supportFragmentManager.findFragmentByTag(PagerProjectBids.TAG) as PagerProjectBids).onBidAdded(
            bid
        )
    }

    companion object {

        fun startActivity(context: Context, projectId: Int) {
            val intent = Intent(context, ProjectDetailActivity::class.java)
            intent.putExtra(EXTRA_1, projectId)
            intent.putExtra(EXTRA_2, true)
            context.startActivity(intent)
        }
    }


}