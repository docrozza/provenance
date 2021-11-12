package io.docrozza.provenance.model

/**
 * A provenance role
 */
interface Role : ProvenanceObject {
    override val kind: Kind get() = Kind.Role
}