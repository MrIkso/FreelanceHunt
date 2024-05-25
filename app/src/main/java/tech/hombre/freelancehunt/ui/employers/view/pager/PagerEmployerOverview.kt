package tech.hombre.freelancehunt.ui.employers.view.pager

import android.os.Bundle
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.RelativeLayout
import androidx.annotation.Keep
import tech.hombre.domain.model.EmployerDetail
import tech.hombre.freelancehunt.R
import tech.hombre.freelancehunt.common.EXTRA_1
import tech.hombre.freelancehunt.databinding.FragmentPagerEmployerOverviewBinding
import tech.hombre.freelancehunt.ui.base.BaseFragment

class PagerEmployerOverview : BaseFragment<FragmentPagerEmployerOverviewBinding>(FragmentPagerEmployerOverviewBinding::inflate) {

    var details: EmployerDetail.Data? = null

    override fun viewReady() {
        arguments?.let {
            details = it.getParcelable(EXTRA_1)
            if (details != null) {
                initOverview(details!!)
            }
        }
    }

    private fun initOverview(details: EmployerDetail.Data) {
        if (!details.attributes.verification.phone) {
            binding.verificatedPhone.alpha = 0.5f
        }
        if (!details.attributes.verification.birth_date) {
            binding.verificatedBirth.alpha = 0.5f
        }
        if (!details.attributes.verification.website) {
            binding.verificatedSite.alpha = 0.5f
        }
        if (!details.attributes.verification.wmid) {
            binding.verificatedBankID.alpha = 0.5f
        }
        if (details.attributes.cv_html != null) {

            if (!binding.summary.setHtmlText(details.attributes.cv_html!!)) {
                val viewId = binding.summary.id
                binding.overviewFragmentContainer.removeView(binding.summary)

                val params: RelativeLayout.LayoutParams = RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.MATCH_PARENT,
                    RelativeLayout.LayoutParams.WRAP_CONTENT
                )

                val webView = WebView(requireContext())
                webView.apply {
                    id = viewId
                    layoutParams = params
                    webViewClient = object  : WebViewClient() {
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
                    loadDataWithBaseURL(
                        null,
                        details.attributes.cv_html ?: "",
                        "text/html",
                        "ru_RU",
                        null
                    )
                }

                binding.overviewFragmentContainer.addView(webView)
            }
        } else binding.summary.text = getString(R.string.no_information)
    }

    companion object {
        @Keep
        val TAG = PagerEmployerOverview::class.java.simpleName

        fun newInstance(details: EmployerDetail.Data): PagerEmployerOverview {
            val fragment = PagerEmployerOverview()
            val extra = Bundle()
            extra.putParcelable(EXTRA_1, details)
            fragment.arguments = extra
            return fragment
        }

    }
}