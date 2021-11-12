package io.docrozza.provenance.model

/**
 * An attribution qualification of an agent
 */
interface Attribution : AgentInfluence {
    override val kind: Kind get() = Kind.Attribution
}