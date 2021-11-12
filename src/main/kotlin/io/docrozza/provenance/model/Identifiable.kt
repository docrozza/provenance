package io.docrozza.provenance.model

import com.stardog.stark.IRI
import com.stardog.stark.Resource

/**
 * Parent interface for describing any object via it's [id] and [kind]
 */
interface Identifiable {
    val id: Resource
    val kind: Kind
    val kindIRI : IRI get() = kind.toIRI()
}