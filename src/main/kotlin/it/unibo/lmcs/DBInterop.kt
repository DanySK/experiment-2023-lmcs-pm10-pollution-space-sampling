package it.unibo.lmcs

import com.mongodb.client.FindIterable
import org.bson.Document
import org.litote.kmongo.KMongo
import org.litote.kmongo.aggregate
import org.litote.kmongo.ascending
import org.litote.kmongo.ensureIndex
import org.litote.kmongo.eq
import org.litote.kmongo.findOne
import org.litote.kmongo.match
import org.litote.kmongo.project
import org.litote.kmongo.projection

val client = KMongo.createClient()
val database = client.getDatabase("test")
val collection = database.getCollection("pm10")

val expectedFirstTime = 1_356_996_500_000L
val firstTime: Long by lazy{
    collection.aggregate<Map<String, Long>>(
        """
        [
          { ${'$'}unwind: "${'$'}samples" },
          { ${'$'}group: { _id: null, minVal: { ${'$'}min: { ${'$'}first: "${'$'}samples" } } } }
        ]
        """.trimIndent()
    ).first()?.values?.first() ?: error("Unable to compute the first time")
}


data class Entry(val name: String, val latitude: Float, val longitude: Float, val samples: List<Pair<Long, Double>>) {
    val times: List<Long> get() = samples.map { it.first }
}

fun idOf(latitude: Float, longitude: Float) =
    collection.findOne(Entry::latitude eq latitude).also { println(longitude) }

fun <T, R : Comparable<R>> List<T>.closestTo(target: R, property: T.() -> R): Int {
    check(this.isNotEmpty())
    var low = 0
    var high = size - 1
    var closest = 0
    while (low <= high) {
        val mid = (low + high) / 2
        val midVal = this[mid].property()
        val cmp = midVal.compareTo(target)
        when (cmp) {
            0 -> return mid
            1 -> { // midval is larger than target
                high = mid - 1
            }
            -1 -> {
                low = mid + 1
                closest = mid
            }
        }
    }
    return closest
}

private data class ValuesQuery(val samples: List<List<Number>>) {
}

fun valuesAfter(name: String, time: Long, count: Int) = collection.aggregate<ValuesQuery>(
    match(Entry::name eq name),
    project(Entry::samples)
).first()?.samples?.let { samples ->
    val start = samples.closestTo(time) { first().toLong() }
    samples.slice(start..(start + count).coerceAtMost(samples.size - 1))
} ?: error("No values found for $name after $time")

fun idOf(index: Int): String = sortedEntries()
    .withoutSamples()
    .skip(index)
    .limit(1)
    .first()
    ?.getString("name")
    ?: error("Unable to find the id of the $index-th entry")

fun sortedEntries(): FindIterable<Document> {
    collection.ensureIndex(Entry::name)
    return collection.find().sort(ascending(Entry::name))
}

fun <T> FindIterable<T>.withoutSamples() = projection(Entry::name, Entry::latitude, Entry::longitude)

fun main() {
    println(idOf(44.703710f,8.033280f))
    println(idOf(collection.countDocuments().toInt() - 1))
    println(idOf(4223))
    println(valuesAfter("IT1524A", expectedFirstTime + 10_038_000_000, 100))
}
