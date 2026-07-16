package com.metrolist.desktop.preferences

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.io.File
import java.util.prefs.Preferences

object DesktopPreferences {
    private val prefs = Preferences.userNodeForPackage(DesktopPreferences::class.java)
    private val prefsDir = File(System.getProperty("user.home"), ".metrolist")
    private val prefsFile = File(prefsDir, "settings.json")

    private val cachedValues = mutableMapOf<String, Any?>()
    private val flows = mutableMapOf<String, MutableStateFlow<Any?>>()

    init {
        prefsDir.mkdirs()
        loadFromFile()
    }

    private fun loadFromFile() {
        if (!prefsFile.exists()) return
        try {
            val text = prefsFile.readText()
            val json = com.metrolist.desktop.preferences.JsonParser.parse(text)
            if (json is Map<*, *>) {
                json.forEach { (k, v) ->
                    if (k is String) {
                        cachedValues[k] = v
                        when (v) {
                            is Boolean -> prefs.put(k, if (v) "true" else "false")
                            is Int -> prefs.putInt(k, v)
                            is Long -> prefs.putLong(k, v)
                            is Float -> prefs.putFloat(k, v)
                            is Double -> prefs.putDouble(k, v)
                            is String -> prefs.put(k, v)
                            else -> prefs.put(k, v.toString())
                        }
                    }
                }
            }
        } catch (_: Exception) {}
    }

    private fun saveToFile() {
        try {
            val entries = prefs.entries()
            val sb = StringBuilder("{\n")
            var first = true
            entries.forEach { entry ->
                if (!first) sb.append(",\n")
                first = false
                sb.append("  \"${entry.key}\": ")
                val value = entry.value
                if (value.matches(Regex("-?\\d+\\.?\\d*"))) {
                    if (value.contains(".")) sb.append(value)
                    else sb.append(value)
                } else if (value == "true" || value == "false") {
                    sb.append(value)
                } else {
                    sb.append("\"${value.replace("\\", "\\\\").replace("\"", "\\\"")}\"")
                }
            }
            sb.append("\n}")
            prefsFile.writeText(sb.toString())
        } catch (_: Exception) {}
    }

    private fun Preferences.entries(): Map<String, String> {
        val keys = keys()
        return keys.associateWith { get(it, "") }
    }

    // === Getters ===
    fun getString(key: String, default: String? = null): String? {
        cachedValues[key]?.let { return it as? String }
        return prefs.get(key, default).also { cachedValues[key] = it }
    }

    fun getInt(key: String, default: Int = 0): Int {
        cachedValues[key]?.let { return (it as? Number)?.toInt() ?: default }
        return prefs.getInt(key, default).also { cachedValues[key] = it }
    }

    fun getLong(key: String, default: Long = 0L): Long {
        cachedValues[key]?.let { return (it as? Number)?.toLong() ?: default }
        return prefs.getLong(key, default).also { cachedValues[key] = it }
    }

    fun getFloat(key: String, default: Float = 0f): Float {
        cachedValues[key]?.let { return (it as? Number)?.toFloat() ?: default }
        return prefs.getFloat(key, default).also { cachedValues[key] = it }
    }

    fun getBoolean(key: String, default: Boolean = false): Boolean {
        cachedValues[key]?.let { return (it as? Boolean) ?: default }
        return prefs.getBoolean(key, default).also { cachedValues[key] = it }
    }

    fun getDouble(key: String, default: Double = 0.0): Double {
        cachedValues[key]?.let { return (it as? Number)?.toDouble() ?: default }
        return prefs.getDouble(key, default).also { cachedValues[key] = it }
    }

    // === Setters ===
    fun putString(key: String, value: String?) {
        if (value == null) { remove(key); return }
        prefs.put(key, value)
        cachedValues[key] = value
        flowFor(key)?.tryEmit(value)
        saveToFile()
    }

    fun putInt(key: String, value: Int) {
        prefs.putInt(key, value)
        cachedValues[key] = value
        flowFor(key)?.tryEmit(value)
        saveToFile()
    }

    fun putLong(key: String, value: Long) {
        prefs.putLong(key, value)
        cachedValues[key] = value
        flowFor(key)?.tryEmit(value)
        saveToFile()
    }

    fun putFloat(key: String, value: Float) {
        prefs.putFloat(key, value)
        cachedValues[key] = value
        flowFor(key)?.tryEmit(value)
        saveToFile()
    }

    fun putBoolean(key: String, value: Boolean) {
        prefs.put(key, if (value) "true" else "false")
        cachedValues[key] = value
        flowFor(key)?.tryEmit(value)
        saveToFile()
    }

    fun putDouble(key: String, value: Double) {
        prefs.putDouble(key, value)
        cachedValues[key] = value
        flowFor(key)?.tryEmit(value)
        saveToFile()
    }

    fun remove(key: String) {
        prefs.remove(key)
        cachedValues.remove(key)
        flowFor(key)?.tryEmit(null)
        saveToFile()
    }

    // === Reactive Flows (DataStore-like API) ===
    fun observeString(key: String, default: String? = null): Flow<String?> {
        return flowFor(key) { getString(key, default) }.map { it as? String? ?: default }
    }

    fun observeBoolean(key: String, default: Boolean = false): Flow<Boolean> {
        return flowFor(key) { getBoolean(key, default) }.map { (it as? Boolean) ?: default }
    }

    fun observeInt(key: String, default: Int = 0): Flow<Int> {
        return flowFor(key) { getInt(key, default) }.map { (it as? Number)?.toInt() ?: default }
    }

    private fun flowFor(key: String, current: (() -> Any?)? = null): MutableStateFlow<Any?> {
        return flows.getOrPut(key) {
            MutableStateFlow(current?.invoke() ?: cachedValues[key])
        }
    }
}

