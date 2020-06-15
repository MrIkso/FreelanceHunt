package tech.hombre.freelancehunt.ui.project.view

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import com.google.android.material.tabs.TabLayout
import kotlinx.android.synthetic.main.activity_project_detail.*
import org.koin.androidx.viewmodel.ext.android.viewModel
import tech.hombre.domain.model.Countries
import tech.hombre.domain.model.MyBidsList
import tech.hombre.domain.model.ProjectDetail
import tech.hombre.freelancehunt.R
import tech.hombre.freelancehunt.common.*
import tech.hombre.freelancehunt.common.extensions.*
import tech.hombre.freelancehunt.ui.base.*
import tech.hombre.freelancehunt.ui.menu.AddBidBottomDialogFragment
import tech.hombre.freelancehunt.ui.menu.BottomMenuBuilder
import tech.hombre.freelancehunt.ui.project.presentation.ProjectDetailViewModel
import tech.hombre.freelancehunt.ui.project.presentation.ProjectPublicViewModel
import tech.hombre.freelancehunt.ui.project.view.pager.PagerProjectBids
import tech.hombre.freelancehunt.ui.project.view.pager.PagerProjectComments
import tech.hombre.freelancehunt.ui.project.view.pager.PagerProjectOverview

class ProjectDetailActivity : BaseActivity(), AddBidBottomDialogFragment.OnBidAddedListener {

    override fun isPrivate() = false

    private val viewModel: ProjectDetailViewModel by viewModel()

    private val projectPublicViewModel: ProjectPublicViewModel by viewModel()

    var countries = listOf<Countries.Data>()

    var projectUrl = ""

    var statusId = 0

    var isOnlyForPlus = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_project_detail)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        if (savedInstanceState == null) {
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
        }
        return super.onOptionsItemSelected(item)
    }

    private fun subscribeToData() {
        viewModel.viewState.subscribe(this, ::handleViewState)
        viewModel.countries.subscribe(this, {
            countries = it
            hideLoading(progressBar)
        })
        viewModel.setCountries()
    }

    private fun handleViewState(viewState: ViewState<ProjectDetail>) {
        when (viewState) {
            is Loading -> showLoading(progressBar)
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
            0 -> fab.hide()
            1 -> {
                if (isOnlyForPlus && appPreferences.getCurrentUserProfile()?.is_plus_active != true) {
                    fab.hide()
                    return
                }
                val status =
                    ProjectStatus.values().find { it.id == statusId }
                if (appPreferences.getCurrentUserType() == UserType.FREELANCER.type && status == ProjectStatus.OPEN_FOR_PROPOSALS) {
                    fab.setImageResource(R.drawable.bid)
                    fab.show()

                } else fab.hide()
            }
            2 -> fab.hide()
        }
    }

    private fun initProjectDetails(details: ProjectDetail.Data) {
        hideLoading(progressBar)

        projectUrl = details.links.self.web

        isOnlyForPlus = details.attributes.is_only_for_plus
        statusId = details.attributes.status.id

        toolbar.subtitle = details.attributes.name

        isplus.visibility = details.attributes.is_only_for_plus.toVisibleState()
        premium.visibility = details.attributes.is_premium.toVisibleState()

        safe.text = getTitleBySafeType(
            this,
            SafeType.values().find { it.type == details.attributes.safe_type }
                ?: SafeType.DIRECT_PAYMENT)

        isremote.visibility = details.attributes.is_remote_job.toVisibleState()
        name.text = details.attributes.name
        status.text = details.attributes.status.name

        if (details.attributes.budget != null) {
            budget.text =
                "${details.attributes.budget!!.amount} ${currencyToChar(details.attributes.budget!!.currency)}"
        } else budget.text = getString(R.string.budget_nan)

        expiredAt.text = details.attributes.expired_at.parseFullDate(true).getTimeAgo()

        fab.setOnClickListener {
            when (tabs.selectedTabPosition) {
                1 -> {
                    if (details.attributes.is_only_for_plus && appPreferences.getCurrentUserProfile()?.is_plus_active == true)
                        BottomMenuBuilder(
                            supportFragmentManager,
                            AddBidBottomDialogFragment.TAG
                        ).buildMenuForAddBid(
                            appPreferences.getCurrentUserProfile()?.is_plus_active ?: false,
                            details.id
                        )
                    else if (!details.attributes.is_only_for_plus) {
                        BottomMenuBuilder(
                            supportFragmentManager,
                            AddBidBottomDialogFragment.TAG
                        ).buildMenuForAddBid(
                            appPreferences.getCurrentUserProfile()?.is_plus_active ?: false,
                            details.id
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

        tabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabReselected(tab: TabLayout.Tab?) {
                containerScroller.scrollTo(0, 0)
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {
            }

            override fun onTabSelected(tab: TabLayout.Tab?) {
                containerScroller.scrollTo(0, 0)
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
        hideLoading(progressBar)
        showError(error)
    }


    private fun showNoInternetError() {
        hideLoading(progressBar)
        snackbar(getString(R.string.no_internet_error_message), projectActivityContainer)
    }

    override fun onBidAdded(
        id: Int,
        days: Int,
        budget: MyBidsList.Data.Attributes.Budget,
        safeType: SafeType,
        comment: String,
        isHidden: Boolean
    ) {
        (supportFragmentManager.findFragmentByTag(PagerProjectBids.TAG) as PagerProjectBids).onBidAdded(
            id,
            days,
            budget,
            safeType,
            comment,
            isHidden
        )
    }


}