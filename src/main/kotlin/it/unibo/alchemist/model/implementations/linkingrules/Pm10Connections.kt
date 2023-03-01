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
        val previous = when {
            center.id > 1 -> environment.nodes[center.id - 1]
            else -> null
        }
        val next = when {
            center.id < environment.nodes.size - 1 -> environment.nodes[center.id + 1]
            else -> null
        }
        val closeby = environment.getNodesWithinRange(center, 1_000_000.0)
            .sortedBy { environment.getDistanceBetweenNodes(center, it) }
            .take(3)
        return Neighborhoods.make(environment, center, closeby + listOfNotNull(previous, next))
    }

    override fun isLocallyConsistent(): Boolean = true
}
