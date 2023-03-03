package it.unibo.alchemist.model.implementations.linkingrules

import it.unibo.alchemist.model.implementations.neighborhoods.Neighborhoods
import it.unibo.alchemist.model.interfaces.Environment
import it.unibo.alchemist.model.interfaces.GeoPosition
import it.unibo.alchemist.model.interfaces.LinkingRule
import it.unibo.alchemist.model.interfaces.Neighborhood
import it.unibo.alchemist.model.interfaces.Node

class Pm10Connections : LinkingRule<Any, GeoPosition> {
    override fun computeNeighborhood(
        center: Node<Any>,
        environment: Environment<Any, GeoPosition>,
    ): Neighborhood<Any> {
        val rangeLink = 100_000.0
        val minNeighbors = 1
        val maxNeighbors = 6
        val close = environment.getNodesWithinRange(center, rangeLink)
            .asSequence()
            .map { it to environment.getDistanceBetweenNodes(center, it) }
            .sortedBy { it.second }
            .take(maxNeighbors)
            .toMap()
            .toMutableMap()
        var farthest: Pair<Node<Any>, Double>? = null
        if (close.size < minNeighbors) {
            for (node in environment.nodes) {
                if (node != center) {
                    val distance = environment.getDistanceBetweenNodes(center, node)
                    when {
                        farthest == null -> {
                            farthest = node to distance
                            close[node] = distance
                        }

                        close.size < minNeighbors -> {
                            close[node] = distance
                            if (distance > farthest.second) {
                                farthest = node to distance
                            }
                        }

                        distance < farthest.second -> {
                            close.remove(farthest.first)
                            close[node] = distance
                            farthest = close.maxBy { it.value }.toPair()
                        }
                    }
                }
            }
        }
        return Neighborhoods.make(environment, center, close.keys)
    }

    override fun isLocallyConsistent(): Boolean = true

}
