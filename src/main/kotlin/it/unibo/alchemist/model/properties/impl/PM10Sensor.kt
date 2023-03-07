package it.unibo.alchemist.model.properties.impl

import com.github.benmanes.caffeine.cache.Caffeine
import it.unibo.alchemist.model.implementations.properties.ProtelisDevice
import it.unibo.alchemist.model.interfaces.GeoPosition
import it.unibo.alchemist.protelis.AlchemistExecutionContext
import it.unibo.lmcs.closestTo
import it.unibo.lmcs.startTime
import it.unibo.lmcs.idOf
import it.unibo.lmcs.valuesAfter
import java.lang.Integer.min

private typealias DataItem = List<Number>
private val DataItem.time: Long get() = get(0).toLong()
private val DataItem.simulatorTime: Double get() = (time - startTime).toDouble()
private val DataItem.pm10: Float get() = get(1).toFloat()


object PM10Sensor {

    const val idLabel = "station-id"
    const val chunkSize = 100

    val data = Caffeine.newBuilder()
        .weakKeys()
        .build<AlchemistExecutionContext<GeoPosition>, List<List<Number>>> { context ->
            val env = context.executionEnvironment
            val id: String = when {
                env.has(idLabel) -> env.get(idLabel).toString()
                else -> idOf((context.deviceUID as ProtelisDevice<*>).id).also { env.put(idLabel, it) }
            }
            val time = context.currentTime
            valuesAfter(id, time.toLong() + startTime, chunkSize)
        }

    @JvmStatic
    fun readPm10(context: AlchemistExecutionContext<GeoPosition>): Number {
        var nextValues = data[context]
        val now: Double = context.currentTime.toDouble()
        if (nextValues.size > 1 && nextValues.last().simulatorTime < now) {
            val newData = data.refresh(context).get()
            nextValues = newData
        }
        val index = nextValues.closestTo(now, DataItem::simulatorTime)
        val (time, pm10) = nextValues[index]
        context.executionEnvironment.put("timeSet", time)
        context.executionEnvironment.put("nextSplit", nextValues[min(nextValues.size - 1, index + 1)].time - now)
        context.executionEnvironment.put("pm10", pm10.toFloat().coerceAtLeast(0f))
        return pm10.toFloat().coerceAtLeast(0f)
    }

    @JvmStatic
    fun aqi(context: AlchemistExecutionContext<GeoPosition>, pm10: Number): Int = when (pm10.toDouble()) {
        in Double.NEGATIVE_INFINITY..45.0 -> 0
        in Double.NEGATIVE_INFINITY..100.0 -> 1
        in Double.NEGATIVE_INFINITY..150.0 -> 2
        in Double.NEGATIVE_INFINITY..200.0 -> 3
        in Double.NEGATIVE_INFINITY..300.0 -> 4
        else -> 5
    }.also { context.executionEnvironment.put("aqi", it) }
}
