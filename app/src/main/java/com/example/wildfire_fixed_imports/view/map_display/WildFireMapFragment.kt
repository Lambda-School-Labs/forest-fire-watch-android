package com.example.wildfire_fixed_imports.view.map_display

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import com.example.wildfire_fixed_imports.ApplicationLevelProvider
import com.example.wildfire_fixed_imports.R
import com.example.wildfire_fixed_imports.util.*
import com.example.wildfire_fixed_imports.viewmodel.view_model_classes.MapViewModel
import com.google.android.material.snackbar.Snackbar
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.plugins.annotation.SymbolManager
import com.mapbox.mapboxsdk.style.layers.Layer
import com.mapbox.mapboxsdk.style.layers.Property.NONE
import com.mapbox.mapboxsdk.style.layers.Property.VISIBLE
import com.mapbox.mapboxsdk.style.layers.PropertyFactory.visibility
import com.mapbox.mapboxsdk.style.layers.TransitionOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber


class WildFireMapFragment : Fragment() {
    // get the correct instance of application level provider
    private val applicationLevelProvider: ApplicationLevelProvider = ApplicationLevelProvider.getApplicaationLevelProviderInstance()
    val TAG: String
        get() = "\nclass: $className -- file name: $fileName -- method: ${StackTraceInfo.invokingMethodName} \n"



    override fun onAttach(context: Context) {
        super.onAttach(context)
        applicationLevelProvider.bottomSheet.visibility = View.VISIBLE
    }

    private lateinit var mapViewModel: MapViewModel
    private lateinit var mapboxMap:MapboxMap
    private lateinit var mapView: MapView



    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {


     // Initialize Home View Model
        mapViewModel = ViewModelProviders.of(this, applicationLevelProvider.mapViewModelFactory).get(
                MapViewModel::class.java)


      Mapbox.getInstance(this.context!!,  getString(R.string.mapbox_access_token))


        val root = inflater.inflate(R.layout.fragment_home, container, false)



        mapView = root.findViewById(R.id.mapview_main)
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync { myMapboxMap ->
            //set the applicationLevelProvider properties to reflect the loaded map
            Timber.i("map loaded async ${System.currentTimeMillis()}")
            applicationLevelProvider.mapboxMap = myMapboxMap
            mapboxMap = myMapboxMap
            applicationLevelProvider.mapboxView = mapView



            initPermissions()
            if (applicationLevelProvider.coarseLocationPermission or
                    applicationLevelProvider.fineLocationPermission) {
                finishLoading()
                Timber.i("First finish loading")
                applicationLevelProvider.currentActivity.locationInit()
            } else {
                initPermissions()
            }
        }


        return root
    }

    fun finishLoading() {
        val style = Style.SATELLITE
        mapboxMap.setStyle(style) {


            it.transition = TransitionOptions(0, 0, false)

            it.resetIconsForNewStyle()

            applicationLevelProvider.currentActivity.enableLocationComponent(it)
            applicationLevelProvider.mapboxStyle = it


            val symbolManager = SymbolManager(applicationLevelProvider.mapboxView, applicationLevelProvider.mapboxMap, applicationLevelProvider.mapboxStyle)
            applicationLevelProvider.symbolManager = symbolManager
            mapViewModel.onMapLoaded()
            Timber.w("$TAG config")

            // start the fire service/immediately to start retrieving fires
            CoroutineScope(Dispatchers.IO).launch {
                mapViewModel.startFireRetrieval()
                mapViewModel.startAQIRetrieval()
            }

        }

        applicationLevelProvider.fireBSIcon.setOnClickListener {
            fireToggleButtonOnClick()
        }
        applicationLevelProvider.aqiCloudBSIcon.setOnClickListener {
            aqiToggleButtonOnClick()
        }

    }

    fun fireToggleButtonOnClick() {
        mapboxMap.getStyle { style ->
            val layer: Layer? = style.getLayer(FIRE_SYMBOL_LAYER)
            if (layer != null) {
                if (VISIBLE == layer.visibility.getValue()) {
                    layer.setProperties(visibility(NONE))
                    applicationLevelProvider.fireLayerVisibility = NONE
                } else {
                    layer.setProperties(visibility(VISIBLE))
                    applicationLevelProvider.fireLayerVisibility = VISIBLE
                }
            }
        }
        mapViewModel.toggleFireRetrieval()
        Timber.i("$TAG toggle fire")
    }

