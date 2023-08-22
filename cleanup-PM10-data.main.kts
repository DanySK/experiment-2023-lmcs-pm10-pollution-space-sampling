#!/usr/bin/env kotlin
@file:Repository("https://repo.maven.apache.org/maven2")
@file:DependsOn("com.github.doyaaaaaken:kotlin-csv-jvm:1.9.2")
@file:DependsOn("org.apache.commons:commons-compress:1.23.0")
@file:DependsOn("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")

import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import org.apache.commons.compress.compressors.zstandard.ZstdCompressorInputStream
import org.apache.commons.compress.compressors.zstandard.ZstdCompressorOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.PrintStream
import java.io.Serializable
import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong
import kotlin.streams.asSequence
import kotlin.streams.asStream

enum class ValidEntries {
    Countrycode,
    Namespace,
    AirQualityNetwork,
    AirQualityStation,
    AirQualityStationEoICode,
    SamplingPoint,
    SamplingProcess,
    Sample,
    AirPollutant,
    AirPollutantCode,
    AveragingTime,
    Concentration,
    UnitOfMeasurement,
    DatetimeBegin,
    DatetimeEnd,
    Validity,
    Verification
}

data class Location(val name: String, val latitude: Float, val longitude: Float) : Serializable {
    constructor(map: Map<String, String>) : this(
        map["Name"]!!, map["Latitude"].doublyfy(), map["Longitude"].doublyfy())

    fun toList() = listOf(name, latitude, longitude)

    override fun toString(): String {
        return "Station $name @ $latitude,$longitude"
    }

    companion object {
        private fun String?.doublyfy() = this!!.replace(',', '.').toFloat()
    }
}

data class DataPoint(val time: Long, val value: Float) : Comparable<DataPoint>, Serializable {
    override fun compareTo(other: DataPoint) = time.compareTo(other.time)
}

val locations = csvReader()
    .readAllWithHeader(File("pm10locations.csv"))
    .map { Location(it) }
    .groupBy { it.name }
    .mapValues { it.value.first() }

val source = File("PM10-data")
val datePattern = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss XX")
val firstTime = 1356998400000 // Jan 01 2013
fun String.toEpochMillis() = ZonedDateTime
    .parse(substringBeforeLast(':'), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss X"))
    .toInstant()
    .toEpochMilli()
operator fun <T> Map<String, T>.get(key: ValidEntries) = get(key.toString())!!
fun Map<String, String>.epoch() = get(ValidEntries.DatetimeBegin).toEpochMillis().let {
    it + (get(ValidEntries.DatetimeEnd).toEpochMillis() - it) / 2
}
fun Map<String, String>.toDataPoint(): DataPoint? = get(ValidEntries.Concentration).takeIf { it.isNotBlank() }?.let {
    DataPoint(epoch(), it.toFloat())
}

fun <T> T.warnWhenNull(message: String) = also {
    if (it == null) println(message)
}

data class CompactData(val latitude: Float, val longitude: Float, val time: Long, val value: Float) : Comparable<CompactData> {
    override fun compareTo(other: CompactData): Int =
        latitude.compareTo(other.latitude).takeIf { it != 0 } ?: longitude.compareTo(other.longitude)
}

fun DataOutputStream.writeData(data: CompactData) {
    writeFloat(data.latitude)
    writeFloat(data.longitude)
    writeLong(data.time)
    writeFloat(data.value)
}

fun DataInputStream.readData(): CompactData = CompactData(readFloat(), readFloat(), readLong(), readFloat())

fun Pair<Location, DataPoint>.compact() = CompactData(first.latitude, first.longitude, second.time, second.value)

fun File.writeData(process: DataOutputStream.() -> Unit) {
    DataOutputStream(ZstdCompressorOutputStream(outputStream().buffered())).use {
        it.process()
    }
}

fun File.tryLoadRawData(): ArrayList<CompactData>? = runCatching {
    DataInputStream(ZstdCompressorInputStream(inputStream().buffered())).use { input ->
        val size = input.readInt()
        return ArrayList<CompactData>(size).apply {
            (1..size).forEach {
                add(input.readData())
                if (it % 1_000_000 == 0) {
                    print("${now()} loaded from file ${it / 1_000_000}M/~${size / 1_000_000}M                     \r")
                }
            }
        }.also { println() }
    }
}.getOrNull()

fun now() = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))
val counter = AtomicLong()
val loadedDataFileName = "entries.jdata"
val loadedData: MutableList<CompactData> = File(loadedDataFileName).tryLoadRawData() ?: source.walkTopDown()
    .asStream()
    .parallel()
    .filter { it.extension == "csv"}
    .map {
        runCatching {
            csvReader().readAllWithHeader(it)
        }
    }
    .filter { it.isSuccess }
    .flatMap { it.getOrNull()!!.parallelStream() }
    .limit(10_000L)
    .map { entry ->
        val stationEoICode = entry[ValidEntries.AirQualityStationEoICode]
        val location = locations[stationEoICode]//.warnWhenNull("Missing Geospatial data for station $stationEoICode")
        location?.let {
            entry.toDataPoint()//.warnWhenNull("Missing sensor data for $entry")
                ?.let { CompactData(location.latitude, location.longitude, it.time, it.value) }
        }
    }
    .peek {
        val processed = counter.getAndIncrement()
        if (processed % 1_000_000L == 0L) {
            val message = "${now()} Processed ${processed / 1_000_000}M data points        \r"
            print(message)
        }
    }
    .filter { it != null }
    .toList()
    .filterNotNull()
    .toMutableList()
    .also { allData ->
        File(loadedDataFileName).writeData {
            writeInt(allData.size)
            allData.forEach {
                writeData(it)
            }
        }
    }

println("\nAll data loaded. Sorting...")

counter.set(0)
loadedData.sort()
var filteredData = loadedData
val locationsToData = locations.values.parallelStream()
    .map { location ->
        val start = System.nanoTime()
        val indexRange = filteredData.binarySearch(CompactData(location.latitude, location.longitude, 0, 0f))
        location to if (indexRange >= 0) {
            fun noMatch(data: CompactData) = data.latitude != location.latitude || data.longitude != location.longitude
            val first = filteredData.subList(0, indexRange + 1).indexOfLast(::noMatch)
            val last = filteredData.subList(indexRange, filteredData.size).indexOfFirst(::noMatch)
            filteredData.subList(first.coerceAtLeast(0), last.coerceAtLeast(0) + indexRange)
        } else {
            emptyList()
        }.also {
            print(
                "${now()} ${counter.incrementAndGet()}/${locations.size} Processed location $location with ${
                    it.size
                } entries in ${
                    TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start)
                }ms                                \r"
            )
        }
    }
    .filter { it.second.isNotEmpty() }
    .asSequence()

PrintStream(ZstdCompressorOutputStream(File("data-summary.json.zstd").outputStream().buffered())).use { file ->
    locationsToData.joinTo(file, prefix = "[\n", postfix = "\n]", separator = ",\n") { (location, dataPoints) ->
        """
            {"name":"${location.name}","latitude":"${location.latitude}","longitude":"${location.longitude}","samples":${
            dataPoints
                .take(10)
                .sortedBy { it.time }
                .joinToString(separator = ",", prefix = "[", postfix = "]") { "[${it.time},${it.value}]" }
        }}
        """.trimIndent()
    }
}
