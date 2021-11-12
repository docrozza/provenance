# provenance

[![standard-readme compliant](https://img.shields.io/badge/readme%20style-standard-brightgreen.svg?style=flat-square)](https://github.com/RichardLitt/standard-readme)

Kotlin library for handling provenance data.

## Table of Contents

- [Background](#background)
- [Install](#install)
- [Usage](#usage)
    - [IO](#io)
    - [DSL](#dsl)
- [Contributing](#contributing)
- [Future Work](#future-work)
- [Maintainers](#maintainers)
- [License](#license)

## Background

Provenance is information about entities, activities, and people involved in producing a piece of data or thing, which
can be used to form assessments about its quality, reliability or trustworthiness. From the W3C primer [here](https://www.w3.org/TR/prov-primer/):

>The provenance of digital objects represents their origins. PROV is a specification to express provenance records,
>which contain descriptions of the entities and activities involved in producing and delivering or otherwise influencing
>a given object. Provenance can be used for many purposes, such as understanding how data was collected so it can be
>meaningfully used, determining ownership and rights over an object, making judgements about information to determine
>whether to trust it, verifying that the process and steps used to obtain a result complies with given requirements, and
>reproducing how something was generated.

This repository provides:
 * IO functionality for reading/writing provenance from/to a [Stardog](https://www.stardog.com) repository
 * a [DSL](https://en.wikipedia.org/wiki/Domain-specific_language) for building provenance models

## Install

```gradle
dependencies {
    compile("io.docrozza:provenance:21.11")
}
```

The project uses [Calendar Versioning](https://calver.org) for version numbers.

## Usage

### IO

```kotlin
fun intoRdf(connectionFactory: Supplier<Connection>) : Bundle {
    /*
     * assume the following RDF structure in Stardog
      GRAPH :b1 {
         :b1 a prov:Bundle .
         :entity a prov:Entity ;
            prov:asInBundle :b2 ;
            prov:mentionOf :activity2 .
      }
      GRAPH :b2 {
         :activity2 a prov:Activity .
         :activity3 a prov:Activity ;
            prov:asInBundle :b3 ;
            prov:mentionOf :agent .
         :activity4 a prov:Activity ;
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
      }
     */
    return KnowledgeGraphBuilder.toBundle(Values.iri("http://www.example.org#"), connectionFactory)
}

fun fromRdf(connectionFactory: Supplier<Connection>) {
  val bundle = intoRdf(connectionFactory)
  KnowledgeGraphScanner.addToGraph(bundle, connectionFactory)
}
```

### DSL

```kotlin
fun dslExample() {
    val DCTERMS = "http://purl.org/dc/terms/"
    val FOAF = "http://xmlns.com/foaf/0.1/"
    val EX = "http://www.example.org#"
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
            generatedAtTime(datetime("2012-04-01T15:21:00"))
            wasRevisionOf(EX.iri("chart1")) {
                generatedAtTime(datetime("2012-03-02T10:30:00"))
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
                    startedAtTime(datetime("2012-03-31T09:21:00"))
                    endedAtTime(datetime("2012-04-01T15:21:00"))
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
}

fun String.iri(localName: String) = Values.iri(this, localName)

fun datetime(text: String) = LocalDateTime.parse(text).toInstant(ZoneOffset.UTC)
```

## Future Work

Adding IO functionality for [PROV-N](https://www.w3.org/TR/prov-n/)

## Contributing

Help appreciated :-) [Open an issue](https://github.com/docrozza/provenance/issues/new) or submit PRs.

NB to use the library or run the tests, a working Stardog installation is required. See
[here](https://docs.stardog.com/get-started/install-stardog/) for more information. To then run the test, the following
gradle project properties need to be set to create the embedded database:
 * stardogHome - the path to the directory to store the database and where the license file is stored
 * stardogLibs - the path to the database library JARs

## Maintainers

[@DocRozza](https://github.com/docrozza)

## License

[MIT](LICENSE) © Rory Steele