    var counter = 0
    fun aqiToggleButtonOnClick() {
        mapViewModel.toggleAQIRetrieval()
        mapboxMap.getStyle { style ->
            val layer: Layer? = style.getLayer("count")
            if (layer != null) {
                if (VISIBLE == layer.visibility.getValue()) {
                    layer.setProperties(visibility(NONE))
                    applicationLevelProvider.aqiLayerVisibility = NONE
                } else {
                    layer.setProperties(visibility(VISIBLE))
                    applicationLevelProvider.aqiLayerVisibility = VISIBLE
                }
            }
            val layer1: Layer? = style.getLayer("unclustered-aqi-points")
            if (layer1 != null) {
                if (VISIBLE == layer1.visibility.getValue()) {
                    layer1.setProperties(visibility(NONE))
                } else {
                    layer1.setProperties(visibility(VISIBLE))
                }
            }

            for (i in 0..2) {
                if (counter == i) {
                    val layer: Layer? = style.getLayer("cluster-$i")
                    if (layer != null) {
                        layer.setProperties(visibility(VISIBLE))
                    }
                } else {
                    val layer: Layer? = style.getLayer("cluster-$i")
                    if (layer != null) {
                        layer.setProperties(visibility(NONE))
                    }
                }
            }
            Toast.makeText(this.context, counter.toString(), Toast.LENGTH_SHORT).show()
            if (counter == 2) {
                counter = 0
            } else {
                counter++
            }
            /*for (i in 0..2) {
                val layer: Layer? = style.getLayer("cluster-$i")
                if (layer != null) {
                    if (VISIBLE == layer.visibility.getValue()) {
                        layer.setProperties(visibility(NONE))
                    } else {
                        layer.setProperties(visibility(VISIBLE))
                    }
                }
            }*/
        }
        Timber.i("$TAG toggle aqi")
    }


    fun initPermissions() {

        checkFineLocationPermission()
        checkInternetPermission()
        checkCoarseLocationPermission()
        Timber.i("init - initpermissions")

    }


    fun checkInternetPermission() {
        Timber.i("init - check internet")
        // Check if the INTERNET permission has been granted
        if (ContextCompat.checkSelfPermission(applicationLevelProvider.applicationContext, Manifest.permission.INTERNET) ==
                PackageManager.PERMISSION_GRANTED) {
            Timber.i("init - internet already available")
            // Permission is already available, set boolean in ApplicationLevelProvider
            applicationLevelProvider.internetPermission = true
            //pop snackbar to notify of permissions
            applicationLevelProvider.showSnackbar("Internet permission: ${applicationLevelProvider.internetPermission} \n " +
                    "Fine Location permission: ${applicationLevelProvider.fineLocationPermission}", Snackbar.LENGTH_SHORT)


        } else {
            // Permission is missing and must be requested.
            requestInternetPermission()
        }
    }

    fun requestInternetPermission() {
        Timber.i("init - request internet")
        // Permission has not been granted and must be requested.
        if (shouldShowRequestPermissionRationale(Manifest.permission.INTERNET)) {
            // Provide an additional rationale to the user if the permission was not granted
            // and the user would benefit from additional context for the use of the permission.
            // Display a SnackBar with a button to request the missing permission.
            applicationLevelProvider.showSnackbar(
                    "INTERNET acess is required for this app to function at all.",
                    Snackbar.LENGTH_INDEFINITE, "OK"
            ) {
                requestPermissions(
                        arrayOf(Manifest.permission.INTERNET),
                        MY_PERMISSIONS_REQUEST_INTERNET
                )
            }

        } else {
            applicationLevelProvider.showSnackbar("INTERNET not available", Snackbar.LENGTH_SHORT)

            // Request the permission. The result will be received in onRequestPermissionResult().
            requestPermissions(arrayOf(Manifest.permission.INTERNET),
                    MY_PERMISSIONS_REQUEST_INTERNET
            )
        }
    }

