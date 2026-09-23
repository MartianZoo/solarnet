package dev.martianzoo.tfm.tools

import dev.martianzoo.pets.ast.Action
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Effect
import dev.martianzoo.pets.ast.Effect.Trigger.ByTrigger
import dev.martianzoo.pets.ast.Effect.Trigger.Or as OrTrigger
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.Instruction.By
import dev.martianzoo.pets.ast.Instruction.Each
import dev.martianzoo.pets.ast.Instruction.Gain
import dev.martianzoo.pets.ast.Instruction.Gated
import dev.martianzoo.pets.ast.Instruction.NoOp
import dev.martianzoo.pets.ast.Instruction.Or
import dev.martianzoo.pets.ast.Instruction.Per
import dev.martianzoo.pets.ast.Instruction.Remove
import dev.martianzoo.pets.ast.Instruction.Then
import dev.martianzoo.pets.ast.Instruction.Transform
import dev.martianzoo.pets.ast.Instruction.Transmute
import dev.martianzoo.pets.ast.InstructionGroup
import dev.martianzoo.pets.ast.Metric
import dev.martianzoo.pets.ast.PetNode
import dev.martianzoo.pets.ast.PropertyName
import dev.martianzoo.pets.ast.Requirement
import dev.martianzoo.pets.ast.ScaledExpression.Scalar.XScalar
import dev.martianzoo.pets.types.Class as PetClass
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.canon.cardActions
import dev.martianzoo.tfm.canon.cardBack
import dev.martianzoo.tfm.canon.cardEffects
import dev.martianzoo.tfm.canon.cardImmediate
import dev.martianzoo.tfm.canon.cardRequirement
import kotlin.test.Test
import kotlin.test.assertEquals

