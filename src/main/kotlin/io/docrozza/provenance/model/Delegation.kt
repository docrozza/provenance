package io.docrozza.provenance.model

/**
 * A delegation qualification of an agent
 */
interface Delegation : AgentInfluence {
    override val kind: Kind get() = Kind.Delegation
}