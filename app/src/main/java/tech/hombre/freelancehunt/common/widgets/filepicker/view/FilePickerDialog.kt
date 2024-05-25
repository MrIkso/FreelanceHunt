package tech.hombre.freelancehunt.common.widgets.filepicker.view

import android.Manifest
import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.Window
import android.widget.AdapterView
import android.widget.ListView
import android.widget.Toast
import tech.hombre.freelancehunt.R
import tech.hombre.freelancehunt.common.widgets.filepicker.controller.DialogSelectionListener
import tech.hombre.freelancehunt.common.widgets.filepicker.controller.NotifyItemChecked
import tech.hombre.freelancehunt.common.widgets.filepicker.controller.adapters.FileListAdapter
import tech.hombre.freelancehunt.common.widgets.filepicker.model.DialogConfigs
import tech.hombre.freelancehunt.common.widgets.filepicker.model.DialogProperties
import tech.hombre.freelancehunt.common.widgets.filepicker.model.FileListItem
import tech.hombre.freelancehunt.common.widgets.filepicker.model.MarkedItemList
import tech.hombre.freelancehunt.common.widgets.filepicker.utils.ExtensionFilter
import tech.hombre.freelancehunt.common.widgets.filepicker.utils.Utility
import tech.hombre.freelancehunt.common.widgets.filepicker.widget.MaterialCheckbox
import tech.hombre.freelancehunt.databinding.DialogMainBinding
import tech.hombre.freelancehunt.framework.app.ViewHelper
import java.io.File
import java.util.*


class FilePickerDialog : Dialog, AdapterView.OnItemClickListener {
	private val ctx: Context
	private var properties: DialogProperties
	private var callbacks: DialogSelectionListener? = null
	private var internalList: ArrayList<FileListItem>
	private var filter: ExtensionFilter
	private var mFileListAdapter: FileListAdapter? = null
	private var titleStr: String? = null
	private var positiveBtnNameStr: String? = null
	private var negativeBtnNameStr: String? = null
	private lateinit var listView: ListView
	private var binding: DialogMainBinding? = null;

	constructor(context: Context) : super(context) {
		this.ctx = context
		properties = DialogProperties()
		filter = ExtensionFilter(properties)
		internalList = ArrayList()
	}

	constructor(context: Context, properties: DialogProperties, themeResId: Int) : super(context, themeResId) {
		this.ctx = context
		this.properties = properties
		filter = ExtensionFilter(properties)
		internalList = ArrayList()
	}

	constructor(context: Context, properties: DialogProperties) : super(context) {
		this.ctx = context
		this.properties = properties
		filter = ExtensionFilter(properties)
		internalList = ArrayList()
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		requestWindowFeature(Window.FEATURE_NO_TITLE)
		binding = DialogMainBinding.inflate(layoutInflater)
		setContentView(binding!!.root)
		listView = findViewById(R.id.fileList)
		val size = MarkedItemList.fileCount
		if (size == 0) {
			binding!!.footer.select.isEnabled = false
			val color: Int = ViewHelper.getAccentColor(ctx)
			binding!!.footer.select.setTextColor(Color.argb(128, Color.red(color), Color.green(color),
					Color.blue(color)))
		}
		if (negativeBtnNameStr != null) {
			binding!!.footer.cancel.text = negativeBtnNameStr
		}
		binding!!.footer.select.setOnClickListener {
			val paths = MarkedItemList.selectedPaths
			if (callbacks != null) {
				callbacks!!.onSelectedFilePaths(paths)
			}
			dismiss()
		}
		binding!!.footer.cancel.setOnClickListener { cancel() }
		mFileListAdapter = FileListAdapter(internalList, ctx, properties)
		mFileListAdapter?.setNotifyItemCheckedListener(object : NotifyItemChecked {
			override fun notifyCheckBoxIsClicked() {
				positiveBtnNameStr = if (positiveBtnNameStr == null) ctx.resources.getString(R.string.choose_button_label) else positiveBtnNameStr
				val size = MarkedItemList.fileCount
				if (size == 0) {
					binding!!.footer.select.isEnabled = false
					val color: Int = ViewHelper.getAccentColor(ctx)
					binding!!.footer.select.setTextColor(Color.argb(128, Color.red(color), Color.green(color),
							Color.blue(color)))
					binding!!.footer.select.text = positiveBtnNameStr
				} else {
					binding!!.footer.select.isEnabled = true
					val color: Int = ViewHelper.getAccentColor(ctx)
					binding!!.footer.select.setTextColor(color)
					val button_label = "$positiveBtnNameStr ($size) "
					binding!!.footer.select.text = button_label
				}
				if (properties.selection_mode == DialogConfigs.SINGLE_MODE) {
					mFileListAdapter!!.notifyDataSetChanged()
				}
			}

		})
		listView.adapter = mFileListAdapter
		setTitle()
	}

