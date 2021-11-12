package io.docrozza.provenance

import com.stardog.stark.Resource
import io.docrozza.provenance.model.*
import io.docrozza.provenance.model.Collection
import java.time.Instant
import java.util.*

/**
 * Visitor for handling provenance entities in a model
 */
abstract class ProvenanceScanner {

    open fun onNonProvenance(nonProvenance: NonProvenance) {}

    open fun onAgent(agent: Agent) {}

    open fun onAgent(agent: Resource) {}

    open fun onDelegation(agent: Agent, delegation: AgentOrDelegation) {}

    open fun onEntity(entity: Entity) {}

    open fun onEntity(entity: Resource) {}

    open fun onAttribution(entity: Entity, attribution: AgentOrAttribution) {}

    open fun onGeneration(entity: Entity, generator: ActivityOrGeneration) {}

    open fun onAlternative(entity: Entity, altEntity: EntityOrRef) {}

    open fun onSpecialization(entity: Entity, specEntity: EntityOrRef) {}

    open fun onDerivation(entity: Entity, derivation: EntityOrDerivation) {}

    open fun onQuotation(entity: Entity, quotation: EntityOrQuotation) {}

    open fun onRevision(entity: Entity, revision: EntityOrRevision) {}

    open fun onSourcing(entity: Entity, source: EntityOrPrimarySource) {}

    open fun onInvalidated(entity: Entity, invalidation: ActivityOrInvalidation) {}

    open fun onPlan(plan: Plan) {}

    open fun onPlan(plan: Resource) {}

    open fun onBundle(bundle: Bundle) {}

    open fun onBundle(bundle: Resource) {}

    open fun onLink(bundle: Bundle, link: Link) {}

    open fun onItem(bundle: Bundle, item: Identifiable) {}

    open fun onCollection(collection: Collection) {}

    open fun onItem(collection: Collection, item: EntityOrRef) {}

    open fun onActivity(activity: Activity) {}

    open fun onActivity(activity: Resource) {}

    open fun onGeneration(activity: Activity, generated: EntityOrRef) {}

    open fun onUsage(activity: Activity, usage: EntityOrUsage) {}

    open fun onInvalidation(activity: Activity, invalidation: EntityOrRef) {}

    open fun onCommunication(activity: Activity, communication: ActivityOrCommunication) {}

    open fun onStart(activity: Activity, start: EntityOrStart) {}

    open fun onEnd(activity: Activity, end: EntityOrEnd) {}

    open fun onAssociation(activity: Activity, association: AgentOrAssociation) {}

    open fun onInfluence(influence: Influence<*>, influencer: RefOrValue<*>) {}

    open fun onUsage(derivation: Derivation, usage: UsageOrRef) {}

    open fun onGeneration(derivation: Derivation, generation: GenerationOrRef) {}

    open fun onPlan(association: Association, plan: PlanOrRef) {}

    open fun onRole(influence: Influence<*>, role: RoleOrRef) {}

    open fun onActivity(influence: Influence<*>, activity: ActivityOrRef) {}

    open fun onEvent(event: InstantaneousEvent, instant: Instant) {}

    open fun onLocation(locatable: Locatable, location: LocationOrRef) {}

    open fun onLocation(location: Location) {}

    open fun onLocation(location: Resource) {}

    open fun onRole(role: Role) {}

    open fun onRole(role: Resource) {}

    private class Scope(val bundle: Bundle) {

        val refs = mutableMapOf<Resource, RefOrValue<*>>()

        val objects = mutableSetOf<Identifiable>()

        fun done() : Sequence<RefOrValue<*>> {
            // remove any refs that we have visited as 'true' object before notifying
            objects.forEach { refs.remove(it.id) }
            return refs.values.asSequence()
        }
    }

    private val bundles = LinkedList<Scope>()

    protected fun currentBundle() : Bundle {
        return scope().bundle
    }

    /**
     * Scans a [bundle]
     *
     * For each item in the bundle, the corresponding **onXYZ** method is called, ex. for each [Entity], [onEntity] will be called.
     */
    fun scan(bundle: Bundle) {
        visitEntity(bundle)
    }

    private fun scope() : Scope {
        return bundles.peek()
    }

    private fun visit(item: Identifiable) {
        when (item) {
            is RefOrValue<*> -> visitRef(item)
            is Referencable<*, *> -> visitReferencable(item)
            is NonProvenance -> visitNonProvenance(item)
            is ProvenanceObject -> {
                if (scope().objects.add(item)) {
                    when (item) {
                        is Influence<*> -> visitInfluence(item)
                        is Role -> onRole(item)
                        is Location -> onLocation(item)
                        is Entity -> visitEntity(item)
                        is Activity -> visitActivity(item)
                        is Agent -> visitAgent(item)
                        else -> Unit
                    }

                    if (item is Locatable) {
                        visitProperty(item.atLocation) { onLocation(item, it) }
                    }

                    visitProperties(item.properties)
                }
            }
            else -> Unit
        }

//        if (item is Locatable) {
//            visitProperty(item.atLocation) { onLocation(item, it) }
//        }
    }

    private fun onRef(id: Resource, kind: Kind) {
        when (kind) {
            Kind.Bundle -> onBundle(id)
            Kind.Plan -> onPlan(id)
            Kind.Entity -> onEntity(id)
            Kind.Agent -> onAgent(id)
            Kind.Activity -> onActivity(id)
            Kind.Role -> onRole(id)
            Kind.Location -> onLocation(id)
            else -> Unit
        }
    }

