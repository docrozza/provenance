package io.docrozza.provenance.model

/**
 * An end qualification of an entity
 */
interface End : EntityInfluence, InstantaneousEvent {
    override val kind: Kind get() = Kind.End
}