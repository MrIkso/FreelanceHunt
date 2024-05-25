package tech.hombre.freelancehunt.ui.main.view.activities

import android.content.DialogInterface
import android.content.res.Configuration
import android.os.Bundle
import android.text.SpannableString
import android.text.util.Linkify
import android.view.MenuItem
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.graphics.drawable.DrawerArrowDrawable
import androidx.core.view.GravityCompat
import androidx.core.view.isVisible
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import tech.hombre.domain.model.MyProfile
import tech.hombre.freelancehunt.R
import tech.hombre.freelancehunt.common.EXTRA_1
import tech.hombre.freelancehunt.common.SKU_PREMIUM
import tech.hombre.freelancehunt.common.UserType
import tech.hombre.freelancehunt.common.extensions.subscribe
import tech.hombre.freelancehunt.common.extensions.switch
import tech.hombre.freelancehunt.common.provider.AutoStartPermissionHelper
import tech.hombre.freelancehunt.common.provider.PowerSaverHelper
import tech.hombre.freelancehunt.common.widgets.BadgeDrawerArrowDrawable
import tech.hombre.freelancehunt.databinding.ActivityContainerBinding
import tech.hombre.freelancehunt.databinding.ItemMenuProfileBinding
import tech.hombre.freelancehunt.databinding.ItemMenuThreadsBinding
import tech.hombre.freelancehunt.databinding.NavigationHeaderBinding
import tech.hombre.freelancehunt.framework.app.AppHelper
import tech.hombre.freelancehunt.framework.tasks.TasksManger
import tech.hombre.freelancehunt.routing.AppNavigator
import tech.hombre.freelancehunt.routing.ScreenType
import tech.hombre.freelancehunt.ui.base.BaseActivity
import tech.hombre.freelancehunt.ui.base.Error
import tech.hombre.freelancehunt.ui.base.Success
import tech.hombre.freelancehunt.ui.base.ViewState
import tech.hombre.freelancehunt.ui.employers.view.EmployersFragment
import tech.hombre.freelancehunt.ui.freelancers.view.FreelancersFragment
import tech.hombre.freelancehunt.ui.main.presentation.MainPublicViewModel
import tech.hombre.freelancehunt.ui.main.presentation.MainViewModel
import tech.hombre.freelancehunt.ui.main.view.fragments.AboutFragment
import tech.hombre.freelancehunt.ui.main.view.fragments.MainFragment
import tech.hombre.freelancehunt.ui.main.view.fragments.SettingFragment
import tech.hombre.freelancehunt.ui.my.bids.view.MyBidsFragment
import tech.hombre.freelancehunt.ui.my.contests.view.MyContestsFragment
import tech.hombre.freelancehunt.ui.my.projects.view.MyProjectsFragment
import tech.hombre.freelancehunt.ui.my.workspaces.view.MyWorkspacesFragment
import tech.hombre.freelancehunt.ui.threads.view.ThreadsFragment
import java.util.*


class MainActivity : BaseActivity<ActivityContainerBinding>(ActivityContainerBinding::inflate) {

    private lateinit var drawerToggle: ActionBarDrawerToggle

    private lateinit var badgeToggleDrawable: BadgeDrawerArrowDrawable
    private lateinit var headerViewBinding: NavigationHeaderBinding

    private val viewModel: MainViewModel by viewModel()

    private val sharedViewModelMain: MainPublicViewModel by viewModel()

    private val tasksManger: TasksManger by inject()

    private var timer = Timer()

    // TODO to preferences
    private val delay = 15000L

