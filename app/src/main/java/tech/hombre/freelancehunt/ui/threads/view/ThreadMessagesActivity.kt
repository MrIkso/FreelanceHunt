package tech.hombre.freelancehunt.ui.threads.view

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.view.Menu
import android.view.MenuItem
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.vivchar.rendererrecyclerviewadapter.*
import org.koin.androidx.viewmodel.ext.android.viewModel
import tech.hombre.domain.model.ThreadMessageList
import tech.hombre.domain.model.ThreadMessageMy
import tech.hombre.domain.model.ThreadMessageOther
import tech.hombre.freelancehunt.R
import tech.hombre.freelancehunt.common.*
import tech.hombre.freelancehunt.common.extensions.*
import tech.hombre.freelancehunt.common.widgets.CustomHtmlTextView
import tech.hombre.freelancehunt.common.widgets.CustomImageView
import tech.hombre.freelancehunt.common.widgets.filepicker.controller.DialogSelectionListener
import tech.hombre.freelancehunt.common.widgets.filepicker.model.DialogConfigs
import tech.hombre.freelancehunt.common.widgets.filepicker.model.DialogProperties
import tech.hombre.freelancehunt.common.widgets.filepicker.view.FilePickerDialog
import tech.hombre.freelancehunt.databinding.ActivityThreadMessagesBinding
import tech.hombre.freelancehunt.ui.base.*
import tech.hombre.freelancehunt.ui.base.ViewState
import tech.hombre.freelancehunt.ui.threads.presentation.ThreadMessagesViewModel
import java.io.File
import java.net.URLEncoder
import java.util.*


class ThreadMessagesActivity : BaseActivity<ActivityThreadMessagesBinding>(ActivityThreadMessagesBinding::inflate) {

    private val viewModel: ThreadMessagesViewModel by viewModel()

    private var threadId = 0

    private var threadUrl = ""

    lateinit var adapter: RendererRecyclerViewAdapter

    private var messagesGroup = arrayListOf<ViewModel>()

    private var messages = arrayListOf<ThreadMessageList.Data>()

    private var timer = Timer()

    // TODO to preferences
    private val delay = 15000L

    lateinit var dialog: FilePickerDialog

    private var isUploading = false

    override fun viewReady() {
        setSupportActionBar(binding.appbar.toolbar)
        setTitle(R.string.thread_view)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        intent?.extras?.let {
            threadId = it.getInt(EXTRA_1, -1)
            threadUrl = it.getString(EXTRA_2, "") ?: ""
            subscribeToData()
            initMessagesList()
            viewModel.getMessages(threadId)
            initViews()
        }
    }

    private fun initViews() {
        binding.attach.setOnClickListener {
            if (!isUploading) showSelectFileDialog()
        }
        binding.send.setOnClickListener {
            binding.list.hideKeyboard()
            if (correctInputs()) {
                viewModel.sendMessage(threadId, binding.editText.savedText.toString())
            }
        }
    }

