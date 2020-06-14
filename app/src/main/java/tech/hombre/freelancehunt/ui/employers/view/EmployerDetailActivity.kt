package tech.hombre.freelancehunt.ui.employers.view

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.badge.BadgeDrawable
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.android.synthetic.main.activity_employer_detail.*
import kotlinx.android.synthetic.main.appbar_fill.*
import org.koin.androidx.viewmodel.ext.android.viewModel
import tech.hombre.domain.model.EmployerDetail
import tech.hombre.domain.model.FreelancerDetail
import tech.hombre.freelancehunt.R
import tech.hombre.freelancehunt.common.EXTRA_1
import tech.hombre.freelancehunt.common.EXTRA_2
import tech.hombre.freelancehunt.common.extensions.*
import tech.hombre.freelancehunt.ui.base.*
import tech.hombre.freelancehunt.ui.employers.presentation.EmployerDetailViewModel
import tech.hombre.freelancehunt.ui.employers.presentation.EmployerPublicViewModel
import tech.hombre.freelancehunt.ui.employers.view.pager.PagerEmployerOverview
import tech.hombre.freelancehunt.ui.employers.view.pager.PagerEmployerProjects
import tech.hombre.freelancehunt.ui.employers.view.pager.PagerEmployerReviews
import tech.hombre.freelancehunt.ui.menu.BottomMenuBuilder
import tech.hombre.freelancehunt.ui.menu.CreateThreadBottomDialogFragment


class EmployerDetailActivity : BaseActivity(), CreateThreadBottomDialogFragment.OnCreateThreadListener {

    override fun isPrivate() = false

    private val viewModel: EmployerDetailViewModel by viewModel()

    private val employerPublicViewModel: EmployerPublicViewModel by viewModel()

    var countryId = -1

    var profileId = -1

    var employerUrl = ""

