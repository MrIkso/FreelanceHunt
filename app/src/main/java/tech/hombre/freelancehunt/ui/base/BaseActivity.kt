package tech.hombre.freelancehunt.ui.base

import android.os.Bundle
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.core.parameter.parametersOf
import tech.hombre.data.common.coroutine.CoroutineContextProvider
import tech.hombre.data.local.LocalProperties
import tech.hombre.freelancehunt.App
import tech.hombre.freelancehunt.R
import tech.hombre.freelancehunt.common.EMPTY_STRING
import tech.hombre.freelancehunt.common.UserType
import tech.hombre.freelancehunt.common.extensions.*
import tech.hombre.freelancehunt.routing.AppFragmentNavigator
import tech.hombre.freelancehunt.routing.AppNavigator
import tech.hombre.freelancehunt.ui.main.view.activities.MainActivity
import tech.hombre.freelancehunt.ui.main.view.fragments.MainFragment

abstract class BaseActivity : AppCompatActivity() {

    protected abstract fun isPrivate(): Boolean

    protected val appNavigator: AppNavigator by inject { parametersOf(this) }

    protected val appFragmentNavigator: AppFragmentNavigator by inject { parametersOf(this) }

    protected val appPreferences: LocalProperties by inject { parametersOf(this) }

    fun showError(errorMessage: String?) =
        toast(errorMessage ?: EMPTY_STRING)

    fun showLoading(progressBar: ProgressBar) = progressBar.visible()

    fun hideLoading(progressBar: ProgressBar) = progressBar.gone()

    private var backPressTimer: Long = 0

    private var _current_user_login: String? = null
    var current_user_login: String
        get() {
            if (_current_user_login == null) {
                _current_user_login = appPreferences.getCurrentUserProfile()?.login
            }
            return _current_user_login!!
        }
        set(value) {
            _current_user_login = value
        }

    fun getCurrentUser() = current_user_login

    override fun onBackPressed() {
        if (this is MainActivity) {
            if (supportFragmentManager.primaryNavigationFragment?.tag != MainFragment.TAG) {
                supportFragmentManager.switch(
                    R.id.fragmentContainer,
                    MainFragment.newInstance(),
                    MainFragment.TAG
                )
            } else if (canExit()) {
                super.onBackPressed()
            }
        } else {
            super.onBackPressed()
        }
    }

    private fun canExit(): Boolean {
        if (backPressTimer + 2000 > System.currentTimeMillis()) {
            return true
        } else {
            toast(getString(R.string.press_again_to_exit))
        }
        backPressTimer = System.currentTimeMillis()
        return false
    }

    override fun onStart() {
        overridePendingTransition(0, 0)
        super.onStart()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (isPrivate() && !validateAuth()) return
    }

    fun isLoggedUser() =
        appPreferences.getCurrentUserProfile() != null && appPreferences.getAccessToken()
            .isNotEmpty()

    open fun validateAuth(): Boolean {
        if (!isLoggedUser()) {
            onLoginRequire()
            return false
        }
        return true
    }

    fun onLoginRequire() {
        appPreferences.resetPreferences()
        val glide = Glide.get(App.instance)
        CoroutineScope(CoroutineContextProvider().io).launch {
            glide.clearDiskCache()
        }
        glide.clearMemory()
        appNavigator.showLoginActivity()
        finishAffinity()
    }

    fun onShowMyProfile() = when (UserType.EMPLOYER.type) {
        appPreferences.getCurrentUserType() -> appNavigator.showEmployerDetails(appPreferences.getCurrentUserId())
        else -> appNavigator.showFreelancerDetails(appPreferences.getCurrentUserId())
    }


}