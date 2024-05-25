package tech.hombre.freelancehunt.ui.login.view

import android.os.Bundle
import org.koin.androidx.viewmodel.ext.android.viewModel
import tech.hombre.domain.model.MyProfile
import tech.hombre.freelancehunt.R
import tech.hombre.freelancehunt.common.EXTRA_1
import tech.hombre.freelancehunt.common.extensions.*
import tech.hombre.freelancehunt.databinding.ActivityLoginBinding
import tech.hombre.freelancehunt.routing.AppNavigator
import tech.hombre.freelancehunt.routing.ScreenType
import tech.hombre.freelancehunt.ui.base.*
import tech.hombre.freelancehunt.ui.login.presentation.LoginViewModel


class LoginActivity : BaseActivity<ActivityLoginBinding>(ActivityLoginBinding::inflate) {

    override val isPublic = true

    private val viewModel: LoginViewModel by viewModel()

    override fun viewReady() {
        setTheme(R.style.WelcomeTheme)
        if (!isLoggedUser()) {
            subscribeToData()
            binding.token.onDone { signIn() }
            binding.doLogin.onClick {
                signIn()
            }
        } else {
            val type = (intent.getSerializableExtra(AppNavigator.SCREEN_TYPE) ?: ScreenType.MAIN) as ScreenType
            val fromNotification = intent.getBooleanExtra(EXTRA_1, false)
            appNavigator.showMainActivity(type, fromNotification)
            finishAffinity()
        }

    }

    private fun signIn() {
        val token = binding.token.text.toString()
        if (token.isNotEmpty()) {
            hideInputs()
            viewModel.checkTokenByMyProfile(token)
        } else showError(getString(R.string.token_is_empty))
    }

    private fun hideInputs() {
        binding.token.gone()
        binding.doLogin.gone()
    }

    private fun showInputs() {
        binding.token.visible()
        binding.doLogin.visible()
    }

    private fun subscribeToData() {
        viewModel.viewState.subscribe(this, ::handleViewState)
    }

    private fun handleViewState(viewState: ViewState<MyProfile>) {
        when (viewState) {
            is Loading -> showLoading(binding.loginLoadingProgress)
            is Success -> saveProfileAndGo(viewState.data)
            is Error -> {
                handleError(viewState.error.localizedMessage)
                showInputs()
            }
            is NoInternetState -> {
                showNoInternetError()
                showInputs()
            }
        }
    }

    private fun handleError(error: String) {
        hideLoading(binding.loginLoadingProgress)
        showError(error)
    }

    private fun showNoInternetError() {
        hideLoading(binding.loginLoadingProgress)
        snackbar(getString(R.string.no_internet_error_message), binding.cardView)
    }

    private fun saveProfileAndGo(myprofile: MyProfile) {
        hideLoading(binding.loginLoadingProgress)
        appPreferences.setCurrentUserId(myprofile.data.id)
        appPreferences.setCurrentUserType(myprofile.data.type)
        appPreferences.setCurrentUserProfile(myprofile.data.attributes)
        appPreferences.setAccessToken(binding.token.text.toString())
        appNavigator.showMainActivity()
        finishAffinity()
    }
}