    override fun viewReady() {
        initViews()
        subscribeToData()
        when (intent.getSerializableExtra(AppNavigator.SCREEN_TYPE)) {
            ScreenType.MAIN -> {
                supportFragmentManager.switch(
                    R.id.fragmentContainer,
                    MainFragment.newInstance(),
                    MainFragment.TAG
                )
                selectMenuItem(R.id.menu_main, true)
            }
            ScreenType.FEED ->
                supportFragmentManager.switch(
                    R.id.fragmentContainer,
                    MainFragment.newInstance(true),
                    MainFragment.TAG
                )
            ScreenType.THREADS -> supportFragmentManager.switch(
                R.id.fragmentContainer,
                ThreadsFragment.newInstance(),
                ThreadsFragment.TAG
            )
            else -> finish()
        }
        if (!intent.getBooleanExtra(EXTRA_1, false))
            CoroutineScope(Dispatchers.Default).launch {
                tasksManger.setupTasks()
            }
        billingClient.init()
        checkPermissions()
    }

    private fun checkPermissions() {
        val intent = PowerSaverHelper.prepareIntentForWhiteListingOfBatteryOptimization(
            this,
            onSuccess = showDeveloperNotify()
        )
        intent.intent?.let {
            startActivity(it)
        } ?: if (!intent.isWhiteListed) {
            if (!appPreferences.isAutoStartPermissionsRequired()) {
                if (AutoStartPermissionHelper.getInstance()
                        .isAutoStartPermissionAvailable(this)
                ) with(
                    AlertDialog.Builder(
                        this,
                        AppHelper.getDialogTheme(appPreferences.getAppTheme())
                    )
                ) {
                    setCancelable(false)
                    setMessage(getString(R.string.autostart_permissions_dialog_info))
                    setPositiveButton(android.R.string.ok) { dialog: DialogInterface, _: Int ->

                        if (AutoStartPermissionHelper.getInstance()
                                .getAutoStartPermission(context)
                        ) {
                            appPreferences.setAutoStartPermissionsRequired()
                            showDeveloperNotify()
                        } else with(
                            AlertDialog.Builder(
                                context,
                                AppHelper.getDialogTheme(appPreferences.getAppTheme())
                            )
                        ) {
                            setCancelable(false)
                            setMessage(getString(R.string.autostart_permissions_required_error))
                            setPositiveButton(android.R.string.ok) { dialog: DialogInterface, _: Int ->
                                appPreferences.setAutoStartPermissionsRequired()
                                showDeveloperNotify()
                            }
                            show()
                        }
                    }
                    show()
                } else {
                    appPreferences.setAutoStartPermissionsRequired()
                    showDeveloperNotify()
                }
            }
            else {

            }
        } else{

        }
    }

    private fun showDeveloperNotify() {
        if (appPreferences.isDeveloperNotifyShowed()) return
        val message = SpannableString(getString(R.string.developer_notify))
        Linkify.addLinks(message, Linkify.ALL)
        with(
            AlertDialog.Builder(
                this,
                AppHelper.getDialogTheme(appPreferences.getAppTheme())
            )
        )
        {
            setCancelable(false)
            setMessage(message)
            setPositiveButton(android.R.string.ok) { dialog: DialogInterface, _: Int ->
                appPreferences.setDeveloperNotifyShowed()
            }
            show()
        }
    }

    override fun onBackPressed() {
        if (binding.drawer.isDrawerOpen(GravityCompat.START)) {
            binding.drawer.closeDrawers()
        } else super.onBackPressed()
    }