    private fun visitNonProvenance(item: NonProvenance) {
        if (scope().objects.add(item)) {
            onNonProvenance(item)
            visitProperties(item.properties)
        }
    }

    private fun visitProperties(properties: Sequence<Attribute>) {
        properties.filterIsInstance(Identifiable::class.java).forEach { visit(it) }
    }

    private fun visitReferencable(item: Referencable<*, *>) {
        when {
            item.isRef -> visitRef(item)
            item.isQualified -> visit(item.qualification())
            else -> visit(item.about())
        }
    }

    private fun visitRef(item: RefOrValue<*>) {
        if (item.isRef) {
            scope().refs[item.id] = item
        } else {
            visit(item.about())
        }
    }

    private fun visitInfluence(influence: Influence<*>) {
        influence.influencer().apply {
            onInfluence(influence, this)
            visit(this)
        }

        visitProperty(influence.hadRole) { onRole(influence, it) }

        when (influence) {
            is ActivityInfluence -> visitProperty(influence.activity) { onActivity(influence, it) }
            is AgentInfluence -> visitProperty(influence.hadActivity) { onActivity(influence, it) }
            is EntityInfluence -> visitProperty(influence.hadActivity) { onActivity(influence, it) }
        }

        if (influence is InstantaneousEvent) {
            influence.atTime?.apply { onEvent(influence, this) }
        }

        if (influence is Derivation) {
            visitProperty(influence.hadUsage) { onUsage(influence, it) }
            visitProperty(influence.hadGeneration) { onGeneration(influence, it) }
        } else if (influence is Association) {
            visitProperty(influence.hadPlan) { onPlan(influence, it) }
        }
    }

    private fun visitAgent(agent: Agent) {
        onAgent(agent)
        visitInfluences(agent.actedOnBehalfOf) { onDelegation(agent, it) }
    }

    private fun visitEntity(entity: Entity) {
        when (entity) {
            is Plan -> onPlan(entity)
            is Collection -> onCollection(entity)
            is Bundle -> {
                bundles.push(Scope(entity))
                onBundle(entity)
                visitProperty(entity.atLocation) { onLocation(entity, it) }
            }
            else -> onEntity(entity)
        }

        visitProperty(entity.specializationOf) { onSpecialization(entity, it) }
        visitProperty(entity.alternateOf) { onAlternative(entity, it) }
        visitProperty(entity.wasAttributedTo) { onAttribution(entity, it) }
        visitProperty(entity.wasGeneratedBy) { onGeneration(entity, it) }
        visitProperty(entity.wasDerivedFrom) { onDerivation(entity, it) }
        visitProperty(entity.hadPrimarySource) { onSourcing(entity, it) }
        visitProperty(entity.wasInvalidatedBy) { onInvalidated(entity, it) }
        visitProperty(entity.wasQuotedFrom) { onQuotation(entity, it) }
        visitProperty(entity.wasRevisionOf) { onRevision(entity, it) }

        when (entity) {
            is Collection -> visitSequence(entity.hadMembers) { onItem(entity, it) }
            is Bundle -> {
                visitSequence(entity.items) { onItem(entity, it) }
                entity.links.forEach {
                    visit(it.subject)
                    onLink(entity, it)

                    val otherRef = it.asInBundle
                    if (!otherRef.isRef) {
                        visitEntity(otherRef.about())
                    }
                }

                // need to peek to keep the bundle in scope for the onRef calls
                val scope = bundles.peek()
                scope.done().forEach { onRef(it.id, it.kind) }
                bundles.pop()
            }
            else -> Unit
        }
    }

    private fun visitActivity(activity: Activity) {
        onActivity(activity)

        visitProperty(activity.generated) { onGeneration(activity, it) }
        visitProperty(activity.wasStartedBy) { onStart(activity, it) }
        visitProperty(activity.wasEndedBy) { onEnd(activity, it) }

        visitInfluences(activity.wasInformedBy) { onCommunication(activity, it) }
        visitInfluences(activity.wasAssociatedWith) { onAssociation(activity, it) }
        visitInfluences(activity.used) { onUsage(activity, it) }
        visitRefs(activity.invalidated) { onInvalidation(activity, it) }
    }

    private fun <V : Identifiable> visitSequence(values: Sequence<V>, handler: (V) -> Unit) {
        values.forEach {
            handler(it)
            visit(it)
        }
    }

    private fun <V : Identifiable, R: RefOrValue<V>> visitRefs(values: Sequence<R>, handler: (R) -> Unit) {
        values.forEach { visitProperty(it, handler) }
    }

    private fun <V : Identifiable, I : Influence<V>, R: Referencable<V, I>> visitInfluences(values: Sequence<R>, handler: (R) -> Unit) {
        values.forEach {
            handler(it)
            visitReferencable(it)
        }
    }

    private fun <T : Identifiable, R: RefOrValue<T>> visitProperty(ref: R?, handler: (R) -> Unit) {
        if (ref != null) {
            handler(ref)
            visitRef(ref)
        }
    }

    private fun <V : Identifiable, I : Influence<V>, R: Referencable<V, I>> visitProperty(ref: R?, handler: (R) -> Unit) {
        if (ref != null) {
            handler(ref)
            visitReferencable(ref)
        }
    }
}