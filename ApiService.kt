package com.example.letsgo

import com.example.letsgo.models.*
import retrofit2.Call
import retrofit2.http.*

interface ApiService {

    /* =========================
       AUTH
    ========================== */
    @POST("registerUser")
    fun registerUser(@Body req: UserRegisterRequest): Call<ApiResponse>

    @POST("registerDriver")
    fun registerDriver(@Body req: DriverRegisterRequest): Call<ApiResponse>

    @POST("login")
    fun loginUser(@Body req: LoginRequest): Call<ApiResponse>

    /* =========================
       PLACES
    ========================== */
    @GET("searchPlaces")
    fun searchPlaces(@Query("q") query: String): Call<List<PlaceSuggestion>>

    /* =========================
       VEHICLES
    ========================== */
    @GET("vehicleOptions")
    fun getVehicleTypes(): Call<List<VehicleType>>

    /* =========================
       USER BOOKING
    ========================== */
    @POST("requestRide")
    fun requestRide(@Body req: RideRequest): Call<RideResponse>

    /* =========================
       DRIVER – RIDE FLOW
    ========================== */
    @GET("rides/pending")
    fun getPendingRides(
        @Query("driverId") driverId: String
    ): Call<List<RideItem>>

    @POST("rides/accept")
    fun acceptRide(
        @Body req: AcceptRideRequest
    ): Call<AcceptRideResponse>

    /* =========================
       DRIVER – LIVE TRACKING
    ========================== */
    @POST("rides/updateDriverLocation")
    fun updateDriverLocation(
        @Body req: DriverLocationUpdateRequest
    ): Call<BasicResponse>

    @POST("rides/updateStatus")
    fun updateRideStatus(
        @Body req: RideStatusUpdateRequest
    ): Call<BasicResponse>

    /* =========================
       USER – TRACKING & PAYMENT
    ========================== */
    @GET("rides/details")
    fun getRideDetails(
        @Query("rideId") rideId: String
    ): Call<RideDetailsResponse>

    @POST("rides/completePayment")
    fun completePayment(
        @Body req: RidePaymentRequest
    ): Call<RidePaymentResponse>

    @POST("rides/rate")
    fun rateRide(
        @Body req: RatingRequest
    ): Call<RatingResponse>

    /* =========================
       DRIVER STATUS
    ========================== */
    @POST("driver/online")
    fun driverOnline(
        @Body req: DriverOnlineRequest
    ): Call<BasicResponse>

    @POST("driver/offline")
    fun driverOffline(
        @Body req: DriverOfflineRequest
    ): Call<BasicResponse>
}
