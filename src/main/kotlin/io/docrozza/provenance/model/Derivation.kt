package io.docrozza.provenance.model

/**
 * A derivation qualification of an entity
 */
interface Derivation : EntityInfluence {
    override val kind: Kind get() = Kind.Derivation
    val hadUsage: UsageOrRef?
    val hadGeneration: GenerationOrRef?
}