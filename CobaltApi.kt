package com.example.data

import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Url
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

data class CobaltRequest(
    val url: String,
    val videoQuality: String = "1080",
    val audioFormat: String = "mp3",
    val isAudioOnly: Boolean = false,
    val downloadMode: String = "video"
)

data class CobaltResponse(
    val status: String, // success, stream, redirect, picker, error
    val url: String?,
    val text: String?,
    val picker: List<CobaltPickerItem>?
)

data class CobaltPickerItem(
    val url: String,
    val type: String?, // video, photo
    val thumb: String?
)

interface CobaltApiService {
    @Headers(
        "Accept: application/json",
        "Content-Type: application/json"
    )
    @POST
    suspend fun getStreamUrl(
        @Url endpointUrl: String,
        @Body request: CobaltRequest
    ): CobaltResponse
}

object RetrofitHelper {
    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    fun getApiService(): CobaltApiService {
        // We use a dummy base URL here since we'll override it dynamically with @Url in the interface
        return Retrofit.Builder()
            .baseUrl("https://api.cobalt.tools/") 
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(CobaltApiService::class.java)
    }
}
