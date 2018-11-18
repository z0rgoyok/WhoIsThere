package com.zabozhanov.whoisthere.network

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.googleapis.extensions.android.gms.auth.GooglePlayServicesAvailabilityIOException
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException
import com.google.api.client.googleapis.media.MediaHttpUploader
import com.google.api.client.googleapis.media.MediaHttpUploaderProgressListener
import com.google.api.client.http.HttpRequestInitializer
import com.google.api.client.http.InputStreamContent
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.client.util.ExponentialBackOff
import com.google.api.services.youtube.YouTube
import com.google.api.services.youtube.YouTubeScopes
import com.google.api.services.youtube.model.Video
import com.google.api.services.youtube.model.VideoSnippet
import com.google.api.services.youtube.model.VideoStatus
import com.zabozhanov.whoisthere.MyApp
import com.zabozhanov.whoisthere.R
import com.zabozhanov.whoisthere.data.GoogleSignInData
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.IOException


@SuppressLint("StaticFieldLeak")
object YouTubeUploadManager {
    private val PREF_ACCOUNT_NAME = "accountName"
    private val SCOPES = arrayOf(YouTubeScopes.YOUTUBE_READONLY, YouTubeScopes.YOUTUBE_UPLOAD)
    private val TAG: String = "YouTubeUploadManager"

    private var context: Context? = null

    init {
        context = MyApp.instance
    }


    public fun uploadVideo(file: File?) {
        Observable.fromCallable {
            uploadVideoReal(file)
        }.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    Log.d(TAG, "Video uploaded: $it")
                }, {
                    it.printStackTrace()
                })
    }

    public fun uploadVideoReal(file: File?): String? {
        val transport = AndroidHttp.newCompatibleTransport()
        val jsonFactory = JacksonFactory.getDefaultInstance()

        var credential = GoogleAccountCredential.usingOAuth2(
                MyApp.instance, SCOPES.asList())
                .setBackOff(ExponentialBackOff())
        credential.selectedAccount = GoogleSignInData.account?.account


        val initializer = HttpRequestInitializer { request ->
            credential?.initialize(request)
            request?.isLoggingEnabled = true
        }


        val youtubeBuilder = YouTube.Builder(transport, jsonFactory, initializer)
        youtubeBuilder.applicationName = context?.getString(R.string.app_name)
        val youtube = youtubeBuilder.build()

        val PRIVACY_STATUS = "unlisted" // or public,private
        val PARTS = "snippet,status,contentDetails"

        var videoId: String? = null
        try {
            val videoObjectDefiningMetadata = Video()
            videoObjectDefiningMetadata.status = VideoStatus().setPrivacyStatus(PRIVACY_STATUS)

            val snippet = VideoSnippet()
            snippet.title = "CALL YOUTUBE DATA API UNLISTED TEST " + System.currentTimeMillis()
            snippet.description = "MyDescription"
            snippet.tags = arrayOf("TaG1,TaG2").toList()
            videoObjectDefiningMetadata.snippet = snippet

            val videoInsert = youtube.videos().insert(
                    PARTS,
                    videoObjectDefiningMetadata,
                    getMediaContent(/*getFileFromUri(data)*/file))/*.setOauthToken(token);*/
            //                    .setKey(API_KEY);

            val uploader = videoInsert.mediaHttpUploader
            uploader.isDirectUploadEnabled = false

            val progressListener = MediaHttpUploaderProgressListener {
                Log.d(TAG, "progressChanged: " + it.uploadState)
                Log.d(TAG, it.uploadState.name)
                when (it.uploadState) {
                    MediaHttpUploader.UploadState.INITIATION_STARTED -> {
                    }
                    MediaHttpUploader.UploadState.INITIATION_COMPLETE -> {
                    }
                    MediaHttpUploader.UploadState.MEDIA_IN_PROGRESS -> {
                    }
                    MediaHttpUploader.UploadState.MEDIA_COMPLETE, MediaHttpUploader.UploadState.NOT_STARTED -> Log.d(TAG, "progressChanged: upload_not_started")
                    null -> {

                    }
                }
            }
            uploader.progressListener = progressListener
            Log.d(TAG, "Uploading..")
            val returnedVideo = videoInsert.execute()
            Log.d(TAG, "Video upload completed")
            videoId = returnedVideo.id
            Log.d(TAG, String.format("videoId = [%s]", videoId))
        } catch (availabilityException: GooglePlayServicesAvailabilityIOException) {
            Log.e(TAG, "GooglePlayServicesAvailabilityIOException", availabilityException)
        } catch (userRecoverableException: UserRecoverableAuthIOException) {
            /*Log.i(TAG, String.format("UserRecoverableAuthIOException: %s",
                    userRecoverableException.message))*/
            var intent = userRecoverableException.intent
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            MyApp.instance?.startActivity(intent)


        } catch (e: IOException) {
            Log.e(TAG, "IOException", e)
        }
        return videoId
    }

    private fun getFileFromUri(uri: Uri): File? {
        try {
            var filePath: String? = null;
            val proj = arrayOf(MediaStore.Video.VideoColumns.DATA)
            val cursor = context?.contentResolver?.query(uri, proj, null, null, null)
            if (cursor?.moveToFirst()!!) {
                val columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Video.VideoColumns.DATA)
                filePath = cursor.getString(columnIndex)
            }
            cursor.close()
            val file = File(filePath)
            cursor.close()
            return file
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    private fun getMediaContent(file: File?): InputStreamContent? {
        var mediaContent = InputStreamContent(
                "video/*",
                BufferedInputStream(FileInputStream(file)))
        mediaContent.length = file?.length()!!
        return mediaContent
    }


    /**
     * Checks whether the device currently has a network connection.
     *
     * @return true if the device has a network connection, false otherwise.
     */
    private fun isDeviceOnline(): Boolean {
        val connMgr = context?.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo = connMgr.activeNetworkInfo
        return networkInfo != null && networkInfo.isConnected
    }

    /**
     * Check that Google Play services APK is installed and up to date.
     *
     * @return true if Google Play Services is available and up to
     * date on this device; false otherwise.
     */
    private fun isGooglePlayServicesAvailable(): Boolean {
        val apiAvailability = GoogleApiAvailability.getInstance()
        val connectionStatusCode = apiAvailability.isGooglePlayServicesAvailable(context)
        return connectionStatusCode == ConnectionResult.SUCCESS
    }

    /**
     * Attempt to resolve a missing, out-of-date, invalid or disabled Google
     * Play Services installation via a user dialog, if possible.
     */
    private fun acquireGooglePlayServices() {
        val apiAvailability = GoogleApiAvailability.getInstance()
        val connectionStatusCode = apiAvailability.isGooglePlayServicesAvailable(context)
        if (apiAvailability.isUserResolvableError(connectionStatusCode)) {
            showGooglePlayServicesAvailabilityErrorDialog(connectionStatusCode)
        }
    }

    private fun showGooglePlayServicesAvailabilityErrorDialog(connectionStatusCode: Int) {

    }


}