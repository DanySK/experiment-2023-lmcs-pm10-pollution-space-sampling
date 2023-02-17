package it.unibo.lmcs

import org.litote.kmongo.KMongo
import org.litote.kmongo.ascending

val client = KMongo.createClient()
val database = client.getDatabase("test")
val collection = database.getCollection("pm10")

data class Entry(val name: String, val latitude: Float, val longitude: Float, val samples: List<Pair<Long, Double>>) {
    val times: List<Long> get() = samples.map { it.first }
}

fun sortedEntries() = collection.find().sort(ascending(Entry::name))
