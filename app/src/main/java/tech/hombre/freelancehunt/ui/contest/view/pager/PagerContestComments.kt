package tech.hombre.freelancehunt.ui.contest.view.pager

import android.os.Bundle
import android.widget.TextView
import androidx.annotation.Keep
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.vivchar.rendererrecyclerviewadapter.*

import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel
import tech.hombre.domain.model.ContestComment
import tech.hombre.freelancehunt.R
import tech.hombre.freelancehunt.common.EXTRA_1
import tech.hombre.freelancehunt.common.UserType
import tech.hombre.freelancehunt.common.extensions.*
import tech.hombre.freelancehunt.common.widgets.CustomHtmlTextView
import tech.hombre.freelancehunt.common.widgets.CustomImageView
import tech.hombre.freelancehunt.databinding.FragmentPagerContestCommentsBinding

import tech.hombre.freelancehunt.ui.base.*
import tech.hombre.freelancehunt.ui.base.ViewState
import tech.hombre.freelancehunt.ui.contest.presentation.ContestCommentsViewModel
import tech.hombre.freelancehunt.ui.contest.presentation.ContestPublicViewModel

class PagerContestComments : BaseFragment<FragmentPagerContestCommentsBinding>(FragmentPagerContestCommentsBinding::inflate) {

    private val viewModel: ContestCommentsViewModel by viewModel()

    private val contestPublicViewModel: ContestPublicViewModel by sharedViewModel()

    lateinit var adapter: RendererRecyclerViewAdapter

    private var projectId = 0

    override fun viewReady() {
        arguments?.let {
            projectId = it.getInt(EXTRA_1)
            if (projectId != 0) {
                initList()
                subscribeToData()
                viewModel.getContestComment(projectId)
            }
        }
    }

    private fun initList() {
        adapter = RendererRecyclerViewAdapter()
        adapter.enableDiffUtil(true)
        adapter.registerRenderer(
            ViewRenderer(
                R.layout.item_contest_comments_list,
                ContestComment.Data::class.java,
                BaseViewRenderer.Binder { model: ContestComment.Data, finder: ViewFinder, payloads: List<Any?>? ->
                    if (model.attributes.is_deleted) {
                        finder
                            .setGone(R.id.clickableView, true)
                            .setGone(R.id.deletedComment, false)
                    } else
                        finder
                            .setGone(R.id.deletedComment, true)
                            .setGone(R.id.clickableView, false)
                            .find<CardView>(R.id.mainView) {
                                it.updateLayoutParams<ConstraintLayout.LayoutParams> {
                                    marginStart =
                                        if (model.attributes.level <= 3) (model.attributes.level - 1) * 50 else 150
                                }
                            }
                            .find(
                                R.id.avatar,
                                ViewProvider<CustomImageView> { avatar ->
                                    avatar.setUrl(
                                        model.attributes.author.avatar.small.url,
                                        isCircle = true
                                    )
                                })
                            .setText(
                                R.id.login,
                                model.attributes.author.let { if (it.first_name.isBlank()) it.login else it.first_name + " " + it.last_name })
                            .setText(
                                R.id.postedAt,
                                model.attributes.posted_at.parseFullDate(true).getTimeAgo()
                            )
                            .find<CustomHtmlTextView>(R.id.comment) {
                                it.setHtmlText(model.attributes.message)
                            }
                            .find<TextView>(R.id.like) {
                                if (model.attributes.likes > 0) {
                                    it.text = model.attributes.likes.toString()
                                    it.visible()
                                } else it.gone()
                            }
                            .setOnClickListener(R.id.clickableView) {
                                if (model.attributes.author.type == UserType.EMPLOYER.type) viewModel.getEmployerDetails(
                                    model.attributes.author.id
                                ) else viewModel.getFreelancerDetails(model.attributes.author.id)
                            }
                }
            )
        )
        binding.list.layoutManager = LinearLayoutManager(activity)
        binding.list.adapter = adapter
        adapter.registerRenderer(
            LoadMoreViewBinder(
                R.layout.item_load_more
            )
        )
    }

    private fun subscribeToData() {
        viewModel.viewState.subscribe(this, ::handleViewState)
        viewModel.freelancerDetails.subscribe(this, {
            when (it) {
                is Loading -> showLoading()
                is Success -> {
                    hideLoading()
                    appNavigator.showFreelancerDetails(it.data.data)
                }
                is Error -> handleError(it.error.localizedMessage)
                is NoInternetState -> showNoInternetError()
            }
        })
        viewModel.employerDetails.subscribe(this, {
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

    private fun handleViewState(viewState: ViewState<ContestComment>) {
        when (viewState) {
            is Loading -> showLoading()
            is Success -> initBids(viewState.data.data)
            is Error -> handleError(viewState.error.localizedMessage)
            is NoInternetState -> showNoInternetError()
        }
    }

    private fun handleError(error: String) {
        hideLoading()
        showError(error, binding.commentsContainer)
    }

    private fun showNoInternetError() {
        hideLoading()
        snackbar(getString(R.string.no_internet_error_message), binding.commentsContainer)
    }

    private fun initBids(comments: List<ContestComment.Data>) {
        hideLoading()
        adapter.setItems(comments)
    }

    companion object {
        @Keep
        val TAG = PagerContestComments::class.java.simpleName

        fun newInstance(profileId: Int): PagerContestComments {
            val fragment = PagerContestComments()
            val extra = Bundle()
            extra.putInt(EXTRA_1, profileId)
            fragment.arguments = extra
            return fragment
        }

    }
}