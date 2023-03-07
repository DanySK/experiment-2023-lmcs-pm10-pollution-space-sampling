package it.unibo.alchemist.loader.export.extractors

import it.unibo.alchemist.loader.export.Extractor
import it.unibo.alchemist.model.implementations.molecules.SimpleMolecule
import it.unibo.alchemist.model.interfaces.Actionable
import it.unibo.alchemist.model.interfaces.Environment
import it.unibo.alchemist.model.interfaces.Molecule
import it.unibo.alchemist.model.interfaces.Node
import it.unibo.alchemist.model.interfaces.Time
import org.apache.commons.math3.stat.descriptive.UnivariateStatistic
import org.apache.commons.math3.stat.descriptive.moment.Mean
import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation
import org.apache.commons.math3.stat.descriptive.rank.Max
import org.apache.commons.math3.stat.descriptive.rank.Min

data class Bubble(val leader: Int, val statMolecule: String, val nodes: List<Node<*>>) {

    val values by lazy {
        nodes.mapNotNull {
            it.getConcentration(SimpleMolecule(statMolecule))?.numeric<Double>()
        }.toDoubleArray()
    }

    inline fun <reified S : UnivariateStatistic> stat(): Double = values.stat<S>()
}

data class Bubbles(val bubbles: List<Bubble>) {

    constructor(
        environment: Environment<*, *>,
        leaderMolecule: String,
        statMolecule: String,
    ): this(
        environment.nodes
            .groupBy { it.getConcentration(SimpleMolecule(leaderMolecule))?.numeric<Int>() }
            .mapNotNull { (leader, nodes) -> leader?.let { Bubble(leader, statMolecule, nodes) } }
    )

    inline fun <reified Inner : UnivariateStatistic, reified Outer : UnivariateStatistic> statOfStat() =
        bubbles.map { it.stat<Inner>() }.stat<Outer>()
}

class CoordDataEport : Extractor<Double> {
    override val columnNames: List<String> = completeNames

    override fun <T> extractData(
        environment: Environment<T, *>,
        reaction: Actionable<T>?,
        time: Time,
        step: Long,
    ): Map<String, Double> = names.flatMap { name ->
        val bubbles: Bubbles = Bubbles(environment, name, "pm10")
        measures.entries.map { (measure, statistic) ->
            "$name-$measure" to bubbles.statistic()
        }
    }.toMap()

    companion object {
        val measures = mapOf<String, Bubbles.() -> Double>(
            "intra-stdev-mean" to { statOfStat<StandardDeviation, Mean>() },
            "intra-stdev-stdev" to { statOfStat<StandardDeviation, StandardDeviation>() },
            "intra-stdev-max" to { statOfStat<StandardDeviation, Max>() },
            "intra-stdev-min" to { statOfStat<StandardDeviation, Min>() },
            "inter-mean-stdev" to { statOfStat<Mean, StandardDeviation>() },
            "bubble-count" to { bubbles.size.toDouble() },
            "bubble-size-mean" to { bubbles.map { it.nodes.size.toDouble() }.stat<Mean>() },
        )
        val names = listOf("distance-border", "distance", "bycountry", "pm10-variance", "pm10-variance-border")
        val completeNames = names.flatMap { name ->
            measures.keys.map { measure ->
                "$name-$measure"
            }
        }
    }
}


private inline fun <reified T : Number> Any.numeric(): T = when {
    this is T -> this
    this is Number -> when (T::class) {
        Int::class -> toInt()
        Double::class -> toDouble()
        Long::class -> toLong()
        else -> TODO()
    } as T
    else -> TODO()
}

private fun Iterable<Double>.stdev() = toList().toDoubleArray().stdev()

private fun DoubleArray.stdev() = stat<StandardDeviation>()

inline fun <reified S: UnivariateStatistic> Iterable<Double>.stat() = toList().toDoubleArray().stat<S>()

inline fun <reified S: UnivariateStatistic> DoubleArray.stat() = when (S::class) {
    StandardDeviation::class -> StandardDeviation()
    Max::class -> Max()
    Min::class -> Min()
    Mean::class -> Mean()
    else -> TODO(S::class.simpleName ?: "???")
}.evaluate(this)

private inline operator fun <reified T : Number> Node<*>.get(molecule: String): T? = get(SimpleMolecule(molecule))

private inline operator fun <reified T : Number> Node<*>.get(molecule: Molecule) =
    getConcentration(molecule)?.numeric<T>()
