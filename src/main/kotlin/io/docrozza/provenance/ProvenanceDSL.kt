@file:Suppress("unused", "MemberVisibilityCanBePrivate")
package io.docrozza.provenance

import com.stardog.stark.IRI
import com.stardog.stark.Resource
import com.stardog.stark.Value
import com.stardog.stark.Values
import com.stardog.stark.vocabs.RDF
import io.docrozza.provenance.model.*
import io.docrozza.provenance.model.Collection
import io.docrozza.provenance.rdf.StardogUtils
import kotlin.collections.HashSet
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.*
import javax.xml.datatype.Duration
import javax.xml.datatype.XMLGregorianCalendar

private typealias RefType = Pair<Resource, Kind>

// not IRIs in the prov ontology but used as a private properties
private val bundleItem = Values.iri(Provenance.namespace, "bundleItem")
private val bundleLinks = Values.iri(Provenance.namespace, "bundleLinks")

@DslMarker
annotation class ProvenanceDsl

/**
 * Base builder for constructing Provenance models via a DSL
 */
abstract class DataBuilder<V> {

    val children = mutableMapOf<IRI, Any>()

    abstract fun build() : V

    protected fun check(field: IRI) : Boolean {
        return children[field] != null
    }

    protected fun <V> ensureValue(value: V?, message: String) : V {
        return value ?: error(message)
    }

    @Suppress("UNCHECKED_CAST")
    protected fun <V> value(field: IRI) : V? {
        return children[field] as V
    }

    @Suppress("UNCHECKED_CAST")
    protected fun values(field: IRI) : MutableSet<Any> {
        return children.compute(field) { _, v -> v ?: mutableSetOf<Any>()} as MutableSet<Any>
    }

    protected fun set(field: IRI, value: Any) {
        children[field] = value
    }

    protected fun add(field: IRI, value: Any) {
        values(field).add(value)
    }

    protected inline fun <reified V : ProvenanceObject> refOrValue(field: IRI) : RefOrValue<V>? {
        val result = children[field]
        return if (result == null) null else asRefOrValue(result)
    }

    protected inline fun <reified V : ProvenanceObject> refOrValues(field: IRI) : Sequence<RefOrValue<V>> {
        return values(field).map { asRefOrValue<V>(it) }.asSequence()
    }

    @Suppress("UNCHECKED_CAST")
    protected inline fun <reified V : ProvenanceObject, reified I : Influence<V>> referencable(field: IRI) : Referencable<V, I>? {
        val result = children[field]
        return if (result == null) null else asReferencable(result)
    }

    @Suppress("UNCHECKED_CAST")
    protected inline fun <reified V : ProvenanceObject, reified I : Influence<V>> referencables(field: IRI) : Sequence<Referencable<V, I>> {
        return values(field).map { asReferencable<V, I>(it) }.asSequence()
    }

