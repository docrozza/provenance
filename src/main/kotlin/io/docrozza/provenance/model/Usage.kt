package io.docrozza.provenance.model

/**
 * A usage qualification of an entity
 */
interface Usage : EntityInfluence, InstantaneousEvent {
    override val kind: Kind get() = Kind.Usage
}