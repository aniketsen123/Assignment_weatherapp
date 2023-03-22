package com.weatherapp.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build

object Constants {

    // TODO (STEP 2: Add the API key and Base URL and Metric unit here from openweathermap.)
    const val APP_ID: String = "96fb864d72f8827b250705957136b216"
    const val BASE_URL: String = "http://api.openweathermap.org/data/"
    const val METRIC_UNIT: String = "metric"
    const val PREFERENCE_NAME="WeatherAppPreference"
    const val WEATHER_RESPONSE="weather_response_data"

    /**
     * This function is used check the weather the device is connected to the Internet or not.
     */
    fun isNetworkAvailable(context: Context): Boolean {
        // It answers the queries about the state of network connectivity.
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network      = connectivityManager.activeNetwork ?: return false
            val activeNetWork = connectivityManager.getNetworkCapabilities(network) ?: return false
            return when {
                activeNetWork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
                activeNetWork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
                //for other device how are able to connect with Ethernet
                activeNetWork.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
                else -> false
            }
        } else {
            // Returns details about the currently active default data network.
            val networkInfo = connectivityManager.activeNetworkInfo
            return networkInfo != null && networkInfo.isConnectedOrConnecting
        }
    }
}