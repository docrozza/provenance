package io.docrozza.provenance.model

/**
 * A provenance collection entity
 */
interface Collection : Entity {
    override val kind: Kind get() = Kind.Collection
    val hadMembers: Sequence<EntityOrRef>
}