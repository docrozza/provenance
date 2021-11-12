package io.docrozza.provenance.model

/**
 * A provenance location
 */
interface Location : ProvenanceObject {
    override val kind: Kind get() = Kind.Location
}