	private fun setTitle() {
		if (binding!!.header.title == null || binding!!.header.dname == null) {
			return
		}
		if (titleStr != null) {
			if (binding!!.header.title.visibility == View.INVISIBLE) {
				binding!!.header.title.visibility = View.VISIBLE
			}
			binding!!.header.title.text = titleStr
			if (binding!!.header.dname.visibility == View.VISIBLE) {
				binding!!.header.dname.visibility = View.INVISIBLE
			}
		} else {
			if (binding!!.header.title.visibility == View.VISIBLE) {
				binding!!.header.title.visibility = View.INVISIBLE
			}
			if (binding!!.header.dname.visibility == View.INVISIBLE) {
				binding!!.header.dname.visibility = View.VISIBLE
			}
		}
	}

	override fun onStart() {
		super.onStart()
		positiveBtnNameStr = if (positiveBtnNameStr == null) ctx.resources.getString(R.string.choose_button_label) else positiveBtnNameStr
		binding!!.footer.select.text = positiveBtnNameStr
		if (Utility.checkStorageAccessPermissions(context)) {
			val currLoc: File
			internalList.clear()
			if (properties.offset.isDirectory && validateOffsetPath()) {
				currLoc = File(properties.offset.absolutePath)
				val parent = FileListItem()
				parent.filename = ctx.getString(R.string.label_parent_dir)
				parent.isDirectory = true
				parent.location = Objects.requireNonNull(currLoc.parentFile)
						.absolutePath
				parent.time = currLoc.lastModified()
				internalList.add(parent)
			} else if (properties.root.exists() && properties.root.isDirectory) {
				currLoc = File(properties.root.absolutePath)
			} else {
				currLoc = File(properties.error_dir.absolutePath)
			}
			binding!!.header.dname!!.text = currLoc.name
			binding!!.header.dirPath!!.text = currLoc.absolutePath
			setTitle()
			internalList = Utility.prepareFileListEntries(internalList, currLoc, filter,
					properties.show_hidden_files)
			mFileListAdapter!!.notifyDataSetChanged()
			listView.onItemClickListener = this
		}
	}

	private fun validateOffsetPath(): Boolean {
		val offsetPath = properties.offset.absolutePath
		val rootPath = properties.root.absolutePath
		return offsetPath != rootPath && offsetPath.contains(rootPath)
	}

	override fun onItemClick(adapterView: AdapterView<*>?, view: View, i: Int, l: Long) {
		if (internalList.size > i) {
			val fitem = internalList[i]
			if (fitem.isDirectory) {
				if (File(fitem.location).canRead()) {
					val currLoc = File(fitem.location)
					binding!!.header.dname!!.text = currLoc.name
					setTitle()
					binding!!.header.dirPath!!.text = currLoc.absolutePath
					internalList.clear()
					if (currLoc.name != properties.root.name) {
						val parent = FileListItem()
						parent.filename = ctx.getString(R.string.label_parent_dir)
						parent.isDirectory = true
						parent.location = Objects.requireNonNull(currLoc
								.parentFile).absolutePath
						parent.time = currLoc.lastModified()
						internalList.add(parent)
					}
					internalList = Utility.prepareFileListEntries(internalList, currLoc, filter,
							properties.show_hidden_files)
					mFileListAdapter!!.notifyDataSetChanged()
				} else {
					Toast.makeText(ctx, R.string.error_dir_access,
							Toast.LENGTH_SHORT).show()
				}
			} else {
				val fmark: MaterialCheckbox = view.findViewById(R.id.file_mark)
				fmark.performClick()
			}
		}
	}

	fun getProperties(): DialogProperties {
		return properties
	}

	fun setProperties(properties: DialogProperties) {
		this.properties = properties
		filter = ExtensionFilter(properties)
	}

	fun setDialogSelectionListener(callbacks: DialogSelectionListener?) {
		this.callbacks = callbacks
	}

	override fun setTitle(titleStr: CharSequence?) {
		this.titleStr = titleStr.toString()
		setTitle()
	}

	fun setPositiveBtnName(positiveBtnNameStr: CharSequence?) {
		if (positiveBtnNameStr != null) {
			this.positiveBtnNameStr = positiveBtnNameStr.toString()
		} else {
			this.positiveBtnNameStr = null
		}
	}

	fun setNegativeBtnName(negativeBtnNameStr: CharSequence?) {
		if (negativeBtnNameStr != null) {
			this.negativeBtnNameStr = negativeBtnNameStr.toString()
		} else {
			this.negativeBtnNameStr = null
		}
	}

