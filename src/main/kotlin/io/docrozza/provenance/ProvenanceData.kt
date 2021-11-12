package io.docrozza.provenance

import com.stardog.stark.Resource
import com.stardog.stark.IRI
import io.docrozza.provenance.model.*
import io.docrozza.provenance.model.Collection
import java.time.Instant

data class RefData<V : ProvenanceObject>(override val id: Resource, override val kind: Kind) : RefOrValue<V> {
    override val isRef = true

    override fun about(): V {
        error("This is a reference to an object and not the object itself")
    }
}

data class ValueData<V : ProvenanceObject>(private val value: V) : RefOrValue<V> {
    override val isRef = false
    override val id = value.id
    override val kind = value.kind
    override fun about() = value
}

data class NonProvenanceData(
    override val id: Resource,
    override val kindIRI: IRI,
    override val properties: Sequence<Attribute> = emptySequence()) : NonProvenance

data class LocationData(
    override val id: Resource,
    override val properties: Sequence<Attribute> = emptySequence()) : Location

data class RoleData(
    override val id: Resource,
    override val properties: Sequence<Attribute> = emptySequence()) : Role

data class EntityData(
    override val id: IRI,
    override val properties: Sequence<Attribute> = emptySequence(),
    override val atLocation: LocationOrRef? = null,
    override val generatedAtTime: Instant? = null,
    override val invalidatedAtTime: Instant? = null,
    override val wasAttributedTo: AgentOrAttribution? = null,
    override val wasGeneratedBy: ActivityOrGeneration? = null,
    override val wasDerivedFrom: EntityOrDerivation? = null,
    override val alternateOf: EntityOrRef? = null,
    override val specializationOf: EntityOrRef? = null,
    override val hadPrimarySource: EntityOrPrimarySource? = null,
    override val wasInvalidatedBy: ActivityOrInvalidation? = null,
    override val wasQuotedFrom: EntityOrQuotation? = null,
    override val wasRevisionOf: EntityOrRevision? = null) : Entity

data class PlanData(
    override val id: IRI,
    override val properties: Sequence<Attribute> = emptySequence(),
    override val atLocation: LocationOrRef? = null,
    override val generatedAtTime: Instant? = null,
    override val invalidatedAtTime: Instant? = null,
    override val wasAttributedTo: AgentOrAttribution? = null,
    override val wasGeneratedBy: ActivityOrGeneration? = null,
    override val wasDerivedFrom: EntityOrDerivation? = null,
    override val alternateOf: EntityOrRef? = null,
    override val specializationOf: EntityOrRef? = null,
    override val hadPrimarySource: EntityOrPrimarySource? = null,
    override val wasInvalidatedBy: ActivityOrInvalidation? = null,
    override val wasQuotedFrom: EntityOrQuotation? = null,
    override val wasRevisionOf: EntityOrRevision? = null) : Plan

data class CollectionData(
    override val id: IRI,
    override val hadMembers: Sequence<EntityOrRef> = emptySequence(),
    override val properties: Sequence<Attribute> = emptySequence(),
    override val atLocation: LocationOrRef? = null,
    override val generatedAtTime: Instant? = null,
    override val invalidatedAtTime: Instant? = null,
    override val wasAttributedTo: AgentOrAttribution? = null,
    override val wasGeneratedBy: ActivityOrGeneration? = null,
    override val wasDerivedFrom: EntityOrDerivation? = null,
    override val alternateOf: EntityOrRef? = null,
    override val specializationOf: EntityOrRef? = null,
    override val hadPrimarySource: EntityOrPrimarySource? = null,
    override val wasInvalidatedBy: ActivityOrInvalidation? = null,
    override val wasQuotedFrom: EntityOrQuotation? = null,
    override val wasRevisionOf: EntityOrRevision? = null) : Collection

data class ActivityData(
    override val id: IRI,
    override val properties: Sequence<Attribute> = emptySequence(),
    override val atLocation: LocationOrRef? = null,
    override val generated: EntityOrRef? = null,
    override val wasAssociatedWith: Sequence<AgentOrAssociation> = emptySequence(),
    override val used: Sequence<EntityOrUsage> = emptySequence(),
    override val invalidated: Sequence<EntityOrRef> = emptySequence(),
    override val wasInformedBy: Sequence<ActivityOrCommunication> = emptySequence(),
    override val wasStartedBy: EntityOrStart? = null,
    override val startedAtTime: Instant? = null,
    override val wasEndedBy: EntityOrEnd? = null,
    override val endedAtTime: Instant? = null) : Activity

data class AgentData(
    override val id: IRI,
    override val type: Agent.Type = Agent.Type.Agent,
    override val properties: Sequence<Attribute> = emptySequence(),
    override val atLocation: LocationOrRef? = null,
    override val actedOnBehalfOf: Sequence<AgentOrDelegation> = emptySequence()) : Agent

data class StartData(
    override val id: Resource,
    override val entity: EntityOrRef,
    override val properties: Sequence<Attribute> = emptySequence(),
    override val atLocation: LocationOrRef? = null,
    override val atTime: Instant? = null,
    override val hadRole: RoleOrRef? = null,
    override val hadActivity: ActivityOrRef?) : Start

