#!/usr/bin/env kotlin
@file:Repository("https://repo.maven.apache.org/maven2")
@file:DependsOn("com.github.doyaaaaaken:kotlin-csv-jvm:1.8.0")

import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import com.github.doyaaaaaken.kotlincsv.dsl.csvWriter
import java.io.File
import kotlin.math.hypot

data class Entry(val name: String, val latitude: Double, val longitude: Double) {
    constructor(map: Map<String, String>) : this(
        map["Name"]!!, map["Latitude"].doublyfy(), map["Longitude"].doublyfy())

    fun distanceFrom(other: Entry) = hypot(longitude - other.longitude, latitude - other.latitude)

    fun toList() = listOf(name, latitude, longitude)

    override fun toString(): String {
        return "[$name@ $latitude,$longitude ]"
    }

    companion object {
        private fun String?.doublyfy() = this!!.replace(',', '.').toDouble()
    }
}

val source = File("locations.csv")
val data = csvReader()
    .readAllWithHeader(source)
    .map { Entry(it) }
    .groupBy { it.name }
    .filterValues {  values ->
        values.zipWithNext { a, b ->
            a.distanceFrom(b) < 1
        }.all { it }
    }
    .mapValues { (_, entries) ->
        entries.zipWithNext { a, b ->
            check(a.distanceFrom(b) < 0.3) {
            "$a -- $b -- ${a.distanceFrom(b)} in $entries"
            }
        }
        val sum = entries.reduce { a, b ->
            Entry(a.name, a.latitude + b.latitude, a.longitude + b.longitude)
        }
        Entry(sum.name, sum.latitude / entries.size, sum.longitude / entries.size)
    }
    .values
    .map { it.toList() }
csvWriter().open("pm10locations.csv") {
    writeRow("Name", "Latitude", "Longitude")
    writeRows(data)
}
