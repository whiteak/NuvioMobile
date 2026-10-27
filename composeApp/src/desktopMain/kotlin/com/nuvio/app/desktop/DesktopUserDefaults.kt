package com.nuvio.app.desktop

import java.io.File
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.longOrNull

/**
 * Desktop stand-in for iOS NSUserDefaults, so the key-value storage code shared with iOS works unchanged.
 * Values live in one JSON file in the app's data folder (see [DesktopPaths]).
 */
class NSUserDefaults private constructor(private val file: File) {
    /** A separate named store, like iOS suites: <name>.json next to settings.json. */
    constructor(suiteName: String) : this(File(DesktopPaths.dataDir, "${suiteName.replace(Regex("[^A-Za-z0-9._-]"), "_")}.json"))

    private val lock = Any()
    private val values: MutableMap<String, Any> = load()

    companion object {
        val standardUserDefaults: NSUserDefaults by lazy { NSUserDefaults(File(DesktopPaths.dataDir, "settings.json")) }
        private val json = Json { prettyPrint = false }
    }

    fun objectForKey(key: String): Any? = synchronized(lock) { values[key] }
    fun stringForKey(key: String): String? = synchronized(lock) { values[key]?.let { it as? String ?: (it as? Number)?.toString() } }
    fun boolForKey(key: String): Boolean = synchronized(lock) {
        when (val v = values[key]) { is Boolean -> v; is Number -> v.toLong() != 0L; is String -> v == "true" || v == "1"; else -> false }
    }
    fun integerForKey(key: String): Long = synchronized(lock) {
        when (val v = values[key]) { is Number -> v.toLong(); is Boolean -> if (v) 1 else 0; is String -> v.toLongOrNull() ?: 0; else -> 0 }
    }
    fun floatForKey(key: String): Float = doubleForKey(key).toFloat()
    fun doubleForKey(key: String): Double = synchronized(lock) {
        when (val v = values[key]) { is Number -> v.toDouble(); is String -> v.toDoubleOrNull() ?: 0.0; else -> 0.0 }
    }
    fun arrayForKey(key: String): List<*>? = synchronized(lock) { values[key] as? List<*> }
    fun stringArrayForKey(key: String): List<*>? = synchronized(lock) { (values[key] as? List<*>)?.filterIsInstance<String>() }
    fun dictionaryRepresentation(): Map<Any?, *> = synchronized(lock) { values.toMap() }

    fun setObject(value: Any?, forKey: String) {
        val stored: Any? = when (value) {
            null -> null
            is String, is Boolean -> value
            is Int, is Long, is Short, is Byte -> (value as Number).toLong()
            is Float, is Double -> (value as Number).toDouble()
            is Iterable<*> -> value.map { it?.toString() }
            is Array<*> -> value.map { it?.toString() }
            else -> value.toString()
        }
        update { if (stored == null) remove(forKey) else put(forKey, stored) }
    }
    fun setBool(value: Boolean, forKey: String) = update { put(forKey, value) }
    fun setInteger(value: Long, forKey: String) = update { put(forKey, value) }
    fun setInteger(value: Int, forKey: String) = update { put(forKey, value.toLong()) }
    fun setFloat(value: Float, forKey: String) = update { put(forKey, value.toDouble()) }
    fun setDouble(value: Double, forKey: String) = update { put(forKey, value) }
    fun removeObjectForKey(key: String) = update { remove(key) }
    fun synchronize(): Boolean = true

    /** Clears this whole store (the name is ignored: one store per file). */
    fun removePersistentDomainForName(@Suppress("UNUSED_PARAMETER") name: String) = update { clear() }

    private inline fun update(change: MutableMap<String, Any>.() -> Unit) {
        synchronized(lock) {
            values.change()
            save()
        }
    }

    private fun load(): MutableMap<String, Any> {
        val text = runCatching { file.readText() }.getOrNull() ?: return mutableMapOf()
        val obj = runCatching { json.parseToJsonElement(text) as? JsonObject }.getOrNull() ?: return mutableMapOf()
        val out = mutableMapOf<String, Any>()
        for ((k, v) in obj) fromJson(v)?.let { out[k] = it }
        return out
    }

    private fun save() {
        val obj = JsonObject(values.mapValues { toJson(it.value) })
        file.parentFile?.mkdirs()
        val tmp = File(file.parentFile, file.name + ".tmp")
        tmp.writeText(json.encodeToString(JsonObject.serializer(), obj))
        if (!tmp.renameTo(file)) {
            file.delete()
            tmp.renameTo(file)
        }
    }

    private fun toJson(v: Any?): JsonElement = when (v) {
        null -> JsonNull
        is String -> JsonPrimitive(v)
        is Boolean -> JsonPrimitive(v)
        is Long -> JsonPrimitive(v)
        is Double -> JsonPrimitive(v)
        is Number -> JsonPrimitive(v)
        is List<*> -> JsonArray(v.map { toJson(it) })
        else -> JsonPrimitive(v.toString())
    }

    private fun fromJson(e: JsonElement): Any? = when (e) {
        is JsonNull -> null
        is JsonArray -> e.map { fromJson(it) }
        is JsonPrimitive -> when {
            e.isString -> e.content
            e.booleanOrNull != null -> e.booleanOrNull
            e.longOrNull != null -> e.longOrNull
            else -> e.doubleOrNull
        }
        is JsonObject -> e.toString()
    }
}

/** Where the desktop app keeps its files: %APPDATA%\<app> on Windows, ~/.<app> elsewhere. */
object DesktopPaths {
    val appName: String = System.getProperty("nuvio.appName")?.takeIf { it.isNotBlank() } ?: "Nuvio"
    val dataDir: File by lazy {
        val appData = System.getenv("APPDATA")
        val dir = if (!appData.isNullOrBlank()) File(appData, appName) else File(System.getProperty("user.home"), ".${appName.lowercase()}")
        dir.apply { mkdirs() }
    }
    val cacheDir: File by lazy { File(dataDir, "cache").apply { mkdirs() } }
}
