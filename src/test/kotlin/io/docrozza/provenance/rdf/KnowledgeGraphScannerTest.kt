package io.docrozza.provenance.rdf

import com.stardog.stark.Values
import com.stardog.stark.vocabs.RDF
import com.stardog.stark.vocabs.RDFS
import io.docrozza.provenance.BundleData
import io.docrozza.provenance.bundle
import io.docrozza.provenance.model.Agent
import io.docrozza.provenance.model.Bundle
import io.docrozza.provenance.rdf.StardogUtils.iri
import io.docrozza.provenance.rdf.StardogUtils.use
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test as test

class KnowledgeGraphScannerTest : ProvenanceTest() {

    @test fun emptyBundle() {
        checkOutput(BundleData(EX.iri("bundle")),":bundle a prov:Bundle .")
    }

    @test fun singleEntity() {
        val bundle = bundle { entity(EX.iri("entity")) }
        checkOutput(bundle, "[] a prov:Bundle . :entity a prov:Entity .")
    }

    @test fun singleActivity() {
        val bundle = bundle { activity(EX.iri("activity")) }
        checkOutput(bundle, "[] a prov:Bundle . :activity a prov:Activity .")
    }

    @test fun singleAgent() {
        val bundle = bundle { agent(EX.iri("agent")) }
        checkOutput(bundle, "[] a prov:Bundle . :agent a prov:Agent .")
    }

    @test fun singlePerson() {
        val bundle = bundle {agent(EX.iri("agent")) { type(Agent.Type.Person) }}
        checkOutput(bundle, "[] a prov:Bundle . :agent a prov:Agent, prov:Person .")
    }

    @test fun singleOrganisation() {
        val bundle = bundle {agent(EX.iri("agent")) { type(Agent.Type.Organization) }}
        checkOutput(bundle, "[] a prov:Bundle . :agent a prov:Agent, prov:Organization .")
    }

    @test fun singleSoftwareAgent() {
        val bundle = bundle { agent(EX.iri("agent")) { type(Agent.Type.Software) }}
        checkOutput(bundle, "[] a prov:Bundle . :agent a prov:Agent, prov:SoftwareAgent .")
    }

    @test fun bundleWithExtraAttributes() {
        val bundle = bundle { attribute(RDF.PREDICATE, RDF.PROPERTY) }
        checkOutput(bundle, "[] rdf:predicate rdf:Property ; a prov:Bundle .")
    }

    @test fun simpleEntities() {
        val bundle = bundle {
            val institution = EX.iri("institution")

            agent(institution)
            val author = agent(EX.iri("author")) {
                type(Agent.Type.Person)
                attribute(RDFS.COMMENT, "what a guy")
                actedOnBehalfOf(institution)
            }
            entity(EX.iri("quote")) {
                attribute(RDFS.LABEL, "cool quote")
                wasAttributedTo(author)
            }
        }
        checkOutput(bundle, """
            [] a prov:Bundle .
            :author a prov:Agent, prov:Person ;                
               rdfs:comment "what a guy" ;                
               prov:actedOnBehalfOf :institution .                
            :institution a prov:Agent .                
            :quote a prov:Entity ;                
               rdfs:label "cool quote" ;                
               prov:wasAttributedTo :author .""".trimIndent())
    }

    @test fun simpleEntitiesAltLayout() {
        val bundle = bundle {
            entity(EX.iri("quote")) {
                attribute(RDFS.LABEL, "cool quote")
                wasAttributedTo(EX.iri("author")) {
                    type(Agent.Type.Person)
                    attribute(RDFS.COMMENT, "what a guy")
                    actedOnBehalfOf(EX.iri("institution"))
                }
            }
        }
        checkOutput(bundle, """
            [] a prov:Bundle .
            :author a prov:Agent, prov:Person ;                
               rdfs:comment "what a guy" ;                
               prov:actedOnBehalfOf :institution .                
            :institution a prov:Agent .                
            :quote a prov:Entity ;                
               rdfs:label "cool quote" ;                
               prov:wasAttributedTo :author .""".trimIndent())
    }

