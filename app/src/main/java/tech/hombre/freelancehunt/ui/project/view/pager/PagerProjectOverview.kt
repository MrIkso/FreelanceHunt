package tech.hombre.freelancehunt.ui.project.view.pager

import android.os.Bundle
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.RelativeLayout
import androidx.annotation.Keep
import com.github.vivchar.rendererrecyclerviewadapter.BaseViewRenderer
import com.github.vivchar.rendererrecyclerviewadapter.RendererRecyclerViewAdapter
import com.github.vivchar.rendererrecyclerviewadapter.ViewFinder
import com.github.vivchar.rendererrecyclerviewadapter.ViewRenderer
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel
import tech.hombre.domain.model.Countries
import tech.hombre.domain.model.ProjectDetail
import tech.hombre.freelancehunt.R
import tech.hombre.freelancehunt.common.EXTRA_1
import tech.hombre.freelancehunt.common.extensions.*
import tech.hombre.freelancehunt.databinding.FragmentPagerProjectOverviewBinding
import tech.hombre.freelancehunt.ui.base.*
import tech.hombre.freelancehunt.ui.employers.presentation.EmployerDetailViewModel
import tech.hombre.freelancehunt.ui.project.presentation.ProjectPublicViewModel


class PagerProjectOverview : BaseFragment<FragmentPagerProjectOverviewBinding>(FragmentPagerProjectOverviewBinding::inflate) {

    var project: ProjectDetail.Data.Attributes? = null

    private val projectPublicViewModel: ProjectPublicViewModel by sharedViewModel()

    private val employerViewModel: EmployerDetailViewModel by viewModel()

    var countries = listOf<Countries.Data>()

    lateinit var adapter: RendererRecyclerViewAdapter

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
        employerViewModel.viewState.subscribe(this, {
            when (it) {
                is Loading -> showLoading()
                is Success -> {
                    hideLoading()
                    appNavigator.showEmployerDetails(it.data.data)
                }
                is Error -> handleError(it.error.localizedMessage)
                is NoInternetState -> showNoInternetError()
            }
        })
    }

    private fun handleError(error: String) {
        hideLoading()
        showError(error, binding.overviewActivityContainer)
    }

    private fun showNoInternetError() {
        hideLoading()
        snackbar(getString(R.string.no_internet_error_message), binding.overviewActivityContainer)
    }


    private fun initOverview(details: ProjectDetail.Data.Attributes) {
        if (details.employer == null) {
            handleError(getString(R.string.only_for_plus))
            return
        }

        binding.publishedAt.text = details.published_at.parseFullDate(true).getTimeAgo()

        binding.publishedAt.setOnClickListener {
            toast(details.published_at.parseFullDate(true).toString())
        }

        initSkills(details.skills)

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
                val viewId = binding.description.id
                binding.overviewActivityContainer.removeView(binding.description)

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
                    loadDataWithBaseURL(null, desc.toString(), "text/html", "ru_RU", null)
                }
                binding.overviewActivityContainer.addView(webView, 2)
            }
        } else binding.description.text = getString(R.string.no_information)

        binding.avatar.setUrl(details.employer!!.avatar.large.url, isCircle = true)
        binding.name.text = "${details.employer!!.first_name} ${details.employer!!.last_name}"
        binding.login.text = details.employer!!.login

        binding.buttonProfile.setOnClickListener {
            employerViewModel.getEmployerDetails(details.employer!!.id)
        }
    }

    private fun initSkills(skills: List<ProjectDetail.Data.Attributes.Skill>) {
        adapter = RendererRecyclerViewAdapter()
        adapter.enableDiffUtil(true)
        adapter.registerRenderer(
            ViewRenderer(
                R.layout.item_project_skill_list,
                ProjectDetail.Data.Attributes.Skill::class.java,
                BaseViewRenderer.Binder { model: ProjectDetail.Data.Attributes.Skill, finder: ViewFinder, payloads: List<Any?>? ->
                    finder
                        .setText(R.id.name, model.name)
                        .setOnClickListener {
                            handleError(getString(R.string.not_implemented_error))
                        }
                }
            )
        )
        binding.skillList.adapter = adapter
        adapter.setItems(skills)
    }

    companion object {
        @Keep
        val TAG = PagerProjectOverview::class.java.simpleName

        fun newInstance(details: ProjectDetail.Data.Attributes): PagerProjectOverview {
            val fragment = PagerProjectOverview()
            val extra = Bundle()
            extra.putParcelable(EXTRA_1, details)
            fragment.arguments = extra
            return fragment
        }

    }
}