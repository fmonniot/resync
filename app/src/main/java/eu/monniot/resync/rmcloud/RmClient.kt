package eu.monniot.resync.rmcloud

import com.squareup.moshi.Moshi
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import java.lang.IllegalStateException
import java.security.MessageDigest
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
    private val blobApi: BlobApi

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
        blobApi = BlobApi.build(client)
    }

    suspend fun refreshUserToken() {
        if (tokens.user != null) return

        val userToken = remarkableApi.renewToken("Bearer ${tokens.device}")
        tokens = Tokens(tokens.device, userToken)
    }

    suspend fun uploadEpub(fileName: String, content: ByteArray) {
        refreshUserToken()

        if (tokens.is15Account()) {
            uploadEpub15(fileName, content)
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

    // Using the sync 1.5 protocol
    private suspend fun uploadEpub15(fileName: String, epubBytes: ByteArray) {
        val (name, ext) = fileName.split(".")
        val documentId = UUID.randomUUID().toString()

        // 	t := time.Now().UnixNano()
        //	tf := strconv.FormatInt(t, 10)
        val timestamp = System.nanoTime().toString()

        println("using timestamp $timestamp")

        val docFiles = mutableMapOf<String, ByteArray>()

        // our epub
        docFiles[fileName] = epubBytes

        // metadata file
        // ref for default: https://github.com/juruen/rmapi/blob/master/archive/zipdoc.go#L242
        docFiles["$documentId.metadata"] = """
            {
                "visibleName":"$name",
                "type":"DocumentType",
                "parent":"",
                "lastModified":"$timestamp",
                "lastOpened":"",
                "version":0,
                "pinned":false,
                "synced":true,
                "modified":false,
                "deleted":false,
                "metadatamodified":false
            }
        """.trimIndent().toByteArray()

        // page data
        // TODO Is this still required? rmapi doesn't seems to add it for epubs
        docFiles["$documentId.pagedata"] = ByteArray(0)

        // content file
        docFiles["$documentId.content"] = """
            {
              "dummyDocument": false,
              "extraMetadata": {
                "LastBrushColor": "",
                "LastBrushThicknessScale": "",
                "LastColor": "",
                "LastEraserThicknessScale": "",
                "LastEraserTool": "",
                "LastPen": "Finelinerv2",
                "LastPenColor": "",
                "LastPenThicknessScale": "",
                "LastPencil": "",
                "LastPencilColor": "",
                "LastPencilThicknessScale": "",
                "LastTool": "Finelinerv2",
                "ThicknessScale": "",
                "LastFinelinerv2Size": "1"
              },
              "fileType": "$ext",
              "fontName": "EB Garamond",
              "lastOpenedPage": 0,
              "lineHeight": 100,
              "margins": 50,
              "orientation": "portrait",
              "pageCount": 0,
              "pages": null,
              "textAlignment": "justify",
              "textScale": 1.2,
              "transform": {
                "m11": 1,
                "m12": 0,
                "m13": 0,
                "m21": 0,
                "m22": 1,
                "m23": 0,
                "m31": 0,
                "m32": 0,
                "m33": 1
              }
            }
        """.trimIndent().toByteArray()

        var blobDoc = BlobDoc(
            emptyList(),
            Entry("", "", documentId, 0)
        )

        // Upload individual files and build the container blob document
        docFiles.forEach { (fileName, fileContent) ->
            println("Uploading file $fileName")

            val hash = contentHash(fileContent)
            val entry = Entry(
                hash,
                "type",
                fileName,
                fileContent.size
            )

            uploadBlob(fileContent, hash)

            blobDoc = blobDoc.withEntry(entry)
        }

        println("Uploading new document index")
        // Build the document index file
        // https://github.com/juruen/rmapi/blob/master/api/sync15/blobdoc.go#L92
        val index = buildIndex(blobDoc.files)

        uploadBlob(index, blobDoc.entry.hash)

        // Everything is uploaded, let's sync the file tree (I think)
        val (rootIndex, rootIndexGeneration) = getRootIndex()

        val newRootIndex = rootIndex + blobDoc.entry
        val newRootIndexContent = buildIndex(newRootIndex)
        val newRootIndexHash = hashEntries(newRootIndex)

        uploadBlob(newRootIndexContent, newRootIndexHash)

        println("updating root, old gen: $rootIndexGeneration")

        /*
        newGeneration, err := b.WriteRootIndex(tree.Hash, tree.Generation)
		log.Info.Println("wrote root, new gen: ", newGeneration)
         */

        val newRootGeneration = writeRootIndex(newRootIndexHash, rootIndexGeneration)
        println("New root was updated with generation $newRootGeneration")
    }

    private fun contentHash(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
        return digest.fold("") { str, it -> str + "%02x".format(it) }
    }

    private suspend fun uploadBlob(content: ByteArray, hash: String) {
        refreshUserToken()

        val response = blobApi.signedUrlForUpload(
            "Bearer ${tokens.user}",
            BlobStorageRequest("PUT", hash)
        )

        val uploadResponse = client.newCall(
            Request.Builder()
                .url(response.url)
                .put(content.toRequestBody())
                .addHeader("User-Agent", "reSync")
                // Seems to be only needed when updating a blob
                //.addHeader("x-goog-if-generation-match", "generation/version (int)")
                .build()
        ).await()
        uploadResponse.body?.close() // release buffer

        if (uploadResponse.code == 412) {
            throw IllegalStateException("Wrong Generation Header")
        }

        if (uploadResponse.code != 200) {
            throw IllegalStateException("Upload failed. Response=$uploadResponse")
        }

        val newGen = uploadResponse.header("x-goog-generation")
        println("Uploaded new blob with generation no $newGen")
    }

    private suspend fun getRootIndex(): Pair<List<Entry>, Int> {

        val (_, hash) = downloadHash("root")
        println("Got latest index with hash=$hash")

        if (hash == null) {
            throw IllegalStateException("No hash associated with root. aborting.")
        }

        val (gen2, rawIndex) = downloadHash(hash)
        println("Index has gen=$gen2\ncontent=$rawIndex")

        if (rawIndex == null || gen2 == null) {
            throw IllegalStateException("No index content or generation. aborting.")
        }

        return parseIndex(rawIndex) to gen2

    }

    private suspend fun writeRootIndex(hash: String, generation: Int): Int {
        refreshUserToken()

        val storage = blobApi.signedUrlForUpload(
            "Bearer ${tokens.user}",
            BlobStorageRequest("PUT", "root", generation = generation.toString())
        )

        val response = client.newCall(
            Request.Builder()
                .url(storage.url)
                .put(hash.toRequestBody())
                .addHeader("User-Agent", "reSync")
                // Seems to be only needed when updating a blob
                //.addHeader("x-goog-if-generation-match", "generation/version (int)")
                .build()
        ).await()
        response.body?.close() // release buffer

        val gen = response.header("x-goog-generation")
        println("Got generation value from header: $gen")

        return gen?.toIntOrNull()
            ?: throw IllegalStateException("Generation isn't present or isn't a number")
    }

    private suspend fun downloadHash(hash: String): Pair<Int?, String?> {
        refreshUserToken()

        val storage = blobApi.signedUrlForDownload(
            "Bearer ${tokens.user}",
            BlobStorageRequest("GET", hash)
        )

        println("Got url for hash `$hash` => ${storage.url}")

        val response = client.newCall(
            Request.Builder().url(storage.url)
                .get().addHeader("User-Agent", "reSync")
                .tag("no body logging")
                .build()
        ).await()

        if (response.code == 404) {
            throw IllegalStateException("Hash not found")
        }

        if (response.code != 200) {
            throw IllegalStateException("Download failed. Response=$response")
        }

        val gen = response.header("x-goog-generation")
        println("Got generation value from header: $gen")

        val generation = gen?.toIntOrNull()
        val body = response.body?.string()

        return generation to body
    }

    suspend fun listDocuments(): List<Document> {
        refreshUserToken()
        return documentStorage.list("Bearer ${tokens.user}")
    }

}

data class BlobDoc(val files: List<Entry>, val entry: Entry) {
    fun withEntry(entry: Entry): BlobDoc =
        BlobDoc(files + entry, entry.copy(hash = hashEntries(files)))
}

fun hashEntries(files: List<Entry>): String {
    // https://github.com/juruen/rmapi/blob/master/api/sync15/common.go#L15
    val hashes = files.sortedBy { it.documentId }.map { fromHex(it.hash) }
    val md = MessageDigest.getInstance("SHA-256")

    hashes.forEach { md.update(it) }

    return toHex(md.digest())
}

fun fromHex(hex: String) =
    hex.chunked(2)
        .map { it.toInt(16).toByte() }
        .toByteArray()

fun toHex(bytes: ByteArray) =
    bytes.fold("") { str, it -> str + "%02x".format(it) }

fun extractScopeFromJwt(token: String): List<String> {
    val parts = token.split(".")
    val payload: ByteArray = Base64.getDecoder().decode(parts[1])
    val str = String(payload)


    // From my experience, fromJson does NOT return null but instead throw…
    // So don't think you are safe because you have handled a null young padawan…
    val scopes = Moshi.Builder()
        .build()
        .adapter(JwtPayload::class.java)
        .fromJson(str)
        ?.scopes

    return scopes?.split(" ") ?: emptyList()
}

fun parseIndex(index: String): List<Entry> {
    val sequence = index.lineSequence().iterator()

    val schema = sequence.next()
    if (schema != "3") {
        throw IllegalStateException("Wrong schema. `3` is supported, got $schema")
    }

    val entries = mutableListOf<Entry>()

    sequence.forEach { entries.add(parseEntry(it)) }

    return entries.toList()
}

fun parseEntry(line: String): Entry {
    return TODO()
}

fun buildIndex(entries: List<Entry>): ByteArray {
    val sb = java.lang.StringBuilder()
    sb.append("3\n")
    entries.forEach { sb.append(it.line).append("\n") }

    return sb.toString().toByteArray()
}


data class JwtPayload(val scopes: String)

data class Entry(
    val hash: String,
    val type: String,
    val documentId: String,
    val size: Int,
) {
    // Used somewhere. TODO When I know more, please change name or add comment saying why it's there
    val line = "$hash:0:$documentId:0:$size"
}