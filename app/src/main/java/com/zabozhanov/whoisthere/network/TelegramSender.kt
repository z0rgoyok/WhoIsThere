package com.zabozhanov.whoisthere.network

import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.request.SendMessage
import com.pengrad.telegrambot.request.SendPhoto
import com.pengrad.telegrambot.request.SendVideo
import com.zabozhanov.whoisthere.data.SharedSettings
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.io.File
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Proxy

object TelegramSender {

    val TAG = "TelegramSender"

    private fun getBot(): TelegramBot {
        val okHttpClient = OkHttpClient.Builder()
                //.proxy(Proxy(Proxy.Type.HTTP, InetSocketAddress.createUnresolved("5.40.202.212", 53281)))
                .addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY))
                .build()

        return TelegramBot.Builder(SharedSettings.botToken)
                .okHttpClient(okHttpClient)
                //.debug()
                .build()
    }

    fun sendVideo(file: String, channel: Any) {
        val sendVideo = SendVideo(channel, File(file))
        val execute = getBot().execute(sendVideo)
        if (!execute.isOk) {
            throw IOException("UPLOAD_VIDEO FAILED: $file")
        }
    }

    fun sendPic(file: String, chatId: Any) {
        val sendPic = SendPhoto(chatId, File(file))
        val execute = getBot().execute(sendPic)
        if (!execute.isOk) {
            throw IOException("UPLOAD_VIDEO FAILED: $file")
        }
    }

    fun sendText(text: String, chatId: Any) {
        val execute = getBot().execute(SendMessage(chatId, text))
        if (!execute.isOk) {
            throw IOException("SEND TEXT FAILED")
        }
    }
}