package com.royalenfield.provisioning.core.network

import android.annotation.SuppressLint
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class GraphQLRequest(
    val query: String,
    val variables: JsonObject? = null
)

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class GraphQLResponse<T>(
    val data: T? = null,
    val errors: List<GraphQLError>? = null
)

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class GraphQLError(
    val message: String,
    val locations: List<JsonObject>? = null,
    val path: List<String>? = null
)

class GraphQLClient(
     val httpClient: HttpClient,
    val baseUrlProvider: () -> String
) {
    suspend inline fun <reified T> execute(request: GraphQLRequest): Result<T> {
        return try {
            val url = "${baseUrlProvider().trimEnd('/')}/graphql"
            val response = httpClient.post(url) {
                setBody(request)
            }.body<GraphQLResponse<T>>()

            if (!response.errors.isNullOrEmpty()) {
                val errorMsg = response.errors.joinToString("; ") { it.message }
                Result.failure(Exception("GraphQL Error: $errorMsg"))
            } else if (response.data != null) {
                Result.success(response.data)
            } else {
                Result.failure(Exception("Empty GraphQL response data"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