    fun checkFineLocationPermission() {
        Timber.i("init - check fine location")
        // Check if the Camera permission has been granted
        if (ContextCompat.checkSelfPermission(applicationLevelProvider.applicationContext, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED) {
            Timber.i("init -  fine location already granted")
            // Permission is already available, set boolean in ApplicationLevelProvider
            applicationLevelProvider.fineLocationPermission = true
            //pop snackbar to notify of permissions
            applicationLevelProvider.showSnackbar("Internet permission: ${applicationLevelProvider.internetPermission} \n " +
                    "Fine Location permission: ${applicationLevelProvider.fineLocationPermission}", Snackbar.LENGTH_SHORT)

        } else {
            // Permission is missing and must be requested.
            requestFineLocationPermission()
        }
    }

    fun checkCoarseLocationPermission() {
        Timber.i("init - check coarse location")
        // Check if the Camera permission has been granted
        if (ContextCompat.checkSelfPermission(applicationLevelProvider.applicationContext, Manifest.permission.ACCESS_COARSE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED) {
            Timber.i("init -  coarse location already granted")
            // Permission is already available, set boolean in ApplicationLevelProvider
            applicationLevelProvider.coarseLocationPermission = true
            //pop snackbar to notify of permissions
            applicationLevelProvider.showSnackbar("coarse permission: ${applicationLevelProvider.coarseLocationPermission} \n " +
                    "Fine Location permission: ${applicationLevelProvider.coarseLocationPermission}", Snackbar.LENGTH_SHORT)

        } else {
            // Permission is missing and must be requested.
            requestCoarseLocationPermission()
        }
    }

    fun requestCoarseLocationPermission() {
        Timber.i("init - request coarse location")
        // Permission has not been granted and must be requested.
        if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_COARSE_LOCATION)) {
            // Provide an additional rationale to the user if the permission was not granted
            // and the user would benefit from additional context for the use of the permission.
            // Display a SnackBar with a button to request the missing permission.
            applicationLevelProvider.showSnackbar(
                    "GPS location data is needed to provide accurate local results",
                    Snackbar.LENGTH_INDEFINITE, "OK"
            ) {
                requestPermissions(
                        arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION),
                        MY_PERMISSIONS_COARSE_LOCATION
                )
            }

        } else {
            applicationLevelProvider.showSnackbar("cOARSE Location not available", Snackbar.LENGTH_SHORT)

            // Request the permission. The result will be received in onRequestPermissionResult().
            requestPermissions(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION),
                    MY_PERMISSIONS_COARSE_LOCATION
            )
        }
    }

    fun requestFineLocationPermission() {
        Timber.i("init - request fine location")
        // Permission has not been granted and must be requested.
        if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)) {
            // Provide an additional rationale to the user if the permission was not granted
            // and the user would benefit from additional context for the use of the permission.
            // Display a SnackBar with a button to request the missing permission.
            applicationLevelProvider.showSnackbar(
                    "GPS location data is needed to provide accurate local results",
                    Snackbar.LENGTH_INDEFINITE, "OK"
            ) {
                requestPermissions(
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                        MY_PERMISSIONS_REQUEST_FINE_LOCATION
                )
            }

        } else {
            applicationLevelProvider.showSnackbar("Fine Location not available", Snackbar.LENGTH_SHORT)

            // Request the permission. The result will be received in onRequestPermissionResult().
            requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    MY_PERMISSIONS_REQUEST_FINE_LOCATION
            )
        }
    }


    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        Timber.i("on request == before while loop permission: ${permissions.toString()} requestcode: $requestCode grantresults: ${grantResults.toString()} ")
        when (requestCode) {
            MY_PERMISSIONS_REQUEST_FINE_LOCATION -> {
                Timber.i("on request == after while loop fine location")
                // If request is cancelled, the result arrays are empty.
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    applicationLevelProvider.fineLocationPermission = true
                    applicationLevelProvider.showSnackbar("Fine Location granted successfully", Snackbar.LENGTH_SHORT)


                } else {
                    applicationLevelProvider.fineLocationPermission = false
                    applicationLevelProvider.showSnackbar("Fine Location not granted", Snackbar.LENGTH_SHORT)
                }
                return
            }
            MY_PERMISSIONS_REQUEST_INTERNET -> {
                Timber.i("on request == after while loop internet")
                // If request is cancelled, the result arrays are empty.
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    applicationLevelProvider.internetPermission = true
                    applicationLevelProvider.showSnackbar("Internet granted successfully", Snackbar.LENGTH_SHORT)

                } else {
                    applicationLevelProvider.internetPermission = false
                    applicationLevelProvider.showSnackbar("Internet not granted", Snackbar.LENGTH_SHORT)
                    //
                    TODO("CAUSE APPLICATION TO EXIT HERE")
                }
                return
            }

            MY_PERMISSIONS_COARSE_LOCATION -> {
                Timber.i("on request == after while loop internet")
                // If request is cancelled, the result arrays are empty.
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    applicationLevelProvider.coarseLocationPermission = true
                    applicationLevelProvider.showSnackbar("Internet granted successfully", Snackbar.LENGTH_SHORT)
                } else {
                    applicationLevelProvider.coarseLocationPermission = false
                    applicationLevelProvider.showSnackbar("Internet not granted", Snackbar.LENGTH_SHORT)
                    //
                    TODO("CAUSE APPLICATION TO EXIT HERE")
                }
                return
            }
            // Add other 'when' lines to check for other
            // permissions this app might request.
            else -> {
                Timber.i("on request == after while loop else")
                // Ignore all other requests.
            }
        }
        applicationLevelProvider.currentActivity.locationInit()
        finishLoading()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        super.onViewCreated(view, savedInstanceState)



    }


/*    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnFabHomePress) {
            listener = context
        } else {
            throw ClassCastException(
                context.toString() + " must implement OnFabHomePress.")
        }
    }



    interface OnFabHomePress {
        fun onFabPRess()
    }*/



    //mapbox boilerplate for surviving config changes
    override fun onStart(): Unit {
        super.onStart()
        mapView.onStart()
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onStop() {
        super.onStop()
        mapView.onStop()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView.onSaveInstanceState(outState)
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView.onDestroy()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
    }
}