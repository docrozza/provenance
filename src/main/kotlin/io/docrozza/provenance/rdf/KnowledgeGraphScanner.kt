package io.docrozza.provenance.rdf

import com.complexible.stardog.api.Adder
import com.complexible.stardog.api.Connection
import com.stardog.stark.*
import com.stardog.stark.vocabs.RDF
import io.docrozza.provenance.*
import io.docrozza.provenance.model.Collection
import io.docrozza.provenance.model.*
import io.docrozza.provenance.rdf.StardogUtils.use
import java.time.Instant
import java.util.function.Supplier

/**
 * Scans a provenance [Bundle] and adds it to Stardog via a [Connection]
 */
class KnowledgeGraphScanner(private val adder: Adder) : ProvenanceScanner() {

    companion object {
        /**
         * Add the [baseBundle] to Stardog using the provided [connectionFactory]
         */
        fun addToGraph(baseBundle: Bundle, connectionFactory: Supplier<Connection>) {
            connectionFactory.use {
                KnowledgeGraphScanner(it.add()).apply { scan(baseBundle) }
            }
        }
    }

    override fun onAgent(agent: Resource) {
        add(agent, Kind.Agent.toIRI())
    }

    override fun onActivity(activity: Resource) {
        add(activity, Kind.Activity.toIRI())
    }

    override fun onEntity(entity: Resource) {
        add(entity, Kind.Entity.toIRI())
    }

    override fun onPlan(plan: Resource) {
        add(plan, Kind.Plan.toIRI())
    }

    override fun onRole(role: Resource) {
        add(role, Kind.Role.toIRI())
    }

    override fun onLocation(location: Resource) {
        add(location, Kind.Location.toIRI())
    }

    override fun onBundle(bundle: Resource) {
        add(bundle, Kind.Bundle.toIRI())
    }

    override fun onItem(collection: Collection, item: EntityOrRef) {
        add(collection.id, Provenance.hadMember, item)
    }

    override fun onNonProvenance(nonProvenance: NonProvenance) {
        add(nonProvenance.id, nonProvenance.kindIRI)
        addAll(nonProvenance.id, nonProvenance.properties)
    }

    override fun onLocation(locatable: Locatable, location: LocationOrRef) {
        add(locatable.id, Provenance.atLocation, location)
    }

    override fun onLocation(location: Location) {
        add(location)
    }

    override fun onRole(role: Role) {
        add(role)
    }

    override fun onLink(bundle: Bundle, link: Link) {
        val id = link.subject.id
        add(id, Provenance.mentionOf, link.mentionOf)
        add(id, Provenance.asInBundle, link.asInBundle.id)
    }

    override fun onAgent(agent: Agent) {
        val id = add(agent)
        add(id, agent.type.toIRI())
    }

    override fun onDelegation(agent: Agent, delegation: AgentOrDelegation) {
        add(agent.id, Provenance.actedOnBehalfOf, Provenance.qualifiedDelegation, delegation)
    }

    override fun onActivity(activity: Activity) {
        val id = add(activity)
        addTime(id, Provenance.startedAtTime, activity.startedAtTime)
        addTime(id, Provenance.endedAtTime, activity.endedAtTime)
    }

    override fun onAssociation(activity: Activity, association: AgentOrAssociation) {
        add(activity.id, Provenance.wasAssociatedWith, Provenance.qualifiedAssociation, association)
    }

    override fun onGeneration(activity: Activity, generated: EntityOrRef) {
        add(activity.id, Provenance.generated, generated)
    }

    override fun onInvalidation(activity: Activity, invalidation: EntityOrRef) {
        add(activity.id, Provenance.invalidated, invalidation)
    }

    override fun onCommunication(activity: Activity, communication: ActivityOrCommunication) {
        add(activity.id, Provenance.wasInformedBy, Provenance.qualifiedCommunication, communication)
    }

    override fun onStart(activity: Activity, start: EntityOrStart) {
        add(activity.id, Provenance.wasStartedBy, Provenance.qualifiedStart, start)
    }

    override fun onEnd(activity: Activity, end: EntityOrEnd) {
        add(activity.id, Provenance.wasEndedBy, Provenance.qualifiedEnd, end)
    }

    override fun onUsage(activity: Activity, usage: EntityOrUsage) {
        add(activity.id, Provenance.used, Provenance.qualifiedUsage, usage)
    }

    override fun onEntity(entity: Entity) {
        doEntity(entity)
    }

    override fun onPlan(plan: Plan) {
        doEntity(plan)
    }

    override fun onCollection(collection: Collection) {
        doEntity(collection)

        if (collection.hadMembers.count() == 0) {
            add(collection.id, Provenance.EmptyCollection)
        }
    }