    @Suppress("UNCHECKED_CAST")
    protected inline fun <reified V : ProvenanceObject> asRefOrValue(v: Any) : RefOrValue<V> {
        return when (v) {
            is V -> ValueData(v)
            else -> {
                // must be a ref point now
                val p = v as RefType
                RefData(p.first, p.second)
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    protected inline fun <reified V : ProvenanceObject, reified I : Influence<V>> asReferencable(v: Any) : Referencable<V, I> {
        return when (v) {
            is I -> Referencable.Qualification(v)
            is V -> Referencable.Value(v)
            else -> {
                // must be a ref point now
                val p = v as RefType
                Referencable.Ref(p.first, p.second)
            }
        }
    }
}

abstract class ObjectWithAttributesBuilder<V>(val id: Resource) : DataBuilder<V>() {

    private val props = mutableListOf<Attribute>()

    fun attribute(property: IRI, value: Value) {
        props.add(Attribute(property, value))
    }

    fun attribute(property: IRI, value: Boolean) {
        props.add(Attribute(property, Values.literal(value)))
    }

    fun attribute(property: IRI, value: Number) {
        props.add(Attribute(property, Values.literal(value)))
    }

    fun attribute(property: IRI, value: String) {
        props.add(Attribute(property, Values.literal(value)))
    }

    fun attribute(property: IRI, value: Duration) {
        props.add(Attribute(property, Values.literal(value)))
    }

    fun attribute(property: IRI, value: GregorianCalendar) {
        props.add(Attribute(property, Values.literal(value)))
    }

    fun attribute(property: IRI, value: XMLGregorianCalendar) {
        props.add(Attribute(property, Values.literal(value)))
    }

    fun attribute(property: IRI, value: Instant, zone: ZoneId = ZoneOffset.UTC) {
        props.add(Attribute(property, StardogUtils.toLiteral(value, zone)))
    }

    fun attributes() : Sequence<Attribute> {
        return props.asSequence()
    }
}

@ProvenanceDsl
class NonProvenanceBuilder(id: Resource, private val kind: IRI) : ObjectWithAttributesBuilder<NonProvenance>(id) {
    override fun build() : NonProvenance {
        return NonProvenanceData(id, kind, attributes())
    }
}

@ProvenanceDsl
class LocationBuilder(id: Resource) : ObjectWithAttributesBuilder<Location>(id) {
    override fun build() : Location {
        return LocationData(id, attributes())
    }
}

@ProvenanceDsl
class RoleBuilder(id: Resource) : ObjectWithAttributesBuilder<Role>(id) {
    override fun build() : Role {
        return RoleData(id, attributes())
    }
}

@ProvenanceDsl
abstract class InfluenceBuilder<V : ProvenanceObject, I: Influence<V>>(id: Resource) : ObjectWithAttributesBuilder<I>(id) {
    fun atLocation(thing: Resource) {
        set(Provenance.atLocation, RefType(thing, Kind.Location))
    }

    fun atLocation(thing: Resource, block: LocationBuilder.() -> Unit) : Location {
        val location = LocationBuilder(thing).apply(block).build()
        atLocation(location)
        return location
    }

    fun atLocation(location: Location) {
        set(Provenance.atLocation, location)
    }

    fun hadRole(thing: Resource) {
        set(Provenance.hadRole, RefType(thing, Kind.Role))
    }

    fun hadRole(thing: Resource, block: RoleBuilder.() -> Unit) : Role {
        val role = RoleBuilder(thing).apply(block).build()
        hadRole(role)
        return role
    }

    fun hadRole(role: Role) {
        set(Provenance.hadRole, role)
    }
}

@ProvenanceDsl
abstract class NonActivityInfluenceBuilder<V : ProvenanceObject, I: Influence<V>>(id: Resource) : InfluenceBuilder<V, I>(id) {
    fun hadActivity(thing: IRI) {
        set(Provenance.hadActivity, RefType(thing, Kind.Activity))
    }

    fun hadActivity(thing: IRI, block: ActivityBuilder.() -> Unit) : Activity {
        val activity = ActivityBuilder(thing).apply(block).build()
        hadActivity(activity)
        return activity
    }

    fun hadActivity(activity: Activity) {
        set(Provenance.hadActivity, activity)
    }
}

@ProvenanceDsl
abstract class AgentInfluenceBuilder<V : AgentInfluence>(id: Resource) : NonActivityInfluenceBuilder<Agent, V>(id) {
    fun agent(thing: IRI) {
        set(Provenance.agent, RefType(thing, Kind.Agent))
    }

    fun agent(thing: IRI, block: AgentBuilder.() -> Unit) : Agent {
        val agent = AgentBuilder(thing).apply(block).build()
        agent(agent)
        return agent
    }

    fun agent(agent: Agent) {
        set(Provenance.agent, agent)
    }
}

@ProvenanceDsl
abstract class ActivityInfluenceBuilder<V : ActivityInfluence>(id: Resource) : InfluenceBuilder<Activity, V>(id) {
    fun activity(thing: IRI) {
        set(Provenance.activity, RefType(thing, Kind.Activity))
    }

    fun activity(thing: IRI, block: ActivityBuilder.() -> Unit) : Activity {
        val activity = ActivityBuilder(thing).apply(block).build()
        activity(activity)
        return activity
    }

    fun activity(activity: Activity) {
        set(Provenance.activity, activity)
    }
}

@ProvenanceDsl
abstract class EntityInfluenceBuilder<V: EntityInfluence>(id: Resource) : NonActivityInfluenceBuilder<Entity, V>(id) {
    fun entity(thing: IRI) {
        set(Provenance.entity, RefType(thing, Kind.Entity))
    }

    fun entity(thing: IRI, block: EntityBuilder.() -> Unit) : Entity {
        val entity = EntityBuilder(thing).apply(block).build()
        entity(entity)
        return entity
    }

    fun entity(entity: Entity) {
        set(Provenance.entity, entity)
    }
}

@ProvenanceDsl
class AttributionBuilder(id: Resource) : AgentInfluenceBuilder<Attribution>(id) {
    override fun build() : Attribution {
        return AttributionData(id,
            agent = ensureValue(refOrValue(Provenance.agent), "No qualifier specified for this Attribution"),
            properties = attributes(),
            atLocation = refOrValue(Provenance.atLocation),
            hadRole = refOrValue(Provenance.hadRole),
            hadActivity = refOrValue(Provenance.hadActivity))
    }
}

@ProvenanceDsl
class DelegationBuilder(id: Resource) : AgentInfluenceBuilder<Delegation>(id) {
    override fun build() : Delegation {
        return DelegationData(id,
            agent = ensureValue(refOrValue(Provenance.agent), "No qualifier specified for this Delegation"),
            properties = attributes(),
            atLocation = refOrValue(Provenance.atLocation),
            hadRole = refOrValue(Provenance.hadRole),
            hadActivity = refOrValue(Provenance.hadActivity))
    }
}

@ProvenanceDsl
class AssociationBuilder(id: Resource) : AgentInfluenceBuilder<Association>(id) {
    fun hadPlan(thing: IRI) {
        set(Provenance.hadPlan, RefType(thing, Kind.Plan))
    }

    fun hadPlan(thing: IRI, block: PlanBuilder.() -> Unit) : Plan {
        val plan = PlanBuilder(thing).apply(block).build()
        hadPlan(plan)
        return plan
    }

    fun hadPlan(plan: Plan) {
        set(Provenance.hadPlan, plan)
    }

    override fun build() : Association {
        return AssociationData(id,
            agent = ensureValue(refOrValue(Provenance.agent), "No qualifier specified for this Association"),
            properties = attributes(),
            atLocation = refOrValue(Provenance.atLocation),
            hadRole = refOrValue(Provenance.hadRole),
            hadActivity = refOrValue(Provenance.hadActivity),
            hadPlan = refOrValue(Provenance.hadPlan))
    }
}

@ProvenanceDsl
class GenerationBuilder(id: Resource) : ActivityInfluenceBuilder<Generation>(id) {
    @Suppress("EXPERIMENTAL_API_USAGE")
    fun atTime(instant: Instant) {
        attribute(Provenance.atTime, instant)
    }

    override fun build() : Generation {
        return GenerationData(id,
            activity = ensureValue(refOrValue(Provenance.activity), "No qualifier specified for this Generation"),
            properties = attributes(),
            atLocation = refOrValue(Provenance.atLocation),
            hadRole = refOrValue(Provenance.hadRole))
    }
}

@ProvenanceDsl
class InvalidationBuilder(id: Resource) : ActivityInfluenceBuilder<Invalidation>(id) {
    @Suppress("EXPERIMENTAL_API_USAGE")
    fun atTime(instant: Instant) {
        attribute(Provenance.atTime, instant)
    }

    override fun build() : Invalidation {
        return InvalidationData(id,
            activity = ensureValue(refOrValue(Provenance.activity), "No qualifier specified for this Invalidation"),
            properties = attributes(),
            atLocation = refOrValue(Provenance.atLocation),
            hadRole = refOrValue(Provenance.hadRole))
    }
}

@ProvenanceDsl
class CommunicationBuilder(id: Resource) : ActivityInfluenceBuilder<Communication>(id) {
    override fun build() : Communication {
        return CommunicationData(id,
            activity = ensureValue(refOrValue(Provenance.activity), "No qualifier specified for this Communication"),
            properties = attributes(),
            atLocation = refOrValue(Provenance.atLocation),
            hadRole = refOrValue(Provenance.hadRole))
    }
}

@ProvenanceDsl
class StartBuilder(id: Resource) : EntityInfluenceBuilder<Start>(id) {
    @Suppress("EXPERIMENTAL_API_USAGE")
    fun atTime(instant: Instant) {
        attribute(Provenance.atTime, instant)
    }

    override fun build() : Start {
        return StartData(id,
            entity = ensureValue(refOrValue(Provenance.entity), "No qualifier specified for this Start"),
            properties = attributes(),
            atLocation = refOrValue(Provenance.atLocation),
            hadRole = refOrValue(Provenance.hadRole),
            hadActivity = refOrValue(Provenance.hadActivity))
    }
}

@ProvenanceDsl
class EndBuilder(id: Resource) : EntityInfluenceBuilder<End>(id) {
    @Suppress("EXPERIMENTAL_API_USAGE")
    fun atTime(instant: Instant) {
        attribute(Provenance.atTime, instant)
    }

    override fun build() : End {
        return EndData(id,
            entity = ensureValue(refOrValue(Provenance.entity), "No qualifier specified for this End"),
            properties = attributes(),
            atLocation = refOrValue(Provenance.atLocation),
            hadRole = refOrValue(Provenance.hadRole),
            hadActivity = refOrValue(Provenance.hadActivity))
    }
}

@ProvenanceDsl
class UsageBuilder(id: Resource) : EntityInfluenceBuilder<Usage>(id) {
    @Suppress("EXPERIMENTAL_API_USAGE")
    fun atTime(instant: Instant) {
        attribute(Provenance.atTime, instant)
    }

    override fun build() : Usage {
        return UsageData(id,
            entity = ensureValue(refOrValue(Provenance.entity), "No qualifier specified for this Usage"),
            properties = attributes(),
            atLocation = refOrValue(Provenance.atLocation),
            hadRole = refOrValue(Provenance.hadRole),
            hadActivity = refOrValue(Provenance.hadActivity))
    }
}

@ProvenanceDsl
abstract class AbstractDerivationBuilder<V : Derivation>(id: Resource) : EntityInfluenceBuilder<V>(id) {
    fun hadUsage(thing: Resource) {
        set(Provenance.hadUsage, RefType(thing, Kind.Usage))
    }

    fun hadUsage(thing: Resource, block: UsageBuilder.() -> Unit) : Usage {
        val usage = UsageBuilder(thing).apply(block).build()
        hadUsage(usage)
        return usage
    }

    fun hadUsage(usage: Usage) {
        set(Provenance.hadUsage, usage)
    }

    fun hadGeneration(thing: Resource) {
        set(Provenance.hadGeneration, RefType(thing, Kind.Generation))
    }

    fun hadGeneration(thing: Resource, block: GenerationBuilder.() -> Unit) : Generation {
        val generation = GenerationBuilder(thing).apply(block).build()
        hadGeneration(generation)
        return generation
    }

    fun hadGeneration(generation: Generation) {
        set(Provenance.hadGeneration, generation)
    }
}

@ProvenanceDsl
class DerivationBuilder(id: Resource) : AbstractDerivationBuilder<Derivation>(id) {
    override fun build() : Derivation {
        return DerivationData(id,
            entity = ensureValue(refOrValue(Provenance.entity), "No qualifier specified for this Derivation"),
            properties = attributes(),
            atLocation = refOrValue(Provenance.atLocation),
            hadRole = refOrValue(Provenance.hadRole),
            hadActivity = ensureValue(refOrValue(Provenance.hadActivity), "No activity specified for this Derivation"),
            hadGeneration = refOrValue(Provenance.hadGeneration),
            hadUsage = refOrValue(Provenance.hadUsage))
    }
}

@ProvenanceDsl
class PrimarySourceBuilder(id: Resource) : AbstractDerivationBuilder<PrimarySource>(id) {
    override fun build() : PrimarySource {
        return PrimarySourceData(id,
            entity = ensureValue(refOrValue(Provenance.entity), "No qualifier specified for this PrimarySource"),
            properties = attributes(),
            atLocation = refOrValue(Provenance.atLocation),
            hadRole = refOrValue(Provenance.hadRole),
            hadActivity = ensureValue(refOrValue(Provenance.hadActivity), "No activity specified for this PrimarySource"),
            hadGeneration = refOrValue(Provenance.hadGeneration),
            hadUsage = refOrValue(Provenance.hadUsage))
    }
}

@ProvenanceDsl
class QuotationBuilder(id: Resource) : AbstractDerivationBuilder<Quotation>(id) {
    override fun build() : Quotation {
        return QuotationData(id,
            entity = ensureValue(refOrValue(Provenance.entity), "No qualifier specified for this Quotation"),
            properties = attributes(),
            atLocation = refOrValue(Provenance.atLocation),
            hadRole = refOrValue(Provenance.hadRole),
            hadActivity = ensureValue(refOrValue(Provenance.hadActivity), "No activity specified for this Quotation"),
            hadGeneration = refOrValue(Provenance.hadGeneration),
            hadUsage = refOrValue(Provenance.hadUsage))
    }
}

@ProvenanceDsl
class RevisionBuilder(id: Resource) : AbstractDerivationBuilder<Revision>(id) {
    override fun build() : Revision {
        return RevisionData(id,
            entity = ensureValue(refOrValue(Provenance.entity), "No qualifier specified for this Revision"),
            properties = attributes(),
            atLocation = refOrValue(Provenance.atLocation),
            hadRole = refOrValue(Provenance.hadRole),
            hadActivity = ensureValue(refOrValue(Provenance.hadActivity), "No activity specified for this Revision"),
            hadGeneration = refOrValue(Provenance.hadGeneration),
            hadUsage = refOrValue(Provenance.hadUsage))
    }
}

@ProvenanceDsl
abstract class AbstractEntityBuilder<E : Entity>(id: Resource) : ObjectWithAttributesBuilder<E>(id) {

    fun atLocation(thing: Resource) {
        set(Provenance.atLocation, RefType(thing, Kind.Location))
    }

    fun atLocation(thing: Resource, block: LocationBuilder.() -> Unit) : Location {
        val location = LocationBuilder(thing).apply(block).build()
        atLocation(location)
        return location
    }

    fun atLocation(location: Location) {
        set(Provenance.atLocation, location)
    }

    fun generatedAtTime(instant: Instant) {
        set(Provenance.generatedAtTime, instant)
    }

    fun invalidatedAtTime(instant: Instant) {
        set(Provenance.invalidatedAtTime, instant)
    }

    fun alternateOf(thing: IRI) {
        set(Provenance.alternateOf, RefType(thing, Kind.Entity))
    }

    fun alternateOf(thing: IRI, block: EntityBuilder.() -> Unit) : Entity {
        val entity = EntityBuilder(thing).apply(block).build()
        alternateOf(entity)
        return entity
    }

    fun alternateOf(entity: Entity) {
        set(Provenance.alternateOf, entity)
    }

    fun specializationOf(thing: IRI) {
        set(Provenance.specializationOf, RefType(thing, Kind.Entity))
    }

    fun specializationOf(thing: IRI, block: EntityBuilder.() -> Unit) : Entity {
        val entity = EntityBuilder(thing).apply(block).build()
        specializationOf(entity)
        return entity
    }

    fun specializationOf(entity: Entity) {
        set(Provenance.specializationOf, entity)
    }

    fun wasAttributedTo(thing: IRI) {
        set(Provenance.wasAttributedTo, RefType(thing, Kind.Agent))
    }

    fun wasAttributedTo(thing: IRI, block: AgentBuilder.() -> Unit) : Agent {
        val agent = AgentBuilder(thing).apply(block).build()
        wasAttributedTo(agent)
        return agent
    }

    fun wasAttributedTo(agent: Agent) {
        set(Provenance.wasAttributedTo, agent)
    }

    fun qualifiedAttribution(block: AttributionBuilder.() -> Unit) : Attribution {
        return qualifiedAttribution(Values.bnode(), block)
    }

    fun qualifiedAttribution(thing: Resource, block: AttributionBuilder.() -> Unit) : Attribution {
        val attribution = AttributionBuilder(thing).apply(block).build()
        qualifiedAttribution(attribution)
        return attribution
    }

    fun qualifiedAttribution(attribution: Attribution) {
        set(Provenance.wasAttributedTo, attribution)
    }

    fun wasGeneratedBy(thing: IRI) {
        set(Provenance.wasGeneratedBy, RefType(thing, Kind.Activity))
    }

    fun wasGeneratedBy(thing: IRI, block: ActivityBuilder.() -> Unit) : Activity {
        val activity = ActivityBuilder(thing).apply(block).build()
        wasGeneratedBy(activity)
        return activity
    }

    fun wasGeneratedBy(activity: Activity) {
        set(Provenance.wasGeneratedBy, activity)
    }

    fun qualifiedGeneration(block: GenerationBuilder.() -> Unit) : Generation {
        return qualifiedGeneration(Values.bnode(), block)
    }

    fun qualifiedGeneration(thing: Resource, block: GenerationBuilder.() -> Unit) : Generation {
        val generation = GenerationBuilder(thing).apply(block).build()
        qualifiedGeneration(generation)
        return generation
    }

    fun qualifiedGeneration(generation: Generation) {
        set(Provenance.wasGeneratedBy, generation)
    }

    fun wasInvalidatedBy(thing: IRI) {
        set(Provenance.wasInvalidatedBy, RefType(thing, Kind.Activity))
    }

    fun wasInvalidatedBy(thing: IRI, block: ActivityBuilder.() -> Unit) : Activity {
        val activity = ActivityBuilder(thing).apply(block).build()
        wasInvalidatedBy(activity)
        return activity
    }

    fun wasInvalidatedBy(activity: Activity) {
        set(Provenance.wasInvalidatedBy, activity)
    }

    fun qualifiedInvalidation(block: InvalidationBuilder.() -> Unit) : Invalidation {
        return qualifiedInvalidation(Values.bnode(), block)
    }

    fun qualifiedInvalidation(thing: Resource, block: InvalidationBuilder.() -> Unit) : Invalidation {
        val invalidation = InvalidationBuilder(thing).apply(block).build()
        qualifiedInvalidation(invalidation)
        return invalidation
    }

    fun qualifiedInvalidation(invalidation: Invalidation) {
        set(Provenance.wasInvalidatedBy, invalidation)
    }

    fun wasDerivedFrom(thing: IRI) {
        set(Provenance.wasDerivedFrom, RefType(thing, Kind.Entity))
    }

    fun wasDerivedFrom(thing: IRI, block: EntityBuilder.() -> Unit) : Entity {
        val entity = EntityBuilder(thing).apply(block).build()
        wasDerivedFrom(entity)
        return entity
    }

    fun wasDerivedFrom(entity: Entity) {
        set(Provenance.wasDerivedFrom, entity)
    }

    fun qualifiedDerivation(block: DerivationBuilder.() -> Unit) : Derivation {
         return qualifiedDerivation(Values.bnode(), block)
    }

    fun qualifiedDerivation(thing: Resource, block: DerivationBuilder.() -> Unit) : Derivation {
        val derivation = DerivationBuilder(thing).apply(block).build()
        qualifiedDerivation(derivation)
        return derivation
    }

    fun qualifiedDerivation(derivation: Derivation) {
        set(Provenance.wasDerivedFrom, derivation)
    }

    fun hadPrimarySource(thing: IRI) {
        set(Provenance.hadPrimarySource, RefType(thing, Kind.Entity))
    }

    fun hadPrimarySource(thing: IRI, block: EntityBuilder.() -> Unit) : Entity {
        val entity = EntityBuilder(thing).apply(block).build()
        hadPrimarySource(entity)
        return entity
    }

    fun hadPrimarySource(entity: Entity) {
        set(Provenance.hadPrimarySource, entity)
    }

    fun qualifiedPrimarySource(block: PrimarySourceBuilder.() -> Unit) : PrimarySource {
        return qualifiedPrimarySource(Values.bnode(), block)
    }

    fun qualifiedPrimarySource(thing: Resource, block: PrimarySourceBuilder.() -> Unit) : PrimarySource {
        val primarySource = PrimarySourceBuilder(thing).apply(block).build()
        qualifiedPrimarySource(primarySource)
        return primarySource
    }

    fun qualifiedPrimarySource(primarySource: PrimarySource) {
        set(Provenance.hadPrimarySource, primarySource)
    }

    fun wasQuotedFrom(thing: IRI) {
        set(Provenance.wasQuotedFrom, RefType(thing, Kind.Entity))
    }

    fun wasQuotedFrom(thing: IRI, block: EntityBuilder.() -> Unit) : Entity {
        val entity = EntityBuilder(thing).apply(block).build()
        wasQuotedFrom(entity)
        return entity
    }

    fun wasQuotedFrom(entity: Entity) {
        set(Provenance.wasQuotedFrom, entity)
    }

    fun qualifiedQuotation(block: QuotationBuilder.() -> Unit) : Quotation {
        return qualifiedQuotation(Values.bnode(), block)
    }

    fun qualifiedQuotation(thing: Resource, block: QuotationBuilder.() -> Unit) : Quotation {
        val quotation = QuotationBuilder(thing).apply(block).build()
        qualifiedQuotation(quotation)
        return quotation
    }

    fun qualifiedQuotation(quotation: Quotation) {
        set(Provenance.wasQuotedFrom, quotation)
    }

    fun wasRevisionOf(thing: IRI) {
        set(Provenance.wasRevisionOf, RefType(thing, Kind.Entity))
    }

    fun wasRevisionOf(thing: IRI, block: EntityBuilder.() -> Unit) : Entity {
        val entity = EntityBuilder(thing).apply(block).build()
        wasRevisionOf(entity)
        return entity
    }

    fun wasRevisionOf(entity: Entity) {
        set(Provenance.wasRevisionOf, entity)
    }

    fun qualifiedRevision(block: RevisionBuilder.() -> Unit) : Revision {
        return qualifiedRevision(Values.bnode(), block)
    }

    fun qualifiedRevision(thing: Resource, block: RevisionBuilder.() -> Unit) : Revision {
        val revision = RevisionBuilder(thing).apply(block).build()
        qualifiedRevision(revision)
        return revision
    }

    fun qualifiedRevision(revision: Revision) {
        set(Provenance.wasRevisionOf, revision)
    }
}

@ProvenanceDsl
class EntityBuilder(id: IRI) : AbstractEntityBuilder<Entity>(id) {
    override fun build() : Entity {
        return EntityData(id as IRI,
            properties = attributes(),
            atLocation = refOrValue(Provenance.atLocation),
            generatedAtTime = value(Provenance.generatedAtTime),
            invalidatedAtTime = value(Provenance.invalidatedAtTime),
            alternateOf = refOrValue(Provenance.alternateOf),
            specializationOf = refOrValue(Provenance.specializationOf),
            wasAttributedTo = referencable(Provenance.wasAttributedTo),
            wasGeneratedBy = referencable(Provenance.wasGeneratedBy),
            wasDerivedFrom = referencable(Provenance.wasDerivedFrom),
            hadPrimarySource = referencable(Provenance.hadPrimarySource),
            wasInvalidatedBy = referencable(Provenance.wasInvalidatedBy),
            wasQuotedFrom = referencable(Provenance.wasQuotedFrom),
            wasRevisionOf = referencable(Provenance.wasRevisionOf))
    }
}

@ProvenanceDsl
class PlanBuilder(id: IRI) : AbstractEntityBuilder<Plan>(id) {
    override fun build() : Plan {
        return PlanData(id as IRI,
            properties = attributes(),
            atLocation = refOrValue(Provenance.atLocation),
            generatedAtTime = value(Provenance.generatedAtTime),
            invalidatedAtTime = value(Provenance.invalidatedAtTime),
            alternateOf = refOrValue(Provenance.alternateOf),
            specializationOf = refOrValue(Provenance.specializationOf),
            wasAttributedTo = referencable(Provenance.wasAttributedTo),
            wasGeneratedBy = referencable(Provenance.wasGeneratedBy),
            wasDerivedFrom = referencable(Provenance.wasDerivedFrom),
            hadPrimarySource = referencable(Provenance.hadPrimarySource),
            wasInvalidatedBy = referencable(Provenance.wasInvalidatedBy),
            wasQuotedFrom = referencable(Provenance.wasQuotedFrom),
            wasRevisionOf = referencable(Provenance.wasRevisionOf))
    }
}

@ProvenanceDsl
class CollectionBuilder(id: IRI) : AbstractEntityBuilder<Collection>(id) {

    class MEMBERS: HashSet<Any>() {
        fun entity(thing: IRI) {
            add(RefType(thing, Kind.Entity))
        }

        fun plan(thing: IRI) {
            add(RefType(thing, Kind.Plan))
        }

        fun bundle(thing: IRI) {
            add(RefType(thing, Kind.Bundle))
        }

        fun entity(thing: IRI, block: EntityBuilder.() -> Unit) : Entity {
            val entity = EntityBuilder(thing).apply(block).build()
            entity(entity)
            return entity
        }

        fun entity(entity: Entity) {
            add(entity)
        }
    }

    fun hadMembers(block: MEMBERS.() -> Unit) {
        val items = MEMBERS()
        set(Provenance.hadMember, items)
        items.apply(block)
    }

    override fun build() : Collection {
        return CollectionData(id as IRI,
            properties = attributes(),
            hadMembers = refOrValues(Provenance.hadMember),
            atLocation = refOrValue(Provenance.atLocation),
            generatedAtTime = value(Provenance.generatedAtTime),
            invalidatedAtTime = value(Provenance.invalidatedAtTime),
            alternateOf = refOrValue(Provenance.alternateOf),
            specializationOf = refOrValue(Provenance.specializationOf),
            wasAttributedTo = referencable(Provenance.wasAttributedTo),
            wasGeneratedBy = referencable(Provenance.wasGeneratedBy),
            wasDerivedFrom = referencable(Provenance.wasDerivedFrom),
            hadPrimarySource = referencable(Provenance.hadPrimarySource),
            wasInvalidatedBy = referencable(Provenance.wasInvalidatedBy),
            wasQuotedFrom = referencable(Provenance.wasQuotedFrom),
            wasRevisionOf = referencable(Provenance.wasRevisionOf))
    }
}

@ProvenanceDsl
class ActivityBuilder(id: IRI) : ObjectWithAttributesBuilder<Activity>(id) {

    class INVALIDATED: HashSet<Any>() {
        fun entity(thing: IRI) {
            add(RefType(thing, Kind.Entity))
        }

        fun entity(thing: IRI, block: EntityBuilder.() -> Unit) : Entity {
            val entity = EntityBuilder(thing).apply(block).build()
            entity(entity)
            return entity
        }

        fun  entity(entity: Entity) {
            add(entity)
        }
    }

    fun invalidated(block: INVALIDATED.() -> Unit) {
        val items = INVALIDATED()
        set(Provenance.invalidated, items)
        items.apply(block)
    }

    fun atLocation(thing: Resource) {
        set(Provenance.atLocation, RefType(thing, Kind.Location))
    }

    fun atLocation(thing: Resource, block: LocationBuilder.() -> Unit) : Location {
        val location = LocationBuilder(thing).apply(block).build()
        atLocation(location)
        return location
    }

    fun atLocation(location: Location) {
        set(Provenance.atLocation, location)
    }

    fun startedAtTime(instant: Instant) {
        set(Provenance.startedAtTime, instant)
    }

    fun endedAtTime(instant: Instant) {
        set(Provenance.endedAtTime, instant)
    }

    fun generated(thing: IRI) {
        set(Provenance.generated, RefType(thing, Kind.Entity))
    }

    fun generated(thing: IRI, block: EntityBuilder.() -> Unit) : Entity {
        val entity = EntityBuilder(thing).apply(block).build()
        generated(entity)
        return entity
    }

    fun generated(entity: Entity) {
        set(Provenance.generated, entity)
    }

    fun wasStartedBy(thing: IRI) {
        set(Provenance.wasStartedBy, RefType(thing, Kind.Entity))
    }

    fun wasStartedBy(thing: IRI, block: EntityBuilder.() -> Unit) : Entity {
        val entity = EntityBuilder(thing).apply(block).build()
        wasStartedBy(entity)
        return entity
    }

    fun wasStartedBy(entity: Entity) {
        set(Provenance.wasStartedBy, entity)
    }

    fun qualifiedStart(block: StartBuilder.() -> Unit) : Start {
        return qualifiedStart(Values.bnode(), block)
    }

    fun qualifiedStart(thing: Resource, block: StartBuilder.() -> Unit) : Start {
        val start = StartBuilder(thing).apply(block).build()
        qualifiedStart(start)
        return start
    }

    fun qualifiedStart(start: Start) {
        set(Provenance.wasStartedBy, start)
    }

    fun wasEndedBy(thing: IRI) {
        set(Provenance.wasEndedBy, RefType(thing, Kind.Entity))
    }

    fun wasEndedBy(thing: IRI, block: EntityBuilder.() -> Unit) : Entity {
        val entity = EntityBuilder(thing).apply(block).build()
        wasEndedBy(entity)
        return entity
    }

    fun wasEndedBy(entity: Entity) {
        set(Provenance.wasEndedBy, entity)
    }

    fun qualifiedEnd(block: EndBuilder.() -> Unit) : End {
        return qualifiedEnd(Values.bnode(), block)
    }

    fun qualifiedEnd(thing: Resource, block: EndBuilder.() -> Unit) : End {
        val end = EndBuilder(thing).apply(block).build()
        qualifiedEnd(end)
        return end
    }

    fun qualifiedEnd(end: End) {
        set(Provenance.wasEndedBy, end)
    }

    fun wasAssociatedWith(thing: IRI) {
        add(Provenance.wasAssociatedWith, RefType(thing, Kind.Agent))
    }

    fun wasAssociatedWith(thing: IRI, block: AgentBuilder.() -> Unit) : Agent {
        val agent = AgentBuilder(thing).apply(block).build()
        wasAssociatedWith(agent)
        return agent
    }

    fun wasAssociatedWith(agent: Agent) {
        add(Provenance.wasAssociatedWith, agent)
    }

    fun qualifiedAssociation(block: AssociationBuilder.() -> Unit) : Association {
        return qualifiedAssociation(Values.bnode(), block)
    }

    fun qualifiedAssociation(thing: Resource, block: AssociationBuilder.() -> Unit) : Association {
        val association = AssociationBuilder(thing).apply(block).build()
        qualifiedAssociation(association)
        return association
    }

    fun qualifiedAssociation(association: Association) {
        add(Provenance.wasAssociatedWith, association)
    }

    fun used(thing: IRI) {
        add(Provenance.used, RefType(thing, Kind.Entity))
    }

    fun used(thing: IRI, block: EntityBuilder.() -> Unit) : Entity {
        val entity = EntityBuilder(thing).apply(block).build()
        used(entity)
        return entity
    }

    fun used(entity: Entity) {
        add(Provenance.used, entity)
    }

    fun qualifiedUsage(block: UsageBuilder.() -> Unit) : Usage {
        return qualifiedUsage(Values.bnode(), block)
    }

    fun qualifiedUsage(thing: Resource, block: UsageBuilder.() -> Unit) : Usage {
        val usage = UsageBuilder(thing).apply(block).build()
        qualifiedUsage(usage)
        return usage
    }

    fun qualifiedUsage(usage: Usage) {
        add(Provenance.used, usage)
    }

    fun wasInformedBy(thing: IRI) {
        add(Provenance.wasInformedBy, RefType(thing, Kind.Activity))
    }

    fun wasInformedBy(thing: IRI, block: ActivityBuilder.() -> Unit) : Activity {
        val activity = ActivityBuilder(thing).apply(block).build()
        wasInformedBy(activity)
        return activity
    }

    fun wasInformedBy(activity: Activity) {
        add(Provenance.wasInformedBy, activity)
    }

    fun qualifiedCommunication(block: CommunicationBuilder.() -> Unit) : Communication {
        return qualifiedCommunication(Values.bnode(), block)
    }

    fun qualifiedCommunication(thing: Resource, block: CommunicationBuilder.() -> Unit) : Communication {
        val communication = CommunicationBuilder(thing).apply(block).build()
        qualifiedCommunication(communication)
        return communication
    }

    fun qualifiedCommunication(communication: Communication) {
        add(Provenance.wasInformedBy, communication)
    }

    override fun build() : Activity {
        return ActivityData(id as IRI,
            properties = attributes(),
            atLocation = refOrValue(Provenance.atLocation),
            startedAtTime = value(Provenance.startedAtTime),
            endedAtTime = value(Provenance.endedAtTime),
            generated = refOrValue(Provenance.generated),
            wasStartedBy = referencable(Provenance.wasStartedBy),
            wasEndedBy = referencable(Provenance.wasEndedBy),
            invalidated = refOrValues(Provenance.invalidated),
            wasAssociatedWith = referencables(Provenance.wasAssociatedWith),
            used = referencables(Provenance.used),
            wasInformedBy = referencables(Provenance.wasInformedBy))
    }
}

@ProvenanceDsl
class AgentBuilder(id: IRI) : ObjectWithAttributesBuilder<Agent>(id) {

    fun type(type: Agent.Type) {
        set(RDF.TYPE, type)
    }

    fun atLocation(thing: Resource) {
        set(Provenance.atLocation, RefType(thing, Kind.Location))
    }

    fun atLocation(thing: Resource, block: LocationBuilder.() -> Unit) : Location {
        val location = LocationBuilder(thing).apply(block).build()
        atLocation(location)
        return location
    }

    fun atLocation(location: Location) {
        set(Provenance.atLocation, location)
    }

    fun actedOnBehalfOf(thing: IRI) {
        add(Provenance.actedOnBehalfOf, RefType(thing, Kind.Agent))
    }

    fun actedOnBehalfOf(thing: IRI, block: AgentBuilder.() -> Unit) : Agent {
        val agent = AgentBuilder(thing).apply(block).build()
        actedOnBehalfOf(agent)
        return agent
    }

    fun actedOnBehalfOf(agent: Agent) {
        add(Provenance.actedOnBehalfOf, agent)
    }

    fun qualifiedDelegation(block: DelegationBuilder.() -> Unit) : Delegation {
        return qualifiedDelegation(Values.bnode(), block)
    }

    fun qualifiedDelegation(thing: Resource, block: DelegationBuilder.() -> Unit) : Delegation {
        val delegation = DelegationBuilder(thing).apply(block).build()
        qualifiedDelegation(delegation)
        return delegation
    }

    fun qualifiedDelegation(delegation: Delegation) {
        add(Provenance.actedOnBehalfOf, delegation)
    }

    override fun build() : Agent {
        return AgentData(id as IRI,
            type = value<Agent.Type>(RDF.TYPE) ?: Agent.Type.Agent,
            properties = attributes(),
            atLocation = refOrValue(Provenance.atLocation),
            actedOnBehalfOf = referencables(Provenance.actedOnBehalfOf))
    }
}

@ProvenanceDsl
class LinkBuilder(private val allowLinkConstructions: Boolean) : DataBuilder<Link>() {

    fun entity(thing: IRI) {
        set(bundleItem, RefType(thing, Kind.Entity))
    }

    fun activity(thing: IRI) {
        set(bundleItem, RefType(thing, Kind.Activity))
    }

    fun agent(thing: IRI) {
        set(bundleItem, RefType(thing, Kind.Agent))
    }

    fun entity(thing: IRI, block: EntityBuilder.() -> Unit) : Entity {
        val entity = EntityBuilder(thing).apply(block).build()
        entity(entity)
        return entity
    }

    fun entity(entity: Entity){
        set(bundleItem, entity)
    }

    fun activity(thing: IRI, block: ActivityBuilder.() -> Unit) : Activity {
        val activity = ActivityBuilder(thing).apply(block).build()
        activity(activity)
        return activity
    }

    fun activity(activity: Activity) {
        set(bundleItem, activity)
    }

    fun agent(thing: IRI, block: AgentBuilder.() -> Unit) : Agent {
        val agent = AgentBuilder(thing).apply(block).build()
        agent(agent)
        return agent
    }

    fun agent(agent: Agent) {
        set(bundleItem, agent)
    }

    fun mentionOf(thing: IRI) {
        set(Provenance.mentionOf, thing)
    }

    fun bundle(thing: Resource) {
        set(Provenance.asInBundle, thing)
    }

    fun bundle(bundle: Bundle) {
        require(allowLinkConstructions) { "Builder is configured to only allow bundle linking via references" }
        set(Provenance.asInBundle, bundle)
    }

    fun bundle(thing: Resource, block: BundleBuilder.() -> Unit) : Bundle {
        require(allowLinkConstructions) { "Builder is configured to only allow bundle linking via references" }
        val bundle = BundleBuilder(thing).apply(block).build()
        set(Provenance.asInBundle, bundle)
        return bundle
    }

    override fun build() : Link {
        return LinkData(
            subject = ensureValue(refOrValue(bundleItem), "No subject has been set for the link"),
            mentionOf = ensureValue(value(Provenance.mentionOf), "No mentionOf has been set for the link"),
            asInBundle = ensureValue(refOrValue(Provenance.asInBundle), "No bundle reference has been set for the link")
        )
    }
}

@ProvenanceDsl
class BundleBuilder(id: Resource, private val allowLinkConstructions: Boolean = true) : AbstractEntityBuilder<Bundle>(id) {

    class LINKS(private val allowLinkConstructions: Boolean) : HashSet<Link>() {
        fun link(block: LinkBuilder.() -> Unit) : Link {
            val link = LinkBuilder(allowLinkConstructions).apply(block).build()
            link(link)
            return link
        }

        fun link(link: Link) {
            add(link)
        }
    }

    fun resource(thing: Resource, kind: IRI) {
        add(bundleItem, NonProvenanceData(thing, kind))
    }

    fun resource(thing: Resource, kind: IRI, block: NonProvenanceBuilder.() -> Unit) : NonProvenance {
        val nonProvenance = NonProvenanceBuilder(thing, kind).apply(block).build()
        resource(nonProvenance)
        return nonProvenance
    }

    fun resource(nonProvenance: NonProvenance) {
        add(bundleItem, nonProvenance)
    }

    fun agent(thing: IRI) {
        add(bundleItem, RefData<Agent>(thing, Kind.Agent))
    }

    fun agent(thing: IRI, block: AgentBuilder.() -> Unit) : Agent {
        val agent = AgentBuilder(thing).apply(block).build()
        agent(agent)
        return agent
    }

    fun agent(agent: Agent) {
        add(bundleItem, agent)
    }

    fun activity(thing: IRI) {
        add(bundleItem, RefData<Activity>(thing, Kind.Activity))
    }

    fun activity(thing: IRI, block: ActivityBuilder.() -> Unit) : Activity {
        val activity = ActivityBuilder(thing).apply(block).build()
        activity(activity)
        return activity
    }

    fun activity(activity: Activity) {
        add(bundleItem, activity)
    }

    fun entity(thing: IRI) {
        add(bundleItem, RefData<Entity>(thing, Kind.Entity))
    }

    fun entity(thing: IRI, block: EntityBuilder.() -> Unit) : Entity {
        val entity = EntityBuilder(thing).apply(block).build()
        entity(entity)
        return entity
    }

    fun entity(entity: Entity) {
        add(bundleItem, entity)
    }

    fun plan(thing: IRI) {
        add(bundleItem, RefData<Plan>(thing, Kind.Plan))
    }

    fun plan(thing: IRI, block: PlanBuilder.() -> Unit) : Plan {
        val plan = PlanBuilder(thing).apply(block).build()
        add(bundleItem, plan)
        return plan
    }

    fun collection(thing: IRI) {
        add(bundleItem, RefData<Collection>(thing, Kind.Collection))
    }

    fun collection(thing: IRI, block: CollectionBuilder.() -> Unit) : Collection {
        val collection = CollectionBuilder(thing).apply(block).build()
        add(bundleItem, collection)
        return collection
    }

    fun role(thing: Resource) {
        add(bundleItem, RefData<Role>(thing, Kind.Role))
    }

    fun role(thing: Resource, block: RoleBuilder.() -> Unit) : Role {
        val role = RoleBuilder(thing).apply(block).build()
        role(role)
        return role
    }

    fun role(role: Role) {
        add(bundleItem, role)
    }

    fun location(thing: Resource) {
        add(bundleItem, RefData<Location>(thing, Kind.Location))
    }

    fun location(thing: Resource, block: LocationBuilder.() -> Unit) : Location {
        val location = LocationBuilder(thing).apply(block).build()
        add(bundleItem, location)
        return location
    }

    fun location(location: Location) {
        add(bundleItem, location)
    }

    fun links(block: LINKS.() -> Unit) {
        val links = LINKS(allowLinkConstructions)
        set(bundleLinks, links)
        links.apply(block)
    }

    override fun build() : Bundle {
        return BundleData(id,
            items = values(bundleItem).map { it as Identifiable }.asSequence(),
            properties = attributes(),
            links = value<LINKS>(bundleLinks)?.asSequence() ?: emptySequence(),
            atLocation = refOrValue(Provenance.atLocation),
            generatedAtTime = value(Provenance.generatedAtTime),
            invalidatedAtTime = value(Provenance.invalidatedAtTime),
            alternateOf = refOrValue(Provenance.alternateOf),
            specializationOf = refOrValue(Provenance.specializationOf),
            wasAttributedTo = referencable(Provenance.wasAttributedTo),
            wasGeneratedBy = referencable(Provenance.wasGeneratedBy),
            wasDerivedFrom = referencable(Provenance.wasDerivedFrom),
            hadPrimarySource = referencable(Provenance.hadPrimarySource),
            wasInvalidatedBy = referencable(Provenance.wasInvalidatedBy),
            wasQuotedFrom = referencable(Provenance.wasQuotedFrom),
            wasRevisionOf = referencable(Provenance.wasRevisionOf))
    }
}

fun bundle(block: BundleBuilder.() -> Unit) : Bundle {
    return bundle(Values.bnode(), block)
}

fun bundle(thing: Resource, block: BundleBuilder.() -> Unit) : Bundle {
    return BundleBuilder(thing).apply(block).build()
}