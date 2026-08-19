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

class RmClient(private var tokens: Tokens) {

    private val client: OkHttpClient
    private val remarkableApi: MyRemarkableApi
    private val documentStorage: DocumentStorageApi

    val clientTokens: Tokens
        get() = tokens

    init {
        val logging = HttpLoggingInterceptor()
        logging.setLevel(HttpLoggingInterceptor.Level.BODY)

        client = OkHttpClient.Builder()
            .addInterceptor(FilteredLoggingInterceptor(logging))
            .build()

        remarkableApi = MyRemarkableApi.build(client)
        documentStorage = DocumentStorageApi.build(client)
    }

    suspend fun refreshUserToken() {
        if (tokens.user != null) return

        val userToken = remarkableApi.renewToken("Bearer ${tokens.device}")
        tokens = Tokens(tokens.device, userToken)
    }

    suspend fun uploadEpub(fileName: String, content: ByteArray) {
        refreshUserToken()

        if (tokens.is15Account()) {
            throw NotImplementedError(
                "Sync 1.5 upload isn't implemented. Use the Share upload method instead."
            )
        } else {
            uploadEpub10(fileName, content)
        }
    }

    // Using the sync 1.0 protocol
    private suspend fun uploadEpub10(fileName: String, content: ByteArray) {
        refreshUserToken()

        val (name, ext) = fileName.split(".")
        val documentId = UUID.randomUUID().toString()

        val archive: ByteArray = make_archive(documentId, ext, content)

        val requests = documentStorage.uploadRequest(
            "Bearer ${tokens.user}", listOf(
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
            "Bearer ${tokens.user}", listOf(
                UpdateMetadataRequest(documentId, "", name, "DocumentType", 1, modifiedTime)
            )
        )

    }

    suspend fun listDocuments(): List<Document> {
        refreshUserToken()
        return documentStorage.list("Bearer ${tokens.user}")
    }

}
