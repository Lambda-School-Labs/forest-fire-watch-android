package com.example.wildfire_fixed_imports.viewmodel.network_controllers

import com.example.wildfire_fixed_imports.ApplicationLevelProvider
import com.example.wildfire_fixed_imports.com.example.wildfire_fixed_imports.RetrofitErrorHandler
import com.example.wildfire_fixed_imports.model.SafeWebBELocation
import com.example.wildfire_fixed_imports.model.SuccessFailWrapper
import com.example.wildfire_fixed_imports.model.WebBELocation
import com.example.wildfire_fixed_imports.model.WebBELocationSubmit
import retrofit2.HttpException
import timber.log.Timber
import java.io.IOException


/*
*
* This class handles CRUD functionality for the Location objects used by the web backend
* */



class UserLocationWebBEController () {

    private val applicationLevelProvider = ApplicationLevelProvider.getApplicaationLevelProviderInstance()

    private val retroImpl = applicationLevelProvider.retrofitWebService

    private var webBEUser = applicationLevelProvider.webUser

    private val TAG = "UserLocationWebBEController"
    /*

    @GET("/api/locations/")
    suspend fun getWebBELocations(@Header("Authorization") token: String): List<WebBELocation>

    @PUT( "/api/locations/{id}")
    suspend fun updateWebBELocation(@Header("Authorization") token: String,@Path("id") id:String, @Body user: SafeWebBELocation)

    @POST("/api/locations/")
    suspend fun postWebBELocation(@Header("Authorization") token: String,@Body address: String,radius:Int): WebBELocation

    @DELETE	("/api/locations/:id")
    suspend fun deleteWebBELocation(@Header("Authorization") token: String,@Path("id") id:String)
    * */

    suspend fun postWebBELocation(address: String, radius: Int): SuccessFailWrapper<WebBELocation> {
        if (webBEUser?.token != null) {
            try {
                Timber.i("$TAG try postWebBELocation triggered")
                val result = retroImpl.postWebBELocation(webBEUser?.token.toString(),
                        WebBELocationSubmit(address, radius))
                Timber.i("$TAG success\n location = $result \n ")
                return SuccessFailWrapper.Success("Success", result)
            } catch (throwable: Throwable) {
                Timber.i("$TAG catch triggered in postWebBELocation")

                return RetrofitErrorHandler(throwable)
            }
        }
        return SuccessFailWrapper.Fail("webbeuser token null, likely not logged in \n")
    }

    suspend fun updateWebBELocation(id: String, location: SafeWebBELocation): SuccessFailWrapper<String> {
        if (webBEUser?.token != null) {
            try {
                Timber.i("$TAG try postWebBELocation triggered")
                val result = retroImpl.updateWebBELocation(webBEUser?.token.toString(), id, location)

                Timber.i("$TAG success\n location = $result \n ")
                return SuccessFailWrapper.Success("Success", result.message)

            } catch (throwable: Throwable) {
                Timber.i("$TAG catch triggered in postWebBELocation")

                return RetrofitErrorHandler(throwable)
            }
        }
        return SuccessFailWrapper.Fail("webbeuser token null, likely not logged in \n")

    }

    suspend fun getWebBELocations(): SuccessFailWrapper<List<WebBELocation>> {
        if (webBEUser?.token != null) {
            try {
                Timber.i("$TAG try postWebBELocation triggered")
                val result = retroImpl.getWebBELocations(webBEUser?.token.toString())

                Timber.i("$TAG success\n location = $result \n ")
                return SuccessFailWrapper.Success("Success", result)

            } catch (throwable: Throwable) {
                Timber.i("$TAG catch triggered in postWebBELocation")

                return RetrofitErrorHandler(throwable)
            }

        }
        return SuccessFailWrapper.Fail("webbeuser token null, likely not logged in \n")
    }


    suspend fun deleteWebBELocation(id:String): SuccessFailWrapper<Unit>{

        if (webBEUser?.token != null) {
            try {
                Timber.i("$TAG try postWebBELocation triggered")
               retroImpl.deleteWebBELocation(webBEUser?.token.toString(),id)

                Timber.i("$TAG success\n location deleted \n ")
                return SuccessFailWrapper.Success("Success")

            } catch (throwable: Throwable) {
                Timber.i("$TAG catch triggered in postWebBELocation")

                return RetrofitErrorHandler(throwable)
            }

        }
        return SuccessFailWrapper.Fail("webbeuser token null, likely not logged in \n")

    }





}