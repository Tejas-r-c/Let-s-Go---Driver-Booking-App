package com.example.letsgo

import com.example.letsgo.NominatimPlace
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

interface NominatimService {
    // GET https://nominatim.openstreetmap.org/search?format=json&q=...&limit=6&addressdetails=1
    @GET("search")
    fun search(
        @Header("User-Agent") userAgent: String,
        @Query("format") format: String = "json",
        @Query("q") query: String,
        @Query("limit") limit: Int = 6,
        @Query("addressdetails") addressDetails: Int = 1,
        @Query("accept-language") acceptLanguage: String? = null
    ): Call<List<NominatimPlace>>
}
