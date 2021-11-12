package io.docrozza.provenance.rdf

import com.stardog.stark.*
import com.stardog.stark.io.RDFFormat
import com.stardog.stark.io.RDFFormats
import io.docrozza.provenance.model.*
import io.docrozza.provenance.rdf.StardogUtils.iri
import io.docrozza.provenance.rdf.StardogUtils.use
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.stream.Collectors
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test as test

class KnowledgeGraphBuilderTest : ProvenanceTest() {

    companion object {
        // agents
        val chartgen = EX.iri("chartgen")
        val derek = EX.iri("derek")
        val edith = EX.iri("edith")

        // activities
        val compile1 = EX.iri("compile1")
        val compose1 = EX.iri("compose1")
        val correct1 = EX.iri("correct1")
        val illustrate1 = EX.iri("illustrate1")

        // entities
        val instructions = EX.iri("instructions")
        val article = EX.iri("article")
        val articleV1 = EX.iri("articleV1")
        val articleV2 = EX.iri("articleV2")
        val chart1 = EX.iri("chart1")
        val chart2 = EX.iri("chart2")
        val composition1 = EX.iri("composition1")
        val dataset1 = EX.iri("dataset1")
        val dataset2 = EX.iri("dataset2")
        val quoteInBlogEntry = EX.iri("quoteInBlogEntry-20130326")
        val regionList = EX.iri("regionList")

        // roles
        val analyst = EX.iri("analyst")
        val composedData = EX.iri("composedData")
        val dataToCompose = EX.iri("dataToCompose")
        val regionsToAggregateBy = EX.iri("regionsToAggregateBy")
        val roles = setOf(analyst, composedData, dataToCompose, regionsToAggregateBy)

        val bundle1 = EX.iri("b1")
        val bundle2 = EX.iri("b2")
        val bundle3 = EX.iri("b3")
        val bundle4 = EX.iri("b4")

        val entity = EX.iri("entity")
        val agent = EX.iri("agent")
        val activity2 = EX.iri("activity2")
        val activity3 = EX.iri("activity3")
        val activity4 = EX.iri("activity4")
        val plan = EX.iri("plan")
    }

    @test fun emptyContent() {
        assertFails { checkInput("", RDFFormats.TURTLE, bundle1) }
    }

    @test fun bundleNotMatchingContext() {
        val rdf = """
            :b1 {
               :activity a prov:Activity .
               :this a prov:Bundle .
            }""".trimIndent()
        assertFails { checkInput(rdf, RDFFormats.TRIG, bundle1) }
    }

    @test fun simpleBundle() {
        val bundle = checkInput(":b1 a prov:Bundle .", RDFFormats.TURTLE, bundle1)
        assertNotNull(bundle)
        assertEquals(bundle1, bundle.id)
    }

    @test fun mixOfQualifiedAndBaseProperty() {
        val rdf = """
            :b1 a prov:Bundle .
            :compose1 a prov:Activity ;
               prov:qualifiedUsage [
                  a prov:Usage ;
                  prov:entity :dataset1 ;
                  prov:hadRole :dataToCompose ;
               ] ;
               prov:used :dataset1, :regionList .""".trimIndent()
        val bundle = checkInput(rdf, RDFFormats.TURTLE, bundle1)
        assertNotNull(bundle)
        assertEquals(bundle1, bundle.id)

        val itemMap = bundle.items
            .groupBy { it.id }
            .map { it.value.first() }

        assertEquals(4, itemMap.size, "should be 4 objects in the bundle")
        itemMap.forEach { obj ->
            when (obj.id) {
                compose1 -> {
                    assertTrue(obj is Activity)
                    assertEquals(0, obj.properties.count())

                    var used = 0
                    obj.used.forEach {
                        if (it.id == regionList) {
                            assertTrue(it.isRef)
                        } else {
                            assertTrue(it.isQualified)
                            val qualification = it.qualification()
                            assertEquals(dataset1, qualification.entity.id)
                            assertEquals(dataToCompose, qualification.hadRole?.id)
                        }

                        used++
                    }

                    assertEquals(2, used)
                }
                dataToCompose -> assertRef(obj)
                dataset1 -> assertRef(obj)
                regionList -> assertRef(obj)
                else -> error("Unrecognized object '${obj.id}'")
            }
        }
    }

