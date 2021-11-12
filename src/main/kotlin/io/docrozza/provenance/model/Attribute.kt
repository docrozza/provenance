package io.docrozza.provenance.model

import com.stardog.stark.IRI
import com.stardog.stark.Value
import com.stardog.stark.vocabs.RDF

/**
 * Represents an attribute [property] for an [IRI] to some RDF [value]
 */
data class Attribute(val property: IRI, val value: Value) {
    companion object {
        /**
         * Utility method for representing a type of provenance object
         */
        fun type(kind: IRI) : Attribute {
            return Attribute(RDF.TYPE, kind)
        }
    }
}