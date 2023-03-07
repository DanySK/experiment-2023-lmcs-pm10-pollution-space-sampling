package it.unibo.lmcs

import com.mongodb.client.MongoIterable
import com.mongodb.client.model.Accumulators.first
import com.mongodb.client.model.Accumulators.last
import com.mongodb.client.model.BsonField
import org.bson.Document
import org.bson.conversions.Bson
import org.litote.kmongo.KMongo
import org.litote.kmongo.aggregate
import org.litote.kmongo.ascending
import org.litote.kmongo.ensureIndex
import org.litote.kmongo.eq
import org.litote.kmongo.fields
import org.litote.kmongo.findOne
import org.litote.kmongo.group
import org.litote.kmongo.include
import org.litote.kmongo.last
import org.litote.kmongo.limit
import org.litote.kmongo.match
import org.litote.kmongo.popLast
import org.litote.kmongo.project
import org.litote.kmongo.projection
import org.litote.kmongo.replaceRoot
import org.litote.kmongo.skip
import org.litote.kmongo.sort

val client = KMongo.createClient()
val database = client.getDatabase("test")
val collection = database.getCollection("pm10")

const val startTime = 1580511600L * 1000 //
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

private data class ValuesQuery(val samples: List<List<Number>>)
private data class SingleValueQuery(val sample: List<Number>)

fun valuesAfter(name: String, time: Long, count: Int): List<List<Number>> =
    collection.aggregate<ValuesQuery>(
        """
            [
                { ${'$'}match : { "name": "$name" } },
                { ${'$'}project: {
                    samples: {
                        ${'$'}filter: {
                            input: "${'$'}samples",
                            cond: {
                                ${'$'}gte: [ "${'$'}${'$'}this", [ NumberLong($time), 0 ] ]
                            },
                            limit: $count
                        }
                    }
                }}
            ]
        """.trimIndent()
    ).first()?.samples?.takeIf { it.isNotEmpty() }
        ?: collection.aggregate<SingleValueQuery>(
            """
                [
                    { ${'$'}match : { "name": "$name" } },
                    { ${'$'}project: {
                        sample: {
                            ${'$'}last: "${'$'}samples"
                        }
                    }}
                ]
            """.trimIndent()
        ).first()?.let { listOf(it.sample) } ?: error("Unable to find any sample for $name")

fun idOf(index: Int): String = sortedEntries(false, skip(index), limit(1))
    .first()
    ?.getString("name")
    ?: error("Unable to find the id of the $index-th entry")

fun sortedEntries(withSamples: Boolean = true, vararg moreSteps: Bson): MongoIterable<Document> {
    collection.ensureIndex(Entry::name)
    val fields = listOf(Entry::name, Entry::latitude, Entry::longitude)
        .plus(listOf(Entry::samples).takeIf { withSamples }.orEmpty())
        .associateBy { it.name }
    return collection.aggregate<Document>(
        group(
            Entry::name,
            first("document", fields)
        ),
        replaceRoot("document".projection),
        sort(ascending(Entry::name)),
        *moreSteps
    )
}

fun main() {
    println(idOf(44.703710f,8.033280f))
    println(idOf(collection.countDocuments().toInt() - 1))
    println(idOf(4223))
    println(valuesAfter("IT1524A", startTime + 10_038_000_000, 100))
}