    override fun onBundle(bundle: Bundle) {
        doEntity(bundle)
    }

    override fun onInfluence(influence: Influence<*>, influencer: RefOrValue<*>) {
        add(influence)
        add(influencer)
        add(influence.id, influence.influenceIRI, influencer.id)
    }

    override fun onEvent(event: InstantaneousEvent, instant: Instant) {
        addTime(event.id, Provenance.atTime, event.atTime)
    }

    override fun onRole(influence: Influence<*>, role: RoleOrRef) {
        add(influence.id, Provenance.hadRole, role.id)
    }

    override fun onActivity(influence: Influence<*>, activity: ActivityOrRef) {
        val property = if (influence is ActivityInfluence) Provenance.activity else Provenance.hadActivity
        add(influence.id, property, activity.id)
    }

    override fun onUsage(derivation: Derivation, usage: UsageOrRef) {
        add(derivation.id, Provenance.hadUsage, usage.id)
    }

    override fun onGeneration(derivation: Derivation, generation: GenerationOrRef) {
        add(derivation.id, Provenance.hadGeneration, generation.id)
    }

    override fun onPlan(association: Association, plan: PlanOrRef) {
        add(association.id, Provenance.hadPlan, plan.id)
    }

    override fun onAlternative(entity: Entity, altEntity: EntityOrRef) {
        add(entity.id, Provenance.alternateOf, altEntity.id)
    }

    override fun onSpecialization(entity: Entity, specEntity: EntityOrRef) {
        add(entity.id, Provenance.specializationOf, specEntity.id)
    }

    override fun onAttribution(entity: Entity, attribution: AgentOrAttribution) {
        add(entity.id, Provenance.wasAttributedTo, Provenance.qualifiedAttribution, attribution)
    }

    override fun onGeneration(entity: Entity, generator: ActivityOrGeneration) {
        add(entity.id, Provenance.wasGeneratedBy, Provenance.qualifiedGeneration, generator)
    }

    override fun onDerivation(entity: Entity, derivation: EntityOrDerivation) {
        add(entity.id, Provenance.wasDerivedFrom, Provenance.qualifiedDerivation, derivation)
    }

    override fun onSourcing(entity: Entity, source: EntityOrPrimarySource) {
        add(entity.id, Provenance.hadPrimarySource, Provenance.qualifiedPrimarySource, source)
    }

    override fun onInvalidated(entity: Entity, invalidation: ActivityOrInvalidation) {
        add(entity.id, Provenance.wasInvalidatedBy, Provenance.qualifiedInvalidation, invalidation)
    }

    override fun onQuotation(entity: Entity, quotation: EntityOrQuotation) {
        add(entity.id, Provenance.wasQuotedFrom, Provenance.qualifiedQuotation, quotation)
    }

    override fun onRevision(entity: Entity, revision: EntityOrRevision) {
        add(entity.id, Provenance.wasRevisionOf, Provenance.qualifiedRevision, revision)
    }

    private fun doEntity(entity: Entity) {
        val id = add(entity)
        addAll(id, entity.properties)
        addTime(id, Provenance.generatedAtTime, entity.generatedAtTime)
        addTime(id, Provenance.invalidatedAtTime, entity.invalidatedAtTime)
    }

    private fun add(resource: Resource, type: IRI) {
        add(resource, RDF.TYPE, type)
    }

    private fun add(item: Identifiable) : Resource {
        return if (item is ProvenanceObject) {
            add(item)
        } else {
            add(item.id, item.kindIRI)
            item.id
        }
    }

    private fun add(item: ProvenanceObject) : Resource {
        val id = item.id
        add(id, item.kindIRI)
        addAll(id, item.properties)
        return id
    }

    private fun add(resource: Resource, property: IRI, value: Value) {
        adder.statement(resource, property, value, currentBundle().id)
    }

    private fun addAll(resource: Resource, attributes: Sequence<Attribute>) {
        attributes.forEach { add(resource, it.property, it.value) }
    }

    private fun add(resource: Resource, property: IRI, item: RefOrValue<*>) {
        add(resource, property, item.id)
        if (item.isRef) {
            add(item)
        } else {
            add(item.about())
        }
    }

    private fun add(resource: Resource, property: IRI, qualifiedProperty: IRI, item: Referencable<*, *>) {
        if (item.isQualified) {
            add(resource, property, item.toRef().id)
            add(resource, qualifiedProperty, add(item.qualification()))
        } else {
            add(resource, property, add(item))
        }
    }

    private fun addTime(resource: Resource, property: IRI, instant: Instant?) {
        if (instant != null) {
            add(resource, property, StardogUtils.toLiteral(instant))
        }
    }
}