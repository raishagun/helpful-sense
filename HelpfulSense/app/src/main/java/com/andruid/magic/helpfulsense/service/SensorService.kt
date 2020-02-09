package com.andruid.magic.helpfulsense.service

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.os.Bundle
import android.os.IBinder
import android.os.Looper
import androidx.annotation.RequiresPermission
import androidx.core.content.getSystemService
import androidx.preference.PreferenceManager
import com.andruid.magic.helpfulsense.R
import com.andruid.magic.helpfulsense.data.ACTION_LOC_SMS
import com.andruid.magic.helpfulsense.data.ACTION_SMS_SENT
import com.andruid.magic.helpfulsense.data.ACTION_STOP_SERVICE
import com.andruid.magic.helpfulsense.data.KEY_MESSAGE
import com.andruid.magic.helpfulsense.util.*
import com.github.nisrulz.sensey.Sensey
import com.github.nisrulz.sensey.ShakeDetector.ShakeListener
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import splitties.toast.toast
import timber.log.Timber
import kotlin.coroutines.CoroutineContext

class SensorService : Service(), CoroutineScope, ConnectionCallbacks, ShakeListener, OnConnectionFailedListener,
        OnSharedPreferenceChangeListener {
    companion object {
        private const val NOTI_ID = 1
    }

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job
    private val job = Job()

    private val locationCallback = MyLocationCallback()
    private val googleApiClient by lazy {
        GoogleApiClient.Builder(applicationContext)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build()
    }

    private lateinit var locationRequest: LocationRequest

    private var apiConnected = false
    private var message = ""
    private var startIntent: Intent? = null

    override fun onCreate() {
        super.onCreate()
        job.start()
        init()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            if (!apiConnected) {
                startIntent = intent
                init()
            } else
                handleIntent(intent)
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()

        PreferenceManager.getDefaultSharedPreferences(this)
                .unregisterOnSharedPreferenceChangeListener(this)
        Sensey.getInstance().stopShakeDetection(this)
        Sensey.getInstance().stop()
        googleApiClient.disconnect()
        LocationServices.getFusedLocationProviderClient(applicationContext)
                .removeLocationUpdates(locationCallback)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onConnected(bundle: Bundle?) {
        apiConnected = true
        locationRequest = LocationRequest()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(1000)
                .setFastestInterval(1000)
                .setNumUpdates(1)
        val notificationManager = getSystemService<NotificationManager>()!!
        notificationManager.notify(NOTI_ID, buildNotification().build())
        startIntent?.let {
            handleIntent(it)
            startIntent = null
        }
    }

    override fun onConnectionSuspended(i: Int) {}

    override fun onConnectionFailed(connectionResult: ConnectionResult) {
        toast(connectionResult.errorMessage ?: "Location services error")
    }

    override fun onShakeDetected() {}

    @SuppressLint("MissingPermission")
    override fun onShakeStopped() {
        Timber.d("onShakeStopped: ")
        message = getString(R.string.shake_msg)
        if (apiConnected && hasLocationPermissions())
            startLocationReq()
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, s: String) {
        if (s == getString(R.string.pref_threshold) || s == getString(R.string.pref_time_stop)) {
            Sensey.getInstance().stopShakeDetection(this)
            initShakeDetection()
        }
    }

    private fun init() {
        startForeground(NOTI_ID, buildProgressNotification().build())

        Sensey.getInstance().init(applicationContext, Sensey.SAMPLING_PERIOD_NORMAL)
        googleApiClient.connect()
        PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(this)
        initShakeDetection()
    }

    private fun initShakeDetection() {
        val threshold = getShakeThreshold()
        val timeStop = getShakeStopTime()
        Sensey.getInstance().startShakeDetection(threshold.toFloat(), timeStop.toLong(), this)
    }

    @SuppressLint("MissingPermission")
    private fun handleIntent(intent: Intent) {
        when (intent.action) {
            ACTION_LOC_SMS -> {
                message = intent.extras?.getString(KEY_MESSAGE) ?: ""
                if (apiConnected && hasLocationPermissions())
                    startLocationReq()
            }
            ACTION_STOP_SERVICE -> {
                stopForeground(true)
                stopSelf()
            }
            ACTION_SMS_SENT -> toast(R.string.sms_sent)
        }
    }

    @RequiresPermission(anyOf = [Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION])
    private fun startLocationReq() {
        LocationServices.getFusedLocationProviderClient(applicationContext)
                .requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper())
    }

    private inner class MyLocationCallback : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            super.onLocationResult(locationResult)
            val loc = locationResult.lastLocation
            Timber.d("loc obtained")
            sendSMS(loc, message)
        }
    }
}