package com.mobdeve.s18.mco.utils

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONException
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class LocationSearchUtils {

    companion object {
        private const val TAG = "LocationSearchUtils"
        private const val NOMINATIM_BASE_URL = "https://nominatim.openstreetmap.org/search"
        private const val USER_AGENT = "PinJournal/1.0 (Android Location Search)"

        data class SearchResult(
            val name: String,
            val latitude: Double,
            val longitude: Double,
            val displayName: String
        )

        suspend fun searchLocation(query: String): List<SearchResult> = withContext(Dispatchers.IO) {
            if (query.isBlank()) {
                Log.d(TAG, "Empty query provided")
                return@withContext emptyList()
            }

            try {
                Log.d(TAG, "Searching for: $query")

                val encodedQuery = URLEncoder.encode(query.trim(), "UTF-8")

                // Enhanced URL with better parameters for Philippine locations
                val searchUrl = buildString {
                    append(NOMINATIM_BASE_URL)
                    append("?q=").append(encodedQuery)
                    append("&format=json")
                    append("&limit=10") // Increased limit for better results
                    append("&countrycodes=ph") // Focus on Philippines
                    append("&addressdetails=1") // Include detailed address info
                    append("&extratags=1") // Include extra tags
                    append("&namedetails=1") // Include name details
                }

                Log.d(TAG, "Search URL: $searchUrl")

                val connection = URL(searchUrl).openConnection() as HttpURLConnection
                connection.apply {
                    requestMethod = "GET"
                    setRequestProperty("User-Agent", USER_AGENT)
                    setRequestProperty("Accept", "application/json")
                    setRequestProperty("Accept-Language", "en-US,en;q=0.9")
                    connectTimeout = 10000 // 10 seconds
                    readTimeout = 15000 // 15 seconds
                }

                val responseCode = connection.responseCode
                Log.d(TAG, "HTTP Response Code: $responseCode")

                if (responseCode != HttpURLConnection.HTTP_OK) {
                    Log.e(TAG, "HTTP request failed with code: $responseCode")
                    return@withContext emptyList()
                }

                val response = connection.inputStream.bufferedReader().use { it.readText() }
                Log.d(TAG, "Response received, length: ${response.length}")

                if (response.isBlank()) {
                    Log.w(TAG, "Empty response received")
                    return@withContext emptyList()
                }

                val jsonArray = JSONArray(response)
                Log.d(TAG, "Found ${jsonArray.length()} results")

                val results = mutableListOf<SearchResult>()
                for (i in 0 until jsonArray.length()) {
                    try {
                        val item = jsonArray.getJSONObject(i)

                        val lat = item.optString("lat").toDoubleOrNull()
                        val lon = item.optString("lon").toDoubleOrNull()

                        if (lat == null || lon == null) {
                            Log.w(TAG, "Invalid coordinates at index $i")
                            continue
                        }

                        val displayName = item.optString("display_name", "Unknown Location")
                        val name = item.optString("name").ifEmpty {
                            // Try to extract name from display_name if name is empty
                            displayName.split(",").firstOrNull()?.trim() ?: "Unknown"
                        }

                        val searchResult = SearchResult(
                            name = name,
                            latitude = lat,
                            longitude = lon,
                            displayName = displayName
                        )

                        results.add(searchResult)
                        Log.d(TAG, "Added result: $name at ($lat, $lon)")

                    } catch (e: JSONException) {
                        Log.e(TAG, "Error parsing result at index $i", e)
                    } catch (e: NumberFormatException) {
                        Log.e(TAG, "Error parsing coordinates at index $i", e)
                    }
                }

                Log.d(TAG, "Returning ${results.size} results")
                results

            } catch (e: IOException) {
                Log.e(TAG, "Network error during search", e)
                emptyList()
            } catch (e: JSONException) {
                Log.e(TAG, "JSON parsing error", e)
                emptyList()
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error during search", e)
                emptyList()
            }
        }
    }
}
