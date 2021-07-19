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

/*
TODO Inline in java class (and refine type if needed)
Field	Type	Description
ID	uuid	The unique ID of this object. UUID-4 format. This is always required!
Version	integer	The item's version counting starts at 1
Message	string	This is used in API replies to return error messages regarding this item
Success	boolean	This is used in API replies to signal that an error occurred handling this item (=false)
BlobURLGet	string	the download URL
BlobURLGetExpires	datetime	when above URL expires
BlobURLPut	string	Where the data for the file can be uploaded
BlobURLPutExpires	datetime	when above URL expires
ModifiedClient	datetime	The last modified date time as set on the client that created the item
Type	type	The type of the object. See below for available types
VissibleName	string	The file's label. Yes, there is a typo in this key.
CurrentPage	integer	The currently open page, starting at 0
Bookmarked	boolean	is this bookmarked?
Parent	uuid	The ID of the parent object or empty if this is a top level object

datetimes are specified in ISO 8601 format.
 */
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
