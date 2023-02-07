#!/usr/bin/env kotlin
@file:Repository("https://repo.maven.apache.org/maven2")
@file:DependsOn("com.google.code.gson:gson:2.10.1")

import com.google.gson.Gson
import com.google.gson.stream.JsonReader
import java.io.File

require(args.size == 2) {
    "ERROR: wrong argument count. Supplied ${args.size}: $args.\nUsage: reduce-large-file.main.kts <input> <output>"
}
data class Entry(val name: String, val latitude: Double, val longitude: Double, val samples: List<List<Float>>)
val result = mutableListOf<Entry>()
File(args[0]).reader().use { fileReader ->
    val reader = JsonReader(fileReader)
    reader.beginArray()
    var count = 0
    while (reader.hasNext() && count < 15) {
        result.add(Gson().fromJson(reader, Entry::class.java))
        count++
    }
}
File(args[1]).writer().use {
    Gson().toJson(result, it)
}
