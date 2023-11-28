package com.appboosty.premiumhelper.toto

import kotlin.random.Random

data class WeightedValueParameter(val name: String, val weighted_values: Map<String, Int>) {

    fun pickRandomValue(): String {

        if (weighted_values.size == 1) return weighted_values.keys.first()

        var random = Random.nextInt(0, weighted_values.values.sumOf { it })

        for (entry in weighted_values) {
            random -= entry.value
            if (random < 0) {
                return entry.key
            }
        }

        throw IllegalStateException("Should never get here!")
    }

    fun hash(): Int {
        return weighted_values.hashCode()
    }

}

fun Map<String, Map<String, Int>>.asWeightedParamsList() : List<WeightedValueParameter> {
    return map { entry -> WeightedValueParameter(entry.key, entry.value) }.toList()
}