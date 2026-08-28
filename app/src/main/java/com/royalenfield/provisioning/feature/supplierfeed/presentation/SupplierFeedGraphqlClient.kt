package com.royalenfield.provisioning.feature.supplierfeed.presentation

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

// Request Data Models
@Serializable
data class GraphQLRequest(
    val query: String,
    val variables: JsonObject? = null
)

// Main Client Configuration
object GraphQLClient {
    private const val ENDPOINT = "https://cbp-in-api.royalenfield.com/ffcomposite/supplier-feed"
    private const val DEFAULT_API_KEY = "09cda167-a36c-4324-a626-dc30dbfc2f37"

    val client = HttpClient(OkHttp) {
//        engine {
//            connectTimeout = 15_000
//            socketTimeout = 15_000
//        }

        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                prettyPrint = true
                isLenient = true
            })
        }

        install(Logging) {
            logger = object : Logger {
                override fun log(message: String) {
                    android.util.Log.d("GraphQLClient", message)
                }
            }
            level = LogLevel.BODY
        }

        defaultRequest {
            contentType(ContentType.Application.Json)
        }
    }

    /**
     * Executes a GraphQL query or mutation.
     *
     * @param query The GraphQL query string
     * @param variables Map/JsonObject of variables (optional)
     * @param apiKey Optional custom API key, defaults to static API_KEY
     * @return JsonObject representing the response body
     */
    suspend fun execute(
        query: String,
        variables: JsonObject? = null,
        apiKey: String? = null
    ): JsonObject {
        val payload = GraphQLRequest(query = query, variables = variables)
        val activeApiKey = apiKey ?: DEFAULT_API_KEY

        return try {
            val response = client.post(ENDPOINT) {
                if (activeApiKey.isNotBlank()) {
                    header("x-api-key", activeApiKey)
                }
                setBody(payload)
            }

            val statusCode = response.status.value
            val responseBody: JsonObject? = try {
                response.body<JsonObject>()
            } catch (e: Exception) {
                null
            }

            // Handle Non-200 HTTP responses
            if (!response.status.isSuccess()) {
                if (responseBody != null && responseBody.containsKey("errors")) {
                    val errMsgs = parseGraphQLErrors(responseBody)
                    throw Exception("HTTP $statusCode: $errMsgs")
                } else if (responseBody != null && responseBody.containsKey("message")) {
                    val msg = responseBody["message"]?.jsonPrimitive?.content ?: "Unknown error"
                    throw Exception("HTTP $statusCode: $msg")
                } else {
                    throw Exception("HTTP request failed with status code $statusCode")
                }
            }

            // Handle 200 OK responses that contain GraphQL execution errors
            if (responseBody != null && responseBody.containsKey("errors")) {
                val errMsgs = parseGraphQLErrors(responseBody)
                throw Exception("GraphQL errors: $errMsgs")
            }

            responseBody ?: throw Exception("Received empty response body")

        } catch (e: Exception) {
            throw Exception("GraphQL request failed: ${e.message}", e)
        }
    }

    private fun parseGraphQLErrors(responseJson: JsonObject): String {
        return responseJson["errors"]?.jsonArray?.joinToString("; ") { element ->
            val errorObj = element.jsonObject
            errorObj["message"]?.jsonPrimitive?.content ?: element.toString()
        } ?: "Unknown GraphQL Error"
    }
}