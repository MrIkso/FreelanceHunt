package tech.hombre.freelancehunt.ui.threads.view

import androidx.annotation.Keep
import androidx.appcompat.widget.AppCompatImageView
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.vivchar.rendererrecyclerviewadapter.*
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel
import tech.hombre.domain.model.ThreadsList
import tech.hombre.freelancehunt.R
import tech.hombre.freelancehunt.common.extensions.getTimeAgo
import tech.hombre.freelancehunt.common.extensions.parseFullDate
import tech.hombre.freelancehunt.common.extensions.snackbar
import tech.hombre.freelancehunt.common.extensions.subscribe
import tech.hombre.freelancehunt.common.widgets.CustomImageView
import tech.hombre.freelancehunt.common.widgets.EndlessScroll
import tech.hombre.freelancehunt.databinding.FragmentThreadsBinding
import tech.hombre.freelancehunt.ui.base.*
import tech.hombre.freelancehunt.ui.base.ViewState
import tech.hombre.freelancehunt.ui.main.presentation.MainPublicViewModel
import tech.hombre.freelancehunt.ui.threads.presentation.ThreadsViewModel
import java.util.*

class ThreadsFragment : BaseFragment<FragmentThreadsBinding>(FragmentThreadsBinding::inflate) {

    private val viewModel: ThreadsViewModel by viewModel()

    private val sharedViewModelMain: MainPublicViewModel by sharedViewModel()

    lateinit var adapter: RendererRecyclerViewAdapter

    val items = arrayListOf<ThreadsList.Data>()

    private var timer = Timer()

    // TODO to preferences
    private val delay = 15000L

    override fun viewReady() {
        initList()
        subscribeToData()
        viewModel.getThreads()
        timer.schedule(timerTask, delay, delay)
    }

    private fun subscribeToData() {
        viewModel.viewState.subscribe(this, ::handleViewState)
    }

    private fun handleViewState(viewState: ViewState<ThreadsList>) {
        when (viewState) {
            is Loading -> showLoading()
            is Success -> initThreadsList(viewState.data)
            is Error -> handleError(viewState.error.localizedMessage)
            is NoInternetState -> showNoInternetError()
        }
    }

    private fun handleError(error: String) {
        hideLoading()
        showError(error, binding.threadsFragmentContainer)
    }

    private fun showNoInternetError() {
        hideLoading()
        snackbar(getString(R.string.no_internet_error_message), binding.threadsFragmentContainer)
    }


    private fun initList() {
        adapter = RendererRecyclerViewAdapter()
        adapter.enableDiffUtil(true)
        adapter.registerRenderer(
            ViewRenderer(
                R.layout.item_threads_list,
                ThreadsList.Data::class.java,
                BaseViewRenderer.Binder { model: ThreadsList.Data, finder: ViewFinder, payloads: List<Any?>? ->
                    finder
                        .find(R.id.icon, ViewProvider<AppCompatImageView> {
                            if (!model.attributes.is_unread) it.alpha = 0.5F else it.alpha = 1F
                        })
                        .setText(
                            R.id.lastAt,
                            model.attributes.last_post_at.parseFullDate(true).getTimeAgo()
                        )
                        .setText(R.id.subject, model.attributes.subject)
                        /*.setImageDrawable(
                            R.id.status,
                            ContextCompat.getDrawable(
                                requireContext(),
                                if (model.attributes.is_unread) R.drawable.check else R.drawable.checkall
                            )
                        )*/
                        .find(
                            R.id.avatar,
                            ViewProvider<CustomImageView> { avatar ->
                                avatar.setUrl(
                                    model.attributes.participants.from.avatar.large.url,
                                    isCircle = true
                                )
                            })
                        .setText(
                            R.id.name,
                            "${model.attributes.participants.from.first_name} ${model.attributes.participants.from.last_name}"
                        )
                        //.setText(R.id.messages, model.attributes.messages_count.getEnding(requireContext(), R.array.ending_messages))
                        .setOnClickListener(R.id.clickableView) {
                            model.attributes.is_unread = false
                            adapter.notifyItemChanged(items.indexOf(model))
                            sharedViewModelMain.setMessagesCounter(items.any { it.attributes.is_unread })
                            ThreadMessagesActivity.startActivity(
                                requireActivity(),
                                model.id,
                                model.links.self.web
                            )
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
                    viewModel.getThreads(viewModel.pagination.next)
                }
            }
        })

        binding.refresh.setOnRefreshListener {
            refreshList(true)
        }
    }

    private fun refreshList(reload: Boolean) {
        items.clear()
        if (reload) {
            adapter.setItems(items)
        }
        viewModel.getThreads()
    }

    private fun initThreadsList(freelancersList: ThreadsList) {
        hideLoading()
        binding.refresh.isRefreshing = false

        items.addAll(freelancersList.data)
        adapter.setItems(items)

        if (items.isEmpty()) return

        val lastMessageId = items.first().attributes.last_post_at.parseFullDate(true)?.time ?: 0
        appPreferences.setLastMessageId(lastMessageId)
    }

    private val timerTask = object : TimerTask() {
        override fun run() {
            activity?.runOnUiThread {
                refreshList(false)
            }

        }
    }

    override fun onDestroyView() {
        timer.cancel()
        timer.purge()
        super.onDestroyView()
    }

    companion object {
        @Keep
        val TAG = ThreadsFragment::class.java.simpleName

        fun newInstance() =
            ThreadsFragment()
    }
}