/**
 * Minimal JSON parser — only handles the subset of JSON our settings file uses.
 * Does NOT replace a proper library — just avoids a dependency for key-value parsing.
 * ponytail: replace with kotlinx.serialization when integrating with the rest of the app.
 */
private object JsonParser {
    fun parse(text: String): Any? {
        val trimmed = text.trim()
        return when {
            trimmed.startsWith("{") -> parseObject(trimmed)
            trimmed.startsWith("[") -> parseArray(trimmed)
            trimmed.startsWith("\"") -> parseString(trimmed)
            trimmed == "true" -> true
            trimmed == "false" -> false
            trimmed == "null" -> null
            else -> trimmed.toDoubleOrNull() ?: trimmed
        }
    }

    private fun parseObject(text: String): Map<String, Any?> {
        val map = mutableMapOf<String, Any?>()
        val content = text.substring(1, text.lastIndexOf('}')).trim()
        if (content.isEmpty()) return map

        val pairs = splitTopLevel(content, ',')
        for (pair in pairs) {
            val colonIdx = pair.indexOf(':')
            if (colonIdx < 0) continue
            val key = parseString(pair.substring(0, colonIdx).trim()) as? String ?: continue
            val value = parse(pair.substring(colonIdx + 1).trim())
            map[key] = value
        }
        return map
    }

    private fun parseArray(text: String): List<Any?> {
        val content = text.substring(1, text.lastIndexOf(']')).trim()
        if (content.isEmpty()) return emptyList()
        return splitTopLevel(content, ',').map { parse(it.trim()) }
    }

    private fun parseString(text: String): String {
        val s = text.trim()
        if (s.length < 2) return s
        return s.substring(1, s.length - 1)
            .replace("\\\"", "\"").replace("\\\\", "\\").replace("\\n", "\n")
    }

    private fun splitTopLevel(text: String, separator: Char): List<String> {
        val parts = mutableListOf<String>()
        var depth = 0
        var start = 0
        for (i in text.indices) {
            when (text[i]) {
                '{', '[' -> depth++
                '}', ']' -> depth--
                separator -> if (depth == 0) {
                    parts.add(text.substring(start, i))
                    start = i + 1
                }
            }
        }
        if (start < text.length) parts.add(text.substring(start))
        return parts
    }
}
