package com.zabozhanov.whoisthere.presentation.presenter

import cafe.adriel.kbus.KBus
import com.arellomobile.mvp.InjectViewState
import com.arellomobile.mvp.MvpPresenter
import com.zabozhanov.whoisthere.data.RecordEvent
import com.zabozhanov.whoisthere.data.RecordEventType
import com.zabozhanov.whoisthere.presentation.view.RecordView

@InjectViewState
class RecordPresenter : MvpPresenter<RecordView>() {

    fun onResume() {
        KBus.subscribe<RecordEvent>(this) {
            when (it.recordEventType) {
                RecordEventType.FINISH -> {
                    viewState.closeSelf()
                }
                RecordEventType.TAKE_PHOTO -> {
                    viewState.takePic()
                }
                RecordEventType.START_VIDEO -> {
                    it.task?.lengthInSec?.let { it1 -> viewState.startRecord(it1) }
                }
                RecordEventType.STOP_VIDEO -> {
                    viewState.stopRecord()
                }
            }
        }
    }

    fun onPause() {
        KBus.unsubscribe(this)
    }
}
