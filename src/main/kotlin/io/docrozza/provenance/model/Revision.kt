package io.docrozza.provenance.model

/**
 * A revision qualification of an entity
 */
interface Revision : Derivation {
    override val kind: Kind get() = Kind.Revision
}