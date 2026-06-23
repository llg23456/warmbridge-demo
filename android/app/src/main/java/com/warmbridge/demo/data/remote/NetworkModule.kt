package com.warmbridge.demo.data.remote

import android.content.Context
import com.warmbridge.demo.BuildConfig
import com.warmbridge.demo.data.local.ApiBaseUrlPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object NetworkModule {

    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BASIC
    }

    val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(180, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .addInterceptor(logging)
        .build()

    private lateinit var appContext: Context

    @Volatile
    private var cachedOverride: String? = null

    @Volatile
    private var prefsLoaded = false

    @Volatile
    private var apiInstance: WarmBridgeApi? = null

    fun init(context: Context) {
        appContext = context.applicationContext
        runBlocking {
            cachedOverride = ApiBaseUrlPreferences(appContext).getOverride()
            prefsLoaded = true
        }
    }

    fun baseUrl(): String = ApiBaseUrlPreferences.effectiveBaseUrl(
        override = if (prefsLoaded) cachedOverride else null,
        buildDefault = BuildConfig.API_BASE_URL,
    )

    val api: WarmBridgeApi
        get() = synchronized(this) {
            apiInstance ?: buildApi(baseUrl()).also { apiInstance = it }
        }

    suspend fun updateBaseUrl(url: String?) {
        ensureInit()
        val toPersist = when {
            url.isNullOrBlank() -> null
            else -> {
                val normalized = ApiBaseUrlPreferences.normalizeBaseUrl(url)
                val buildDefault = ApiBaseUrlPreferences.normalizeBaseUrl(BuildConfig.API_BASE_URL)
                if (normalized == buildDefault) null else normalized
            }
        }
        ApiBaseUrlPreferences(appContext).setOverride(toPersist)
        synchronized(this) {
            cachedOverride = toPersist
            apiInstance = null
        }
    }

    suspend fun testHealth(baseUrl: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val normalized = ApiBaseUrlPreferences.normalizeBaseUrl(baseUrl)
            require(ApiBaseUrlPreferences.isValidHttpUrl(normalized)) { "invalid url" }
            val client = OkHttpClient.Builder()
                .connectTimeout(8, TimeUnit.SECONDS)
                .readTimeout(8, TimeUnit.SECONDS)
                .build()
            val request = Request.Builder()
                .url("${normalized}health")
                .get()
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    error("HTTP ${response.code}")
                }
            }
        }
    }

    private fun buildApi(baseUrl: String): WarmBridgeApi {
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(WarmBridgeApi::class.java)
    }

    private fun ensureInit() {
        check(::appContext.isInitialized) { "NetworkModule.init() must be called from Application" }
    }
}