    @test fun simpleQualifications() {
        val bundle = bundle {
            entity(EX.iri("quote")) {
                attribute(RDFS.LABEL, "cool quote")
                qualifiedAttribution {
                    agent(EX.iri("author")) {
                        type(Agent.Type.Person)
                        attribute(RDFS.COMMENT, "what a guy")
                        qualifiedDelegation {
                            agent(EX.iri("institution"))
                            hadRole(EX.iri("writer"))
                        }
                    }
                    atLocation(EX.iri("StickySituation"))
                }
            }
        }
        checkOutput(bundle, """
            [] a prov:Bundle .
            :StickySituation a prov:Location .                
            :author a prov:Agent, prov:Person ;                
               rdfs:comment "what a guy" ;                
               prov:actedOnBehalfOf :institution ;                
               prov:qualifiedDelegation [                
                  a prov:Delegation ;                
                  prov:agent :institution ;                
                  prov:hadRole :writer ;                
               ] .                
            :institution a prov:Agent .                
            :quote a prov:Entity ;                
               rdfs:label "cool quote" ;                
               prov:qualifiedAttribution [                
                  a prov:Attribution ;                
                  prov:agent :author ;                
                  prov:atLocation :StickySituation ;                
               ] ;                
               prov:wasAttributedTo :author .                
            :writer a prov:Role .""".trimIndent())
    }

    @test fun primerExample() {
        val dataset1 = EX.iri("dataset1")

        val bundle = bundle {
            val derek = agent(EX.iri("derek")) {
                type(Agent.Type.Person)
                attribute(FOAF.iri("givenName"), "Derek")
                attribute(FOAF.iri("mbox"), Values.iri("mailto:derek@example.org"))
                actedOnBehalfOf(EX.iri("chartgen")) {
                    type(Agent.Type.Organization)
                    attribute(FOAF.iri("name"), "Chart Generators Inc")
                }
            }

            entity(EX.iri("chart2")) {
                generatedAtTime(datetime("2012-04-01T15:21:00Z"))
                wasRevisionOf(EX.iri("chart1")) {
                    generatedAtTime(datetime("2012-03-02T09:30:00Z"))
                    wasGeneratedBy(EX.iri("illustrate1")) {
                        wasAssociatedWith(derek)
                        used(EX.iri("composition1")) {
                            qualifiedGeneration {
                                activity(EX.iri("compose1")) {
                                    qualifiedUsage {
                                        entity(dataset1)
                                        hadRole(EX.iri("dataToCompose"))
                                    }
                                    qualifiedUsage {
                                        entity(EX.iri("regionList"))
                                        hadRole(EX.iri("regionsToAggregateBy"))
                                    }
                                    qualifiedAssociation {
                                        agent(derek)
                                        hadRole(EX.iri("analyst"))
                                    }
                                }
                                hadRole(EX.iri("composedData"))
                            }
                        }
                    }
                    wasAttributedTo(derek)
                }
                wasDerivedFrom(EX.iri("dataset2")) {
                    wasRevisionOf(dataset1)
                    wasGeneratedBy(EX.iri("correct1")) {
                        startedAtTime(datetime("2012-03-31T09:21:00Z"))
                        endedAtTime(datetime("2012-04-01T15:21:00Z"))
                        qualifiedAssociation {
                            agent(EX.iri("edith"))
                            hadPlan(EX.iri("instructions"))
                        }
                    }
                }
            }

            activity(EX.iri("compile1"))

            val article = entity(EX.iri("article")) { attribute(DCTERMS.iri("title"), "Crime rises in cities") }

            entity(EX.iri("quoteInBlogEntry-20130326")) {
                wasQuotedFrom(article)
            }

            entity(EX.iri("articleV2")) {
                specializationOf(article)
                alternateOf(EX.iri("articleV1")) {
                    specializationOf(article)
                }
            }
        }

        checkOutput(bundle, """
            [] a prov:Bundle .
            :analyst a prov:Role .                
            :article dcterms:title "Crime rises in cities" ;                
               a prov:Entity .                
            :articleV1 a prov:Entity ;                
               prov:specializationOf :article .                
            :articleV2 a prov:Entity ;                
               prov:alternateOf :articleV1 ;                
               prov:specializationOf :article .                
            :chart1 a prov:Entity ;                
               prov:generatedAtTime "2012-03-02T09:30:00Z"^^xsd:dateTime ;                
               prov:wasAttributedTo :derek ;                
               prov:wasGeneratedBy :illustrate1 .                
            :chart2 a prov:Entity ;                
               prov:generatedAtTime "2012-04-01T15:21:00Z"^^xsd:dateTime ;                
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
               prov:endedAtTime "2012-04-01T15:21:00Z"^^xsd:dateTime ;                
               prov:qualifiedAssociation [                
                  a prov:Association ;                
                  prov:agent :edith ;                
                  prov:hadPlan :instructions ;                
               ] ;                
               prov:startedAtTime "2012-03-31T09:21:00Z"^^xsd:dateTime ;                
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
            :regionsToAggregateBy a prov:Role .""".trimIndent())
    }

