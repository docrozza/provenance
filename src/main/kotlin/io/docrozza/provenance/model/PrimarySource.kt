package io.docrozza.provenance.model

/**
 * A source qualification of an entity
 */
interface PrimarySource : Derivation {
    override val kind: Kind get() = Kind.PrimarySource
}