	fun markFiles(paths: List<String?>?) {
		if (paths != null && paths.isNotEmpty()) {
			if (properties.selection_mode == DialogConfigs.SINGLE_MODE) {
				val temp = File(paths[0])
				when (properties.selection_type) {
					DialogConfigs.DIR_SELECT -> if (temp.exists() && temp.isDirectory) {
						val item = FileListItem()
						item.filename = temp.name
						item.isDirectory = temp.isDirectory
						item.isMarked = true
						item.time = temp.lastModified()
						item.location = temp.absolutePath
						MarkedItemList.addSelectedItem(item)
					}
					DialogConfigs.FILE_SELECT -> if (temp.exists() && temp.isFile) {
						val item = FileListItem()
						item.filename = temp.name
						item.isDirectory = temp.isDirectory
						item.isMarked = true
						item.time = temp.lastModified()
						item.size = temp.length()
						item.location = temp.absolutePath
						MarkedItemList.addSelectedItem(item)
					}
					DialogConfigs.FILE_AND_DIR_SELECT -> if (temp.exists()) {
						val item = FileListItem()
						item.filename = temp.name
						item.isDirectory = temp.isDirectory
						item.isMarked = true
						item.time = temp.lastModified()
						item.location = temp.absolutePath
						MarkedItemList.addSelectedItem(item)
					}
				}
			} else {
				for (path in paths) {
					when (properties.selection_type) {
						DialogConfigs.DIR_SELECT -> {
							val temp = File(path)
							if (temp.exists() && temp.isDirectory) {
								val item = FileListItem()
								item.filename = temp.name
								item.isDirectory = temp.isDirectory
								item.isMarked = true
								item.time = temp.lastModified()
								item.location = temp.absolutePath
								MarkedItemList.addSelectedItem(item)
							}
						}
						DialogConfigs.FILE_SELECT -> {
							val temp = File(path)
							if (temp.exists() && temp.isFile) {
								val item = FileListItem()
								item.filename = temp.name
								item.isDirectory = temp.isDirectory
								item.isMarked = true
								item.time = temp.lastModified()
								item.size = temp.length()
								item.location = temp.absolutePath
								MarkedItemList.addSelectedItem(item)
							}
						}
						DialogConfigs.FILE_AND_DIR_SELECT -> {
							val temp = File(path)
							if (temp.exists() && (temp.isFile || temp.isDirectory)) {
								val item = FileListItem()
								item.filename = temp.name
								item.isDirectory = temp.isDirectory
								item.isMarked = true
								item.time = temp.lastModified()
								item.location = temp.absolutePath
								MarkedItemList.addSelectedItem(item)
							}
						}
					}
				}
			}
		}
	}

	override fun show() {
		if (!Utility.checkStorageAccessPermissions(ctx)) {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
				(ctx as Activity).requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), EXTERNAL_READ_PERMISSION_GRANT)
			}
		} else {
			super.show()
			positiveBtnNameStr = if (positiveBtnNameStr == null) ctx.resources.getString(R.string.choose_button_label) else positiveBtnNameStr
			binding!!.footer.select!!.text = positiveBtnNameStr
			val size = MarkedItemList.fileCount
			if (size == 0) {
				binding!!.footer.select!!.text = positiveBtnNameStr
			} else {
				val buttonLabel = "$positiveBtnNameStr ($size) "
				binding!!.footer.select!!.text = buttonLabel
			}
		}
	}

	override fun onBackPressed() {
		val currentDirName = binding!!.header.dname!!.text.toString()
		if (internalList.size > 0) {
			val fitem = internalList[0]
			val currLoc = File(fitem.location)
			if (currentDirName == properties.root.name ||
					!currLoc.canRead()) {
				super.onBackPressed()
			} else {
				binding!!.header.dname!!.text = currLoc.name
				binding!!.header.dirPath!!.text = currLoc.absolutePath
				internalList.clear()
				if (currLoc.name != properties.root.name) {
					val parent = FileListItem()
					parent.filename = ctx.getString(R.string.label_parent_dir)
					parent.isDirectory = true
					parent.location = Objects.requireNonNull(currLoc.parentFile)
							.absolutePath
					parent.time = currLoc.lastModified()
					internalList.add(parent)
				}
				internalList = Utility.prepareFileListEntries(internalList, currLoc, filter,
						properties.show_hidden_files)
				mFileListAdapter!!.notifyDataSetChanged()
			}
			setTitle()
		} else {
			super.onBackPressed()
		}
	}

	override fun dismiss() {
		MarkedItemList.clearSelectionList()
		internalList.clear()
		super.dismiss()
	}

	companion object {
		const val EXTERNAL_READ_PERMISSION_GRANT = 112
	}
}