package com.example.letsgo

import com.google.gson.annotations.SerializedName

data class NominatimPlace(
    @SerializedName("place_id") val placeId: Long,
    @SerializedName("display_name") val displayName: String,
    @SerializedName("lat") val lat: String,
    @SerializedName("lon") val lon: String,
    @SerializedName("type") val type: String?,
    @SerializedName("class") val clazz: String?,          // "place", "highway", etc
    @SerializedName("importance") val importance: Double? // optional
)
