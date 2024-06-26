package tech.hombre.freelancehunt.ui.main.view.fragments

import android.os.Bundle
import android.view.MenuItem
import androidx.annotation.Keep
import com.google.android.material.bottomnavigation.BottomNavigationView
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import tech.hombre.freelancehunt.R
import tech.hombre.freelancehunt.common.EXTRA_1
import tech.hombre.freelancehunt.common.extensions.subscribe
import tech.hombre.freelancehunt.common.extensions.switch
import tech.hombre.freelancehunt.databinding.FragmentMainBinding
import tech.hombre.freelancehunt.ui.base.BaseFragment
import tech.hombre.freelancehunt.ui.main.presentation.MainPublicViewModel


class MainFragment : BaseFragment<FragmentMainBinding>(FragmentMainBinding::inflate), BottomNavigationView.OnNavigationItemSelectedListener {

    private val sharedViewModelMain: MainPublicViewModel by sharedViewModel()

    override fun viewReady() {
        subscribeToData()
        binding.bottomNavigationView.setOnNavigationItemSelectedListener(this)
        arguments?.let {
            val isFeed = it.getBoolean(EXTRA_1)
            binding.bottomNavigationView.selectedItemId = if (isFeed) R.id.menu_feed else R.id.menu_projects
        }
        binding.fab.setOnClickListener {
            when (binding.bottomNavigationView.selectedItemId) {
                R.id.menu_projects -> sharedViewModelMain.onFabClickAction("project_filter")
                R.id.menu_contests -> sharedViewModelMain.onFabClickAction("contest_filter")
            }
        }
    }

    private fun subscribeToData() {
        sharedViewModelMain.feedBadgeCounter.subscribe(this) {
            val menuItemId = binding.bottomNavigationView.menu.getItem(0).itemId
            val badgeDrawable = binding.bottomNavigationView.getOrCreateBadge(menuItemId)
            if (it > 0) {
                badgeDrawable.number = it
                badgeDrawable.isVisible = true
            } else {
                badgeDrawable.isVisible = false
            }
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        hideShowFab(item.itemId)
        return when (item.itemId) {
            R.id.menu_feed -> {
                childFragmentManager.switch(
                    R.id.fragmentContainer,
                    FeedFragment.newInstance(),
                    FeedFragment.TAG
                )
                true
            }
            R.id.menu_projects -> {
                childFragmentManager.switch(
                    R.id.fragmentContainer,
                    ProjectsFragment.newInstance(),
                    ProjectsFragment.TAG
                )
                true
            }
            R.id.menu_contests -> {
                childFragmentManager.switch(
                    R.id.fragmentContainer,
                    ContestsFragment.newInstance(),
                    ContestsFragment.TAG
                )
                true
            }
            else -> false
        }
    }

    private fun hideShowFab(itemId: Int) {
        when(itemId) {
            //R.id.menu_contests,
            R.id.menu_projects -> {
                binding.fab.setImageResource(R.drawable.filter)
                binding.fab.show()
            }
            else -> binding.fab.hide()
        }
    }

    companion object {
        @Keep
        val TAG = MainFragment::class.java.simpleName

        fun newInstance(isFeed: Boolean = false): MainFragment {
            val fragment = MainFragment()
            val extra = Bundle()
            extra.putBoolean(EXTRA_1, isFeed)
            fragment.arguments = extra
            return fragment
        }
    }
}