    @test fun primerExample() {
        val rdf = """
            :b1 a prov:Bundle .
            :analyst a prov:Role .
            :article dcterms:title "Crime rises in cities" ;
               a prov:Entity .
            :articleV1 a prov:Entity ;
               prov:specializationOf :article .
            :articleV2 a prov:Entity ;
               prov:alternateOf :articleV1 ;
               prov:specializationOf :article .
            :chart1 a prov:Entity ;
               prov:generatedAtTime "2012-03-02T10:30:00Z"^^xsd:dateTime ;
               prov:wasAttributedTo :derek ;
               prov:wasGeneratedBy :illustrate1 .
            :chart2 a prov:Entity ;
               prov:generatedAtTime "2012-04-01T16:21:00+01:00"^^xsd:dateTime ;
               prov:wasDerivedFrom :dataset2 ;
               prov:wasRevisionOf :chart1 .
            :chartgen a prov:Agent, prov:Organization ;
               foaf:name "Chart Generators Inc" .
            :compile1 a prov:Activity .
            :compose1 a prov:Activity ;
               prov:qualifiedAssociation [
                  a prov:Association ;
                  prov:agent :derek ;
                  prov:hadRole :analyst ;
               ] ;
               prov:qualifiedUsage [
                  a prov:Usage ;
                  prov:entity :dataset1 ;
                  prov:hadRole :dataToCompose ;
               ], [
                  a prov:Usage ;
                  prov:entity :regionList ;
                  prov:hadRole :regionsToAggregateBy ;
               ] ;
               prov:used :dataset1, :regionList ;
               prov:wasAssociatedWith :derek .
            :composedData a prov:Role .
            :composition1 a prov:Entity ;
               prov:qualifiedGeneration [
                  a prov:Generation ;
                  prov:activity :compose1 ;
                  prov:hadRole :composedData ;
               ] ;
               prov:wasGeneratedBy :compose1 .
            :correct1 a prov:Activity ;
               prov:endedAtTime "2012-04-01T16:21:00+01:00"^^xsd:dateTime ;
               prov:qualifiedAssociation [
                  a prov:Association ;
                  prov:agent :edith ;
                  prov:hadPlan :instructions ;
               ] ;
               prov:startedAtTime "2012-03-31T10:21:00+01:00"^^xsd:dateTime ;
               prov:wasAssociatedWith :edith .
            :dataToCompose a prov:Role .
            :dataset1 a prov:Entity .
            :dataset2 a prov:Entity ;
               prov:wasGeneratedBy :correct1 ;
               prov:wasRevisionOf :dataset1 .
            :derek a prov:Agent, prov:Person ;
               prov:actedOnBehalfOf :chartgen ;
               foaf:givenName "Derek" ;
               foaf:mbox <mailto:derek@example.org> .
            :edith a prov:Agent .
            :illustrate1 a prov:Activity ;
               prov:used :composition1 ;
               prov:wasAssociatedWith :derek .
            :instructions a prov:Plan .
            :quoteInBlogEntry-20130326 a prov:Entity ;
               prov:wasQuotedFrom :article .
            :regionList a prov:Entity .
            :regionsToAggregateBy a prov:Role .""".trimIndent()

        val bundle = checkInput(rdf, RDFFormats.TURTLE, bundle1)
        assertNotNull(bundle)
        assertEquals(bundle1, bundle.id)
        assertEquals(0, bundle.links.count())
        assertEquals(0, bundle.bundleIncludes())
        assertEquals(0, bundle.properties.count())

        var count = 0
        bundle.items.forEach {
            when (it) {
                is Activity -> checkActivites(it)
                is Agent -> checkAgent(it)
                is Entity -> checkEntity(it)
                is Plan -> checkPlan(it)
                is Role -> checkRole(it)
                else -> Unit
            }

            count++
        }
        assertEquals(22, count, "should have found all the object in the bundle")
    }

//    @test fun cyclicReferences() {
//        // cycle in the bundles
//        val rdf = """
//                :other {
//                   :otherActivity a prov:Activity ;
//                      prov:asInBundle :this ;
//                      prov:mentionOf :activity .
//                   :other a prov:Bundle .
//                }
//                :this {
//                   :activity a prov:Activity ;
//                      prov:asInBundle :other ;
//                      prov:mentionOf :otherActivity .
//                   :this a prov:Bundle .
//                }""".trimIndent()
//        assertFails { checkInput(rdf, RDFFormats.TRIG, EX.iri("this")) }
//        assertFails { checkInput(rdf, RDFFormats.TRIG, EX.iri("other")) }
//    }
//
//    @test fun extendedCyclicReferences() {
//        // cycle in the bundles
//        val rdf = """
//                :other {
//                   :otherActivity a prov:Activity ;
//                      prov:asInBundle :another ;
//                      prov:mentionOf :anotherActivity .
//                   :other a prov:Bundle .
//                }
//                :another {
//                   :anotherActivity a prov:Activity ;
//                      prov:asInBundle :this ;
//                      prov:mentionOf :activity .
//                   :another a prov:Bundle .
//                }
//                :this {
//                   :activity a prov:Activity ;
//                      prov:asInBundle :this ;
//                      prov:mentionOf :otherActivity .
//                   :this a prov:Bundle .
//                }""".trimIndent()
//        assertFails { checkInput(rdf, RDFFormats.TRIG, EX.iri("this")) }
//        assertFails { checkInput(rdf, RDFFormats.TRIG, EX.iri("other")) }
//    }

