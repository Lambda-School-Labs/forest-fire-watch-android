package com.example.wildfire_fixed_imports.viewmodel

import android.app.Activity
import android.graphics.Color
import android.view.View
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.example.wildfire_fixed_imports.ApplicationLevelProvider
import com.example.wildfire_fixed_imports.MainActivity
import com.example.wildfire_fixed_imports.model.AQIdata
import com.example.wildfire_fixed_imports.model.DSFires
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.style.layers.BackgroundLayer
import com.mapbox.mapboxsdk.style.layers.PropertyFactory
import kotlinx.coroutines.*
import timber.log.Timber


/*
*           master Controller
*   master controller is repsonsible for recieving instructions on what lens are to be drawn to the map and then gathering data from network controller
*  consoloidating and managing that data and then sending  instructions to the view controllers on what to draw
*
*  Master controller is the central joining point of the controllers, the viewmodel and the view
* this is perhaps a violation of MVVM as stated but it really seems to be the best choice from what we can recken for this app.
*
*
* */
class MasterController() {

    //set correct mapbox map and the view containing the mapbox map via dependency injection

    private val applicationLevelProvider = ApplicationLevelProvider.getApplicaationLevelProviderInstance()

    private val targetMap: MapboxMap by lazy {
        applicationLevelProvider.mapboxMap
    }
    private val mapboxView: View by lazy {
        applicationLevelProvider.mapboxView
    }

    //additional dependency injection
    private val currentActivity : Activity = applicationLevelProvider.currentActivity

    //create heatmapcontroller scoped to class
    private val heatMapController = applicationLevelProvider.heatMapController

    //grab the viewmodel
    private val mapViewModel = applicationLevelProvider.appMapViewModel

    //markercontroller ref
    private val markerController = applicationLevelProvider.markerController

    private val TAG = "MapController"

    var initialized=false

    //create live data
    private val _fireData = MutableLiveData<List<DSFires>>().apply {
        value= listOf<DSFires>()
    }
    val fireData: LiveData<List<DSFires>> = _fireData
    var fireObserver:Observer<List<DSFires>>
    private val _AQIData = MutableLiveData<List<AQIdata>>().apply {
        value= listOf<AQIdata>()
    }
    val AQIData: LiveData<List<AQIdata>> = _AQIData
    var AQIObserver:Observer<List<AQIdata>>

    fun traceResult() {

    }

    init {
        /* temp testing remove*/

       Timber.i("$TAG init")


        // Create the fire observer which updates the UI.
        fireObserver = Observer{ list ->
            // Update the UI, in this case, a TextView.
            Timber.i("$TAG init create fire observer")
            if (!initialized) {
                Timber.i("$TAG init fire list reached observer ${list.toString()}")
                removeAllFires()
                addAllFires(list)
            }
            else {
                Timber.i("$TAG new aqi list reached observer ${list.toString()}")
                addAllFires(list)
            }
        }
        AQIObserver = Observer{ list ->
            // Update the UI, in this case, a TextView.
            Timber.i("$TAG init create aqi observer")
            if (!initialized) {
                Timber.i("$TAG  init aqi list reached observer ${list.toString()}")

            }
            else {
                Timber.i("$TAG new aqi list reached observer ${list.toString()}")

            }

        }


        // Observe the LiveData, passing in this activity as the LifecycleOwner and the observer.
        fireData.observe(currentActivity as LifecycleOwner, fireObserver)
        AQIData.observe(currentActivity as LifecycleOwner, AQIObserver)


        // start the fire service immediately to start retrieving fires

        CoroutineScope(Dispatchers.IO).launch {
            mapViewModel.startFireRetrieval()
        }



        var iterator = 0
        val arrayOfStyles:ArrayList<String> = arrayListOf(
                Style.DARK,
                Style.MAPBOX_STREETS,
                Style.OUTDOORS,
                Style.LIGHT,
                Style.SATELLITE,
                Style.SATELLITE_STREETS,
                Style.TRAFFIC_DAY,
                Style.TRAFFIC_NIGHT)


        if (currentActivity is MainActivity) {
            currentActivity.setFabOnclick {
                Timber.i("$TAG iterator = ${iterator} \n size = ${arrayOfStyles.size}")
                    if(iterator>=arrayOfStyles.size-1) {
                        Timber.i("$TAG iterator>=arrayOfStyles.siz")
                        iterator=0
                    }
                else {
                        iterator++
                        Timber.i("$TAG iterator++ = $iterator")
                    }
                Timber.i("$TAG setting map style to ${arrayOfStyles[iterator]}")
                targetMap.setStyle(arrayOfStyles[iterator])
                   // heatMapController.initializeHeatMapExtended()
                }
                }


        }


    fun addAllFires(DSFires:List<DSFires>) {
        for (i in DSFires.indices) {
            val current = DSFires[i]
            Timber.i("$i and ${current.toString()}")

            CoroutineScope(Dispatchers.Main).launch {

                markerController.addMarker(current.latlng(), current.name, current.type)

            }

        }

    }


    fun handleFireData(fireList: List<DSFires>){

        Timber.i(fireList.toString())
        diffFireData(fireList)
    }

    fun diffFireData(fireList: List<DSFires>) {
        //TODO("implement quality diffing, for now we will just check the whole list and replace if needed")
        if (fireList !=_fireData.value) {
            _fireData.postValue(fireList)
            fireData.value
            Timber.i("firedata live data after diff ${fireData.value}")
            Timber.i("_firedata live data after diff ${fireData.value}")
        }
        _fireData.postValue(fireList)
        fireData.value
        Timber.i("firedata live data after diff ${fireData.value}")
        Timber.i("_firedata live data after diff ${fireData.value}")
    }


/*
    suspend fun popit(){

        TODO("this is an async example that requires you to later .await() ")
        val result = CoroutineScope(Dispatchers.IO).async {
            applicationLevelProvider.retrofitDSService.getDSFireLocations()

        }


            Timber.i(System.currentTimeMillis().toString())


        TODO("this is how you would get out of a coroutine scope and back to a regular nonthread pattern ")
        CoroutineScope(Dispatchers.IO).launch {
            Timber.i(System.currentTimeMillis().toString())
            Toast.makeText(applicationLevelProvider.applicationContext, result.await().toString(), Toast.LENGTH_SHORT).show()
            delay(2000)
       popit2()
        }
    }
    fun popit2() {
        Toast.makeText(applicationLevelProvider.applicationContext, "this was not suspended", Toast.LENGTH_SHORT).show()

    }*/

    fun removeAllFires() {
        targetMap.markers.removeAll(targetMap.markers)
    }

    fun addbackgroundtomap() {
        targetMap.getStyle {
            val backgroundLayer = BackgroundLayer("background-layer")
            backgroundLayer.setProperties(PropertyFactory.backgroundColor(Color.BLUE))

            // Add background layer to map
            it.addLayer(backgroundLayer)
        }
    }



    }

