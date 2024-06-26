package tech.hombre.freelancehunt.ui.my.bids.view

import android.graphics.PorterDuff
import android.widget.TextView
import androidx.annotation.Keep
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.vivchar.rendererrecyclerviewadapter.*

import org.koin.androidx.viewmodel.ext.android.viewModel
import tech.hombre.domain.model.MyBidsList
import tech.hombre.freelancehunt.R
import tech.hombre.freelancehunt.common.BidStatus
import tech.hombre.freelancehunt.common.ProjectStatus
import tech.hombre.freelancehunt.common.UserType
import tech.hombre.freelancehunt.common.extensions.*
import tech.hombre.freelancehunt.common.widgets.EndlessScroll
import tech.hombre.freelancehunt.databinding.FragmentMyBidsBinding
import tech.hombre.freelancehunt.ui.base.*
import tech.hombre.freelancehunt.ui.base.ViewState
import tech.hombre.freelancehunt.ui.menu.BottomMenuBuilder
import tech.hombre.freelancehunt.ui.menu.ListMenuBottomDialogFragment
import tech.hombre.freelancehunt.ui.menu.model.MenuItem
import tech.hombre.freelancehunt.ui.my.bids.presentation.MyBidsViewModel

class MyBidsFragment : BaseFragment<FragmentMyBidsBinding>(FragmentMyBidsBinding::inflate), ListMenuBottomDialogFragment.BottomListMenuListener {

    private val viewModel: MyBidsViewModel by viewModel()

    lateinit var adapter: RendererRecyclerViewAdapter

    val items = arrayListOf<MyBidsList.Data>()

    override fun viewReady() {
        initList()
        subscribeToData()
        viewModel.getMyBids(1)
    }

    private fun subscribeToData() {
        viewModel.viewState.subscribe(this, ::handleViewState)
        viewModel.bidAction.subscribe(this, ::handleBidAction)
        viewModel.projectDetails.subscribe(this, {
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

    private fun handleViewState(viewState: ViewState<MyBidsList>) {
        when (viewState) {
            is Loading -> showLoading()
            is Success -> initMyBidsList(viewState.data)
            is Error -> handleError(viewState.error.localizedMessage)
            is NoInternetState -> showNoInternetError()
        }
    }

    private fun handleBidAction(viewState: ViewState<Pair<Int, String>>) {
        hideLoading()
        when (viewState) {
            is Success -> {
                updateBid(viewState.data)
            }

            else -> {}
        }
    }

    private fun updateBid(result: Pair<Int, String>) {
        val position = items.indexOf(items.find { it.id == result.first })
        items[position].attributes.status = result.second
        adapter.notifyItemChanged(position)
    }

    private fun handleError(error: String) {
        hideLoading()
        showError(error, binding.bidsFragmentContainer)
    }

    private fun showNoInternetError() {
        hideLoading()
        snackbar(getString(R.string.no_internet_error_message), binding.bidsFragmentContainer)
    }

    private fun initList() {
        adapter = RendererRecyclerViewAdapter()
        adapter.enableDiffUtil(true)
        adapter.registerRenderer(
            ViewRenderer(
                R.layout.item_my_bids_list,
                MyBidsList.Data::class.java,
                BaseViewRenderer.Binder { model: MyBidsList.Data, finder: ViewFinder, payloads: List<Any?>? ->
                    finder
                        .find(R.id.status, ViewProvider<TextView> {
                            it.text = model.attributes.project.status.name
                            it.background.setColorFilter(
                                ContextCompat.getColor(
                                    requireContext(),
                                    getColorByProjectStatus(getProjectStatus(model.attributes.project.status.id))
                                ), PorterDuff.Mode.SRC_OVER
                            )
                        })
                        .find<TextView>(R.id.bidStatus) {
                            val status = getBidStatus(model.attributes.status)
                            it.text = getTitleByBidStatus(requireContext(), status)
                            it.background.setColorFilter(
                                ContextCompat.getColor(
                                    requireContext(),
                                    getColorByFreelancerStatus(status)
                                ), PorterDuff.Mode.SRC_OVER
                            )
                        }
                        .setText(R.id.name, model.attributes.project.name)
                        .setText(R.id.comment, model.attributes.comment)
                        .find(R.id.mybudget, ViewProvider<TextView> {
                            if (model.attributes.project.budget != null) {
                                it.text =
                                    "${model.attributes.project.budget!!.amount} ${currencyToChar(
                                        model.attributes.project.budget!!.currency
                                    )}"
                                it.visible()
                            } else it.gone()
                        })
                        .setText(
                            R.id.mybudget,
                            "${model.attributes.budget.amount} ${currencyToChar(model.attributes.budget.currency)}"
                        )
                        .setText(
                            R.id.days,
                            model.attributes.days.getEnding(requireContext(), R.array.ending_days)
                        )
                        .setOnClickListener(R.id.clickableView) {

                            val bidStatus = getBidStatus(model.attributes.status)
                            val isRevoked = bidStatus == BidStatus.REVOKED

                            val openForBids =
                                getProjectStatus(model.attributes.project.status.id) == ProjectStatus.OPEN_FOR_PROPOSALS
                            if (!openForBids && appPreferences.getCurrentUserType() != UserType.FREELANCER.type) {
                                viewModel.getProjectDetails(model.attributes.project.self)
                                return@setOnClickListener
                            }
                            if (appPreferences.getCurrentUserType() == UserType.FREELANCER.type) {
                                if (model.attributes.freelancer.id == appPreferences.getCurrentUserId() && openForBids) {
                                    BottomMenuBuilder(
                                        requireContext(),
                                        childFragmentManager,
                                        ListMenuBottomDialogFragment.TAG
                                    ).buildMenuForFreelancerBid(
                                        model.attributes.project.id,
                                        model.id,
                                        isRevoked
                                    )
                                } else viewModel.getProjectDetails(model.attributes.project.self)
                            } else viewModel.getProjectDetails(model.attributes.project.self)
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

        binding.list.addOnScrollListener(object : EndlessScroll() {
            override fun onLoadMore(page: Int, totalItemsCount: Int) {
                if (viewModel.pagination.next.isNotEmpty()) {
                    adapter.showLoadMore()
                    viewModel.getMyBids(page)
                }
            }
        })

        binding.refresh.setOnRefreshListener {
            items.clear()
            adapter.setItems(items)
            viewModel.getMyBids(1)
        }

        binding.bidsRevoked.setOnClickListener {
            setFilter("revoked")
        }
        binding.bidsActive.setOnClickListener {
            setFilter("active")
        }
        binding.bidsRejected.setOnClickListener {
            setFilter("rejected")
        }
    }

    private fun setFilter(status: String) {
        items.clear()
        adapter.setItems(items)
        viewModel.getMyBids(1, status)
    }

    private fun initMyBidsList(freelancersList: MyBidsList) {
        hideLoading()
        binding.refresh.isRefreshing = false

        val reversed = appPreferences.getProjectBidsListReversed()
        val itemsList = if (reversed) freelancersList.data.reversed() else freelancersList.data
        items.addAll(itemsList)
        adapter.setItems(items)
    }

    override fun onMenuItemSelected(primaryId: Int, secondaryId: Int, position: Int, model: MenuItem) {
        when (model.tag) {
            "revoke" -> viewModel.revokeProjectBids(primaryId, secondaryId)
            "restore" -> { }
            "view" -> appNavigator.showProjectDetails(primaryId)
        }
    }

    companion object {
        @Keep
        val TAG = MyBidsFragment::class.java.simpleName

        fun newInstance() = MyBidsFragment()
    }
}