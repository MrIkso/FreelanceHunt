package tech.hombre.freelancehunt.ui.main.view.fragments

import android.content.Intent
import android.net.Uri
import androidx.annotation.Keep

import tech.hombre.freelancehunt.R
import tech.hombre.freelancehunt.databinding.FragmentAboutBinding
import tech.hombre.freelancehunt.ui.base.BaseFragment


class AboutFragment : BaseFragment<FragmentAboutBinding>(FragmentAboutBinding::inflate) {

    override fun viewReady() {
        binding.buttonTelegram.setOnClickListener { showTelegram() }
        binding.buttonGithub.setOnClickListener {
            openUrl(
                requireContext(),
                getString(R.string.github_url)
            )
        }
        binding.buttonWeb.setOnClickListener { openUrl(requireContext(), getString(R.string.url)) }
        binding.buttonEmail.setOnClickListener { emailMe() }
    }

    private fun showTelegram() {
        val telegram = Intent(Intent.ACTION_VIEW)
        telegram.data = Uri.parse("tg:resolve?domain=freelancehunt_android")
        startActivity(Intent.createChooser(telegram, getString(R.string.send_with)))
    }

    private fun emailMe() {
        val intent = Intent(Intent.ACTION_SENDTO)
        intent.data = Uri.parse("mailto:");
        intent.putExtra(Intent.EXTRA_EMAIL, arrayOf(getString(R.string.email)))
        startActivity(Intent.createChooser(intent, getString(R.string.send_email)))
    }

    companion object {
        @Keep
        val TAG = AboutFragment::class.java.simpleName

        fun newInstance() = AboutFragment()
    }
}

