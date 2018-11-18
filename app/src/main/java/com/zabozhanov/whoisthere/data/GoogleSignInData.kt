package com.zabozhanov.whoisthere.data

import android.accounts.Account
import android.annotation.SuppressLint
import android.content.Context
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.common.api.Scope
import com.google.api.services.youtube.YouTubeScopes
import com.zabozhanov.whoisthere.MyApp
import com.zabozhanov.whoisthere.R

@SuppressLint("StaticFieldLeak")
object GoogleSignInData {

    private var context: Context? = null
    private val YOUTUBE_SCOPE = "https://www.googleapis.com/auth/youtube"

    var account: GoogleSignInAccount? = null
    var googleSignInClient: GoogleSignInClient? = null
    var mGoogleApiClient: GoogleApiClient? = null

    init {
        context = MyApp.instance
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(context?.getString(R.string.default_web_client_id))
                .requestScopes(Scope(YOUTUBE_SCOPE), Scope(YouTubeScopes.YOUTUBE_UPLOAD))
                .requestEmail()
                .build()
        googleSignInClient = context?.let { GoogleSignIn.getClient(it, gso) }
        mGoogleApiClient = context?.let {
            GoogleApiClient.Builder(it)
                    //.enableAutoManage(this, this)
                    .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                    .build()
        }
    }
}