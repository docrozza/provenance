package io.docrozza.provenance.model

/**
 * A provenance plan entity
 */
interface Plan : Entity {
    override val kind: Kind get() = Kind.Plan
}