package io.docrozza.provenance.model

import com.stardog.stark.IRI

/**
 * A provenance link between a [subject] and a [asInBundle] via a [mentionOf]
 */
interface Link {
    val subject: ThingOrRef
    val mentionOf: IRI
    val asInBundle: BundleOrRef
}