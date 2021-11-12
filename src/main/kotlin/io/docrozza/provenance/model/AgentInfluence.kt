package io.docrozza.provenance.model

import com.stardog.stark.IRI
import io.docrozza.provenance.Provenance

/**
 * A provenance qualification of an agent
 */
interface AgentInfluence : Influence<Agent> {
    override val influenceIRI: IRI get() = io.docrozza.provenance.Provenance.agent
    val agent: AgentOrRef
    val hadActivity: ActivityOrRef?

    override fun influencer(): AgentOrRef {
        return agent
    }
}