    @test fun nestedBundles() {
        val rdf = """
            :b1 {
               :b1 a prov:Bundle .
               :entity a prov:Entity ;
                  prov:asInBundle :b2 ;
                  prov:mentionOf :activity2 .
            }
            :b2 {
               :activity2 a prov:Activity .
               :activity3 a prov:Activity ;
                  prov:asInBundle :b3 ;
                  prov:mentionOf :agent .
               :activity4 a prov:Activity ;
                  prov:asInBundle :b4 ;
                  prov:mentionOf :plan .
               :b2 a prov:Bundle .
            }
            :b3 {
               :agent a prov:Agent .
               :b3 a prov:Bundle .
            }
            :b4 {
               :b4 a prov:Bundle .
               :plan a prov:Plan .
            }""".trimIndent()

        checkBundle(checkInput(rdf, RDFFormats.TRIG, bundle1))
    }

    private fun checkInput(content: String, format: RDFFormat, context: Resource) : Bundle {
        if (content.isNotEmpty()) {
            connectionFactory.use {
                val rdfStream = "$PREFIXES\n$content".byteInputStream()

                val io = it.add().io().format(format)
                if (format == RDFFormats.TURTLE) {
                    io.context(context).stream(rdfStream)
                } else {
                    io.stream(rdfStream)
                }
            }
        }

        return KnowledgeGraphBuilder.toBundle(context, connectionFactory)
    }

    private fun checkPlan(entity: Entity) {
        assertEquals(instructions, entity.id)
        assertEquals(0, entity.properties.count())
    }

    private fun checkRole(role: Role) {
        assertTrue(roles.contains(role.id))
        assertEquals(0, role.properties.count())
    }

    private fun checkEntity(entity: Entity) {
        when (entity.id) {
            article -> {
                assertEquals(1, entity.properties.count())
                assertEquals("Crime rises in cities", label(entity, DCTERMS.iri("title")))
            }
            articleV1 -> {
                assertEquals(0, entity.properties.count())
                assertEquals(article, entity.specializationOf?.id)
            }
            articleV2 -> {
                assertEquals(0, entity.properties.count())
                assertEquals(article, entity.specializationOf?.id)
                assertEquals(articleV1, entity.alternateOf?.id)
            }
            chart1 -> {
                assertEquals(0, entity.properties.count())
                assertEquals("2012-03-02T10:30:00Z", datetime(entity.generatedAtTime))
                assertEquals(derek, entity.wasAttributedTo?.id)
                assertEquals(illustrate1, entity.wasGeneratedBy?.toRef()?.id)
            }
            chart2 -> {
                assertEquals(0, entity.properties.count())
                assertEquals("2012-04-01T15:21:00Z", datetime(entity.generatedAtTime))
                assertEquals(dataset2, entity.wasDerivedFrom?.id)
                assertEquals(chart1, entity.wasRevisionOf?.toRef()?.id)
            }
            composition1 -> {
                assertEquals(0, entity.properties.count())
                val generation = entity.wasGeneratedBy!!
                assertTrue(generation.isQualified)
                val qualification = generation.qualification()
                assertEquals(compose1, qualification.activity.id)
                assertEquals(composedData, qualification.hadRole?.id)
            }
            dataset1 -> {
                assertEquals(0, entity.properties.count())
            }
            dataset2 -> {
                assertEquals(0, entity.properties.count())
                assertEquals(correct1, entity.wasGeneratedBy?.id)
                assertEquals(dataset1, entity.wasRevisionOf?.toRef()?.id)
            }
            quoteInBlogEntry -> {
                assertEquals(0, entity.properties.count())
                assertEquals(article, entity.wasQuotedFrom?.toRef()?.id)
            }
            regionList -> {
                assertEquals(0, entity.properties.count())
            }
            else -> error("Unexpected agent: ${entity.id}")
        }
    }

