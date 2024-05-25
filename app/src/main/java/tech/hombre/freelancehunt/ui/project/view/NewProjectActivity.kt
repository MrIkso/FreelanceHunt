package tech.hombre.freelancehunt.ui.project.view

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import org.koin.androidx.viewmodel.ext.android.viewModel
import tech.hombre.domain.model.MyBidsList
import tech.hombre.domain.model.ProjectDetail
import tech.hombre.domain.model.SkillList
import tech.hombre.freelancehunt.R
import tech.hombre.freelancehunt.common.*
import tech.hombre.freelancehunt.common.extensions.gone
import tech.hombre.freelancehunt.common.extensions.snackbar
import tech.hombre.freelancehunt.common.extensions.subscribe
import tech.hombre.freelancehunt.common.extensions.visible
import tech.hombre.freelancehunt.databinding.ActivityNewJobBinding
import tech.hombre.freelancehunt.framework.app.AppHelper
import tech.hombre.freelancehunt.ui.base.*
import tech.hombre.freelancehunt.ui.project.presentation.NewProjectViewModel
import java.text.SimpleDateFormat
import java.util.*


class NewProjectActivity : BaseActivity<ActivityNewJobBinding>(ActivityNewJobBinding::inflate) {

    private val viewModel: NewProjectViewModel by viewModel()

    private var skills = listOf<SkillList.Data>()

    var checkedSkills = booleanArrayOf()

    private var freelancerId = 0

    private var isPersonal = false

    private var endDate = ""

    override fun viewReady() {
        setSupportActionBar(binding.appbar.toolbar)
        setTitle(R.string.new_project)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        intent?.extras?.let {
            subscribeToData()
            isPersonal = it.getBoolean(EXTRA_1, false)
            freelancerId = it.getInt(EXTRA_2, 0)
            val freelancerLogin = it.getString(EXTRA_3, "")
            if (isPersonal && freelancerLogin.isNotEmpty()) supportActionBar?.subtitle = String.format(getString(
                            R.string.personal_project_for), freelancerLogin)
            initViews()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_new_project, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_open -> openUrl(this, "https://freelancehunt.com/project/add")
        }
        return super.onOptionsItemSelected(item)
    }

