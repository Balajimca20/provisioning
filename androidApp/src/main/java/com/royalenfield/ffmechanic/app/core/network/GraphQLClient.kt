package com.royalenfield.ffmechanic.app.core.network

import com.royalenfield.ffmechanic.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class GraphQLError(val message: String? = null)

@Serializable
data class GraphQLResponse<T>(
    val data: T? = null,
    val errors: List<GraphQLError>? = null,
)

/** Thrown for both transport-level and GraphQL-level errors, mirroring graphql_client.py's single Exception type. */
class GraphQLException(message: String) : Exception(message)

/**
 * Port of graphql_client.py's GraphQLClient. Talks to the Supplier Feed GraphQL API.
 *
 * Endpoint and key now come from BuildConfig (set per dev/uat/prod flavor in
 * androidApp/build.gradle.kts from gradle.properties' URL_FF_* / API_KEY_* entries),
 * replacing the hardcoded ENDPOINT/API_KEY constants the desktop client used.
 *
 * TODO(SECURITY): the key is still embedded client-side per-flavor, matching the original
 * desktop tool's behavior. You asked to keep it embedded for now and flag it — before wider
 * rollout, move this behind a backend proxy that injects the key server-side so it can't be
 * pulled from the APK.
 */
@Singleton
class GraphQLClient @Inject constructor() {

    companion object {
        // Original hardcoded endpoint was "https://cbp-in-api.royalenfield.com/ffcomposite/supplier-feed",
        // which is URL_FF_PROD + "ffcomposite/supplier-feed". Reconstructed per-flavor here —
        // double check this path suffix is correct for dev/uat too, gradle.properties doesn't
        // confirm that beyond the prod value matching.
        val ENDPOINT: String = BuildConfig.FF_BASE_URL.trimEnd('/') + "/ffcomposite/supplier-feed"
        val API_KEY: String = BuildConfig.SUPPLIER_FEED_API_KEY
    }

    @PublishedApi
    internal val json = Json { ignoreUnknownKeys = true; explicitNulls = false }

    // Original Python client set verify=False (skips TLS cert validation) for a corporate
    // endpoint. Kept here for behavioral parity but this is a real weakening of transport
    // security — prefer fixing the corporate cert chain (add it to the device's trust store
    // via a network_security_config) over disabling verification app-wide.
    @PublishedApi
    internal val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    /**
     * Executes a GraphQL query/mutation. [T] is the shape of the `data` object you expect back,
     * e.g. `GetDeviceData(val getDevice: DeviceProfile?)`.
     */
    suspend inline fun <reified T> execute(
        query: String,
        variables: Map<String, JsonElement>? = null,
        apiKey: String? = null,
    ): T = withContext(Dispatchers.IO) {
        val payload = buildJsonPayload(query, variables)

        val requestBuilder = Request.Builder()
            .url(ENDPOINT)
            .post(payload.toRequestBody("application/json".toMediaType()))
            .addHeader("Content-Type", "application/json")

        val key = apiKey ?: API_KEY
        if (key.isNotEmpty()) requestBuilder.addHeader("x-api-key", key)

        val response = try {
            client.newCall(requestBuilder.build()).execute()
        } catch (e: Exception) {
            throw GraphQLException("GraphQL request failed: ${e.message}")
        }

        response.use { resp ->
            val bodyString = resp.body?.string().orEmpty()

            // Reified inline decode: works because this whole function is `inline fun <reified T>`,
            // so the compiler plugin can resolve the serializer for GraphQLResponse<T> too.
            val parsed: GraphQLResponse<T>? = try {
                json.decodeFromString<GraphQLResponse<T>>(bodyString)
            } catch (_: Exception) {
                null
            }

            if (!resp.isSuccessful) {
                val errMsg = parsed?.errors?.joinToString("; ") { it.message ?: it.toString() }
                throw GraphQLException("HTTP ${resp.code}: ${errMsg ?: resp.message}")
            }

            if (!parsed?.errors.isNullOrEmpty()) {
                throw GraphQLException("GraphQL errors: ${parsed!!.errors!!.joinToString("; ") { it.message ?: it.toString() }}")
            }

            parsed?.data ?: throw GraphQLException("GraphQL response had no data")
        }
    }

    fun buildJsonPayload(query: String, variables: Map<String, JsonElement>?): String {
        val map = buildMap<String, JsonElement> {
            put("query", kotlinx.serialization.json.JsonPrimitive(query))
            if (variables != null) put("variables", kotlinx.serialization.json.JsonObject(variables))
        }
        return json.encodeToString(
            kotlinx.serialization.json.JsonObject.serializer(),
            kotlinx.serialization.json.JsonObject(map)
        )
    }
}
