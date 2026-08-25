package com.metrolist.desktop.storage

import com.metrolist.desktop.sync.HikalistCloudinaryConfig
import java.io.File
import java.util.concurrent.TimeUnit
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject

data class CloudinaryUploadResult(
    val url: String,
    val publicId: String,
    val bytes: Long,
)

class CloudinaryImageUploader(
    private val cloudName: String = HikalistCloudinaryConfig.CLOUD_NAME,
    private val uploadPreset: String = HikalistCloudinaryConfig.UPLOAD_PRESET,
    private val maxBytes: Long = DEFAULT_MAX_BYTES,
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .build()

    fun upload(file: File, publicId: String): CloudinaryUploadResult {
        require(file.isFile) { "Choose a valid image file" }
        require(file.length() in 1..maxBytes) { "Image must be ${maxBytes / BYTES_PER_MB} MB or smaller" }
        val mediaType = mediaTypeFor(file)

        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("upload_preset", uploadPreset)
            .addFormDataPart("public_id", publicId)
            .addFormDataPart("file", file.name, file.asRequestBody(mediaType))
            .build()

        val request = Request.Builder()
            .url("https://api.cloudinary.com/v1_1/$cloudName/image/upload")
            .post(requestBody)
            .build()

        client.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw IllegalArgumentException(remoteMessage(body) ?: "Image upload failed (${response.code})")
            }
            val json = JSONObject(body)
            val secureUrl = json.optString("secure_url")
            require(secureUrl.isNotBlank()) { "The image service returned no URL" }
            return CloudinaryUploadResult(
                url = secureUrl,
                publicId = json.optString("public_id", publicId),
                bytes = json.optLong("bytes", file.length()),
            )
        }
    }

    private fun mediaTypeFor(file: File) = when (file.extension.lowercase()) {
        "jpg", "jpeg" -> "image/jpeg".toMediaType()
        "png" -> "image/png".toMediaType()
        "webp" -> "image/webp".toMediaType()
        else -> throw IllegalArgumentException("Image must be JPG, PNG, or WebP")
    }

    private fun remoteMessage(body: String): String? = runCatching {
        JSONObject(body).getJSONObject("error").getString("message")
    }.getOrNull()

    companion object {
        const val DEFAULT_MAX_BYTES = 10L * 1024L * 1024L
        private const val BYTES_PER_MB = 1024L * 1024L
    }
}
