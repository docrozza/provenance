package io.docrozza.provenance.model

/**
 * A quotation qualification of an entity
 */
interface Quotation : Derivation {
    override val kind: Kind get() = Kind.Quotation
}