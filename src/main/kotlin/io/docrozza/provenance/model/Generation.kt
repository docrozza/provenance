package io.docrozza.provenance.model

/**
 * A generation qualification of an activity
 */
interface Generation : ActivityInfluence, InstantaneousEvent {
    override val kind: Kind get() = Kind.Generation
}