internal class PetsCardTutorialTest {
  @Test
  internal fun acceptedSlidesRevealCardsOnlyAfterRequiredConcepts() {
    val sieve =
        CardSieve(
            Canon.cards.toList(),
            mutableListOf(
                // Permanent scope exclusions for the concept slides audited here.
                excludeCardsMatching { card, _, _, _, _ ->
                  cardBack(card)?.className !in
                      setOf(cn("PreludeCard"), cn("ProjectCard"), cn("StandardCorporationCard"))
                },
                excludeCardsMatching { _, _, requirement, _, _ ->
                  requirement != null &&
                      !isRequirementOnlyAboutTags(requirement) &&
                      !containsOnlyMinimumGlobalParameterRequirements(requirement) &&
                      !isRequirementOnlyAboutCardResources(requirement)
                },
                excludeCardsMatching { card, _, _, _, _ ->
                  card.declaration.invariants.isNotEmpty()
                },
                excludeCardsMatching { card, _, _, _, _ ->
                  PropertyName("autoSelectWhen") in card.declaration.properties
                },
                excludeCardsMatching { _, immediate, _, _, _ ->
                  containsUnsupportedTutorialSyntax(
                      immediate,
                      ACCEPTED_SEQUENCE_BASE_CLASS_NAMES +
                          setOf(
                              cn("OceanTile"),
                              cn("ProjectCard"),
                              cn("StandardResource"),
                              cn("This"),
                          ) +
                          CARD_RESOURCE_CLASS_NAMES,
                      allowInstructionChoice = true,
                      allowThisCardResource = true,
                      allowExplicitOtherCardResource = true,
                      allowUnboundCardResource = true,
                  )
                },
                excludeCardsMatching { _, _, _, actions, _ ->
                  actions.any {
                    containsUnsupportedTutorialSyntax(
                        it,
                        ACCEPTED_SEQUENCE_BASE_CLASS_NAMES +
                            setOf(
                                cn("OceanTile"),
                                cn("ProjectCard"),
                                cn("StandardResource"),
                                cn("This"),
                            ) +
                            CARD_RESOURCE_CLASS_NAMES,
                        allowInstructionChoice = true,
                        allowThisCardResource = true,
                        allowExplicitOtherCardResource = true,
                        allowUnboundCardResource = true,
                    )
                  }
                },
                excludeCardsMatching { _, _, _, _, effects ->
                  effects.any {
                    containsUnsupportedTutorialSyntax(
                        it,
                        ACCEPTED_SEQUENCE_BASE_CLASS_NAMES +
                            setOf(
                                cn("OceanTile"),
                                cn("ProjectCard"),
                                cn("StandardResource"),
                                cn("This"),
                                cn("End"),
                                cn("VictoryPoint"),
                            ) +
                            CARD_RESOURCE_CLASS_NAMES,
                        allowInstructionChoice = true,
                        allowThisCardResource = true,
                        allowExplicitOtherCardResource = true,
                        allowUnboundCardResource = true,
                        allowGatedInstruction = true,
                    )
                  }
                },
                excludeCardsMatching { _, immediate, _, actions, effects ->
                  (listOfNotNull<PetNode>(immediate) + actions + effects).any {
                    cn("StandardResource") in it.descendantsOfType<ClassName>()
                  }
                },
                excludeCardsMatching { _, immediate, _, actions, effects ->
                  (listOfNotNull<PetNode>(immediate) + actions + effects).any {
                    it.descendantsOfType<Then>().isNotEmpty()
                  }
                },
                excludeCardsMatching { card, _, _, _, _ ->
                  card.className == cn("HomeostasisBureau")
                },
                excludeCardsMatching { _, _, _, _, effects ->
                  effects.any { it.trigger.descendantsOfType<OrTrigger>().isNotEmpty() }
                },
                excludeCardsMatching { _, immediate, _, actions, effects ->
                  (listOfNotNull<PetNode>(immediate) + actions + effects).any { node ->
                    node.descendantsOfType<Metric.Scaled>().any { it.unit > 1 }
                  }
                },
                excludeCardsMatching { _, immediate, _, actions, effects ->
                  (listOfNotNull<PetNode>(immediate) + actions + effects).any {
                    cn("VictoryPoint") in it.descendantsOfType<ClassName>()
                  } || effects.any { it.descendantsOfType<Gated>().isNotEmpty() }
                },
                excludeCardsMatching { _, _, _, _, effects -> effects.isNotEmpty() },
                // Removed from bottom to top as concepts are introduced. One release may span
                // several visual slides when the outline subdivides a concept for pacing.
                excludeCardsMatching { _, _, requirement, _, _ ->
                  requirement?.descendantsOfType<ClassName>()?.contains(cn("OceanTile")) == true
                },
                excludeCardsMatching { _, immediate, _, actions, effects ->
                  (listOfNotNull<PetNode>(immediate) + actions + effects).any {
                    cn("OceanTile") in it.descendantsOfType<ClassName>()
                  }
                },
                excludeCardsMatching { _, immediate, _, actions, effects ->
                  (listOfNotNull<PetNode>(immediate) + actions + effects).any {
                    it.descendantsOfType<Or>().isNotEmpty()
                  }
                },
                excludeCardsMatching { _, immediate, _, actions, effects ->
                  (listOfNotNull<PetNode>(immediate) + actions + effects).any {
                    cn("ProjectCard") in it.descendantsOfType<ClassName>()
                  }
                },
                excludeCardsMatching { _, immediate, _, actions, effects ->
                  (listOfNotNull<PetNode>(immediate) + actions + effects)
                      .flatMap { it.descendantsOfType<Gain>() }
                      .any {
                        it.gaining.className in CARD_RESOURCE_CLASS_NAMES &&
                            hasNoWrittenArgumentsOrRefinement(it.gaining)
                      }
                },
                excludeCardsMatching { _, immediate, requirement, actions, effects ->
                  (listOfNotNull<PetNode>(requirement, immediate) + actions + effects)
                      .flatMap { it.descendantsOfType<Expression>() }
                      .filter { it.className in CARD_RESOURCE_CLASS_NAMES }
                      .any { expression ->
                        (!expression.argumentsSpecified &&
                            expression.arguments.isEmpty() &&
                            expression.refinement == null) ||
                            (expression.argumentsSpecified &&
                                expression.arguments.size == 1 &&
                                !(expression.arguments.single().simple &&
                                    expression.arguments.single().className == cn("This") &&
                                    expression.refinement == null))
                      }
                },
                excludeCardsMatching { _, immediate, _, actions, effects ->
                  (listOfNotNull<PetNode>(immediate) + actions + effects).any { node ->
                    node.descendantsOfType<ClassName>().any(CARD_RESOURCE_CLASS_NAMES::contains)
                  } || actions.size > 1
                },
                excludeCardsMatching { _, _, _, actions, _ -> actions.isNotEmpty() },
                excludeCardsMatching { _, immediate, _, actions, effects ->
                  (listOfNotNull<PetNode>(immediate) + actions + effects).any { node ->
                    node.descendantsOfType<Per>().isNotEmpty() ||
                        node.descendantsOfType<Action.Cost.Per>().isNotEmpty()
                  }
                },
                excludeCardsMatching { _, _, requirement, _, _ ->
                  requirement != null &&
                      !containsOnlyMinimumGlobalParameterRequirements(requirement)
                },
                excludeCardsMatching { _, _, requirement, _, _ -> requirement != null },
                excludeCardsMatching { _, immediate, _, actions, effects ->
                  (listOfNotNull<PetNode>(immediate) + actions + effects).any { node ->
                    node.descendantsOfType<ClassName>().any {
                      it in setOf(cn("OxygenStep"), cn("TemperatureStep"), cn("VenusStep"))
                    }
                  }
                },
                excludeCardsMatching { _, immediate, _, _, _ ->
                  productionTransformsIn(immediate).any {
                    it.descendantsOfType<Remove>().isNotEmpty()
                  }
                },
                excludeCardsMatching { _, immediate, _, _, effects ->
                  containsRemoveOutsideProductionTransform(immediate) ||
                      effects.any { it.descendantsOfType<Remove>().isNotEmpty() }
                },
                excludeCardsMatching { _, immediate, _, _, _ ->
                  productionTransformsIn(immediate).any {
                    InstructionGroup.of(it.instruction).size != 1
                  }
                },
                excludeCardsMatching { _, immediate, _, _, _ ->
                  productionTransformsIn(immediate).isNotEmpty() &&
                      (immediate == null ||
                          immediate.instructions.size != 1 ||
                          immediate.instructions.single() !is Transform ||
                          (immediate.instructions.single() as Transform).transformKind != "PROD")
                },
                excludeCardsMatching { _, immediate, _, _, _ ->
                  productionTransformsIn(immediate).isNotEmpty()
                },
                excludeCardsMatching { _, immediate, _, _, _ ->
                  containsUnsupportedTutorialSyntax(
                      immediate,
                      setOf(cn("MC"), cn("EventCard"), cn("TradeFleet")),
                      allowInstructionChoice = true,
                  )
                },
                excludeCardsMatching { _, immediate, _, _, _ ->
                  immediate != null && !immediate.isEmpty()
                },
            ),
        )

    assertEquals(emptySet(), sieve.newMatches())

    assertEquals(
        setOf("Donation"),
        sieve.removeLastPredicateAndFindNewMatches(),
    )
    assertEquals(
        setOf("MineralDeposit", "ReleaseOfInertGases", "SupplyDrop"),
        sieve.removeLastPredicateAndFindNewMatches(),
    )
    assertEquals(
        setOf(
            "AcquiredCompany",
            "AdaptedLichen",
            "GeothermalPower",
            "GiantSpaceMirror",
            "ImportOfAdvancedGhg",
            "MicroMills",
            "Mine",
            "PowerGeneration",
            "PowerPlant",
            "SolarReflectors",
            "Soletta",
            "Sponsors",
            "TitaniumMine",
        ),
        sieve.removeLastPredicateAndFindNewMatches(),
    )
    assertEquals(
        setOf(
            "AlliedBank",
            "ImportedGhg",
            "MiningOperations",
            "Mohole",
            "NitrogenShipment",
            "OrbitalConstructionYard",
            "SolarWindPower",
            "Supplier",
        ),
        sieve.removeLastPredicateAndFindNewMatches(),
    )
    assertEquals(
        setOf(
            "AntiDesertificationTechniques",
            "Biofuels",
            "DomeFarming",
            "IndustrialMicrobes",
            "MartianIndustries",
            "MetalsCompany",
            "MoholeExcavation",
        ),
        sieve.removeLastPredicateAndFindNewMatches(),
    )
    assertEquals(
        setOf("BusinessEmpire", "GalileanMining", "Potatoes"),
        sieve.removeLastPredicateAndFindNewMatches(),
    )
    assertEquals(
        setOf(
            "BiosphereSupport",
            "BuildingIndustries",
            "CarbonateProcessing",
            "FuelFactory",
            "FueledGenerators",
            "GhgFactories",
            "InvestmentLoan",
            "Loan",
            "LunarBeam",
            "MagneticFieldDome",
            "NuclearPower",
            "PeroxidePower",
            "RadChemFactory",
            "SocietySupport",
        ),
        sieve.removeLastPredicateAndFindNewMatches(),
    )
    assertEquals(
        setOf(
            "DeepWellHeating",
            "GhgImportFromVenus",
            "GiantSolarCollector",
            "GiantSolarShade",
            "HugeAsteroid",
            "MetalRichAsteroid",
            "OrbitalReflectors",
            "SmeltingPlant",
            "StripMine",
            "VenusL1Shade",
            "WaterToVenus",
        ),
        sieve.removeLastPredicateAndFindNewMatches(),
    )
    assertEquals(
        setOf(
            "Bushes",
            "Grass",
            "Heather",
            "IshtarMining",
            "Lichen",
            "NeutralizerFactory",
        ),
        sieve.removeLastPredicateAndFindNewMatches(),
    )
    assertEquals(
        setOf(
            "CoronaExtractor",
            "DuskLaserMining",
            "FusionPower",
            "LunaGovernor",
            "MagneticShield",
            "MiningQuota",
            "Omnicourt",
            "SisterPlanetSupport",
            "SpaceHotels",
            "VenusGovernor",
        ),
        sieve.removeLastPredicateAndFindNewMatches(),
    )
    assertEquals(
        setOf(
            "Cartel",
            "Insects",
            "PowerGrid",
            "RobotPollinators",
            "Satellites",
            "SulphurExports",
        ),
        sieve.removeLastPredicateAndFindNewMatches(),
    )
    assertEquals(
        setOf(
            "CaretakerContract",
            "EquatorialMagnetizer",
            "Ironworks",
            "Meltworks",
            "OreProcessor",
            "SpaceMirrors",
            "Steelworks",
            "Teslaract",
            "UndergroundDetonations",
            "VenusMagnetizer",
        ),
        sieve.removeLastPredicateAndFindNewMatches(),
    )
    assertEquals(
        setOf(
            "DeuteriumExport",
            "ExtractorBalloons",
            "ForcedPrecipitation",
            "GhgProducingBacteria",
            "JetStreamMicroscrappers",
            "LocalShading",
            "NitriteReducingBacteria",
            "RegolithEaters",
        ),
        sieve.removeLastPredicateAndFindNewMatches(),
    )
    assertEquals(
        setOf("FloatingRefinery", "SoilEnrichment"),
        sieve.removeLastPredicateAndFindNewMatches(),
    )
    assertEquals(
        setOf(
            "AerobrakedAmmoniaAsteroid",
            "FloaterPrototypes",
            "FloaterTechnology",
            "ImportedNitrogen",
            "ImportedNutrients",
            "SymbioticFungus",
            "VenusSoils",
        ),
        sieve.removeLastPredicateAndFindNewMatches(),
    )
    assertEquals(
        setOf(
            "BactoviralResearch",
            "Biolab",
            "DevelopmentCenter",
            "IoResearchOutpost",
            "TechnologyDemonstration",
            "UnmiContractor",
        ),
        sieve.removeLastPredicateAndFindNewMatches(),
    )
    assertEquals(
        setOf(
            "ArtificialPhotosynthesis",
            "AsteroidRights",
            "AtmoCollectors",
            "BioPrintingFacility",
            "DirectedHeatUsage",
            "LocalHeatTrapping",
            "LunarExports",
        ),
        sieve.removeLastPredicateAndFindNewMatches(),
    )
    assertEquals(
        setOf(
            "AquiferTurbines",
            "BlackPolarDust",
            "CometAiming",
            "ConvoyFromEuropa",
            "GreatAquifer",
            "IceAsteroid",
            "IceCapMelting",
            "ImportedHydrogen",
            "MoholeLake",
            "PermafrostExtraction",
            "PolarIndustries",
            "SubterraneanReservoir",
            "TowingAComet",
        ),
        sieve.removeLastPredicateAndFindNewMatches(),
    )
    assertEquals(
        setOf("Algae", "Moss", "NitrophilicMoss", "SnowAlgae", "WaterSplittingPlant"),
        sieve.removeLastPredicateAndFindNewMatches(),
    )
    assertEquals(
        setOf(
            "AlbedoPlants",
            "InterplanetaryCinematics",
            "MediaGroup",
            "MeatIndustry",
            "PointLuna",
            "TerraformingDeal",
            "TopsoilContract",
        ),
        sieve.removeLastPredicateAndFindNewMatches(),
    )
    assertEquals(
        setOf(
            "AdvancedEcosystems",
            "AerialMappers",
            "Airliners",
            "AiCentral",
            "AsteroidMining",
            "AtalantaPlanitiaLab",
            "Atmoscoop",
            "BeamFromAThoriumAsteroid",
            "BreathingFilters",
            "BribedCommittee",
            "CallistoPenalMines",
            "EarthElevator",
            "EosChasmaNationalPark",
            "Farming",
            "FoodFactory",
            "GeneRepair",
            "HeavyTaxation",
            "HousePrinting",
            "InterstellarColonyShip",
            "IoMiningIndustries",
            "JovianEmbassy",
            "KelpFarming",
            "LakeMarineris",
            "LightningHarvest",
            "LargeConvoy",
            "Livestock",
            "LuxuryFoods",
            "MethaneFromTitan",
            "MirandaResort",
            "NoctisFarming",
            "OrbitalCleanup",
            "Penguins",
            "PhysicsComplex",
            "PublicBaths",
            "RedSpotObservatory",
            "RefugeeCamps",
            "Research",
            "SecurityFleet",
            "SixteenPsyche",
            "SoilFactory",
            "SolarPower",
            "SpaceElevator",
            "SfMemorial",
            "SterlingVents",
            "StratosphericBirds",
            "SubCrustMeasurements",
            "TectonicStressPower",
            "TerraformingGanymede",
            "TitanAirScrapping",
            "TransNeptuneProbe",
            "Trees",
            "TropicalResort",
            "TundraFarming",
            "VenusianAnimals",
            "VestaShipyard",
            "WavePower",
            "Windmills",
            "LagrangeObservatory",
            "Solarnet",
        ),
        sieve.removeLastPredicateAndFindNewMatches(),
    )
    assertEquals(
        setOf(
            "AsteroidHollowing",
            "Extremophiles",
            "FloaterLeasing",
            "FloatingHabs",
            "JovianLanterns",
            "LunarMining",
            "MainBeltAsteroids",
            "MedicalLab",
            "SolarProbe",
            "Tardigrades",
            "VenusianInsects",
            "Worms",
        ),
        sieve.removeLastPredicateAndFindNewMatches(),
    )
  }

