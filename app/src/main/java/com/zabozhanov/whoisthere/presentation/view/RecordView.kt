package com.zabozhanov.whoisthere.presentation.view

import com.arellomobile.mvp.MvpView

interface RecordView : MvpView {
    fun startRecord(lengthInSec: Int)
    fun stopRecord()
    fun takePic()
    fun closeSelf()
}
