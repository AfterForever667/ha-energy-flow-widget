package dev.dph.energyflow.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

sealed class HaResult<out T> {
    data class Success<T>(val value: T) : HaResult<T>()
    data class Failure(val message: String, val cause: Throwable? = null) : HaResult<Nothing>()
}

/**
 * Thin wrapper around Home Assistant's REST API. One instance is cheap to create per call;
 * OkHttpClient itself is shared/cached because it owns a connection pool and thread pools.
 */
class HaClient(
    private val baseUrl: String,
    private val token: String,
) {
    private val json = Json { ignoreUnknownKeys = true }

    private fun normalizedBase(): String = baseUrl.trimEnd('/')

    private fun request(path: String): Request =
        Request.Builder()
            .url("${normalizedBase()}$path")
            .header("Authorization", "Bearer $token")
            .header("Content-Type", "application/json")
            .build()

    /** Confirms the URL + token pair actually authenticates against this HA instance. */
    suspend fun testConnection(): HaResult<Unit> = withContext(Dispatchers.IO) {
        try {
            http.newCall(request("/api/")).execute().use { response ->
                when {
                    response.isSuccessful -> HaResult.Success(Unit)
                    response.code == 401 -> HaResult.Failure("Unauthorized — check the access token.")
                    else -> HaResult.Failure("HA responded with HTTP ${response.code}.")
                }
            }
        } catch (e: IOException) {
            HaResult.Failure(networkErrorMessage(e), e)
        }
    }

    /** Fetches every entity state — used by the config screen to populate entity pickers. */
    suspend fun fetchAllStates(): HaResult<List<HaState>> = withContext(Dispatchers.IO) {
        try {
            http.newCall(request("/api/states")).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext HaResult.Failure("HA responded with HTTP ${response.code}.")
                }
                val body = response.body?.string().orEmpty()
                val states = json.decodeFromString<List<HaState>>(body)
                HaResult.Success(states)
            }
        } catch (e: IOException) {
            HaResult.Failure(networkErrorMessage(e), e)
        } catch (e: Exception) {
            HaResult.Failure("Couldn't parse Home Assistant's response.", e)
        }
    }

    /** Fetches only the entities the widget actually needs, one request per entity, in parallel. */
    suspend fun fetchStates(entityIds: Collection<String>): HaResult<Map<String, HaState>> =
        withContext(Dispatchers.IO) {
            try {
                val ids = entityIds.distinct().filter { it.isNotBlank() }
                val fetched = ids.map { id ->
                    async {
                        runCatching {
                            http.newCall(request("/api/states/$id")).execute().use { response ->
                                if (response.isSuccessful) {
                                    val body = response.body?.string().orEmpty()
                                    id to json.decodeFromString<HaState>(body)
                                } else {
                                    null
                                }
                            }
                        }.getOrNull()
                        // Missing/renamed entities (or a per-entity failure) are simply omitted;
                        // the widget shows "—" for them rather than failing the whole refresh.
                    }
                }.awaitAll()

                val result = LinkedHashMap<String, HaState>(ids.size)
                for (pair in fetched) {
                    if (pair != null) result[pair.first] = pair.second
                }
                HaResult.Success(result)
            } catch (e: IOException) {
                HaResult.Failure(networkErrorMessage(e), e)
            } catch (e: Exception) {
                HaResult.Failure("Couldn't parse Home Assistant's response.", e)
            }
        }

    private fun networkErrorMessage(e: IOException): String = when (e) {
        is java.net.UnknownHostException -> "Can't resolve host — check the HA URL."
        is java.net.SocketTimeoutException -> "Connection timed out."
        is javax.net.ssl.SSLException -> "TLS/SSL error — check the HA URL uses a valid HTTPS certificate."
        else -> e.message ?: "Network error reaching Home Assistant."
    }

    companion object {
        // Shared across all HaClient instances: avoids spinning up a new connection pool
        // and dispatcher thread pool on every widget refresh tick.
        private val http: OkHttpClient by lazy {
            val dispatcher = okhttp3.Dispatcher().apply {
                // Default maxRequestsPerHost (5) would serialize most of the benefit away since
                // every fetchStates() call hits the same HA host.
                maxRequestsPerHost = 32
                maxRequests = 32
            }
            OkHttpClient.Builder()
                .dispatcher(dispatcher)
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .build()
        }
    }
}
