package eu.monniot.resync.rmcloud

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import java.time.Instant
import java.util.*

class RmClient(private val deviceToken: String) {

    companion object {
        /**
         * Create a client based on an existing device token. This method will throw if it is
         * incapable of retrieving said token.
         *
         * @throws IllegalStateException when there is no device token stored in shared preferences
         */
        fun fromSharedPreferences(): RmClient {

            return RmClient("") // TODO: 12/22/20
        }
    }

    private var userToken: String? = null

    private val client: OkHttpClient
    private val remarkableApi: MyRemarkableApi
    private val documentStorage: DocumentStorageApi

    init {
        val logging = HttpLoggingInterceptor()
        logging.setLevel(HttpLoggingInterceptor.Level.BODY)

        client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()

        remarkableApi = MyRemarkableApi.build(client)
        documentStorage = DocumentStorageApi.build(client)
    }

    private suspend fun userToken(): String {
        if (userToken != null) {
            return userToken!!
        }

        userToken = remarkableApi.renewToken("Bearer $deviceToken")
        return userToken!!
    }

    suspend fun uploadEpub(fileName: String, content: ByteArray) {
        val (name, ext) = fileName.split(".")
        val documentId = UUID.randomUUID().toString()

        val archive: ByteArray = make_archive(documentId, ext, content)

        val requests = documentStorage.uploadRequest(
            "Bearer ${userToken()}", listOf(
                UploadRequestPayload(
                    documentId, "DocumentType", 1
                )
            )
        )
        val request = requests[0]

        val uploadResponse = client.newCall(
            Request.Builder()
                .url(request.BlobURLPut)
                .put(archive.toRequestBody())
                .build()
        ).await()
        uploadResponse.body?.close() // release buffer

        val modifiedTime = Instant.now().toString()
        documentStorage.updateMetadata(
            "Bearer ${userToken()}", listOf(
                UpdateMetadataRequest(documentId, "", name, "DocumentType", 1, modifiedTime)
            )
        )

    }
}

