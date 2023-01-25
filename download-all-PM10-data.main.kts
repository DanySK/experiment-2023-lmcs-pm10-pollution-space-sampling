#!/usr/bin/env kotlin
@file:Repository("https://repo.maven.apache.org/maven2")

import java.io.File
import java.net.URL

val countryCodesPage = URL("https://countrycode.org").readText()
val extractCodes = Regex("<td>\\s*(\\w\\w)\\s*\\/\\s*\\w\\w\\w\\s*<\\/td>")
val codes = extractCodes
    .findAll(countryCodesPage)
    .map { it.groupValues[1] }
    .toList()
    .parallelStream()
val queries = codes.map {
    "https://fme.discomap.eea.europa.eu/fmedatastreaming/AirQualityDownload/AQData_Extract.fmw" +
        "?CountryCode=$it" +
        "&CityName=" +
        "&Pollutant=5" +
        "&Year_from=2013" +
        "&Year_to=2022" +
        "&Station=" +
        "&Samplingpoint=" +
        "&Source=All" +
        "&Output=TEXT" +
        "&UpdateDate=&TimeCoverage=Year"
}
val destination = File("PM10-data")
destination.mkdirs()
val cleaner = Regex(".*(https?:\\/\\/[^\\s]+)")
val urls = queries.map { runCatching { URL(it).readText() } }
    .map { it.getOrNull() }
    .flatMap { it!!.lines().parallelStream() }
    .map { it.trim() }
    .filter { !it.isNullOrBlank() }
    .map { cleaner.matchEntire(it)?.groupValues?.get(1) }
    .filter { it != null }
    .peek { println(it) }
    .map { URL(it) } //"http${it.substringAfter("http")}".trim()) }
    .forEach { File(destination, it.path.substringAfterLast('/')).writeText(it.readText()) }
