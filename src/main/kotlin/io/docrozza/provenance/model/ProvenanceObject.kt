package io.docrozza.provenance.model

import com.stardog.stark.IRI
import com.stardog.stark.Value

/**
 * Parent interface for describing a provenance specific object
 */
interface ProvenanceObject : Identifiable {
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