data class EndData(
    override val id: Resource,
    override val entity: EntityOrRef,
    override val properties: Sequence<Attribute> = emptySequence(),
    override val atLocation: LocationOrRef? = null,
    override val atTime: Instant? = null,
    override val hadRole: RoleOrRef? = null,
    override val hadActivity: ActivityOrRef?) : End

data class UsageData(
    override val id: Resource,
    override val entity: EntityOrRef,
    override val properties: Sequence<Attribute> = emptySequence(),
    override val atLocation: LocationOrRef? = null,
    override val atTime: Instant? = null,
    override val hadRole: RoleOrRef? = null,
    override val hadActivity: ActivityOrRef? = null) : Usage

data class DerivationData(
    override val id: Resource,
    override val entity: EntityOrRef,
    override val properties: Sequence<Attribute> = emptySequence(),
    override val atLocation: LocationOrRef? = null,
    override val hadRole: RoleOrRef? = null,
    override val hadActivity: ActivityOrRef,
    override val hadUsage: UsageOrRef? = null,
    override val hadGeneration: GenerationOrRef? = null) : Derivation

data class PrimarySourceData(
    override val id: Resource,
    override val entity: EntityOrRef,
    override val properties: Sequence<Attribute> = emptySequence(),
    override val atLocation: LocationOrRef? = null,
    override val hadRole: RoleOrRef? = null,
    override val hadActivity: ActivityOrRef,
    override val hadUsage: UsageOrRef? = null,
    override val hadGeneration: GenerationOrRef? = null) : PrimarySource

data class QuotationData(
    override val id: Resource,
    override val entity: EntityOrRef,
    override val properties: Sequence<Attribute> = emptySequence(),
    override val atLocation: LocationOrRef? = null,
    override val hadRole: RoleOrRef? = null,
    override val hadActivity: ActivityOrRef,
    override val hadUsage: UsageOrRef? = null,
    override val hadGeneration: GenerationOrRef? = null) : Quotation

data class RevisionData(
    override val id: Resource,
    override val entity: EntityOrRef,
    override val properties: Sequence<Attribute> = emptySequence(),
    override val atLocation: LocationOrRef? = null,
    override val hadRole: RoleOrRef? = null,
    override val hadActivity: ActivityOrRef,
    override val hadUsage: UsageOrRef? = null,
    override val hadGeneration: GenerationOrRef? = null) : Revision

data class GenerationData(
    override val id: Resource,
    override val activity: ActivityOrRef,
    override val properties: Sequence<Attribute> = emptySequence(),
    override val atLocation: LocationOrRef? = null,
    override val atTime: Instant? = null,
    override val hadRole: RoleOrRef? = null) : Generation

data class InvalidationData(
    override val id: Resource,
    override val activity: ActivityOrRef,
    override val properties: Sequence<Attribute> = emptySequence(),
    override val atLocation: LocationOrRef? = null,
    override val atTime: Instant? = null,
    override val hadRole: RoleOrRef? = null) : Invalidation

data class CommunicationData(
    override val id: Resource,
    override val activity: ActivityOrRef,
    override val properties: Sequence<Attribute> = emptySequence(),
    override val atLocation: LocationOrRef? = null,
    override val hadRole: RoleOrRef? = null) : Communication

data class DelegationData(
    override val id: Resource,
    override val agent: AgentOrRef,
    override val properties: Sequence<Attribute> = emptySequence(),
    override val atLocation: LocationOrRef? = null,
    override val hadRole: RoleOrRef? = null,
    override val hadActivity: ActivityOrRef? = null) : Delegation

data class AttributionData(
    override val id: Resource,
    override val agent: AgentOrRef,
    override val properties: Sequence<Attribute> = emptySequence(),
    override val atLocation: LocationOrRef? = null,
    override val hadRole: RoleOrRef? = null,
    override val hadActivity: ActivityOrRef? = null) : Attribution

data class AssociationData(
    override val id: Resource,
    override val agent: AgentOrRef,
    override val properties: Sequence<Attribute> = emptySequence(),
    override val atLocation: LocationOrRef? = null,
    override val hadRole: RoleOrRef? = null,
    override val hadActivity: ActivityOrRef? = null,
    override val hadPlan: PlanOrRef? = null) : Association

data class BundleData(
    override val id: Resource,
    override val items: Sequence<Identifiable> = emptySequence(),
    override val links: Sequence<Link> = emptySequence(),
    override val properties: Sequence<Attribute> = emptySequence(),
    override val atLocation: LocationOrRef? = null,
    override val generatedAtTime: Instant? = null,
    override val invalidatedAtTime: Instant? = null,
    override val wasAttributedTo: AgentOrAttribution? = null,
    override val wasGeneratedBy: ActivityOrGeneration? = null,
    override val wasDerivedFrom: EntityOrDerivation? = null,
    override val alternateOf: EntityOrRef? = null,
    override val specializationOf: EntityOrRef? = null,
    override val hadPrimarySource: EntityOrPrimarySource? = null,
    override val wasInvalidatedBy: ActivityOrInvalidation? = null,
    override val wasQuotedFrom: EntityOrQuotation? = null,
    override val wasRevisionOf: EntityOrRevision? = null) : Bundle

data class LinkData(
    override val subject: ThingOrRef,
    override val mentionOf: IRI,
    override val asInBundle: BundleOrRef) : Link
