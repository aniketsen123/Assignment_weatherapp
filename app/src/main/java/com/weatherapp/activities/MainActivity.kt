package com.weatherapp.activities

import android.Manifest
import android.annotation.SuppressLint
import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.location.*
import com.google.gson.Gson
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import com.weatherapp.R
import com.weatherapp.models.WeatherResponse
import com.weatherapp.network.WeatherService
import com.weatherapp.utils.Constants
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.progress_dialog.*
import retrofit.*
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {


    private lateinit var mFusedLocationClient: FusedLocationProviderClient
    var progessDialog:Dialog?=null
    private lateinit var sharedPreferences:SharedPreferences
    @RequiresApi(Build.VERSION_CODES.N)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        sharedPreferences=getSharedPreferences(Constants.PREFERENCE_NAME,Context.MODE_PRIVATE)
        setupUI()
        if (!isLocationEnabled()) {
            Toast.makeText(
                this,
                "Your location provider is turned off. Please turn it on.",
                Toast.LENGTH_SHORT
            ).show()


            val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            startActivity(intent)
        } else {
            Dexter.withActivity(this)
                .withPermissions(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
                .withListener(object : MultiplePermissionsListener {
                    override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                        if (report!!.areAllPermissionsGranted()) {
                            requestLocationData()
                        }

                        if (report.isAnyPermissionPermanentlyDenied) {
                            Toast.makeText(
                                this@MainActivity,
                                "You have denied location permission. Please allow it is mandatory.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }

                    override fun onPermissionRationaleShouldBeShown(
                        permissions: MutableList<PermissionRequest>?,
                        token: PermissionToken?
                    ) {
                        showRationalDialogForPermissions()
                    }
                }).onSameThread()
                .check()
        }
    }


    private fun isLocationEnabled(): Boolean {
val locationManager: LocationManager =
    getSystemService(Context.LOCATION_SERVICE) as LocationManager
return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(
LocationManager.NETWORK_PROVIDER
)
}


private fun showRationalDialogForPermissions() {
    AlertDialog.Builder(this)
        .setMessage("It Looks like you have turned off permissions required for this feature. It can be enabled under Application Settings")
        .setPositiveButton(
            "GO TO SETTINGS"
        ) { _, _ ->
            try {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                val uri = Uri.fromParts("package", packageName, null)
                intent.data = uri
                startActivity(intent)
            } catch (e: ActivityNotFoundException) {
                e.printStackTrace()
            }
        }
        .setNegativeButton("Cancel") { dialog,
                                       _ ->
            dialog.dismiss()
        }.show()
}


@SuppressLint("MissingPermission")
private fun requestLocationData() {

    val mLocationRequest = LocationRequest()
    mLocationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY

    mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
    mFusedLocationClient.requestLocationUpdates(
        mLocationRequest, mLocationCallback,
        Looper.myLooper()
    )
}


private val mLocationCallback = object : LocationCallback() {
    override fun onLocationResult(locationResult: LocationResult) {
        val mLastLocation: Location = locationResult.lastLocation
        val latitude = mLastLocation.latitude
        Log.i("Current Latitude", "$latitude")

        val longitude = mLastLocation.longitude
        Log.i("Current Longitude", "$longitude")

        // TODO (STEP 6: Pass the latitude and longitude as parameters in function)
        getLocationWeatherDetails(latitude, longitude)
    }
}


private fun getLocationWeatherDetails(latitude: Double, longitude: Double) {

    if (Constants.isNetworkAvailable(this@MainActivity)) {


        val retrofit: Retrofit = Retrofit.Builder()

            .baseUrl(Constants.BASE_URL)

            .addConverterFactory(GsonConverterFactory.create())

            .build()

        val service: WeatherService =
            retrofit.create<WeatherService>(WeatherService::class.java)

        val listCall: Call<WeatherResponse> = service.getWeather(
            latitude, longitude, Constants.METRIC_UNIT, Constants.APP_ID
        )
        showProfressDialog()

        listCall.enqueue(object : Callback<WeatherResponse> {
            @RequiresApi(Build.VERSION_CODES.N)
            @SuppressLint("SetTextI18n")
            override fun onResponse(
                response: Response<WeatherResponse>,
                retrofit: Retrofit
            ) {


                if (response.isSuccess) {
                    hideProgressDialog()

                    val weatherList: WeatherResponse = response.body()
                    val weatherResponseJsonString=Gson().toJson(weatherList)
                    val editor=sharedPreferences.edit()
                    editor.putString(Constants.WEATHER_RESPONSE,weatherResponseJsonString)
                    editor.apply()
                    //setupUI()
                    Log.i("Response Result", "$weatherList")
                } else {

                    val sc = response.code()
                    when (sc) {
                        400 -> {
                            Log.e("Error 400", "Bad Request")
                        }
                        404 -> {
                            Log.e("Error 404", "Not Found")
                        }
                        else -> {
                            Log.e("Error", "Generic Error")
                        }
                    }
                }
            }

            override fun onFailure(t: Throwable) {
                Log.e("Errorrrrr", t.message.toString())
                hideProgressDialog()
            }
        })


    } else {
        Toast.makeText(
            this@MainActivity,
            "No internet connection available.",
            Toast.LENGTH_SHORT
        ).show()
    }
}


override fun onCreateOptionsMenu(menu: Menu?): Boolean {
    menuInflater.inflate(R.menu.menu_main,menu)
    return super.onCreateOptionsMenu(menu)
}

override fun onOptionsItemSelected(item: MenuItem): Boolean {
    when(item.itemId)
    {
        R.id.iv_refresh->requestLocationData()
    }
    return super.onOptionsItemSelected(item)
}

private fun showProfressDialog()
{
    progessDialog= Dialog(this)
    progessDialog!!.setContentView(R.layout.progress_dialog)
    progessDialog!!.show()

}
private fun hideProgressDialog()
{
    if(progessDialog!=null)
        progessDialog!!.dismiss()
}
@RequiresApi(Build.VERSION_CODES.N)
private fun setupUI() {
    val weatherJsonString = sharedPreferences.getString(Constants.WEATHER_RESPONSE, "")
    if (!weatherJsonString.isNullOrEmpty()) {
        val weatherList=Gson().fromJson(weatherJsonString,WeatherResponse::class.java)
        for (i in weatherList.weather.indices) {
            tv_main.text = weatherList.weather[i].main
            tv_main_description.text = weatherList.weather[i].description
            tv_temp.text =
                weatherList.main.temp.toString() + getUnit(application.resources.configuration.locales.toString())
            tv_sunrise_time.text = timeit(weatherList.sys.sunrise)
            tv_sunset_time.text = timeit(weatherList.sys.sunset)
            tv_humidity.text = weatherList.main.humidity.toString() + " %"
            tv_max.text = weatherList.main.temp_max.toString()
            tv_min.text = weatherList.main.temp_min.toString()
            tv_name.text = weatherList.name
            tv_speed.text = weatherList.wind.speed.toString()
            tv_country.text = weatherList.sys.country
            when (weatherList.weather[i].icon) {
                "01d" -> iv_main.setImageResource(R.drawable.sunny)
                "02d" -> iv_main.setImageResource(R.drawable.cloud)
                "03d" -> iv_main.setImageResource(R.drawable.cloud)
                "04d" -> iv_main.setImageResource(R.drawable.cloud)
                "04n" -> iv_main.setImageResource(R.drawable.cloud)
                "10d" -> iv_main.setImageResource(R.drawable.rain)
                "11d" -> iv_main.setImageResource(R.drawable.storm)
                "13d" -> iv_main.setImageResource(R.drawable.snowflake)
                "01n" -> iv_main.setImageResource(R.drawable.cloud)
                "02n" -> iv_main.setImageResource(R.drawable.cloud)
                "03n" -> iv_main.setImageResource(R.drawable.cloud)
                "10n" -> iv_main.setImageResource(R.drawable.cloud)
                "11n" -> iv_main.setImageResource(R.drawable.rain)
                "13n" -> iv_main.setImageResource(R.drawable.snowflake)

            }
        }
    }
}

private fun getUnit(values: String): String? {
    var value="C"
    if(values=="US"|| values=="LR"||values=="MM" )
        value="F"
    return value
}
private fun timeit(timex:Long):String
{
    val date=Date(timex*1000L)
    var sdf=SimpleDateFormat("HH:mm:ss",Locale.UK)
    sdf.timeZone= TimeZone.getDefault()
    return sdf.format(date)

}
}