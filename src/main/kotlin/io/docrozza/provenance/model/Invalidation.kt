package io.docrozza.provenance.model

/**
 * A invalidation qualification of an activity
 */
interface Invalidation : ActivityInfluence, InstantaneousEvent {
    override val kind: Kind get() = Kind.Invalidation
}