package com.zabozhanov.whoisthere.ui.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.arellomobile.mvp.presenter.InjectPresenter
import com.google.android.gms.common.api.GoogleApiClient
import com.zabozhanov.whoisthere.R
import com.zabozhanov.whoisthere.data.SharedSettings
import com.zabozhanov.whoisthere.presentation.presenter.SettingsPresenter
import com.zabozhanov.whoisthere.presentation.view.SettingsView
import kotlinx.android.synthetic.main.activity_settings.*


class SettingsActivity : BaseActivity(), SettingsView{

    private val RC_SIGN_IN = 9001
    private val RC_RECOVERABLE = 9002

    companion object {
        const val TAG = "SettingsActivity"
        fun getIntent(context: Context): Intent = Intent(context, SettingsActivity::class.java)
    }

    @InjectPresenter
    lateinit var mSettingsPresenter: SettingsPresenter


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        edChannelName.setText(SharedSettings.channelName)
        edBotToken.setText(SharedSettings.botToken)
        btnSaveSettings.setOnClickListener {
            SharedSettings.botToken = edBotToken.text.toString()
            SharedSettings.channelName = edChannelName.text.toString()
        }
    }




/*
    override fun onStart() {
        super.onStart()
        val opr = Auth.GoogleSignInApi.silentSignIn(GoogleSignInData.mGoogleApiClient)
        if (opr.isDone) {
            val result = opr.get()
            handleSignInResult(result)
        }
    }

    private fun handleSignInResult(result: GoogleSignInResult) {
        if (result.isSuccess) {
            val account = result.signInAccount
            updateUI(account?.account)
            //getSubscriptions()
        } else {
            updateUI(null)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_SIGN_IN) {
            val result = Auth.GoogleSignInApi.getSignInResultFromIntent(data)
            handleSignInResult(result)
        }

        // Handling a user-recoverable auth exception
        if (requestCode == RC_RECOVERABLE) {
            if (resultCode == Activity.RESULT_OK) {
                //getSubscriptions()
            } else {
                Toast.makeText(this, "failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun signIn() {
        val signInIntent = Auth.GoogleSignInApi.getSignInIntent(GoogleSignInData.mGoogleApiClient)
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    private fun signOut() {
        Auth.GoogleSignInApi.signOut(GoogleSignInData.mGoogleApiClient).setResultCallback { updateUI(null) }
    }

    private fun hideProgressDialog() {

    }

    private fun updateUI(user: Account?) {
        if (user == null) {
            btnAccount.text = getString(R.string.signin)
            btnAccount.setOnClickListener {
                signIn()
            }
        } else {
            btnAccount.text = user.name
            btnAccount.setOnClickListener {
                signOut()
            }
        }
    }

    private fun showProgressDialog() {

    }

    override fun onConnectionFailed(p0: ConnectionResult) {

    }*/
}