    private fun initViews() {
        setSupportActionBar(binding.appbar.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        val header = binding.drawerLayout.navigation.getHeaderView(0)
        headerViewBinding = NavigationHeaderBinding.bind(header)

        drawerToggle = ActionBarDrawerToggle(
            this,
            binding.drawer,
            binding.appbar.toolbar,
            R.string.drawer_open,
            R.string.drawer_close
        )
        drawerToggle.isDrawerIndicatorEnabled = true
        drawerToggle.syncState()

        binding.drawer.addDrawerListener(drawerToggle)
        binding.drawerLayout.navigation.menu.findItem(R.id.menu_contests).isVisible =
            appPreferences.getCurrentUserType() == UserType.EMPLOYER.type
        binding.drawerLayout.navigation.menu.findItem(R.id.menu_bids).isVisible =
            appPreferences.getCurrentUserType() == UserType.FREELANCER.type
        binding.drawerLayout.navigation.menu.findItem(R.id.menu_projects).isVisible =
            appPreferences.getCurrentUserType() == UserType.EMPLOYER.type
        binding.drawerLayout.navigation.setNavigationItemSelectedListener {
            if (binding.drawerLayout.navigation.checkedItem != it) {
                when (it.itemId) {
                    R.id.menu_main -> supportFragmentManager.switch(
                        R.id.fragmentContainer,
                        MainFragment.newInstance(),
                        MainFragment.TAG
                    )
                    R.id.menu_profile -> onShowMyProfile()
                    R.id.menu_logout -> onLoginRequire()
                    R.id.menu_about -> supportFragmentManager.switch(
                        R.id.fragmentContainer,
                        AboutFragment.newInstance(),
                        AboutFragment.TAG
                    )
                    R.id.menu_contests -> supportFragmentManager.switch(
                        R.id.fragmentContainer,
                        MyContestsFragment(),
                        MyContestsFragment.TAG
                    )
                    R.id.menu_settings -> supportFragmentManager.switch(
                        R.id.fragmentContainer,
                        SettingFragment(),
                        SettingFragment.TAG
                    )
                    R.id.menu_freelancers -> supportFragmentManager.switch(
                        R.id.fragmentContainer,
                        FreelancersFragment.newInstance(),
                        FreelancersFragment.TAG
                    )
                    R.id.menu_employers -> supportFragmentManager.switch(
                        R.id.fragmentContainer,
                        EmployersFragment.newInstance(),
                        EmployersFragment.TAG
                    )
                    R.id.menu_threads -> supportFragmentManager.switch(
                        R.id.fragmentContainer,
                        ThreadsFragment.newInstance(),
                        ThreadsFragment.TAG
                    )
                    R.id.menu_bids -> supportFragmentManager.switch(
                        R.id.fragmentContainer,
                        MyBidsFragment.newInstance(),
                        MyBidsFragment.TAG
                    )
                    R.id.menu_projects -> supportFragmentManager.switch(
                        R.id.fragmentContainer,
                        MyProjectsFragment.newInstance(),
                        MyProjectsFragment.TAG
                    )
                    R.id.menu_workspaces -> supportFragmentManager.switch(
                        R.id.fragmentContainer,
                        MyWorkspacesFragment.newInstance(),
                        MyWorkspacesFragment.TAG
                    )
                    R.id.menu_buy_premium -> {
                        premiumDialog(this)
                    }
                }
                if (it.itemId != R.id.menu_profile && it.itemId != R.id.menu_buy_premium) {
                    selectMenuItem(it.itemId, true)
                }
            }
            binding.drawer.closeDrawers()
            true
        }
        updateHeader(appPreferences.getCurrentUserProfile()!!)
    }

    private fun premiumDialog(activity: MainActivity) = with(
        AlertDialog.Builder(
            this,
            AppHelper.getDialogTheme(appPreferences.getAppTheme())
        )
    ) {
        setTitle(getString(R.string.freelancehunt_premium))
        setMessage(getString(R.string.premium_caption))
        setPositiveButton(android.R.string.yes) { dialog: DialogInterface, _: Int ->
            billingClient.launchBilling(activity, SKU_PREMIUM)
        }
        setNegativeButton(android.R.string.no) { dialog: DialogInterface, _: Int -> }
        show()
    }

    private fun subscribeToData() {
        viewModel.viewState.subscribe(this, ::handleViewState)
        viewModel.hasMessages.subscribe(this, ::handleMessagesState)
        viewModel.feedEvents.subscribe(this, ::handleFeedState)
        sharedViewModelMain.messagesCounter.subscribe(this) {
            updateDrawer(it, null)
        }
        billingClient.isPremium.subscribe(this, ::handlePremiumState)
        viewModel.checkTokenByMyProfile(appPreferences.getAccessToken())
        viewModel.checkFeed()
        viewModel.refreshCountriesList()
        viewModel.refreshSkillsList()
        timer.schedule(timerTask, delay, delay)
    }

    private fun handlePremiumState(isPremium: Boolean) {

        headerViewBinding.isPlus.isVisible = isPremium
        binding.drawerLayout.navigation.menu.findItem(R.id.menu_buy_premium).isVisible = !isPremium
    }

    private fun handleViewState(viewState: ViewState<MyProfile.Data.Attributes>) {
        when (viewState) {
            is Success -> {
                updateHeader(viewState.data)
                updateDrawer(null, viewState.data.rating)
                appPreferences.setCurrentUserProfile(viewState.data)
                viewModel.checkMessages()
            }
            is Error -> handleError(viewState.error.localizedMessage)
            else -> {}
        }
    }

    private fun updateDrawer(newMassages: Boolean?, rating: Int?) {
        if (rating != null) {
            binding.drawerLayout.navigation.menu.findItem(R.id.menu_profile).actionView?.let {
                val menuProfileBinding = ItemMenuProfileBinding.bind(it)
                menuProfileBinding.subtitle.text = rating.toString()
            }
        }
        if (newMassages != null) {
            binding.drawerLayout.navigation.menu.findItem(R.id.menu_threads)?.let { menuItem ->
                val menuThreadsBinding = ItemMenuThreadsBinding.bind(menuItem.actionView!!)
                if (newMassages) {

                    menuThreadsBinding.icon.setImageResource(R.drawable.mail)
                    menuThreadsBinding.subtitle.text = getString(R.string.have_messages)
                    badgeToggleDrawable = BadgeDrawerArrowDrawable(supportActionBar?.themedContext)
                    drawerToggle.drawerArrowDrawable = badgeToggleDrawable

                } else {
                    menuThreadsBinding.icon.setImageResource(R.drawable.mail_empty)
                    menuThreadsBinding.subtitle.text = getString(R.string.not_have_messages)
                    badgeToggleDrawable = BadgeDrawerArrowDrawable(supportActionBar?.themedContext)
                    drawerToggle.drawerArrowDrawable =
                        DrawerArrowDrawable(supportActionBar?.themedContext)
                }
            }
        }

    }

    private fun handleMessagesState(messageViewState: ViewState<Boolean>) {
        when (messageViewState) {
            is Success -> {
                updateDrawer(messageViewState.data, null)
            }
            is Error -> handleError(messageViewState.error.localizedMessage)
            else -> {}
        }
    }

    private fun handleFeedState(feedViewState: ViewState<Int>) {
        when (feedViewState) {
            is Success -> {
                sharedViewModelMain.setFeedBadgeCounter(feedViewState.data)
            }
            is Error -> handleError(feedViewState.error.localizedMessage)
            else -> {}
        }
    }

    private fun handleError(error: String) {
        showError(error)
    }

    private fun updateHeader(profile: MyProfile.Data.Attributes) {
        val header = binding.drawerLayout.navigation.getHeaderView(0)
        header.apply {
            headerViewBinding.avatar.setUrl(profile.avatar.large.url, isCircle = true)
            headerViewBinding.avatar.setOnClickListener { onShowMyProfile() }
            headerViewBinding.isPlus.isVisible = profile.is_plus_active
            headerViewBinding.name.text = "${profile.first_name} ${profile.last_name}"
            headerViewBinding.userType.text =
                if (appPreferences.getCurrentUserType() == UserType.EMPLOYER.type) getString(R.string.employer) else getString(
                    R.string.freelancer
                )
        }

    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        drawerToggle.syncState()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        drawerToggle.onConfigurationChanged(newConfig)
    }

    private val timerTask = object : TimerTask() {
        override fun run() {
            runOnUiThread {
                if (binding.drawerLayout.navigation.checkedItem != binding.drawerLayout.navigation.menu.findItem(R.id.menu_threads)) viewModel.checkMessages()
            }
        }
    }

    override fun finish() {
        timer.cancel()
        timer.purge()
        super.finish()
    }

}
