package it.unibo.alchemist.loader.deployments

import it.unibo.alchemist.loader.deployments.Deployment
import it.unibo.alchemist.model.implementations.positions.LatLongPosition
import it.unibo.alchemist.model.interfaces.GeoPosition
import it.unibo.lmcs.Entry
import it.unibo.lmcs.sortedEntries
import jdk.jfr.FlightRecorder
import org.litote.kmongo.fields
import org.litote.kmongo.findValue
import org.litote.kmongo.include
import java.util.stream.Stream
import kotlin.streams.asStream

class PM10() : Deployment<GeoPosition> {

    override fun stream(): Stream<GeoPosition> = sortedEntries()
        .projection(fields(include(Entry::latitude, Entry::longitude)))
        .map {
            LatLongPosition(it["latitude"].doublyfy(), it["longitude"].doublyfy())
        }
        .asSequence()
        .asStream()

    companion object {
        private fun Any?.doublyfy(): Double = when(this) {
            is Double -> this
            is String -> toDouble()
            is Int -> toDouble()
            is Long -> toDouble()
            is Float -> toDouble()
            else -> throw IllegalArgumentException("Cannot convert $this to Double")
        }
    }
}

//
//fun main() {
//    sortedEntries().limit(10)
//        .filter(PM10.Entry::samples / PM10.Entry::times lt  0L)
//        .forEach {
//            println(it.keys)
//            println(it["latitude"])
//            println(it["longitude"])
//            println(LatLongPosition(it["latitude"].toString().toDouble(), it["longitude"].toString().toDouble()))
//            println((it["samples"] as? List<*>)?.binarySearchBy()take(10))
//        }
//}
