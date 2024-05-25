package tech.hombre.freelancehunt.ui.contest.view.pager

import android.os.Bundle
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.RelativeLayout
import androidx.annotation.Keep
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel
import tech.hombre.domain.model.ContestDetail
import tech.hombre.domain.model.Countries
import tech.hombre.freelancehunt.R
import tech.hombre.freelancehunt.common.EXTRA_1
import tech.hombre.freelancehunt.common.extensions.getTimeAgo
import tech.hombre.freelancehunt.common.extensions.parseFullDate
import tech.hombre.freelancehunt.common.extensions.snackbar
import tech.hombre.freelancehunt.common.extensions.subscribe
import tech.hombre.freelancehunt.databinding.FragmentPagerContestOverviewBinding
import tech.hombre.freelancehunt.ui.base.*
import tech.hombre.freelancehunt.ui.contest.presentation.ContestOverviewViewModel
import tech.hombre.freelancehunt.ui.contest.presentation.ContestPublicViewModel


class PagerContestOverview : BaseFragment<FragmentPagerContestOverviewBinding>(FragmentPagerContestOverviewBinding::inflate) {

    var project: ContestDetail.Data.Attributes? = null

    private val contestPublicViewModel: ContestPublicViewModel by sharedViewModel()

    private val viewModel: ContestOverviewViewModel by viewModel()

    var countries = listOf<Countries.Data>()

    override fun viewReady() {
        arguments?.let {
            project = it.getParcelable(EXTRA_1)
            if (project != null) {
                subscribeToData()
                initOverview(project!!)
            }
        }
    }

    private fun subscribeToData() {
        viewModel.employerDetails.subscribe(this) {
            when (it) {
                is Loading -> showLoading()
                is Success -> {
                    hideLoading()
                    appNavigator.showEmployerDetails(it.data.data)
                }

                is Error -> handleError(it.error.localizedMessage)
                is NoInternetState -> showNoInternetError()
            }
        }
    }

    private fun handleError(error: String) {
        hideLoading()
        showError(error, binding.overviewActivityContainer)
    }

    private fun showNoInternetError() {
        hideLoading()
        snackbar(getString(R.string.no_internet_error_message), binding.overviewActivityContainer)
    }


    private fun initOverview(details: ContestDetail.Data.Attributes) {
        if (details.description_html != null) {
            val desc = StringBuilder()
            desc.append(details.description_html!!)

            details.updates.forEach { update ->
                desc.append("<b><i>")
                desc.append(getString(R.string.added))
                desc.append(update.published_at.parseFullDate(true).getTimeAgo())
                desc.append("</i></b>")
                desc.append("<br>")
                desc.append(update.description_html)
            }

            if (!binding.description.setHtmlText(desc.toString())) {
                binding.root.removeView(binding.description)

                val params: RelativeLayout.LayoutParams = RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.MATCH_PARENT,
                    RelativeLayout.LayoutParams.WRAP_CONTENT
                )

                val webView = WebView(requireContext())
                webView.apply {
                    layoutParams = params
                    webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(
                            view: WebView?,
                            request: WebResourceRequest?
                        ): Boolean {
                            request?.let {
                                val url = request.url
                                openUrl(requireContext(), url.toString())
                            }
                            return true
                        }
                    }
                    settings.javaScriptEnabled = false
                    settings.javaScriptCanOpenWindowsAutomatically = true
                    settings.mediaPlaybackRequiresUserGesture = true
                    webChromeClient = WebChromeClient()
                    loadDataWithBaseURL(null, desc.toString(), "text/html", "ru_RU", null)
                }
                binding.root.addView(webView, 2)
            }

        } else binding.description.text = getString(R.string.no_information)

        binding.avatar.setUrl(details.employer.avatar.large.url, isCircle = true)
        binding.name.text = "${details.employer.first_name} ${details.employer.last_name}"
        binding.login.text = details.employer.login

        binding.buttonProfile.setOnClickListener {
            viewModel.getEmployerDetails(details.employer.id)
        }

    }

    companion object {
        @Keep
        val TAG = PagerContestOverview::class.java.simpleName

        fun newInstance(details: ContestDetail.Data.Attributes): PagerContestOverview {
            val fragment = PagerContestOverview()
            val extra = Bundle()
            extra.putParcelable(EXTRA_1, details)
            fragment.arguments = extra
            return fragment
        }

    }
}