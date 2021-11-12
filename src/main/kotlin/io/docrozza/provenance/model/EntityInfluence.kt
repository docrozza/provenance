package io.docrozza.provenance.model

import com.stardog.stark.IRI
import io.docrozza.provenance.Provenance

/**
 * A provenance qualification of an entity
 */
interface EntityInfluence : Influence<Entity> {
    override val influenceIRI: IRI get() = io.docrozza.provenance.Provenance.entity
    val entity: EntityOrRef
    val hadActivity: ActivityOrRef?

    override fun influencer(): EntityOrRef {
        return entity
    }
}