    private fun checkAgent(agent: Agent) {
        when (agent.id) {
            chartgen -> {
                assertEquals(Agent.Type.Organization, agent.type)
                assertEquals(1, agent.properties.count())
                assertEquals("Chart Generators Inc", label(agent, FOAF.iri("name")))
            }
            derek -> {
                assertEquals(Agent.Type.Person, agent.type)
                assertEquals(2, agent.properties.count())
                assertEquals("Derek", label(agent, FOAF.iri("givenName")))
                assertEquals(Values.iri("mailto:derek@example.org"), ref(agent, FOAF.iri("mbox")))
                assertEquals(chartgen, agent.actedOnBehalfOf.first().id)
            }
            edith -> {
                assertEquals(Agent.Type.Agent, agent.type)
                assertEquals(0, agent.properties.count())
            }
            else -> error("Unexpected agent: ${agent.id}")
        }
    }

    private fun checkActivites(activity: Activity) {
        when (activity.id) {
            compile1 -> {
                assertEquals(0, activity.properties.count())
            }
            compose1 -> {
                assertEquals(0, activity.properties.count())

                val used = activity.used
                    .filter { it.isQualified }
                    .map { it.qualification() }
                    .map { Pair(it.entity.id, it.hadRole?.id) }
                    .toSet()
                assertEquals(2, used.size)
                assertEquals(
                    setOf(Pair(dataset1, dataToCompose), Pair(regionList, regionsToAggregateBy)),
                    used)

                val association = activity.wasAssociatedWith.first()
                assertTrue(association.isQualified)
                val qualification = association.qualification()
                assertEquals(derek, qualification.agent.id)
                assertEquals(analyst, qualification.hadRole?.id)

            }
            correct1 -> {
                assertEquals(0, activity.properties.count())
                assertEquals("2012-03-31T09:21:00Z", datetime(activity.startedAtTime))
                assertEquals("2012-04-01T15:21:00Z", datetime(activity.endedAtTime))

                val association = activity.wasAssociatedWith.first()
                assertTrue(association.isQualified)
                val qualification = association.qualification()
                assertEquals(edith, qualification.agent.id)
                assertEquals(instructions, qualification.hadPlan?.id)
            }
            illustrate1 -> {
                assertEquals(0, activity.properties.count())
                assertEquals(EX.iri("composition1"), activity.used.first().id)
                assertEquals(derek, activity.wasAssociatedWith.first().id)
            }
            else -> error("Unexpected activity: ${activity.id}")
        }
    }

    private fun ref(obj: ProvenanceObject, property: IRI) : Resource {
        return obj.values(property)
            .filterIsInstance(Resource::class.java)
            .first()
    }

    private fun datetime(instant: Instant?) : String {
        return if (instant == null) "" else DateTimeFormatter.ISO_DATE_TIME.format(instant.atOffset(ZoneOffset.UTC))
    }

    private fun label(obj: ProvenanceObject, property: IRI) : String {
        return obj.values(property)
            .filterIsInstance(Literal::class.java)
            .first()
            .label()
    }

    private fun assertRef(obj: Identifiable) {
        assertTrue(obj is RefOrValue<*>)
        assertTrue(obj.isRef)
    }

    private fun checkBundle(bundle: Bundle) {
        when (bundle.id) {
            bundle1 -> {
                assertEquals(setOf(entity), items(bundle))
                val includes = includes(bundle)
                assertEquals(1, includes.size)
                checkBundle(includes.first())
            }
            bundle2 -> {
                assertEquals(setOf(activity2, activity3, activity4), items(bundle))
                val includes = includes(bundle)
                assertEquals(2, includes.size)
                includes.forEach { checkBundle(it) }
            }
            bundle3 -> {
                assertEquals(setOf(agent), items(bundle))
                assertTrue(includes(bundle).isEmpty())
            }
            bundle4 -> {
                assertEquals(setOf(plan), items(bundle))
                assertTrue(includes(bundle).isEmpty())
            }
            else -> error("Unexpected bundle ${bundle.id}")
        }
    }

    private fun items(bundle: Bundle) : Set<IRI> {
        return bundle.items.map { it.id as IRI }.toSet()
    }

    private fun includes(bundle: Bundle) : Set<Bundle> {
        return bundle.links.map { it.asInBundle.about() }.toSet()
    }
}