package com.example.tracker.data.remote

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Fetches text over HTTP. Abstracted so remote data sources can be tested without a network. */
fun interface HttpClient {
    /** Returns the response body of a GET to [url]; throws [IOException] on failure or a non-2xx status. */
    suspend fun get(url: String): String
}

/** [HttpClient] on the platform's [HttpURLConnection], so the app needs no networking library. */
class UrlConnectionHttpClient(
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) : HttpClient {

    override suspend fun get(url: String): String = withContext(dispatcher) {
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = TIMEOUT_MILLIS
            connection.readTimeout = TIMEOUT_MILLIS
            connection.setRequestProperty("Accept", "application/json")
            val status = connection.responseCode
            if (status !in 200..299) throw IOException("GET $url failed with HTTP $status")
            connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }

    private companion object {
        const val TIMEOUT_MILLIS = 15_000
    }
}
