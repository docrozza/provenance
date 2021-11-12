package io.docrozza.provenance.model

import com.stardog.stark.IRI

/**
 * A provenance qualification
 */
interface Influence<T : ProvenanceObject> : Locatable {
    override val kind: Kind get() = Kind.Influence
    val influenceIRI: IRI
    val hadRole: RoleOrRef?
    fun influencer() : RefOrValue<T>
}