    @test fun locationsWithExtraAttributes() {
        val activity = EX.iri("activity")
        val entity = EX.iri("entity")
        val location1 = EX.iri("location1")
        val location2 = EX.iri("location2")
        val extra1 = EX.iri("extra1")
        val extra2 = EX.iri("extra2")

        val bundle = bundle(EX.iri("this")) {
            entity(entity) {
                qualifiedGeneration {
                    activity(activity)
                    atLocation(location1)
                }
            }

            location(location1) {
                attribute(RDF.PREDICATE, extra1)
            }
            location(location2) {
                attribute(RDF.PREDICATE, extra2)
            }

            resource(extra1, RDF.PROPERTY) {
                attribute(RDF.PREDICATE, RDF.BAG)
            }
            resource(extra2, RDF.PROPERTY) {
                attribute(RDF.PREDICATE, RDF.SEQ)
            }
        }
        checkOutput(bundle, """
            :activity a prov:Activity .                
            :entity a prov:Entity ;                
               prov:qualifiedGeneration [                
                  a prov:Generation ;                
                  prov:activity :activity ;                
                  prov:atLocation :location1 ;                
               ] ;                
               prov:wasGeneratedBy :activity .                
            :extra1 rdf:predicate rdf:Bag ;                
               a rdf:Property .                
            :extra2 rdf:predicate rdf:Seq ;                
               a rdf:Property .                
            :location1 rdf:predicate :extra1 ;                
               a prov:Location .                
            :location2 rdf:predicate :extra2 ;                
               a prov:Location .                
            :this a prov:Bundle .""".trimIndent())
    }

    @test fun entityExtraAttribute() {
        val activity = EX.iri("activity")
        val entity = EX.iri("entity")
        val extra = EX.iri("extra")

        val bundle = bundle(EX.iri("this")) {
            entity(entity) {
                qualifiedGeneration {
                    activity(activity)
                }
            }

            resource(entity, io.docrozza.provenance.Provenance.Entity) {
                attribute(RDF.PREDICATE, extra)
            }
        }
        checkOutput(bundle, """
            :activity a prov:Activity .                
            :entity rdf:predicate :extra ;                
               a prov:Entity ;                
               prov:qualifiedGeneration [                
                  a prov:Generation ;                
                  prov:activity :activity ;                
               ] ;                
               prov:wasGeneratedBy :activity .                
            :this a prov:Bundle .""".trimIndent())
    }

    @test fun entityExtraAttributeExtraType() {
        val activity = EX.iri("activity")
        val entity = EX.iri("entity")
        val extra = EX.iri("extra")

        val bundle = bundle(EX.iri("this")) {
            entity(entity) {
                qualifiedGeneration {
                    activity(activity)
                }
            }

            resource(entity, RDF.PROPERTY) {
                attribute(RDF.PREDICATE, extra)
            }
        }
        checkOutput(bundle, """
            :this a prov:Bundle .
            :activity a prov:Activity .                
            :entity rdf:predicate :extra ;                
               a rdf:Property, prov:Entity ;                
               prov:qualifiedGeneration [                
                  a prov:Generation ;                
                  prov:activity :activity ;                
               ] ;                
               prov:wasGeneratedBy :activity .""".trimIndent())
    }

    @test fun activitiesAcrossBundles() {
        val activity1 = EX.iri("activity1")
        val activity2 = EX.iri("activity2")

        val bundle = bundle(EX.iri("this")) {
            activity(activity1)

            links {
                link {
                    activity(activity1)
                    mentionOf(activity2)
                    bundle(EX.iri("other")) {
                        activity(activity2)
                    }
                }
            }
        }
        checkOutput(bundle, """
            GRAPH :other {                
               :activity2 a prov:Activity .                
               :other a prov:Bundle .                
            }                
            GRAPH :this {                
               :activity1 a prov:Activity ;                
                  prov:asInBundle :other ;                
                  prov:mentionOf :activity2 .                
               :this a prov:Bundle .                
            }""".trimIndent(), true)
    }

