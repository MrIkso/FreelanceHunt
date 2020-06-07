package tech.hombre.freelancehunt.framework.tasks

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import org.koin.core.KoinComponent
import org.koin.core.inject
import tech.hombre.data.local.LocalProperties
import tech.hombre.domain.model.onFailure
import tech.hombre.domain.model.onSuccess
import tech.hombre.domain.repository.ThreadsListRepository
import tech.hombre.freelancehunt.R
import tech.hombre.freelancehunt.common.MAX_LINES
import tech.hombre.freelancehunt.common.extensions.getEnding
import tech.hombre.freelancehunt.framework.notifications.AndroidNotificationService
import tech.hombre.freelancehunt.framework.notifications.SimpleNotification
import tech.hombre.freelancehunt.routing.ScreenType


class ThreadsWorker(
    val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams), KoinComponent {

    private val threadsRepository: ThreadsListRepository by inject()

    private val appPreferences: LocalProperties by inject()

    private val notificationService: AndroidNotificationService by inject()

    override suspend fun doWork(): Result {
        try {
            Log.e("ThreadsWorker", "doWork")
            if (appPreferences.getCurrentUserId() != -1) {
                threadsRepository.getThreadsList("threads")
                    .onSuccess {
                        val lastChecked = appPreferences.getLastMessageId()
                        val new =
                            it.data.filter { thread -> thread.attributes.is_unread && thread.id > lastChecked }
                        if (new.isNotEmpty()) {
                            appPreferences.setLastMessageId(new.first().id)
                            Log.e("ThreadsWorker", "Notify")
                            notificationService.notify(
                                SimpleNotification(
                                    String.format(
                                        context.getString(R.string.notify_new_message),
                                        new.size.getEnding(context, R.array.ending_messages)
                                    ),
                                    new.map { "<b>${it.attributes.participants.from.login}:</b> <i>${it.attributes.subject}</i>" }
                                        .chunked(MAX_LINES)[0],
                                    ScreenType.THREADS,
                                    new.size - MAX_LINES
                                )
                            )
                        }
                    }
                    .onFailure { Log.e("ThreadsWorker", it.throwable.message, it.throwable) }
            } else Log.e("FeedWorker", "Skip")
            return Result.success()
        } catch (e: Exception) {
            Log.e("ThreadsWorker", e.message, e)
            return Result.failure()
        }
    }

    companion object {
        const val WORK_NAME = "ThreadsWorker"
    }


}