  @Test
  internal fun candidateConceptsMatchExpectedCanonicalCards() {
    assertEquals(
        setOf(
            "Archaebacteria",
            "ColonizerTrainingCamp",
            "DesignedMicroorganisms",
            "DustSeals",
            "ElectroCatapult",
            "ExtremeColdFungus",
            "MartianSurvey",
            "ProtectedGrowth",
            "SpinInducingAsteroid",
            "StaticHarvesting",
        ),
        candidateMatches(allowRequirement = { it is Requirement.Max }) { _, _, requirement, _, _ ->
          requirement is Requirement.Max
        },
    )
    assertEquals(
        setOf("Aphrodite", "ArcticAlgae", "HomeostasisBureau"),
        candidateMatches(
            deckClassNames =
                setOf(cn("PreludeCard"), cn("ProjectCard"), cn("StandardCorporationCard")),
            extraClassNames = setOf(cn("Anyone")),
            allowByTrigger = true,
            allowRequirement = { requirement ->
              isRequirementOnlyAboutTags(requirement) ||
                  containsOnlyMinimumGlobalParameterRequirements(requirement) ||
                  requirement is Requirement.Max ||
                  isRequirementOnlyAboutCardResources(requirement)
            },
        ) { card, _, _, _, effects ->
          card.className == cn("HomeostasisBureau") ||
              effects.any { it.descendantsOfType<ByTrigger>().isNotEmpty() }
        },
    )
    assertEquals(
        setOf("Arklight"),
        candidateMatches { _, _, _, _, effects ->
          effects.any { it.trigger.descendantsOfType<OrTrigger>().isNotEmpty() }
        },
    )
    assertEquals(
        setOf("CloudTourism", "SaturnSurfing"),
        candidateMatches(allowMetricMaximum = true) { _, immediate, _, actions, effects ->
          (listOfNotNull<PetNode>(immediate) + actions + effects).any {
            it.descendantsOfType<Metric.Max>().isNotEmpty()
          }
        },
    )
    assertEquals(
        setOf("Mangrove", "ProtectedValley"),
        candidateMatches(extraClassNames = SIMPLE_TILE_CLASS_NAMES + SIMPLE_AREA_CLASS_NAMES) {
            _,
            immediate,
            _,
            actions,
            effects ->
          hasOnlySimpleExplicitTilePlacements(immediate, actions, effects)
        },
    )
    assertEquals(
        setOf(
            "AntiGravityTechnology",
            "CheungShingMars",
            "EarthCatapult",
            "EarthOffice",
            "MassConverter",
            "QuantumExtractor",
            "Shuttles",
            "SkyDocks",
            "SpaceStation",
            "Teractor",
            "TerralabsResearch",
            "VenusWaystation",
            "WarpDrive",
        ),
        candidateMatches(
            extraClassNames = setOf(cn("PayingFor"), cn("Owed"), cn("CardFront"), cn("Class")),
        ) { _, _, _, _, effects ->
          effects.any { effect ->
            cn("PayingFor") in effect.trigger.descendantsOfType<ClassName>() &&
                effect.instruction.descendantsOfType<Remove>().any {
                  it.removing.className == cn("Owed")
                }
          }
        },
    )
    assertEquals(
        setOf(
            "BiomassCombustors",
            "Birds",
            "CloudSeeding",
            "EnergyTapping",
            "Fish",
            "Hackers",
            "HeatTrappers",
            "PowerSupplyConsortium",
            "SmallAnimals",
            "SubZeroSaltFish",
        ),
        candidateMatches(extraClassNames = setOf(cn("Anyone"))) { _, immediate, _, _, _ ->
          productionTransformsIn(immediate).any { transform ->
            transform.descendantsOfType<Remove>().any {
              cn("Anyone") in it.removing.descendantsOfType<ClassName>()
            }
          }
        },
    )
    assertEquals(
        setOf(
            "AsteroidCard",
            "BigAsteroid",
            "Comet",
            "GiantIceAsteroid",
            "ImpactorSwarm",
            "MiningExpedition",
            "SmallAsteroid",
        ),
        candidateMatches(extraClassNames = setOf(cn("Anyone"))) { _, immediate, _, _, _ ->
          var removesResourceFromAnyPlayer = false
          immediate?.visitDescendants { descendant ->
            when {
              descendant is Transform && descendant.transformKind == "PROD" -> false
              descendant is Remove -> {
                if (cn("Anyone") in descendant.removing.descendantsOfType<ClassName>()) {
                  removesResourceFromAnyPlayer = true
                }
                false
              }
              else -> true
            }
          }
          removesResourceFromAnyPlayer
        },
    )
  }

