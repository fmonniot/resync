package eu.monniot.resync.rmcloud

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import retrofit2.http.*
import java.util.*

interface MyRemarkableApi {

    @POST("/token/json/2/user/new")
    suspend fun renewToken(@Header("Authorization") authorization: String): String

    @POST("/token/json/2/device/new")
    suspend fun register(@Body body: RegistrationPayload): String

    companion object {

        fun build(client: OkHttpClient): MyRemarkableApi {
            val retrofit = Retrofit.Builder()
                .baseUrl("https://webapp-production-dot-remarkable-production.appspot.com")
                .addConverterFactory(ScalarsConverterFactory.create())
                .addConverterFactory(MoshiConverterFactory.create())
                .client(client)
                .build()

            return retrofit.create(MyRemarkableApi::class.java)
        }
    }
}

data class RegistrationPayload(val code: String, val deviceDesc: String, val deviceID: String) {
    companion object {
        fun fromCode(code: String) =
            RegistrationPayload(code, "mobile-android", UUID.randomUUID().toString())
    }
}

interface DocumentStorageApi {
    @GET("/document-storage/json/2/docs")
    suspend fun list(@Header("Authorization") authorization: String): List<Document>

    @PUT("/document-storage/json/2/upload/request")
    suspend fun uploadRequest(
        @Header("Authorization") authorization: String,
        @Body body: List<UploadRequestPayload>
    ): List<UploadRequestResponse>

    @PUT("/document-storage/json/2/upload/update-status")
    suspend fun updateMetadata(
        @Header("Authorization") authorization: String,
        @Body body: List<UpdateMetadataRequest>
    )

    companion object {
        fun build(client: OkHttpClient): DocumentStorageApi {
            val retrofit = Retrofit.Builder()
                .baseUrl("https://document-storage-production-dot-remarkable-production.appspot.com")
                .addConverterFactory(MoshiConverterFactory.create())
                .client(client)
                .build()

            return retrofit.create(DocumentStorageApi::class.java)
        }
    }
}

data class Document(
    val ID: String,
    val Version: Int,
    val Message: String,
    val Success: Boolean,
    val BlobURLGet: String,
    val BlobURLGetExpires: String,
    val ModifiedClient: String,
    val Type: String, // DocumentType or CollectionType
    val VissibleName: String,
    val CurrentPage: Int,
    val Bookmarked: Boolean,
    val Parent: String
)

data class UploadRequestPayload(val ID: String, val Type: String, val Version: Int)
data class UploadRequestResponse(
    val ID: String,
    val Version: Int,
    val Message: String,
    val Success: Boolean,
    val BlobURLPut: String,
    val BlobURLPutExpires: String
)

data class UpdateMetadataRequest(
    val ID: String,
    val Parent: String,
    val VissibleName: String,
    val Type: String,
    val Version: Int,
    val ModifiedClient: String
)
