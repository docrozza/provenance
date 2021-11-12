package io.docrozza.provenance.model

import com.stardog.stark.IRI
import com.stardog.stark.Value

/**
 * Represents and non-provenance objects
 */
interface NonProvenance : Identifiable {
    override val kind: Kind get() = Kind.NonProvenance

    /**
     * The properties of this object
     */
    val properties: Sequence<Attribute>

    /**
     * Returns the values of a specific [property] as a [Sequence]
     */
    fun values(property: IRI) : Sequence<Value> {
        return properties
            .filter { it.property == property }
            .map { it.value }
    }
}