package tech.hombre.freelancehunt.ui.my.projects.view

import androidx.annotation.Keep
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.vivchar.rendererrecyclerviewadapter.*
import kotlinx.android.synthetic.main.fragment_my_projects.*
import org.koin.androidx.viewmodel.ext.android.viewModel
import tech.hombre.domain.model.MyProjectsList
import tech.hombre.domain.model.ProjectDetail
import tech.hombre.freelancehunt.R
import tech.hombre.freelancehunt.common.SafeType
import tech.hombre.freelancehunt.common.extensions.*
import tech.hombre.freelancehunt.common.widgets.CustomImageView
import tech.hombre.freelancehunt.common.widgets.EndlessScroll
import tech.hombre.freelancehunt.ui.base.*
import tech.hombre.freelancehunt.ui.base.ViewState
import tech.hombre.freelancehunt.ui.my.projects.presentation.MyProjectsViewModel

class MyProjectsFragment : BaseFragment() {

    private val viewModel: MyProjectsViewModel by viewModel()

    override fun getLayout() = R.layout.fragment_my_projects

    lateinit var adapter: RendererRecyclerViewAdapter

    val items = arrayListOf<ProjectDetail.Data>()

    override fun viewReady() {
        initList()
        subscribeToData()
        viewModel.getMyProjectsLists()

        addProjectButton.setOnClickListener {
            appNavigator.showNewProjectDialog()
        }
    }

    private fun subscribeToData() {
        viewModel.viewState.subscribe(this, ::handleViewState)
        viewModel.details.subscribe(this, {
            when (it) {
                is Loading -> showLoading()
                is Success -> {
                    hideLoading()
                    appNavigator.showProjectDetails(it.data.data)
                }
                is Error -> handleError(it.error.localizedMessage)
                is NoInternetState -> showNoInternetError()
            }
        })
    }

    private fun handleViewState(viewState: ViewState<MyProjectsList>) {
        when (viewState) {
            is Loading -> showLoading()
            is Success -> initMyProjectsList(viewState.data)
            is Error -> handleError(viewState.error.localizedMessage)
            is NoInternetState -> showNoInternetError()
        }
    }

    private fun handleError(error: String) {
        hideLoading()
        showError(error, projectsFragmentContainer)
    }

    private fun showNoInternetError() {
        hideLoading()
        snackbar(getString(R.string.no_internet_error_message), projectsFragmentContainer)
    }

    private fun initList() {
        adapter = RendererRecyclerViewAdapter()
        adapter.enableDiffUtil(true)
        adapter.registerRenderer(
            ViewRenderer(
                R.layout.item_project_list,
                ProjectDetail.Data::class.java,
                BaseViewRenderer.Binder { model: ProjectDetail.Data, finder: ViewFinder, payloads: List<Any?>? ->
                    finder
                        .setGone(R.id.budget, model.attributes.budget == null)
                        .setGone(R.id.premium, !model.attributes.is_premium)
                        .setText(
                            R.id.safe,
                            getTitleBySafeType(
                                requireContext(),
                                SafeType.values().find { it.type == model.attributes.safe_type }
                                    ?: SafeType.DIRECT_PAYMENT))
                        .setGone(R.id.isremote, !model.attributes.is_remote_job)
                        .setText(R.id.name, model.attributes.name)
                        .setText(R.id.description, model.attributes.description.collapse(300))
                        .find(
                            R.id.avatar,
                            ViewProvider<CustomImageView> { avatar ->
                                avatar.setUrl(
                                    model.attributes.employer?.avatar?.small?.url ?: "",
                                    isCircle = true
                                )
                            })
                        .setText(
                            R.id.login,
                            model.attributes.employer?.let { if (it.first_name.isBlank()) it.login else it.first_name + " " + it.last_name }
                                ?: "N/A")
                        .setText(R.id.bidCount, model.attributes.bid_count.toString())
                        .setText(
                            R.id.budget,
                            if (model.attributes.budget != null) "${model.attributes.budget!!.amount} ${currencyToChar(
                                model.attributes.budget!!.currency
                            )}" else ""
                        )
                        .setGone(R.id.isplus, !model.attributes.is_only_for_plus)
                        .setOnClickListener(R.id.clickableView) {
                            if (model.attributes.is_only_for_plus && appPreferences.getCurrentUserProfile()?.is_plus_active == true) {
                                appNavigator.showProjectDetails(model)
                            } else if (!model.attributes.is_only_for_plus)
                                appNavigator.showProjectDetails(model)
                            else
                                handleError(getString(R.string.only_for_plus))
                        }
                }
            )
        )
        list.layoutManager = LinearLayoutManager(context)
        list.adapter = adapter
        adapter.registerRenderer(
            LoadMoreViewBinder(
                R.layout.item_load_more
            )
        )

        list.addOnScrollListener(object : EndlessScroll() {
            override fun onLoadMore(page: Int, totalItemsCount: Int) {
                if (viewModel.pagination.next.isNotEmpty()) {
                    adapter.showLoadMore()
                    viewModel.getMyProjectsLists(viewModel.pagination.next)
                }
            }
        })

        refresh.setOnRefreshListener {
            items.clear()
            adapter.setItems(items)
            viewModel.getMyProjectsLists()
        }
    }


    private fun initMyProjectsList(projectsList: MyProjectsList) {
        hideLoading()
        refresh.isRefreshing = false

        items.addAll(projectsList.data)
        adapter.setItems(items)
    }


    companion object {
        @Keep
        val TAG = MyProjectsFragment::class.java.simpleName

        fun newInstance() = MyProjectsFragment()
    }
}