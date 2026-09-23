package com.myhotspot.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Application entry point.
 *
 * Hilt is used purely for dependency injection so that the `platform`
 * wrappers (AndroidWifiManager, AndroidConnectivityManager, ...) can be
 * swapped for fakes in tests. It has no bearing on networking behavior.
 */
@HiltAndroidApp
class MyHotspotApplication : Application()