    private fun initViews() {
        binding.description.setOnTouchListener { view, event ->
            view.parent.requestDisallowInterceptTouchEvent(true)
            when (event.action and MotionEvent.ACTION_MASK) {
                MotionEvent.ACTION_UP -> view.parent.requestDisallowInterceptTouchEvent(false)
            }
            false
        }

        binding.skillsList.setOnClickListener {
            with(
                AlertDialog.Builder(
                    this,
                    AppHelper.getDialogTheme(appPreferences.getAppTheme())
                )
            ) {
                setTitle(getString(R.string.select_category))
                    .setMultiChoiceItems(
                        skills.map { it.name }.toTypedArray(),
                        checkedSkills
                    ) { dialog, which, isChecked ->
                        checkedSkills[which] = isChecked


                        val checkedSize = checkedSkills.filter { it }.size
                        if (checkedSize > 0) {
                            var placeholder = ""
                            checkedSkills.forEachIndexed { index, skill ->
                                if (skill) {
                                    placeholder += skills[index].name + ","
                                }
                            }
                            binding.skillsList.text =
                                placeholder.removeRange(placeholder.length - 1, placeholder.length)
                        } else binding.skillsList.text = getString(R.string.select)
                    }
                setPositiveButton("OK") { dialog, which -> }
                show()
            }
        }

        binding.endDateButton.setOnClickListener {
            val calendar: Calendar = Calendar.getInstance()

            calendar.time = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.getDefault()).parse(endDate)

            val yy: Int = calendar.get(Calendar.YEAR)
            val mm: Int = calendar.get(Calendar.MONTH)
            val dd: Int = calendar.get(Calendar.DAY_OF_MONTH)

            val picker = DatePickerDialog(
                this,
                DatePickerDialog.OnDateSetListener { view, year, month, dayOfMonth ->

                    val calendar = Calendar.getInstance()
                    calendar.set(year, month, dayOfMonth)

                    val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.getDefault())
                    endDate = format.format(calendar.time)

                    binding.endDateButton.text = String.format(
                        "%s/%s/%s",
                        if (dayOfMonth > 9) dayOfMonth else "0$dayOfMonth",
                        if (month + 1 > 9) month + 1 else "0${month + 1}",
                        year
                    )

                },
                yy,
                mm,
                dd
            )
            picker.datePicker.minDate = System.currentTimeMillis() - 1000
            picker.show()
        }

        if (!isPersonal) {
            binding.plusView.visible()
            binding.isForPlus.isEnabled = appPreferences.getCurrentUserProfile()?.is_plus_active ?: false
        } else binding.plusView.gone()

        val calendar: Calendar = Calendar.getInstance()
        val yy: Int = calendar.get(Calendar.YEAR)
        val mm: Int = calendar.get(Calendar.MONTH) + 1
        val dd: Int = calendar.get(Calendar.DAY_OF_MONTH)
        binding.endDateButton.text = String.format("%s/%s/%s", if (dd > 9) dd else "0$dd", if (mm > 9) mm else "0$mm", yy)
        val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.getDefault())
        endDate = format.format(calendar.time)

        binding.buttonSubmit.setOnClickListener {
            if (correctInputs()) {
                val currency = CurrencyType.values()[binding.budgetType.selectedItemPosition]
                val budget = MyBidsList.Data.Attributes.Budget(
                    binding.budgetValue.text.toString().toInt(),
                    currency.currency
                )
                val safe = SafeType.values()[binding.safeType.selectedItemPosition]
                val checkedSkill = arrayListOf<Int>()
                checkedSkills.forEachIndexed { index, skill ->
                    if (skill) {
                        checkedSkill.add(skills[index].id)
                    }
                }

                if (isPersonal)
                    viewModel.addNewPersonalProject(
                        binding.projectTitle.text.toString(),
                        freelancerId,
                        isPersonal,
                        budget,
                        safe.type ?: "employer",
                        binding.description.savedText.toString(),
                        checkedSkill,
                        endDate
                    ) else
                    viewModel.addNewProject(
                        binding.projectTitle.text.toString(),
                        budget,
                        safe.type ?: "employer",
                        binding.description.savedText.toString(),
                        checkedSkill,
                        endDate,
                        binding.isForPlus.isChecked
                    )

            } else {
                showError(getString(R.string.check_inputs))
            }
        }
    }

    private fun correctInputs(): Boolean {
        return endDate.isNotEmpty() &&
                !binding.projectTitle.text.isNullOrEmpty() &&
                !binding.budgetValue.text.isNullOrEmpty() &&
                binding.description.savedText.isNotEmpty() && checkedSkills.any { it }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    private fun subscribeToData() {
        viewModel.viewState.subscribe(this, ::handleViewState)
        viewModel.skills.subscribe(this, {
            skills = it
            checkedSkills = skills.map { false }.toBooleanArray()
            hideLoading(binding.appbar.progressBar)
        })
        viewModel.setSkills()
    }

    private fun handleViewState(viewState: ViewState<ProjectDetail>) {
        when (viewState) {
            is Loading -> showLoading(binding.appbar.progressBar)
            is Success -> {
                hideLoading(binding.appbar.progressBar)
                finish()
                appNavigator.showProjectDetails(viewState.data.data)
            }
            is Error -> {
                handleError(viewState.error.localizedMessage)
            }
            is NoInternetState -> showNoInternetError()
        }
    }

    private fun handleError(error: String) {
        hideLoading(binding.appbar.progressBar)
        showError(error)
    }


    private fun showNoInternetError() {
        hideLoading(binding.appbar.progressBar)
        snackbar(getString(R.string.no_internet_error_message), binding.newProjectContainer)
    }

    override fun onBackPressed() {
        if (canExit()) super.onBackPressed()
    }

    companion object {

        fun startActivity(context: Context, isPersonal: Int, freelancerId: Int, freelancerLogin: String = "") {
            val intent = Intent(context, NewProjectActivity::class.java)
            intent.putExtra(EXTRA_1, isPersonal)
            intent.putExtra(EXTRA_2, freelancerId)
            intent.putExtra(EXTRA_3, freelancerLogin)
            context.startActivity(intent)
        }
    }


}