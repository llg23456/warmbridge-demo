package com.warmbridge.demo.data.remote

import okhttp3.MultipartBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

interface WarmBridgeApi {

    @GET("api/tags")
    suspend fun tags(): TagsResponse

    @GET("api/feed")
    suspend fun feed(
        @Query("tag") tag: String? = null,
        @Query("channel") channel: String? = null,
    ): FeedResponse

    @GET("api/items/{id}")
    suspend fun item(@Path("id") id: String): FeedItemDto

    @POST("api/explain")
    suspend fun explain(@Body body: ExplainRequest): ExplainResponse

    @POST("api/share")
    suspend fun share(@Body body: ShareRequest): ShareResponse

    @Multipart
    @POST("api/image/explain")
    suspend fun imageExplain(@Part file: MultipartBody.Part): ImageExplainResponse

    @POST("api/video/quickparse")
    suspend fun videoQuick(@Body body: VideoQuickRequest): VideoQuickResponse

    @POST("api/tts")
    suspend fun tts(@Body body: TtsRequest): TtsResponseDto
}