    @test fun agentsAcrossBundles() {
        val agent1 = EX.iri("agent1")
        val agent2 = EX.iri("agent2")

        val bundle = bundle(EX.iri("this")) {
            agent(agent1)

            links {
                link {
                    agent(agent1)
                    mentionOf(agent2)
                    bundle(EX.iri("other")) {
                        agent(agent2)
                    }
                }
            }
        }
        checkOutput(bundle, """
            GRAPH :other {                
               :agent2 a prov:Agent .                
               :other a prov:Bundle .                
            }                
            GRAPH :this {                
               :agent1 a prov:Agent ;                
                  prov:asInBundle :other ;                
                  prov:mentionOf :agent2 .                
               :this a prov:Bundle .                
            }""".trimIndent(), true)
    }

    @test fun entitiesAcrossBundles() {
        val entity1 = EX.iri("entity1")
        val entity2 = EX.iri("entity2")

        val bundle = bundle(EX.iri("this")) {
            entity(entity1)

            links {
                link {
                    entity(entity1)
                    mentionOf(entity2)
                    bundle(EX.iri("other")) {
                        entity(entity2)
                    }
                }
            }
        }
        checkOutput(bundle, """
            GRAPH :other {                
               :entity2 a prov:Entity .                
               :other a prov:Bundle .                
            }                
            GRAPH :this {                
               :entity1 a prov:Entity ;                
                  prov:asInBundle :other ;                
                  prov:mentionOf :entity2 .                
               :this a prov:Bundle .                
            }""".trimIndent(), true)
    }

    @test fun nestedBundles() {
        val entity = EX.iri("entity")
        val agent = EX.iri("agent")
        val activity1 = EX.iri("activity1")
        val activity2 = EX.iri("activity2")
        val plan = EX.iri("plan")

        val bundle = bundle(EX.iri("b1")) {
            entity(entity)

            links {
                link {
                    entity(entity)
                    mentionOf(activity1)
                    bundle(EX.iri("b2")) {
                        activity(activity1)
                        activity(activity2)

                        links {
                            link {
                                activity(activity2)
                                mentionOf(plan)
                                bundle(EX.iri("b4")) {
                                    plan(plan)
                                }
                            }
                            link {
                                activity(activity1)
                                mentionOf(agent)
                                bundle(EX.iri("b3")) {
                                    agent(agent)
                                }
                            }
                        }
                    }
                }
            }
        }
        checkOutput(bundle, """
            GRAPH :b1 {                
               :b1 a prov:Bundle .                
               :entity a prov:Entity ;                
                  prov:asInBundle :b2 ;                
                  prov:mentionOf :activity1 .                
            }                
            GRAPH :b2 {                
               :activity1 a prov:Activity ;                
                  prov:asInBundle :b3 ;                
                  prov:mentionOf :agent .                
               :activity2 a prov:Activity ;                
                  prov:asInBundle :b4 ;                
                  prov:mentionOf :plan .                
               :b2 a prov:Bundle .                
            }                
            GRAPH :b3 {                
               :agent a prov:Agent .                
               :b3 a prov:Bundle .                
            }                
            GRAPH :b4 {                
               :b4 a prov:Bundle .                
               :plan a prov:Plan .                
            }""".trimIndent(), true)
    }

    private fun checkOutput(bundle: Bundle, expected: String, useNamed: Boolean = false) {
        KnowledgeGraphScanner.addToGraph(bundle, connectionFactory)

        val askQuery = """
            PREFIX dcterms: <$DCTERMS>
            PREFIX prov: <$PROV>
            PREFIX foaf: <$FOAF>
            PREFIX : <$EX>
            ASK FROM ${if (useNamed) "NAMED" else "" } <tag:stardog:api:context:all> WHERE {
                $expected
            }""".trimIndent()
        assertTrue(connectionFactory.use { it.ask(askQuery).execute() }, "Should find the query pattern: $askQuery")
    }
}