  /**
   * Adapts a positive "this card still needs an unrevealed concept" test to the keep-predicate
   * shape consumed by [CardSieve]. The parameter types here are the real Pets AST types used by
   * every exclusion lambda above.
   */
  private fun excludeCardsMatching(
      cardNeedsUnrevealedConcept:
          (
              card: PetClass,
              immediate: InstructionGroup?,
              requirement: Requirement?,
              actions: List<Action>,
              effects: List<Effect>,
          ) -> Boolean,
  ): (PetClass) -> Boolean = { card ->
    !cardNeedsUnrevealedConcept(
        card,
        cardImmediate(card),
        cardRequirement(card),
        cardActions(card),
        cardEffects(card),
    )
  }

  private fun candidateMatches(
      extraClassNames: Set<ClassName> = emptySet(),
      deckClassNames: Set<ClassName> =
          setOf(cn("PreludeCard"), cn("ProjectCard"), cn("StandardCorporationCard")),
      allowInstructionChoice: Boolean = false,
      allowByTrigger: Boolean = false,
      allowMetricMaximum: Boolean = false,
      allowUnboundCardResources: Boolean = true,
      allowRequirement: (Requirement) -> Boolean = {
        isRequirementOnlyAboutTags(it) ||
            containsOnlyMinimumGlobalParameterRequirements(it) ||
            isRequirementOnlyAboutCardResources(it)
      },
      candidate:
          (
              card: PetClass,
              immediate: InstructionGroup?,
              requirement: Requirement?,
              actions: List<Action>,
              effects: List<Effect>,
          ) -> Boolean,
  ): Set<String> {
    val allowedClassNames =
        ACCEPTED_SEQUENCE_BASE_CLASS_NAMES +
            setOf(
                cn("OceanTile"),
                cn("ProjectCard"),
                cn("This"),
                cn("End"),
                cn("VictoryPoint"),
            ) +
            CARD_RESOURCE_CLASS_NAMES +
            extraClassNames
    return Canon.cards
        .asSequence()
        .filter { cardBack(it)?.className in deckClassNames }
        .filter { card -> cardRequirement(card)?.let(allowRequirement) != false }
        .filter { it.declaration.invariants.isEmpty() }
        .filter { PropertyName("autoSelectWhen") !in it.declaration.properties }
        .filter { card ->
          !containsUnsupportedTutorialSyntax(
              cardImmediate(card),
              allowedClassNames,
              allowInstructionChoice,
              allowMetricMaximum = allowMetricMaximum,
              allowThisCardResource = true,
              allowExplicitOtherCardResource = true,
              allowUnboundCardResource = allowUnboundCardResources,
          )
        }
        .filter { card ->
          cardActions(card).none {
            containsUnsupportedTutorialSyntax(
                it,
                allowedClassNames,
                allowInstructionChoice,
                allowMetricMaximum = allowMetricMaximum,
                allowThisCardResource = true,
                allowExplicitOtherCardResource = true,
                allowUnboundCardResource = allowUnboundCardResources,
            )
          }
        }
        .filter { card ->
          listOfNotNull(cardImmediate(card)).none {
            it.descendantsOfType<Gated>().isNotEmpty()
          } && cardActions(card).none { it.descendantsOfType<Gated>().isNotEmpty() }
        }
        .filter { card ->
          cardEffects(card).none {
            containsUnsupportedTutorialSyntax(
                it,
                allowedClassNames,
                allowMetricMaximum = allowMetricMaximum,
                allowThisCardResource = true,
                allowExplicitOtherCardResource = true,
                allowUnboundCardResource = allowUnboundCardResources,
                allowGatedInstruction = true,
                allowByTrigger = allowByTrigger,
            )
          }
        }
        .filter { card ->
          (listOfNotNull<PetNode>(cardImmediate(card)) + cardActions(card) + cardEffects(card))
              .none {
                it.descendantsOfType<Then>().isNotEmpty()
              }
        }
        .filter { card ->
          candidate(
              card,
              cardImmediate(card),
              cardRequirement(card),
              cardActions(card),
              cardEffects(card),
          )
        }
        .mapTo(linkedSetOf()) { it.className.toString() }
  }

