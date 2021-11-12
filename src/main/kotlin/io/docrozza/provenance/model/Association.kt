package io.docrozza.provenance.model

/**
 * An association qualification of an agent
 */
interface Association : AgentInfluence {
    override val kind: Kind get() = Kind.Association
    val hadPlan: PlanOrRef?
}