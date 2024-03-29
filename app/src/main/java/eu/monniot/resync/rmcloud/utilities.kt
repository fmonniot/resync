package eu.monniot.resync.rmcloud

import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Interceptor
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

suspend fun Call.await(): Response {

    return suspendCancellableCoroutine { continuation ->

        continuation.invokeOnCancellation {
            cancel()
        }

        enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                continuation.resume(response)
            }

            override fun onFailure(call: Call, e: IOException) {
                // Don't bother with resuming the continuation if it is already cancelled.
                if (continuation.isCancelled) return
                continuation.resumeWithException(e)
            }
        })
    }
}

class FilteredLoggingInterceptor(private val logger: HttpLoggingInterceptor) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        if (chain.request().tag() == "no body logging") {
            logger.level = HttpLoggingInterceptor.Level.HEADERS
        }

        return logger.intercept(chain)
    }
}
