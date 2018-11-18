package com.zabozhanov.whoisthere.data

import android.content.SharedPreferences
import com.zabozhanov.whoisthere.MyApp

object SharedSettings {
    private val DURATION = "DURATION"
    private val BOT_TOKEN = "BOT_TOKEN"
    private val CHANNEL_NAME = "CHANNEL_TO_SEND"
    private val KNOCK_COUNT = "KNOCK_COUNT"
    private var prefs: SharedPreferences? = null

    var duration: Long
        get() {
            return prefs?.getLong(DURATION, 2000)!!
        }
        set(value) {
            prefs?.edit()?.putLong(DURATION, value)?.apply()
        }


    var knocksInPeriod: Long
        get() {
            return prefs?.getLong(KNOCK_COUNT, 2)!!
        }
        set(value) {
            prefs?.edit()?.putLong(KNOCK_COUNT, value)?.apply()
        }


    var botToken: String
        get() = prefs?.getString(BOT_TOKEN, "")!!
        set(value) {
            prefs?.edit()?.putString(BOT_TOKEN, value)?.apply()
        }

    var channelName: String
        get() = prefs?.getString(CHANNEL_NAME, "")!!
        set(value) {
            prefs?.edit()?.putString(CHANNEL_NAME, value)?.apply()
        }


    init {
        prefs = MyApp.instance?.getSharedPreferences("com.zabozhanov.whoisthere.prefs", 0)
    }
}