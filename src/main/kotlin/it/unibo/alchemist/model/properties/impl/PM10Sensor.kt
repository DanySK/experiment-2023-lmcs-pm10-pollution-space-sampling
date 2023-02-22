package it.unibo.alchemist.model.properties.impl

import it.unibo.alchemist.model.interfaces.Environment
import it.unibo.alchemist.model.interfaces.GeoPosition
import it.unibo.alchemist.model.interfaces.Layer
import it.unibo.alchemist.model.interfaces.Node
import it.unibo.alchemist.model.interfaces.NodeProperty

class PM10Layer(val environment: Environment<Any?, GeoPosition>) : Layer<Double, GeoPosition> {

    override fun getValue(p: GeoPosition?): Double {
//        val now = environment.simulation.time
        TODO("Not yet implemented")
    }


}