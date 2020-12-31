package eu.monniot.resync.rmcloud

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import java.time.Instant
import java.util.*
import java.util.concurrent.TimeUnit

// TODO Creating OkHttp client should be a common function
suspend fun exchangeCodeForDeviceToken(code: String): String {
    val logging = HttpLoggingInterceptor()
    logging.setLevel(HttpLoggingInterceptor.Level.BODY)

    val client = OkHttpClient.Builder()
        .readTimeout(15, TimeUnit.SECONDS) // This API is very often slow (10+ seconds)
        .addInterceptor(logging)
        .build()

    val remarkableApi = MyRemarkableApi.build(client)

    return remarkableApi.register(RegistrationPayload.fromCode(code))
}

class RmClient(tokens: Tokens) {

    private val deviceToken: String
    private var userToken: String?

    private val client: OkHttpClient
    private val remarkableApi: MyRemarkableApi
    private val documentStorage: DocumentStorageApi

    init {
        val logging = HttpLoggingInterceptor()
        logging.setLevel(HttpLoggingInterceptor.Level.BASIC)

        client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()

        remarkableApi = MyRemarkableApi.build(client)
        documentStorage = DocumentStorageApi.build(client)

        deviceToken = tokens.device
        userToken = tokens.user
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

