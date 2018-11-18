package com.zabozhanov.whoisthere.data

import android.content.Intent
import android.os.Handler
import cafe.adriel.kbus.KBus
import com.zabozhanov.whoisthere.MyApp
import com.zabozhanov.whoisthere.network.TelegramSender
import com.zabozhanov.whoisthere.ui.activity.RecordActivity
import java.io.Serializable
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.concurrent.schedule

enum class TaskType {
    RECORD_VIDEO,
    TAKE_PHOTO
}

data class Task(val type: TaskType, val lengthInSec: Int, val chatId: Any) : Serializable

enum class RecordEventType {
    START_VIDEO,
    STOP_VIDEO,
    FINISH,
    TAKE_PHOTO
}

data class RecordEvent(val recordEventType: RecordEventType, val task: Task? = null)

data class RecordEventCallBackEvent(val filename: String?)

object TasksQueue {

    private val waitingPhotoTasks = mutableListOf<Task>()
    private val waitingVideoTasks = mutableListOf<Task>()

    init {
        KBus.subscribe<RecordEventCallBackEvent>(this) {
            when {
                it.filename == null -> {
                }
                it.filename.contains(".jpg") -> {
                    for (waitingPhotoTask in waitingPhotoTasks) {
                        TelegramSender.sendPic(it.filename, waitingPhotoTask.chatId)
                    }
                }
                it.filename.contains(".mp4") -> {
                    for (waitingVideoTask in waitingVideoTasks) {
                        TelegramSender.sendVideo(it.filename, waitingVideoTask.chatId)
                    }
                }
            }
            if (queue.size > 0) {
                queue.removeAt(0)
            }
            processTask()
        }
    }

    private val queue = mutableListOf<Task>()
    private var currentTask: Task? = null

    private var isVideoRecording = false

    fun addTask(task: Task) {
        queue.add(task)
        processTask()
    }

    private val PHOTO_TAKE_DURATION = TimeUnit.SECONDS.toMillis(5)

    private fun processTask() {
        if (queue.size == 0) {
            KBus.post(RecordEvent(RecordEventType.FINISH))
            return
        }

        if (currentTask != null) {
            return
        }

        val task = queue[0]
        currentTask = task

        var delay = 0L
        if (!isRecordActivityStarted()) {
            startRecordActivity()
            delay = 1500
        }

        val recordHandler = Handler()

        if (task.type == TaskType.RECORD_VIDEO) {
            isVideoRecording = true
        }

        Timer().schedule(delay) {
            when (task.type) {
                TaskType.RECORD_VIDEO -> {
                    recordHandler.removeCallbacksAndMessages(null)
                    recordHandler.postDelayed({
                        KBus.post(RecordEvent(RecordEventType.FINISH, task))
                    }, task.lengthInSec.toLong())
                    KBus.post(RecordEvent(RecordEventType.START_VIDEO, task))
                }
                TaskType.TAKE_PHOTO -> {
                    recordHandler.removeCallbacksAndMessages(null)
                    recordHandler.postDelayed({
                        KBus.post(RecordEvent(RecordEventType.FINISH, task))
                    }, PHOTO_TAKE_DURATION)
                    KBus.post(RecordEvent(RecordEventType.TAKE_PHOTO, task))
                }
            }
        }

        /*queue.removeAt(0)*/
    }

    private fun startRecordActivity() {
        MyApp.instance?.let {
            val intent = RecordActivity.getIntent(it)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            it.startActivity(intent)
        }
    }

    private fun isRecordActivityStarted(): Boolean {
        return RecordActivity.isStarted
    }
}