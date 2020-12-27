package eu.monniot.resync

import eu.monniot.resync.rmcloud.DocumentStorageApi
import eu.monniot.resync.rmcloud.MyRemarkableApi
import eu.monniot.resync.rmcloud.RmCloud
import kotlinx.coroutines.*
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.time.Instant


@ObsoleteCoroutinesApi
@ExperimentalCoroutinesApi
class RmCloudTest {

    private val mainThreadSurrogate = newSingleThreadContext("UI thread")

    @Before
    fun setUp() {
        Dispatchers.setMain(mainThreadSurrogate)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain() // reset main dispatcher to the original Main dispatcher
        mainThreadSurrogate.close()
    }

    @Test
    fun instant() {
        println(Instant.now().toString())
    }

    @Test
    fun runLive() = runBlocking {

        val logging = HttpLoggingInterceptor()
        logging.setLevel(HttpLoggingInterceptor.Level.BODY)

        val client: OkHttpClient = OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()

        val myRemarkableApi = MyRemarkableApi.build(client)

        // https://document-storage-production-dot-remarkable-production.appspot.com

        val token = myRemarkableApi.renewToken("Bearer ${RmCloud.DEVICE_TOKEN}")

        val documentStorageApi = DocumentStorageApi.build(client)
        println(documentStorageApi.list("Bearer $token"))

    }

}