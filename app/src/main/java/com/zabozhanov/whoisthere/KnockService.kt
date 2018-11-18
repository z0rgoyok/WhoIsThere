package com.zabozhanov.whoisthere

import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.SensorManager
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Handler
import android.os.IBinder
import android.support.v4.app.NotificationCompat
import android.widget.Toast
import cafe.adriel.kbus.KBus
import com.google.android.gms.auth.api.signin.GoogleSignInResult
import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.UpdatesListener
import com.zabozhanov.whoisthere.data.*
import com.zabozhanov.whoisthere.network.TelegramSender
import com.zabozhanov.whoisthere.ui.activity.MainActivity
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import okhttp3.OkHttpClient


class KnockService : Service() {

    private var knockCountInPeriod = 0
    private var handler: Handler? = null
    private var sensorManager: SensorManager? = null
    private var player: MediaPlayer = MediaPlayer()
    private var isPaused = false
    private var telegramBot: TelegramBot
    private val processedMessages = mutableListOf<Int>()
    private var isMessageQueueCleared = false

    init {
        telegramBot = TelegramBot(SharedSettings.botToken)
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0,
                notificationIntent, 0)

        val notification = NotificationCompat.Builder(this)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(getString(R.string.running_notify))
                .setContentIntent(pendingIntent).build()
        startForeground(1999, notification)

        val okHttpClient = OkHttpClient.Builder()
                //.proxy(Proxy(Proxy.Type.HTTP, InetSocketAddress.createUnresolved("5.40.202.212", 53281)))
                //.addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY))
                .build()

        telegramBot = TelegramBot.Builder(SharedSettings.botToken)
                .okHttpClient(okHttpClient).build()


        telegramBot.setUpdatesListener(UpdatesListener { updates ->
            val requestPic = mutableListOf<Long>()
            val requestVideo5 = mutableListOf<Long>()

            if (!isMessageQueueCleared) {
                isMessageQueueCleared = true
                return@UpdatesListener UpdatesListener.CONFIRMED_UPDATES_ALL
            }

            updates?.forEach {
                if (processedMessages.contains(it.updateId())) {
                    return@forEach
                }
                processedMessages.add(it.updateId())
                it.message().let {
                    if (it.text().toLowerCase() == "/takepic") {
                        requestPic.add(it.chat().id())
                    }
                    if (updates[0].message().text().toLowerCase() == "/takevideo5") {
                        requestVideo5.add(it.chat().id())
                    }
                }
            }

            if (requestPic.size > 0) {
                takePic(requestPic[0])
            }

            /*requestPic.forEach {

                }
                requestVideo5.forEach {

                }*/
            requestPic.clear()
            requestVideo5.clear()

            UpdatesListener.CONFIRMED_UPDATES_ALL
        })

        KBus.subscribe<ServiceEvent>(this) {
            val event = it
            when (event.msg) {
                ServiceMsg.PAUSE_30 -> {
                    isPaused = true
                    Handler().postDelayed({
                        isPaused = false
                    }, 30000)
                }
                ServiceMsg.UPLOAD_VIDEO -> {
                    Observable.fromCallable {
                        TelegramSender.sendVideo(event.file, SharedSettings.channelName)
                    }.retry(10)
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe({

                            }, {

                            })
                }
                ServiceMsg.UPLOAD_PHOTO -> {
                    Observable.fromCallable {
                        event.chatId?.let {
                            TelegramSender.sendPic(event.file, it)
                        }
                    }.retry(10)
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe({

                            }, {

                            })
                }
            }
        }
    }


    private fun takePic(chatId: Long) {
        Handler(mainLooper).post {
            TasksQueue.addTask(Task(TaskType.TAKE_PHOTO, 10, chatId))
        }
    }


    private fun handleSignInResult(result: GoogleSignInResult) {
        if (result.isSuccess) {
            GoogleSignInData.account = result.signInAccount
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val descriptor = assets.openFd("message.mp3")
        player = MediaPlayer()
        player.setDataSource(descriptor.fileDescriptor, descriptor.startOffset, descriptor.length)
        descriptor.close()

        player.prepare()
        player.setVolume(1f, 1f)
        player.isLooping = false
        val am = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        am.setStreamVolume(
                AudioManager.STREAM_MUSIC,
                am.getStreamMaxVolume(AudioManager.STREAM_MUSIC),
                0)
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager?

        val spikeDetector = SpikeDetector(sensorManager)
        spikeDetector.start {
            if (isPaused) {
                return@start
            }
            if (handler == null) {
                handler = Handler()
                handler?.postDelayed({
                    handler = null
                    if (knockCountInPeriod >= SharedSettings.knocksInPeriod) {
                        knocked()
                    }
                }, SharedSettings.duration)
                knockCountInPeriod = 0
            }
            knockCountInPeriod++
        }


        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        KBus.unsubscribe(this)
    }


    private fun knocked() {
        Toast.makeText(MyApp.instance, "Knock!!!", Toast.LENGTH_SHORT).show()
        player.seekTo(0)
        player.start()
        TasksQueue.addTask(Task(TaskType.RECORD_VIDEO, 30, SharedSettings.channelName))
    }

    enum class ServiceMsg {
        PAUSE_30,
        UPLOAD_VIDEO,
        UPLOAD_PHOTO
    }

    data class ServiceEvent(val msg: ServiceMsg, val file: String = "", val chatId: Any? = null)

}
