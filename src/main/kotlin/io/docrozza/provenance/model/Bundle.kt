package io.docrozza.provenance.model

/**
 * A provenance bundle entity
 */
interface Bundle : Entity {
    override val kind: Kind get() = Kind.Bundle

    /**
     * The [items] of the bundle
     */
    val items: Sequence<Identifiable>

    /**
     * The [links] from entities in this bundle to other bundles
     */
    val links: Sequence<Link>

    /**
     * Returns the number of included bundles
     */
    fun bundleIncludes() : Int {
        return bundleIncludes(this)
    }

    private fun bundleIncludes(bundle: Bundle) : Int {
        var count = 0
        bundle.links.map { it.asInBundle }.forEach {
            count++
            if (!it.isRef) {
                count += bundleIncludes(it.about())
            }
        }
        return count
    }
}