    private lateinit var pagerAdapter: PagerAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_employer_detail)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        if (savedInstanceState == null) {
            intent?.extras?.let {
                subscribeToData()
                val locally = it.getBoolean(EXTRA_2, false)
                if (!locally) {
                    val profile: EmployerDetail.Data?  = it.getParcelable(EXTRA_1)
                    initEmployerDetails(profile!!)
                } else {
                    viewModel.getEmployerDetails(it.getInt(EXTRA_1))
                }
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_employer, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_share -> shareUrl(this, employerUrl)
        }
        return super.onOptionsItemSelected(item)
    }

    private fun subscribeToData() {
        viewModel.viewState.subscribe(this, ::handleViewState)
        viewModel.action.subscribe(this, ::handleActionViewState)
        viewModel.countries.subscribe(this, {
            if (countryId != -1) {
                val country = it.find { it.id == countryId }
                if (country != null) locationIcon.setUrlSVG("https://freelancehunt.com/static/images/flags/4x3/${country.iso2.toLowerCase()}.svg")
            }
        })
        employerPublicViewModel.badgeCounter.subscribe(this) { badges ->
            tabs.getTabAt(badges.first)?.let {
                val badge: BadgeDrawable = it.orCreateBadge
                badge.isVisible = true
                badge.number = badges.second
            }
        }
        viewModel.setCountries()
    }

    private fun handleViewState(viewState: ViewState<EmployerDetail>) {
        when (viewState) {
            is Loading -> showLoading(progressBar)
            is Success -> {
                initEmployerDetails(viewState.data.data)
                viewModel.setCountries()
            }
            is Error -> handleError(viewState.error.localizedMessage)
            is NoInternetState -> showNoInternetError()
        }
    }

    private fun handleActionViewState(viewState: ViewState<String>) {
        hideLoading(progressBar)
        when (viewState) {
            is Success -> {
                when (viewState.data) {
                    "message" -> toast(getString(R.string.message_sent))
                }
            }
        }
    }

    private fun initEmployerDetails(details: EmployerDetail.Data) {
        hideLoading(progressBar)

        profileId = details.id

        employerUrl = details.links.self.web

        toolbar.subtitle = details.attributes.login

        pagerAdapter = PagerAdapter(this, details)
        with(pager) {
            adapter = pagerAdapter
            offscreenPageLimit = 3
            isUserInputEnabled = false
        }
        val tabsTitles = resources.getStringArray(R.array.employer_tabs)
        val tabsIcons = resources.getStringArray(R.array.employer_tabs_icons)
        TabLayoutMediator(tabs, pager, false, false) { tab, position ->
            tab.text = tabsTitles[position]
            tab.icon = ContextCompat.getDrawable(this, getDrawableIdByName(tabsIcons[position]))
            pager.setCurrentItem(tab.position, false)
        }.attach()
        pager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                val view = pagerAdapter.getViewAtPosition(position)
                view?.let {
                    updatePagerHeightForChild(view, pager)
                }
            }
        })

        avatar.setUrl(details.attributes.avatar.large.url, isCircle = true)
        name.text = "${details.attributes.first_name} ${details.attributes.last_name}"
        login.text = details.attributes.login
        rating.text = details.attributes.rating.toString()
        verified.visibility = details.attributes.verification.identity.toVisibleState()
        isplus.visibility = details.attributes.is_plus_active.toVisibleState()
        location.text = details.attributes.location?.let {
            if (details.attributes.location!!.country != null && details.attributes.location!!.city != null)
                "${details.attributes.location!!.country!!.name}, ${details.attributes.location!!.city!!.name}"
            else if (details.attributes.location!!.country != null)
                details.attributes.location!!.country!!.name else ""
        }
        if (details.attributes.location != null && details.attributes.location!!.country != null) {
            countryId = details.attributes.location!!.country!!.id
        }
        voteup.text = details.attributes.positive_reviews.toString()
        votedown.text = details.attributes.negative_reviews.toString()
        if (details.attributes.birth_date != null) {
            birthdate.text = details.attributes.birth_date!!.parseSimpleDate()
                ?.let { calculateAge(it).getEnding(this, R.array.ending_years) }
        } else birthdate.gone()

        visitedAt.text = details.attributes.visited_at?.parseFullDate(true).getTimeAgo()

        if (profileId != appPreferences.getCurrentUserId()) {
            buttonMessage.visible()
            buttonMessage.setOnClickListener {
                BottomMenuBuilder(
                    supportFragmentManager,
                    CreateThreadBottomDialogFragment.TAG
                ).buildMenuForCreateThread(profileId)
            }
        }
    }

    private fun updatePagerHeightForChild(view: View, pager: ViewPager2) {
        view.post {
            val wMeasureSpec =
                View.MeasureSpec.makeMeasureSpec(view.width, View.MeasureSpec.EXACTLY)
            val hMeasureSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            view.measure(wMeasureSpec, hMeasureSpec)

            if (pager.layoutParams.height != view.measuredHeight) {
                pager.layoutParams = (pager.layoutParams)
                    .also { lp ->
                        lp.height = view.measuredHeight
                    }
            }
        }
    }

    private fun handleError(error: String) {
        hideLoading(progressBar)
        showError(error)
    }

    private fun showNoInternetError() {
        hideLoading(progressBar)
        snackbar(getString(R.string.no_internet_error_message), employersActivityContainer)
    }

    override fun onThreadCreated(subject: String, message: String, toProfileId: Int) {
        viewModel.sendMessage(toProfileId, subject, message)
    }

    private inner class PagerAdapter(
        fa: FragmentActivity,
        details: EmployerDetail.Data
    ) : FragmentStateAdapter(fa) {

        val fragments = arrayListOf<Fragment>(
            PagerEmployerOverview.newInstance(details),
            PagerEmployerProjects.newInstance(details.id),
            PagerEmployerReviews.newInstance(details.id)
        )

        override fun getItemCount(): Int = 3

        fun getViewAtPosition(position: Int) = fragments[position].view


        override fun createFragment(position: Int): Fragment = when (position) {
            0 -> fragments[0]
            1 -> fragments[1]
            2 -> fragments[2]
            else -> fragments[0]
        }
    }

}