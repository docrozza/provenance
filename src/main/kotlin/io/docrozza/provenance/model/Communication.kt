package io.docrozza.provenance.model

/**
 * A communication qualification of an activity
 */
interface Communication : ActivityInfluence {
    override val kind: Kind get() = Kind.Communication
}