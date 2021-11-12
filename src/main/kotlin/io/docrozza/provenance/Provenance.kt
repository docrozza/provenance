@file:Suppress("unused")
package io.docrozza.provenance

import io.docrozza.provenance.rdf.StardogUtils.iri

/**
 * Provenance [ontology](https://www.w3.org/TR/prov-o/)
 */
object Provenance {
    
    const val namespace = "http://www.w3.org/ns/prov#"

    val Entity = namespace.iri("Entity")
    val Activity = namespace.iri("Activity")
    val Agent = namespace.iri("Agent")
    val Collection = namespace.iri("Collection")
    val EmptyCollection = namespace.iri("EmptyCollection")
    val Bundle = namespace.iri("Bundle")
    val Plan = namespace.iri("Plan")
    val Location = namespace.iri("Location")
    val Influence = namespace.iri("Influence")
    val Usage = namespace.iri("Usage")
    val Start = namespace.iri("Start")
    val End = namespace.iri("End")
    val Derivation = namespace.iri("Derivation")
    val PrimarySource = namespace.iri("PrimarySource")
    val Quotation = namespace.iri("Quotation")
    val Revision = namespace.iri("Revision")
    val Generation = namespace.iri("Generation")
    val Communication = namespace.iri("Communication")
    val Invalidation = namespace.iri("Invalidation")
    val Attribution = namespace.iri("Attribution")
    val Association = namespace.iri("Association")
    val Delegation = namespace.iri("Delegation")
    val Role = namespace.iri("Role")
    val Person = namespace.iri("Person")
    val SoftwareAgent = namespace.iri("SoftwareAgent")
    val Organization = namespace.iri("Organization")
    val EntityInfluence = namespace.iri("EntityInfluence")
    val ActivityInfluence = namespace.iri("ActivityInfluence")
    val AgentInfluence = namespace.iri("AgentInfluence")
    val InstantaneousEvent = namespace.iri("InstantaneousEvent")

    val wasGeneratedBy = namespace.iri("wasGeneratedBy")
    val wasDerivedFrom = namespace.iri("wasDerivedFrom")
    val wasAttributedTo = namespace.iri("wasAttributedTo")
    val startedAtTime = namespace.iri("startedAtTime")
    val used = namespace.iri("used")
    val wasInformedBy = namespace.iri("wasInformedBy")
    val endedAtTime = namespace.iri("endedAtTime")
    val wasAssociatedWith = namespace.iri("wasAssociatedWith")
    val actedOnBehalfOf = namespace.iri("actedOnBehalfOf")
    val alternateOf = namespace.iri("alternateOf")
    val specializationOf = namespace.iri("specializationOf")
    val generatedAtTime = namespace.iri("generatedAtTime")
    val hadPrimarySource = namespace.iri("hadPrimarySource")
    val value = namespace.iri("value")
    val wasQuotedFrom = namespace.iri("wasQuotedFrom")
    val wasRevisionOf = namespace.iri("wasRevisionOf")
    val invalidatedAtTime = namespace.iri("invalidatedAtTime")
    val wasInvalidatedBy = namespace.iri("wasInvalidatedBy")
    val hadMember = namespace.iri("hadMember")
    val wasStartedBy = namespace.iri("wasStartedBy")
    val wasEndedBy = namespace.iri("wasEndedBy")
    val invalidated = namespace.iri("invalidated")
    val atLocation = namespace.iri("atLocation")
    val generated = namespace.iri("generated")
    val influenced = namespace.iri("influenced")
    val wasInfluencedBy = namespace.iri("wasInfluencedBy")
    val qualifiedInfluence = namespace.iri("qualifiedInfluence")
    val influencer = namespace.iri("influencer")
    val qualifiedGeneration = namespace.iri("qualifiedGeneration")
    val qualifiedDerivation = namespace.iri("qualifiedDerivation")
    val qualifiedPrimarySource = namespace.iri("qualifiedPrimarySource")
    val qualifiedQuotation = namespace.iri("qualifiedQuotation")
    val qualifiedRevision = namespace.iri("qualifiedRevision")
    val qualifiedAttribution = namespace.iri("qualifiedAttribution")
    val qualifiedInvalidation = namespace.iri("qualifiedInvalidation")
    val qualifiedStart = namespace.iri("qualifiedStart")
    val qualifiedUsage = namespace.iri("qualifiedUsage")
    val qualifiedCommunication = namespace.iri("qualifiedCommunication")
    val qualifiedAssociation = namespace.iri("qualifiedAssociation")
    val qualifiedEnd = namespace.iri("qualifiedEnd")
    val qualifiedDelegation = namespace.iri("qualifiedDelegation")
    val hadUsage = namespace.iri("hadUsage")
    val hadGeneration = namespace.iri("hadGeneration")
    val hadPlan = namespace.iri("hadPlan")
    val hadActivity = namespace.iri("hadActivity")
    val atTime = namespace.iri("atTime")
    val hadRole = namespace.iri("hadRole")
    @get:JvmName("entity")
    val entity = namespace.iri("entity")
    @get:JvmName("activity")
    val activity = namespace.iri("activity")
    @get:JvmName("agent")
    val agent = namespace.iri("agent")

    val mentionOf = namespace.iri("mentionOf")
    val asInBundle = namespace.iri("asInBundle")

    val qualifications = mapOf(
        wasGeneratedBy to qualifiedGeneration,
        wasDerivedFrom to qualifiedDerivation,
        hadPrimarySource to qualifiedPrimarySource,
        wasQuotedFrom to qualifiedQuotation,
        wasRevisionOf to qualifiedRevision,
        wasAttributedTo to qualifiedAttribution,
        wasInvalidatedBy to qualifiedInvalidation,
        wasStartedBy to qualifiedStart,
        used to qualifiedUsage,
        wasInformedBy to qualifiedCommunication,
        wasAssociatedWith to qualifiedAssociation,
        wasEndedBy to qualifiedEnd,
        actedOnBehalfOf to qualifiedDelegation
    )
}