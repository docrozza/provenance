package io.docrozza.provenance.model

import com.stardog.stark.IRI
import io.docrozza.provenance.Provenance

/**
 * A provenance qualification of an activity
 */
interface ActivityInfluence : Influence<Activity> {
    override val influenceIRI: IRI get() = io.docrozza.provenance.Provenance.activity
    val activity: ActivityOrRef

    override fun influencer(): ActivityOrRef {
        return activity
    }
}