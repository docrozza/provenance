package io.docrozza.provenance.model

/**
 * A start qualification of an entity
 */
interface Start : EntityInfluence, InstantaneousEvent {
    override val kind: Kind get() = Kind.Start
}