    private fun correctInputs(): Boolean {
        return binding.editText.savedText.isNotEmpty()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_thread, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_share -> shareUrl(this, threadUrl)
            R.id.action_open -> openUrl(this, threadUrl)
        }
        return super.onOptionsItemSelected(item)
    }

    private fun subscribeToData() {
        viewModel.viewState.subscribe(this, ::handleViewState)
        viewModel.message.subscribe(this, ::handleMessageViewState)
        viewModel.uploading.subscribe(this, ::handleUploadingViewState)
    }

    private fun handleViewState(viewState: ViewState<ThreadMessageList>) {
        when (viewState) {
            is Loading -> showLoading(binding.appbar.progressBar)
            is Success -> initMessages(viewState.data.data)
            is Error -> handleError(viewState.error.localizedMessage)
            is NoInternetState -> showNoInternetError()
        }
    }

    private fun handleMessageViewState(viewState: ViewState<ThreadMessageList.Data>) {
        when (viewState) {
            is Success -> addMessage(viewState.data)
            else -> {}
        }
    }

    private fun handleUploadingViewState(viewState: ViewState<ThreadMessageList.Data>) {
        when (viewState) {
            is Success -> addAttachMessage(viewState.data)
            is Error -> handleError(
                viewState.error.localizedMessage ?: getString(R.string.internet_error_message)
            )
            is NoInternetState -> showNoInternetError()
            else -> {}
        }
        isUploading = false
        binding.attachProgress.invisible()
    }

    private fun addMessage(message: ThreadMessageList.Data) {
        binding.editText.setText("")
        messagesGroup.add(ThreadMessageMy(message))
        adapter.setItems(messagesGroup)
        binding.list.postDelayed(
            { binding.list.scrollToPosition(adapter.itemCount - 1) },
            100
        )

    }

    private fun addAttachMessage(message: ThreadMessageList.Data) {
        messagesGroup.add(ThreadMessageMy(message))
        adapter.setItems(messagesGroup)
        binding.list.postDelayed(
            { binding.list.scrollToPosition(adapter.itemCount - 1) },
            100
        )
    }

    private fun initMessagesList() {
        adapter = RendererRecyclerViewAdapter()
        adapter.enableDiffUtil(true)
        adapter.registerRenderer(
            ViewRenderer(
                R.layout.item_thread_message_my,
                ThreadMessageMy::class.java,
                BaseViewRenderer.Binder { model: ThreadMessageMy, finder: ViewFinder, payloads: List<Any?>? ->
                    finder
                        .find(
                            R.id.avatar,
                            ViewProvider<CustomImageView> { avatar ->
                                avatar.setUrl(
                                    model.data.attributes.participants.from.avatar.small.url,
                                    isCircle = true
                                )
                                avatar.setOnClickListener {
                                    if (model.data.attributes.participants.from.type == UserType.EMPLOYER.type) {
                                        appNavigator.showEmployerDetails(model.data.attributes.participants.from.id)
                                    } else appNavigator.showFreelancerDetails(model.data.attributes.participants.from.id)
                                }
                            })
                        .find<CustomHtmlTextView>(R.id.text) {
                            if (model.data.attributes.message_html.isNotEmpty()) {
                                it.setHtmlText(model.data.attributes.message_html, false, false)
                                it.visible()
                            } else it.gone()
                        }
                        .setText(
                            R.id.postedAt,
                            model.data.attributes.posted_at.parseFullDate(true).getTimeAgo()
                        )


                    if (model.data.attributes.attachments.isNotEmpty()) {
                        val attachmentsAdapter = RendererRecyclerViewAdapter()
                        attachmentsAdapter.registerRenderer(
                            ViewRenderer(
                                R.layout.item_threads_attachment_my,
                                ThreadMessageList.Data.Attributes.Attachment::class.java,
                                BaseViewRenderer.Binder { model: ThreadMessageList.Data.Attributes.Attachment, finder: ViewFinder, payloads: List<Any?>? ->
                                    finder
                                        .find(
                                            R.id.thumbnail,
                                            ViewProvider<CustomImageView> { thumbnail ->
                                                if (model.thumbnail_url.isNullOrEmpty()) {
                                                    thumbnail.setUrlSVG(
                                                        "https://freelancehunt.com/static/images/file-types/${getFileTypeByExtension(
                                                            model.url.extension()
                                                        )}.svg"
                                                    )
                                                } else thumbnail.setUrl(
                                                    model.thumbnail_url ?: ""
                                                )
                                            })
                                        .setText(
                                            R.id.title,
                                            model.name
                                        )
                                        .setText(
                                            R.id.size,
                                            model.size.toLong().humanReadableBytes()
                                        )
                                        .setOnClickListener {
                                            val browserIntent = Intent(
                                                Intent.ACTION_VIEW,
                                                Uri.parse(model.url)
                                            )
                                            startActivity(browserIntent)
                                        }
                                }
                            )
                        )
                        finder.find<RecyclerView>(R.id.attachments).adapter = attachmentsAdapter
                        attachmentsAdapter.setItems(model.data.attributes.attachments)
                    }

                }
            )
        )
        adapter.registerRenderer(
            ViewRenderer(
                R.layout.item_thread_message,
                ThreadMessageOther::class.java,
                BaseViewRenderer.Binder { model: ThreadMessageOther, finder: ViewFinder, payloads: List<Any?>? ->
                    finder
                        .find(
                            R.id.avatar,
                            ViewProvider<CustomImageView> { avatar ->
                                avatar.setUrl(
                                    model.data.attributes.participants.from.avatar.small.url,
                                    isCircle = true
                                )
                                avatar.setOnClickListener {
                                    if (model.data.attributes.participants.from.type == UserType.EMPLOYER.type) {
                                        appNavigator.showEmployerDetails(model.data.attributes.participants.from.id)
                                    } else appNavigator.showFreelancerDetails(model.data.attributes.participants.from.id)
                                }
                            })
                        .find<CustomHtmlTextView>(R.id.text) {
                            if (model.data.attributes.message_html.isNotEmpty()) {
                                it.setHtmlText(model.data.attributes.message_html, false, false)
                                it.visible()
                            } else it.gone()
                        }
                        .setText(
                            R.id.postedAt,
                            model.data.attributes.posted_at.parseFullDate(true).getTimeAgo()
                        )
                    if (model.data.attributes.attachments.isNotEmpty()) {
                        val attachmentsAdapter = RendererRecyclerViewAdapter()
                        attachmentsAdapter.registerRenderer(
                            ViewRenderer(
                                R.layout.item_threads_attachment,
                                ThreadMessageList.Data.Attributes.Attachment::class.java,
                                BaseViewRenderer.Binder { model: ThreadMessageList.Data.Attributes.Attachment, finder: ViewFinder, payloads: List<Any?>? ->
                                    finder
                                        .find(
                                            R.id.thumbnail,
                                            ViewProvider<CustomImageView> { thumbnail ->
                                                if (model.thumbnail_url.isNullOrEmpty()) {
                                                    thumbnail.setUrlSVG(
                                                        "https://freelancehunt.com/static/images/file-types/${getFileTypeByExtension(
                                                            model.url.extension()
                                                        )}.svg"
                                                    )
                                                } else thumbnail.setUrl(
                                                    model.thumbnail_url ?: ""
                                                )
                                            })
                                        .setText(
                                            R.id.title,
                                            model.name
                                        )
                                        .setText(
                                            R.id.size,
                                            model.size.toLong().humanReadableBytes()
                                        )
                                        .setOnClickListener {
                                            val browserIntent = Intent(
                                                Intent.ACTION_VIEW,
                                                Uri.parse(model.url)
                                            )
                                            startActivity(browserIntent)
                                        }
                                }
                            )
                        )
                        finder.find<RecyclerView>(R.id.attachments).adapter = attachmentsAdapter
                        attachmentsAdapter.setItems(model.data.attributes.attachments)
                    }
                }
            )
        )
        binding.list.layoutManager = LinearLayoutManager(this).apply { stackFromEnd = true }
        binding.list.adapter = adapter

        binding.refresh.setOnRefreshListener {
            viewModel.getMessages(threadId)
        }

        timer.schedule(timerTask, delay, delay)
    }

    private fun initMessages(messages: List<ThreadMessageList.Data>) {
        hideLoading(binding.appbar.progressBar)
        binding.refresh.isRefreshing = false
        this.messages = messages as ArrayList<ThreadMessageList.Data>
        messagesGroup = messages.map {
            if (it.attributes.participants.from.login == getCurrentUser()) ThreadMessageMy(it) else ThreadMessageOther(
                it
            )
        } as ArrayList<ViewModel>
        adapter.setItems(messagesGroup)
    }

    private fun handleError(error: String) {
        hideLoading(binding.appbar.progressBar)
        showError(error)
    }

    private fun showNoInternetError() {
        hideLoading(binding.appbar.progressBar)
        snackbar(getString(R.string.no_internet_error_message), binding.threadActivityContainer)
    }

    private val timerTask = object : TimerTask() {
        override fun run() {
            runOnUiThread {
                if (!isUploading) viewModel.getMessages(threadId)
            }
        }
    }

    override fun finish() {
        timer.cancel()
        timer.purge()
        super.finish()
    }

    private fun showSelectFileDialog() {
        if (Build.VERSION.SDK_INT >= 29) {
            startActivityForResult(
                Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "*/*"
                    addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                },
                REQUEST_CODE_FILEPICKER
            )
        } else {
            val properties = DialogProperties().apply {
                selection_mode = DialogConfigs.SINGLE_MODE
                selection_type = DialogConfigs.FILE_SELECT
                root = File(DialogConfigs.DEFAULT_DIR)
                error_dir = File(DialogConfigs.DEFAULT_DIR)
                offset = File(DialogConfigs.DEFAULT_DIR)
                show_hidden_files = false
            }
            dialog = FilePickerDialog(this, properties)
            dialog.setTitle(getString(R.string.attach_selector))
            dialog.setDialogSelectionListener(object : DialogSelectionListener {
                override fun onSelectedFilePaths(files: Array<String?>) {
                    val path = files[0]
                    path?.let {
                        binding.attachProgress.visible()
                        isUploading = true
                        viewModel.uploadAttach(
                            threadId,
                            path,
                            path.substringAfterLast("/")
                        ) { progress ->
                            binding.attachProgress.progress = (progress * 100.0).toInt()
                        }
                    }
                }
            })
            dialog.show()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            FilePickerDialog.EXTERNAL_READ_PERMISSION_GRANT -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (this::dialog.isInitialized) dialog.show()
                } else {
                    showError(getString(R.string.permissions_denied))
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && requestCode == REQUEST_CODE_FILEPICKER) {
            data?.data?.also { uri ->
                val filename = uri.filename()
                val fileSize = uri.filesize()

                if (filename.extension().toLowerCase() !in ATTACH_FILE_EXTENSIONS) {
                    toast(
                        String.format(
                            getString(R.string.attach_invalid_file),
                            ATTACH_FILE_EXTENSIONS.joinToString()
                        )
                    )
                    return
                }
                if (fileSize > ATTACH_MAX_FILESIZE) {
                    toast(
                        String.format(
                            getString(R.string.attach_max_size),
                            ATTACH_MAX_FILESIZE.humanReadableBytes()
                        )
                    )
                    return
                }
                binding.attachProgress.visible()
                isUploading = true
                viewModel.uploadAttach(
                    uri,
                    threadId,
                    URLEncoder.encode(filename, "utf-8")
                ) { progress ->
                    binding.attachProgress.progress = (progress * 100.0).toInt()
                }
            }
        }
    }

    companion object {

        fun startActivity(context: Context, threadId: Int, threadUrl: String) {
            val intent = Intent(context, ThreadMessagesActivity::class.java)
            intent.putExtra(EXTRA_1, threadId)
            intent.putExtra(EXTRA_2, threadUrl)
            context.startActivity(intent)
        }
    }

}