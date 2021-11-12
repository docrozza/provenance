package io.docrozza.provenance.rdf

import com.complexible.stardog.api.Connection
import com.stardog.stark.*
import com.stardog.stark.vocabs.RDF
import io.docrozza.provenance.*
import io.docrozza.provenance.model.*
import io.docrozza.provenance.rdf.StardogUtils.use
import java.util.function.Supplier
import java.util.stream.Collectors
import kotlin.reflect.KClass

/**
 * Converts an RDF model to a provenance [Bundle]
 */
@Suppress("EXPERIMENTAL_API_USAGE")
class KnowledgeGraphBuilder private constructor(
    private val bundleId: Resource, private val traverseLinks: Boolean, private val dataProvider: (Resource) -> Set<Statement>) {

    companion object {

        // do more specific types first
        private val types = setOf(
            Provenance.Activity,
            Provenance.Person, Provenance.Organization, Provenance.SoftwareAgent, Provenance.Agent,
            Provenance.Plan, Provenance.Collection, Provenance.EmptyCollection, Provenance.Entity,
            Provenance.Location, Provenance.Role)

        /**
         * Assumes one [Bundle] is fully defined in each context found in database accessed via [connectionFactory]
         * Will return the top-most [Bundle], i.e. the bundle that is not the source link of any other bundle.
         */
        fun toBundle(bundleId: Resource, connectionFactory: Supplier<Connection>) : Bundle {
            val dataProvider: (Resource) -> Set<Statement> = { id ->
                connectionFactory.use { it.get().context(id).statements().collect(Collectors.toSet()) }
            }
            val bundleStmts = dataProvider(bundleId)
            require(bundleStmts.isNotEmpty()) { "No statements found for the top-level bundle with the id: $bundleId" }

            val multiBundle = bundleStmts.any { it.predicate() == Provenance.asInBundle }
            return KnowledgeGraphBuilder(bundleId, multiBundle, dataProvider).build()
        }
    }

    private val data: Set<Statement> = dataProvider(bundleId)

    private val tracked = mutableMapOf<Resource, Identifiable>()

    private val refs = mutableMapOf<Resource, KClass<out ProvenanceObject>>()

    private fun attributed(id: Resource) : Boolean {
        return data
            .filter { it.subject() == id }
            .filterNot { it.predicate() == RDF.TYPE }
            .firstOrNull() != null
    }

    private inline fun <reified V : ProvenanceObject, I : Resource> bind(id: I, withResource: (I) -> Unit, withObj: (V) -> Unit,
             withBuild: (I) -> V) {
        val value = tracked[id]
        if (value == null) {
            if (attributed(id)) {
                tracked[id] = withBuild(id)
            } else {
                withResource(id)
                refs[id] = V::class
            }
        } else {
            require(value is V) { "Found object '$id' but is was not the expected type" }
            withObj(value)
        }
    }

    private fun build() : Bundle {
        val bundleContext = data.any {
            it.subject() == bundleId &&
            it.predicate() == RDF.TYPE &&
            it.`object`() == Provenance.Bundle &&
            it.context() == bundleId
        }
        require(bundleContext) { "There is no bundle in this context with the same ID: $bundleId" }

        return bundle(bundleId) {
            buildNonProvenance(this)
            buildItems(this)
            buildLinks(this)
        }
    }

    private fun buildNonProvenance(scope: BundleBuilder) {
        scope.apply {
            data
                .filter {
                    if (it.predicate().namespace() != Provenance.namespace) {
                        false
                    } else {
                        val obj = it.`object`()
                        !(it.predicate() == RDF.TYPE && obj is IRI && obj.namespace() != Provenance.namespace)
                    }
                }
                .groupBy { it.subject() }
                .asSequence()
                .filter { it.value.any { st -> st.predicate() == RDF.TYPE && st.`object`() is IRI } }
                .map { Triple(it.key, it.value.first { st -> st.predicate() == RDF.TYPE }.`object`() as IRI, it.value) }
                .forEach {
                    resource(it.first, it.second) {
                        it.third
                            .filterNot { st -> st.predicate() == RDF.TYPE && st.`object`() != it.second }
                            .forEach { st -> attribute(st.predicate(), st.`object`()) }
                    }
                }
        }
    }

    private fun buildLinks(scope: BundleBuilder) {
        if (data.any { it.predicate() == Provenance.asInBundle }) {
            scope.apply {
                links {
                    data
                        .filter { it.predicate() ==  Provenance.asInBundle }
                        .filter { it.subject() is IRI }
                        .forEach {
                            val bundleId = it.`object`()
                            require(bundleId is Resource) { "Bundle mentionOf must be a Resource" }

                            link {
                                buildLink(it.subject() as IRI, this)

                                val mention = objectResources(it.subject(), Provenance.mentionOf)
                                    .filterIsInstance(IRI::class.java)
                                    .firstOrNull()
                                if (mention != null) {
                                    mentionOf(mention)
                                }

                                if (traverseLinks) {
                                    bundle(KnowledgeGraphBuilder(bundleId, true, dataProvider).build())
                                } else {
                                    bundle(bundleId)
                                }
                            }
                        }
                }
            }
        }
    }

    private fun buildLink(id: IRI, scope: LinkBuilder) {
        val types = objectResources(id, RDF.TYPE)
            .filterIsInstance(IRI::class.java)
            .toMutableSet()
        if (types.isNotEmpty()) {
            if (types.size > 1) {
                // remove the sub-classes
                types.remove(Provenance.Organization)
                types.remove(Provenance.SoftwareAgent)
                types.remove(Provenance.Person)
                types.remove(Provenance.Plan)
                types.remove(Provenance.Collection)
                types.remove(Provenance.EmptyCollection)

                require(types.size == 1) { "Link mention is a member of multiple types: $types" }
            }

            scope.apply {
                when (types.first()) {
                    Provenance.Activity -> if (attributed(id)) activity(id) else activity(tracked[id] as Activity)
                    Provenance.Agent -> if (attributed(id)) agent(id) else agent(tracked[id] as Agent)
                    else -> if (attributed(id)) entity(id) else entity(tracked[id] as Entity)
                }
            }
        }
    }

    private fun buildItems(scope: BundleBuilder) {
        scope.apply {
            types.asSequence()
                .flatMap { type ->
                    data
                        .filter { it.predicate() == RDF.TYPE && it.`object`() == type }
                        .map { Pair(type, it.subject()) }
                }
                .forEach { pair ->
                    val type = pair.first
                    val id = pair.second
                    when {
                        type == Provenance.Activity && id is IRI -> bind(id,
                            { activity(it) },
                            { activity(it) },
                            { activity(it) { buildActivity(scope, this) }})
                        type == Provenance.Person && id is IRI -> bind(id,
                            { agent(it) },
                            { agent(it) },
                            { agent(it) { buildAgent(scope, Agent.Type.Person, this) }})
                        type == Provenance.Organization && id is IRI -> bind(id,
                            { agent(it) },
                            { agent(it) },
                            { agent(it) { buildAgent(scope, Agent.Type.Organization, this) }})
                        type == Provenance.SoftwareAgent && id is IRI -> bind(id,
                            { agent(it) },
                            { agent(it) },
                            { agent(it) { buildAgent(scope, Agent.Type.Software, this) }})
                        type == Provenance.Agent && id is IRI -> {
                            if (!tracked.containsKey(id)) {
                                bind(id,
                                    { agent(it) },
                                    { agent(it) },
                                    { agent(it) { buildAgent(scope, this) }})
                            }
                        }
                        type == Provenance.Plan && id is IRI -> bind(id,
                            { plan(it) },
                            { entity(it) },
                            { plan(it) { buildEntity(scope, this) }})
                        (type == Provenance.EmptyCollection || type == Provenance.Collection) && id is IRI -> bind(id,
                            { collection(it) },
                            { entity(it) },
                            { collection(it) { buildEntity(scope, this) }})
                        type == Provenance.Entity && id is IRI -> {
                            if (!tracked.containsKey(id)) {
                                bind(id,
                                    { entity(it) },
                                    { entity(it) },
                                    { entity(it) { buildEntity(scope, this) }})
                            }
                        }
                        type == Provenance.Role -> bind(id,
                            { role(it) },
                            { role(it) },
                            { role(it) { addAttributes(this) }})
                        type == Provenance.Location -> bind(id,
                            { location(it) },
                            { location(it) },
                            { location(it) { addAttributes(this) }})
                        else -> error("'$id' of type '${type.localName()}' not recognized")
                    }
                }

            refs.asSequence()
                .filterNot { tracked.containsKey(it.key) }
                .forEach {
                    val id = it.key
                    val refClass = it.value
                    when {
                        refClass == Activity::class && id is IRI -> activity(id)
                        refClass == Agent::class && id is IRI -> agent(id)
                        refClass == Entity::class && id is IRI -> entity(id)
                        refClass == Plan::class && id is IRI -> plan(id)
                        refClass == Location::class && id is IRI -> location(id)
                        refClass == Role::class -> role(id)
                        else -> error("Reference class '${refClass}' not recognized")
                    }
                }
        }
    }

    private fun addToBundle(bundle: BundleBuilder, activity: Activity) : Activity {
        bundle.activity(activity)
        return activity
    }

    private fun addToBundle(bundle: BundleBuilder, agent: Agent) : Agent {
        bundle.agent(agent)
        return agent
    }

    private fun addToBundle(bundle: BundleBuilder, entity: Entity) : Entity {
        bundle.entity(entity)
        return entity
    }

    private fun addToBundle(bundle: BundleBuilder, entity: Plan) : Plan {
        bundle.entity(entity)
        return entity
    }

    private fun buildActivity(bundle: BundleBuilder, scope: ActivityBuilder) {
        scope.apply {
            addAttributes(this)

            addLiteral(id, Provenance.startedAtTime) { startedAtTime(StardogUtils.toInstant(it)) }
            addLiteral(id, Provenance.endedAtTime) { endedAtTime(StardogUtils.toInstant(it)) }

            bindIRI(id, Provenance.generated,
                { generated(it) },
                { generated(it) },
                { addToBundle(bundle, generated(it) { buildEntity(bundle, this) })})

            if (data.any { it.subject() == id && it.predicate() == Provenance.invalidated }) {
                invalidated {
                    bindAll(id, Provenance.invalidated,
                        { entity(it) },
                        { entity(it) },
                        { addToBundle(bundle, entity(it) { buildEntity(bundle, this) })})
                }
            }

            qualified(id, Provenance.wasStartedBy,
                { wasStartedBy(it) },
                { wasStartedBy(it) },
                { addToBundle(bundle, wasStartedBy(it) { buildEntity(bundle, this) })},
                { qualifiedStart(it) { buildEntityInfluence(bundle, this) }})

            qualified(id, Provenance.wasEndedBy,
                { wasEndedBy(it) },
                { wasEndedBy(it) },
                { addToBundle(bundle, wasEndedBy(it) { buildEntity(bundle, this) })},
                { qualifiedEnd(it) { buildEntityInfluence(bundle, this) }})

            qualifiedAll(id, Provenance.wasAssociatedWith,
                { wasAssociatedWith(it) },
                { wasAssociatedWith(it) },
                { addToBundle(bundle, wasAssociatedWith(it) { buildAgent(bundle, this) })},
                { qualifiedAssociation(it) { buildAssociation(bundle, this) }})

            qualifiedAll(id, Provenance.used,
                { used(it) },
                { used(it) },
                { addToBundle(bundle, used(it) { buildEntity(bundle, this) })},
                { qualifiedUsage(it) { buildEntityInfluence(bundle, this) }})

            qualifiedAll(id, Provenance.wasInformedBy,
                { wasInformedBy(it) },
                { wasInformedBy(it) },
                { addToBundle(bundle, wasInformedBy(it) { buildActivity(bundle, this) })},
                { qualifiedCommunication(it) { buildActivityInfluence(bundle, this) }})
        }
    }

    private fun buildAgent(bundle: BundleBuilder, scope: AgentBuilder) {
        val type = if (scope.id is IRI) findAltAgentType(scope.id) else Agent.Type.Agent
        buildAgent(bundle, type, scope)
    }

    private fun buildAgent(bundle: BundleBuilder, type: Agent.Type, scope: AgentBuilder) {
        scope.apply {
            type(type)
            addAttributes(this)

            qualifiedAll(id, Provenance.actedOnBehalfOf,
                { actedOnBehalfOf(it) },
                { actedOnBehalfOf(it) },
                { addToBundle(bundle, actedOnBehalfOf(it) { buildAgent(bundle, this) })},
                { qualifiedDelegation(it) { buildAgentInfluence(bundle, this) }})
        }
    }

    private fun buildEntity(bundle: BundleBuilder, scope: AbstractEntityBuilder<*>) {
        scope.apply {
            addAttributes(this)

            addLiteral(id, Provenance.generatedAtTime) { generatedAtTime(StardogUtils.toInstant(it)) }
            addLiteral(id, Provenance.invalidatedAtTime) { invalidatedAtTime(StardogUtils.toInstant(it)) }

            bindIRI(id, Provenance.alternateOf,
                { alternateOf(it) },
                { alternateOf(it) },
                { addToBundle(bundle, alternateOf(it) { buildEntity(bundle, this) })})

            bindIRI(id, Provenance.specializationOf,
                { specializationOf(it) },
                { specializationOf(it) },
                { addToBundle(bundle, specializationOf(it) { buildEntity(bundle, this) })})

            qualified(id, Provenance.wasAttributedTo,
                { wasAttributedTo(it) },
                { wasAttributedTo(it) },
                { addToBundle(bundle, wasAttributedTo(it) { buildAgent(bundle, this) })},
                { qualifiedAttribution(it) { buildAgentInfluence(bundle, this) }})

            qualified(id, Provenance.wasGeneratedBy,
                { wasGeneratedBy(it) },
                { wasGeneratedBy(it) },
                { addToBundle(bundle, wasGeneratedBy(it) { buildActivity(bundle, this) })},
                { qualifiedGeneration(it) { buildActivityInfluence(bundle, this) }})

            qualified(id, Provenance.wasInvalidatedBy,
                { wasInvalidatedBy(it) },
                { wasInvalidatedBy(it) },
                { addToBundle(bundle, wasInvalidatedBy(it) { buildActivity(bundle, this) })},
                { qualifiedInvalidation(it) { buildActivityInfluence(bundle, this) }})

            qualified(id, Provenance.wasDerivedFrom,
                { wasDerivedFrom(it) },
                { wasDerivedFrom(it) },
                { addToBundle(bundle, wasDerivedFrom(it) { buildEntity(bundle, this) })},
                { qualifiedDerivation(it) { buildDerivation(bundle, this) }})

            qualified(id, Provenance.hadPrimarySource,
                { hadPrimarySource(it) },
                { hadPrimarySource(it) },
                { addToBundle(bundle, hadPrimarySource(it) { buildEntity(bundle, this) })},
                { qualifiedPrimarySource(it) { buildDerivation(bundle, this) }})

            qualified(id, Provenance.wasQuotedFrom,
                { wasQuotedFrom(it) },
                { wasQuotedFrom(it) },
                { addToBundle(bundle, wasQuotedFrom(it) { buildEntity(bundle, this) })},
                { qualifiedQuotation(it) { buildDerivation(bundle, this) }})

            qualified(id, Provenance.wasRevisionOf,
                { wasRevisionOf(it) },
                { wasRevisionOf(it) },
                { addToBundle(bundle, wasRevisionOf(it) { buildEntity(bundle, this) })},
                { qualifiedRevision(it) { buildDerivation(bundle, this) }})
        }
    }

    private fun buildInfluence(scope: InfluenceBuilder<*, *>) {
        scope.apply {
            addAttributes(this)

            addLiteral(id, Provenance.atTime) { attribute(Provenance.atTime, StardogUtils.toInstant(it)) }

            bindResource(id, Provenance.atLocation,
                { atLocation(it) },
                { atLocation(it) },
                { atLocation(it) { addAttributes(this) }})

            bindResource(id, Provenance.hadRole,
                { hadRole(it) },
                { hadRole(it) },
                { hadRole(it) { addAttributes(this) }})
        }
    }

    private fun buildActivityInfluence(bundle: BundleBuilder, scope: ActivityInfluenceBuilder<*>) {
        scope.apply {
            buildInfluence(this)

            bindIRI(id, Provenance.activity,
                { activity(it) },
                { activity(it) },
                { addToBundle(bundle, activity(it) { buildActivity(bundle, this) })})
        }
    }

    private fun buildAgentInfluence(bundle: BundleBuilder, scope: AgentInfluenceBuilder<*>) {
        scope.apply {
            buildInfluence(this)

            bindIRI(id, Provenance.agent,
                { agent(it) },
                { agent(it) },
                { addToBundle(bundle, agent(it) { buildAgent(bundle, this) })})

            bindIRI(id, Provenance.hadActivity,
                { hadActivity(it) },
                { hadActivity(it) },
                { addToBundle(bundle, hadActivity(it) { buildActivity(bundle, this) })})
        }
    }

    private fun buildEntityInfluence(bundle: BundleBuilder, scope: EntityInfluenceBuilder<*>) {
        scope.apply {
            buildInfluence(this)

            bindIRI(id, Provenance.entity,
                { entity(it) },
                { entity(it) },
                { addToBundle(bundle, entity(it) { buildEntity(bundle, this) })})

            bindIRI(id, Provenance.hadActivity,
                { hadActivity(it) },
                { hadActivity(it) },
                { addToBundle(bundle, hadActivity(it) { buildActivity(bundle, this) })})
        }
    }

    private fun buildAssociation(bundle: BundleBuilder, scope: AssociationBuilder) {
        scope.apply {
            buildAgentInfluence(bundle, this)

            bindIRI(id, Provenance.hadPlan,
                { hadPlan(it) },
                { hadPlan(it) },
                { addToBundle(bundle, hadPlan(it) { buildEntity(bundle, this) })})
        }
    }

    private fun buildDerivation(bundle: BundleBuilder, scope: AbstractDerivationBuilder<*>) {
        scope.apply {
            buildInfluence(this)

            bindIRI(id, Provenance.hadGeneration,
                { hadGeneration(it) },
                { hadGeneration(it) },
                { hadGeneration(it) { buildActivityInfluence(bundle, this) }})

            bindIRI(id, Provenance.hadUsage,
                { hadUsage(it) },
                { hadUsage(it) },
                { hadUsage(it) { buildEntityInfluence(bundle, this) }})
        }
    }

    private fun addAttributes(scope: ObjectWithAttributesBuilder<*>) {
        scope.apply {
            data
                .filter { it.subject() == id }
                .filterNot { it.predicate().namespace() == Provenance.namespace || it.predicate() == RDF.TYPE }
                .forEach { attribute(it.predicate(), it.`object`()) }
        }
    }

    private fun findAltAgentType(id: IRI): Agent.Type {
        return when (val otherType = objects(id, RDF.TYPE).filterNot { it == Provenance.Agent }.firstOrNull() ?: Provenance.Agent) {
            Provenance.Agent -> Agent.Type.Agent
            Provenance.Person -> Agent.Type.Person
            Provenance.Organization -> Agent.Type.Organization
            Provenance.SoftwareAgent -> Agent.Type.Software
            else -> error("Invalid agent type: $otherType")
        }
    }

    private fun addLiteral(id: Resource, property: IRI, adder: (Literal) -> Unit) {
        objects(id, property).firstOrNull()?.also {
            require(it is Literal) {
                "Object '$id' for property '${property.localName()}' was not of the correct type '${it.javaClass.simpleName}'"
            }
            adder(it)
        }
    }

    private inline fun <reified V : ProvenanceObject> bindIRI(id: Resource, property: IRI, withResource: (IRI) -> Unit,
          withObj: (V) -> Unit, withBuild: (IRI) -> V) {
        objects(id, property).firstOrNull()?.also {
            require(it is IRI) {
                "Object '$id' for property '${property.localName()}' was not of the correct type '${it.javaClass.simpleName}'"
            }
            bind(it, withResource, withObj, withBuild)
        }
    }

    private inline fun <reified V : ProvenanceObject> bindResource(id: Resource, property: IRI, withResource: (Resource) -> Unit,
           withObj: (V) -> Unit, withBuild: (Resource) -> V) {
        objects(id, property).firstOrNull()?.also {
            require(it is Resource) {
                "Object '$id' for property '${property.localName()}' was not of the correct type '${it.javaClass.simpleName}'"
            }
            bind(it, withResource, withObj, withBuild)
        }
    }

    private inline fun <reified V : ProvenanceObject> bindAll(id: Resource, property: IRI, withResource: (IRI) -> Unit,
          withObj: (V) -> Unit, withBuild: (IRI) -> V) {
        objects(id, property).forEach {
            require(it is IRI) {
                "Object '$id' for property '${property.localName()}' was not of the correct type '${it.javaClass.simpleName}'"
            }
            bind(it, withResource, withObj, withBuild)
        }
    }

    private inline fun <reified V : ProvenanceObject> qualified(id: Resource, property: IRI, withResource: (IRI) -> Unit,
        withObj: (V) -> Unit, withBuild: (IRI) -> V, qAdder: (Resource) -> Influence<V>) {
        val qualProperty = Provenance.qualifications[property]
        require(qualProperty != null) { "No qualified property found for: ${property.localName()}" }
        val qualifications = mutableSetOf<Resource>()
        objectResources(id, qualProperty)
            .firstOrNull()
            ?.also { qualifications.add(qAdder(it).influencer().id) }

        objectResources(id, property)
            .filterIsInstance(IRI::class.java)
            .filterNot { qualifications.contains(it) }
            .firstOrNull()
            ?.also { bind(it, withResource, withObj, withBuild) }
    }

    private inline fun <reified V : ProvenanceObject> qualifiedAll(id: Resource, property: IRI, withResource: (IRI) -> Unit,
       withObj: (V) -> Unit, withBuild: (IRI) -> V, qAdder: (Resource) -> Influence<V>) {
        val qualProperty = Provenance.qualifications[property]
        require(qualProperty != null) { "No qualified property found for: ${property.localName()}" }
        val qualifications = mutableSetOf<Resource>()
        objectResources(id, qualProperty).forEach { qualifications.add(qAdder(it).influencer().id) }

        objectResources(id, property)
            .filterIsInstance(IRI::class.java)
            .filterNot { qualifications.contains(it) }
            .forEach { bind(it, withResource, withObj, withBuild) }
    }

    private fun objects(id: Resource, property: IRI) : Sequence<Value> {
        return data
            .asSequence()
            .filter { it.subject() == id && it.predicate() == property }
            .map { it.`object`() }
    }

    private fun objectResources(id: Resource, property: IRI) : Sequence<Resource> {
        return objects(id, property).filterIsInstance(Resource::class.java)
    }
}