  private class CardSieve(
      private val cards: List<PetClass>,
      private val activeKeepPredicates: MutableList<(PetClass) -> Boolean>,
  ) {
    private val returned = mutableSetOf<PetClass>()

    fun newMatches(): Set<String> =
        cards
            .filter { card -> card !in returned && activeKeepPredicates.all { it(card) } }
            .onEach(returned::add)
            .mapTo(linkedSetOf()) { it.className.toString() }

    fun removeLastPredicateAndFindNewMatches(): Set<String> {
      activeKeepPredicates.removeLast()
      return newMatches()
    }
  }

  private companion object {
    val ACCEPTED_SEQUENCE_BASE_CLASS_NAMES: Set<ClassName> =
        setOf(
                "AnimalTag",
                "BuildingTag",
                "CityTag",
                "Energy",
                "EarthTag",
                "Heat",
                "JovianTag",
                "MC",
                "MicrobeTag",
                "OxygenStep",
                "Plant",
                "PlantTag",
                "PowerTag",
                "ScienceTag",
                "SpaceTag",
                "Steel",
                "TemperatureStep",
                "TerraformRating",
                "Titanium",
                "VenusTag",
                "VenusStep",
                "EventCard",
                "TradeFleet",
            )
            .mapTo(linkedSetOf(), ::cn)
    val GLOBAL_STEP_CLASS_NAMES: Set<ClassName> =
        setOf("OxygenStep", "TemperatureStep", "VenusStep").mapTo(linkedSetOf(), ::cn)
    val TUTORIAL_TAG_CLASS_NAMES: Set<ClassName> =
        ACCEPTED_SEQUENCE_BASE_CLASS_NAMES.filterTo(linkedSetOf()) {
          it.toString().endsWith("Tag")
        }
    val CARD_RESOURCE_CLASS_NAMES: Set<ClassName> =
        Canon.classTable
            .allClasses()
            .filterTo(linkedSetOf()) {
              it.isSubtypeOf(Canon.classTable.getClass(cn("CardResource")))
            }
            .mapTo(linkedSetOf()) { it.className }
    val TILE_CLASS_NAMES: Set<ClassName> =
        Canon.classTable
            .allClasses()
            .filterTo(linkedSetOf()) { it.isSubtypeOf(Canon.classTable.getClass(cn("Tile"))) }
            .mapTo(linkedSetOf()) { it.className }
    val SIMPLE_AREA_CLASS_NAMES: Set<ClassName> =
        setOf("LandArea", "MarsArea", "NoctisArea", "VolcanicArea", "WaterArea")
            .mapTo(linkedSetOf(), ::cn)
    val SIMPLE_TILE_CLASS_NAMES: Set<ClassName> =
        setOf("CityTile", "GreeneryTile", "OceanTile").mapTo(linkedSetOf(), ::cn)

    fun hasOnlySimpleExplicitTilePlacements(
        immediate: InstructionGroup?,
        actions: List<Action>,
        effects: List<Effect>,
    ): Boolean {
      val tiles =
          (listOfNotNull<PetNode>(immediate) + actions + effects)
              .flatMap { it.descendantsOfType<Expression>() }
              .filter { it.className in TILE_CLASS_NAMES }
      return tiles.isNotEmpty() &&
          tiles.all { tile ->
            tile.argumentsSpecified &&
                tile.arguments.size == 1 &&
                tile.arguments.single().simple &&
                tile.arguments.single().className in SIMPLE_AREA_CLASS_NAMES &&
                tile.className in SIMPLE_TILE_CLASS_NAMES &&
                tile.refinement == null &&
                tile.typeVariableName == null
          }
    }

    fun isRequirementOnlyAboutCardResources(requirement: Requirement): Boolean {
      val classNames = requirement.descendantsOfType<ClassName>()
      return classNames.isNotEmpty() && classNames.all(CARD_RESOURCE_CLASS_NAMES::contains)
    }

    fun isRequirementOnlyAboutTags(requirement: Requirement): Boolean {
      val classNames = requirement.descendantsOfType<ClassName>()
      return classNames.isNotEmpty() && classNames.all(TUTORIAL_TAG_CLASS_NAMES::contains)
    }

    fun containsOnlyMinimumGlobalParameterRequirements(requirement: Requirement): Boolean =
        Requirement.split(requirement).all { part ->
          part is Requirement.Min &&
              part.descendantsOfType<ClassName>().let { classNames ->
                classNames.isNotEmpty() &&
                    classNames.all { it in GLOBAL_STEP_CLASS_NAMES || it == cn("OceanTile") }
              }
        }

    fun containsUnsupportedTutorialSyntax(
        node: PetNode?,
        allowedClassNames: Set<ClassName>,
        allowInstructionChoice: Boolean = false,
        allowMetricMaximum: Boolean = false,
        allowThisCardResource: Boolean = false,
        allowExplicitOtherCardResource: Boolean = false,
        allowUnboundCardResource: Boolean = false,
        allowGatedInstruction: Boolean = false,
        allowByTrigger: Boolean = false,
    ): Boolean {
      if (node == null) return false
      val expressions = node.descendantsOfType<Expression>()
      val explicitOtherCardResourceExpressions = expressions.filter {
        it.className in CARD_RESOURCE_CLASS_NAMES && hasOneExplicitArgumentOtherThanExactlyThis(it)
      }
      return node.descendantsOfType<ClassName>().any { it !in allowedClassNames } ||
          expressions.any { expression ->
            expression.typeVariableName != null ||
                when {
                  expression.className in CARD_RESOURCE_CLASS_NAMES ->
                      !((allowThisCardResource &&
                          hasExactlyThisAsItsOnlyArgumentWithoutRefinement(expression)) ||
                          (allowExplicitOtherCardResource &&
                              hasOneExplicitArgumentOtherThanExactlyThis(expression)) ||
                          (allowUnboundCardResource &&
                              hasNoWrittenArgumentsOrRefinement(expression)))
                  expression.className == cn("This") -> !allowThisCardResource || !expression.simple
                  else -> false
                }
          } ||
          node.descendantsOfType<Transform>().any { it.transformKind != "PROD" } ||
          node.descendantsOfType<Action.Cost.Transform>().any { it.transformKind != "PROD" } ||
          node.descendantsOfType<Metric.Transform>().isNotEmpty() ||
          (!allowMetricMaximum && node.descendantsOfType<Metric.Max>().isNotEmpty()) ||
          node.descendantsOfType<By>().isNotEmpty() ||
          (!allowByTrigger && node.descendantsOfType<ByTrigger>().isNotEmpty()) ||
          node.descendantsOfType<Each>().isNotEmpty() ||
          (!allowGatedInstruction && node.descendantsOfType<Gated>().isNotEmpty()) ||
          node.descendantsOfType<NoOp>().isNotEmpty() ||
          node.descendantsOfType<Transmute>().isNotEmpty() ||
          node.descendantsOfType<XScalar>().isNotEmpty() ||
          node.descendantsOfType<Requirement>().any { requirement ->
            !allowExplicitOtherCardResource ||
                explicitOtherCardResourceExpressions
                    .flatMap { it.arguments }
                    .none { requirement in it.descendantsOfType<Requirement>() }
          } ||
          (!allowInstructionChoice && node.descendantsOfType<Or>().isNotEmpty())
    }

    fun productionTransformsIn(node: PetNode?): List<Transform> =
        node?.descendantsOfType<Transform>().orEmpty().filter {
          it.transformKind == "PROD"
        }

    fun containsRemoveOutsideProductionTransform(node: PetNode?): Boolean {
      var found = false
      node?.visitDescendants { descendant ->
        when {
          descendant is Transform && descendant.transformKind == "PROD" -> false
          descendant is Remove -> {
            found = true
            false
          }
          else -> true
        }
      }
      return found
    }

    fun hasExactlyThisAsItsOnlyArgumentWithoutRefinement(expression: Expression): Boolean =
        expression.argumentsSpecified &&
            expression.arguments.size == 1 &&
            expression.arguments.single().simple &&
            expression.arguments.single().className == cn("This") &&
            expression.refinement == null

    fun hasOneExplicitArgumentOtherThanExactlyThis(expression: Expression): Boolean =
        expression.argumentsSpecified &&
            expression.arguments.size == 1 &&
            !hasExactlyThisAsItsOnlyArgumentWithoutRefinement(expression)

    fun hasNoWrittenArgumentsOrRefinement(expression: Expression): Boolean =
        !expression.argumentsSpecified &&
            expression.arguments.isEmpty() &&
            expression.refinement == null
  }
}
