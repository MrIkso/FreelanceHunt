package tech.hombre.freelancehunt.ui.freelancers.view

import android.graphics.PorterDuff
import android.widget.TextView
import androidx.annotation.Keep
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.vivchar.rendererrecyclerviewadapter.*

import org.koin.androidx.viewmodel.ext.android.viewModel
import tech.hombre.domain.model.Countries
import tech.hombre.domain.model.FreelancerDetail
import tech.hombre.domain.model.FreelancersList
import tech.hombre.freelancehunt.R
import tech.hombre.freelancehunt.common.extensions.*
import tech.hombre.freelancehunt.common.widgets.CustomImageView
import tech.hombre.freelancehunt.common.widgets.EndlessScroll
import tech.hombre.freelancehunt.databinding.FragmentFreelancersBinding
import tech.hombre.freelancehunt.ui.base.*
import tech.hombre.freelancehunt.ui.base.ViewState
import tech.hombre.freelancehunt.ui.freelancers.presentation.FreelancersViewModel
import tech.hombre.freelancehunt.ui.menu.BottomMenuBuilder
import tech.hombre.freelancehunt.ui.menu.FreelancersFilterBottomDialogFragment

class FreelancersFragment : BaseFragment<FragmentFreelancersBinding>(FragmentFreelancersBinding::inflate), FreelancersFilterBottomDialogFragment.OnSubmitFreelancersFilter {

    private val viewModel: FreelancersViewModel by viewModel()

    lateinit var adapter: RendererRecyclerViewAdapter

    val items = arrayListOf<FreelancerDetail.Data>()

    var countries = listOf<Countries.Data>()

    override fun viewReady() {
        initList()
        subscribeToData()
        viewModel.getFreelancers(1)
    }

    private fun subscribeToData() {
        viewModel.viewState.subscribe(this, ::handleViewState)
        viewModel.countries.subscribe(this, {
            countries = it
        })

        viewModel.setCountries()
    }

    private fun handleViewState(viewState: ViewState<FreelancersList>) {
        when (viewState) {
            is Loading -> showLoading()
            is Success -> initFreelancersList(viewState.data)
            is Error -> handleError(viewState.error.localizedMessage)
            is NoInternetState -> showNoInternetError()
        }
    }

    private fun handleError(error: String) {
        hideLoading()
        showError(error, binding.freelancersFragmentContainer)
    }

    private fun showNoInternetError() {
        hideLoading()
        snackbar(getString(R.string.no_internet_error_message), binding.freelancersFragmentContainer)
    }


    private fun initList() {
        adapter = RendererRecyclerViewAdapter()
        adapter.enableDiffUtil(true)
        adapter.registerRenderer(
            ViewRenderer(
                R.layout.item_freelancers_list,
                FreelancerDetail.Data::class.java,
                BaseViewRenderer.Binder { model: FreelancerDetail.Data, finder: ViewFinder, payloads: List<Any?>? ->
                    finder
                        .setGone(R.id.verified, !model.attributes.verification.identity)
                        .find(R.id.status, ViewProvider<TextView> {
                            it.text = model.attributes.status.name
                            it.background.setColorFilter(
                                ContextCompat.getColor(
                                    requireContext(),
                                    getColorByFreelancerStatus(getFreelancerStatus(model.attributes.status.id))
                                ), PorterDuff.Mode.SRC_OVER
                            )
                        })
                        .setGone(R.id.isplus, !model.attributes.is_plus_active)
                        .setText(R.id.rating, model.attributes.rating.toString())
                        .setText(R.id.voteup, model.attributes.positive_reviews.toString())
                        .setText(R.id.votedown, model.attributes.negative_reviews.toString())
                        .find(
                            R.id.avatar,
                            ViewProvider<CustomImageView> { avatar ->
                                avatar.setUrl(
                                    model.attributes.avatar.large.url,
                                    isCircle = true
                                )
                            })
                        .setText(
                            R.id.name,
                            "${model.attributes.first_name} ${model.attributes.last_name}"
                        )
                        .setText(
                            R.id.skill,
                            model.attributes.skills.joinToString(separator = ", ") { it.name })
                        .find(R.id.location, ViewProvider<TextView> {
                            it.text = model.attributes.location?.let {
                                if (model.attributes.location!!.country != null && model.attributes.location!!.city != null)
                                    "${model.attributes.location!!.country!!.name}, ${model.attributes.location!!.city!!.name}"
                                else if (model.attributes.location!!.country != null)
                                    model.attributes.location!!.country!!.name else ""
                            }
                        })
                        .find(R.id.locationIcon, ViewProvider<CustomImageView> { locationIcon ->
                            if (model.attributes.location != null && model.attributes.location!!.country != null) {
                                val country =
                                    countries.find { it.id == model.attributes.location!!.country!!.id }
                                if (country != null) locationIcon.setUrlSVG("https://freelancehunt.com/static/images/flags/4x3/${country.iso2.toLowerCase()}.svg")
                            }
                        })
                        .setText(
                            R.id.serviceOn,
                            model.attributes.created_at.parseFullDate(
                                true
                            ).getSimpleTimeAgo(requireContext())
                        )
                        .setOnClickListener(R.id.clickableView) {
                            appNavigator.showFreelancerDetails(model.id)
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
                    viewModel.getFreelancers(page)
                }
            }
        })

        binding.refresh.setOnRefreshListener {
            refreshList()
        }

        binding.fab.setOnClickListener {
            BottomMenuBuilder(
                requireContext(),
                childFragmentManager,
                FreelancersFilterBottomDialogFragment.TAG
            ).buildMenuForFreelancersFilter(
                viewModel.skills,
                viewModel.countryId,
                viewModel.cityId
            )
        }
    }

    private fun refreshList() {
        items.clear()
        adapter.setItems(items)
        viewModel.getFreelancers(1)
    }

    private fun initFreelancersList(freelancersList: FreelancersList) {
        hideLoading()
        binding.refresh.isRefreshing = false

        items.addAll(freelancersList.data)
        adapter.setItems(items)
    }

    override fun onSubmitProjectFilter(skills: IntArray, countryId: Int, cityId: Int) {
        viewModel.setFreelancersFilters(skills, countryId, cityId)
        refreshList()
    }

    companion object {
        @Keep
        val TAG = FreelancersFragment::class.java.simpleName

        fun newInstance() = FreelancersFragment()
    }
}