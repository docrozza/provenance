package io.docrozza.provenance.model

/**
 * Represents a provenance object with some location
 */
interface Locatable : ProvenanceObject {
    val atLocation: LocationOrRef?
}