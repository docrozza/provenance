package io.docrozza.provenance.model

import com.stardog.stark.IRI
import io.docrozza.provenance.Provenance

/**
 * A provenance agent
 */
interface Agent : Locatable {

    enum class Type {
        Agent, Person, Organization, Software;

        fun toIRI() : IRI {
            return when (this) {
                Agent -> io.docrozza.provenance.Provenance.Agent
                Person -> io.docrozza.provenance.Provenance.Person
                Organization -> io.docrozza.provenance.Provenance.Organization
                Software -> io.docrozza.provenance.Provenance.SoftwareAgent
            }
        }
    }

    override val kind: Kind get() = Kind.Agent
    val type: Type
    val actedOnBehalfOf: Sequence<AgentOrDelegation>
}