package dev.martianzoo.pets.types

import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.api.CustomClass
import dev.martianzoo.pets.api.Exceptions.InvalidGameConfigException
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.data.ClassSelection
import dev.martianzoo.pets.data.GamePremise
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

/**
 * Which Classes a game premise includes through its selection closure. This is construction policy
 * rather than Type meaning, so `docs/type-system-spec.md` describes inhabitance (section 12), not
 * how the declaration closure is constructed.
 */
internal class PremiseSelectionTest {

  @Test
  internal fun `Audit is included in every premise`() {
    gameView(testCatalog("CLASS Unselected")).isIncluded(cn("Audit")) shouldBe true
  }

  @Test
  internal fun `custom class requirements are included with the custom class only`() {
    val declarations =
        """
        CLASS DependencySource : Custom
        CLASS RuntimeDependency
        """
            .trimIndent()
    val implementation =
        object : CustomClass(cn("DependencySource")) {
          override val requiredClassNames = setOf(cn("RuntimeDependency"))
        }
    val catalog =
        testCatalog(
            declarations,
            setOf(implementation),
        )

    val unselected = gameView(catalog)
    unselected.isIncluded(cn("RuntimeDependency")) shouldBe false

    val selected = gameView(catalog, "DependencySource")
    selected.isIncluded(cn("RuntimeDependency")) shouldBe true
  }

  @Test
  internal fun `dependency signatures include available vocabulary`() {
    val catalog =
        testCatalog(
            """
            CLASS SelectedContent<AvailableVocabulary>
            CLASS AvailableVocabulary
            """
                .trimIndent()
        )
    val table = gameView(catalog, "SelectedContent")

    table.isIncluded(cn("AvailableVocabulary")) shouldBe true
  }

  @Test
  internal fun `NOT exclusions do not add Classes to the premise closure`() {
    val catalog =
        testCatalog(
            """
            ABSTRACT CLASS Domain
            CLASS Holder<Domain>
            CLASS Included : Domain
            CLASS Excluded : Domain
            """
                .trimIndent()
        )
    val table = gameView(catalog, "Holder", "Included")

    table.isIncluded(cn("Holder")) shouldBe true
    table.isIncluded(cn("Excluded")) shouldBe false
    table.isInhabited(table.resolve(te("Holder<Domain(NOT Excluded)>"))) shouldBe true
  }

  @Test
  internal fun `structural dependencies include catalog-known classes`() {
    val catalog = testCatalog("CLASS Selected<Dependency>\nCLASS Dependency")

    val table = gameView(catalog, "Selected")

    table.isIncluded(cn("Dependency")) shouldBe true
  }

  @Test
  internal fun `locked vocabulary names the Module that makes it available`() {
    val catalog =
        testCatalog(
            "CLASS Locked\nCLASS UnlockingModule",
            moduleSelections = mapOf(cn("UnlockingModule") to emptySet()),
            classAvailabilityModules = mapOf(cn("Locked") to setOf(cn("UnlockingModule"))),
        )

    shouldThrow<InvalidGameConfigException> { gameView(catalog, "Locked") }
  }

  @Test
  internal fun `premise rejects a structurally included unrequested Module`() {
    val catalog =
        testCatalog(
            """
            ABSTRACT CLASS Module
            CLASS Requested<Other> : Module
            CLASS Other : Module
            """
                .trimIndent(),
            moduleSelections =
                mapOf(
                    cn("Requested") to emptySet(),
                    cn("Other") to emptySet(),
                ),
        )
    val premise = GamePremise(catalog, setOf(cn("Requested")), emptySet(), emptySet())

    shouldThrow<InvalidGameConfigException> { premise.classTable }
  }

  @Test
  internal fun `premise rejects structural inclusion of an excluded class`() {
    val catalog = testCatalog("CLASS Selected<Excluded>\nCLASS Excluded")
    val premise =
        GamePremise(
            catalog,
            emptySet(),
            setOf(
                ClassSelection(cn("Selected")),
                ClassSelection(cn("Excluded"), included = false),
            ),
            emptySet(),
        )

    shouldThrow<InvalidGameConfigException> { premise.classTable }
  }

  @Test
  internal fun `premise rejects structural inclusion of a conditionally excluded class`() {
    val catalog =
        testCatalog(
            """
            ABSTRACT CLASS Module
            CLASS Requested : Module { This:: EffectTarget }
            CLASS Conditional { HAS Flag }
            CLASS EffectTarget<Conditional>
            CLASS Flag
            """
                .trimIndent(),
            moduleSelections =
                mapOf(
                    cn("Requested") to
                        setOf(ClassSelection(cn("Conditional"), requirement = parse("Flag")))
                ),
        )
    val premise = GamePremise(catalog, setOf(cn("Requested")), emptySet(), emptySet())

    shouldThrow<InvalidGameConfigException> { premise.classTable }
  }

  @Test
  internal fun `class metrics do not include the represented class`() {
    val catalog = testCatalog("CLASS Querying { HAS MAX 0 Class<Represented> }\nCLASS Represented")
    val table = gameView(catalog, "Querying")

    table.isIncluded(cn("Represented")) shouldBe false
  }

  @Test
  internal fun `premise requirements reject selected abstract domains without inhabitants`() {
    val catalog =
        testCatalog(
            """
            ABSTRACT CLASS Selectable { requirement = Requirement? }
            ABSTRACT CLASS Domain
            CLASS Available : Domain
            CLASS Related<Domain>
            CLASS Candidate
            CLASS OtherCandidate
            CLASS Selected : Selectable {
              requirement = HAS "Candidate(HAS Related<Domain>) OR OtherCandidate(HAS Related<Domain>)"
            }
            """
                .trimIndent()
        )

    shouldThrow<InvalidGameConfigException> {
      gameView(catalog, "Selected", "Domain", "Related", "Candidate", "OtherCandidate")
    }

    val viable =
        gameView(catalog, "Selected", "Available", "Related", "Candidate", "OtherCandidate")
    viable.isInhabited(cn("Selected")) shouldBe true
    viable.isInhabited(cn("Available")) shouldBe true
  }

  @Test
  internal fun `premise requirements reject a false conjunct`() {
    val catalog =
        testCatalog(
            """
            ABSTRACT CLASS Selectable { requirement = Requirement? }
            ABSTRACT CLASS Empty
            ABSTRACT CLASS AlsoEmpty
            CLASS Available
            CLASS Selected : Selectable { requirement = HAS "Available, Empty" }
            CLASS AllTrue : Selectable { requirement = HAS "MAX 0 Empty, MAX 0 AlsoEmpty" }
            CLASS PartlyUnknown : Selectable { requirement = HAS "MAX 0 Empty, MAX 0 Available" }
            """
                .trimIndent()
        )

    shouldThrow<InvalidGameConfigException> {
      gameView(catalog, "Selected", "Available", "Empty")
    }
    gameView(catalog, "AllTrue", "Empty", "AlsoEmpty").isInhabited(cn("AllTrue")) shouldBe true
    gameView(catalog, "PartlyUnknown", "Empty", "Available")
        .isInhabited(cn("PartlyUnknown")) shouldBe true
  }

  @Test
  internal fun `only inevitable reachable mandatory removals make a premise unviable`() {
    val catalog =
        testCatalog(
            """
            ABSTRACT CLASS Empty
            ABSTRACT CLASS AlsoEmpty
            CLASS Available
            CLASS Direct { This:: -Empty! }
            CLASS Nested { This:: Available THEN -Empty! }
            CLASS EveryAlternative { This:: (-Empty! OR -AlsoEmpty!) }
            CLASS Optional { This:: -Empty? }
            CLASS OneAlternative { This:: (-Empty! OR Available!) }
            CLASS FalseGate { This:: (Empty: -Empty!) }
            CLASS ZeroTimes { This:: -Empty! / (Empty OR AlsoEmpty) }
            CLASS NonzeroTimes { This:: -Empty! / 1 }
            """
                .trimIndent()
        )

    listOf("Direct", "Nested", "EveryAlternative", "NonzeroTimes").forEach { selected ->
      shouldThrow<InvalidGameConfigException> { gameView(catalog, selected) }
    }
    listOf("Optional", "OneAlternative", "FalseGate", "ZeroTimes").forEach { selected ->
      gameView(catalog, selected).isInhabited(cn(selected)) shouldBe true
    }
  }

  @Test
  internal fun `reachable constructive instructions include their destination`() {
    val catalog =
        testCatalog(
            """
            CLASS Selected { This:: Constructed }
            CLASS Constructed
            """
                .trimIndent()
        )

    val table = gameView(catalog, "Selected")

    table.isIncluded(cn("Constructed")) shouldBe true
  }

  @Test
  internal fun `configuration counting does not treat a mixed hierarchy as Module-only`() {
    val catalog =
        testCatalog(
            """
            ABSTRACT CLASS Module
            ABSTRACT CLASS Mixed
            CLASS SelectedModule : Module, Mixed
            CLASS Ordinary : Mixed
            CLASS Source { This IF 2 Mixed: Constructed }
            CLASS Constructed
            """
                .trimIndent(),
            moduleSelections = mapOf(cn("SelectedModule") to emptySet()),
        )
    val premise =
        GamePremise(
            catalog,
            setOf(cn("SelectedModule")),
            setOf(ClassSelection(cn("Ordinary")), ClassSelection(cn("Source"))),
            emptySet(),
        )

    val table = premise.classTable

    table.isIncluded(cn("Constructed")) shouldBe true
  }

  @Test
  internal fun `bare trigger does not include its externally issued protocol`() {
    val catalog =
        testCatalog(
            """
            CLASS Selected { Protocol: Constructed }
            CLASS Protocol
            CLASS Constructed
            """
                .trimIndent()
        )

    val table = gameView(catalog, "Selected")

    table.isIncluded(cn("Protocol")) shouldBe false
    table.isIncluded(cn("Constructed")) shouldBe false
  }

  @Test
  internal fun `positive invariants include their required inhabitants`() {
    val catalog = testCatalog("CLASS Selected { HAS =1 Required }\nCLASS Required")

    val table = gameView(catalog, "Selected")

    table.isIncluded(cn("Required")) shouldBe true
  }

  @Test
  internal fun `conjunctive and alternative positive invariants include their domains`() {
    val catalog =
        testCatalog(
            """
            CLASS Selected { HAS First, Second OR Third }
            CLASS First
            CLASS Second
            CLASS Third
            """
                .trimIndent()
        )

    val table = gameView(catalog, "Selected")

    listOf("First", "Second", "Third").forEach { table.isIncluded(cn(it)) shouldBe true }
  }

  @Test
  internal fun `constructive instructions include only when their trigger and gate can be reached`() {
    val catalog =
        testCatalog(
            """
            CLASS Selected {
              ProtocolTrigger<TriggerArgument>: Triggered
              X ProtocolTrigger: XTriggered
              This:: (Class<GateProtocol>: Gated)
            }
            CLASS ProtocolTrigger<TriggerArgument>
            CLASS TriggerArgument
            CLASS Triggered
            CLASS XTriggered
            CLASS GateProtocol
            CLASS Gated
            """
                .trimIndent()
        )

    val dormant = gameView(catalog, "Selected")
    val reachable =
        gameView(catalog, "Selected", "ProtocolTrigger", "TriggerArgument", "GateProtocol")

    dormant.isIncluded(cn("Triggered")) shouldBe false
    dormant.isIncluded(cn("XTriggered")) shouldBe false
    dormant.isIncluded(cn("Gated")) shouldBe false
    dormant.isIncluded(cn("ProtocolTrigger")) shouldBe false
    reachable.isIncluded(cn("Triggered")) shouldBe true
    reachable.isIncluded(cn("XTriggered")) shouldBe true
    reachable.isIncluded(cn("Gated")) shouldBe true
    reachable.isIncluded(cn("ProtocolTrigger")) shouldBe true
  }

  @Test
  internal fun `included empty domains do not make triggers or gates reachable`() {
    val catalog =
        testCatalog(
            """
            ABSTRACT CLASS Empty
            CLASS Selected {
              Empty: Triggered
              This:: (Empty: Gated)
            }
            CLASS Triggered
            CLASS Gated
            """
                .trimIndent()
        )

    val table = gameView(catalog, "Selected", "Empty")

    table.isIncluded(cn("Empty")) shouldBe true
    table.isInhabited(cn("Empty")) shouldBe false
    table.isIncluded(cn("Triggered")) shouldBe false
    table.isIncluded(cn("Gated")) shouldBe false
  }

  @Test
  internal fun `structural supertypes become included`() {
    val catalog = testCatalog("CLASS Selected : Base\nABSTRACT CLASS Base")

    val table = gameView(catalog, "Selected")

    table.isIncluded(cn("Base")) shouldBe true
  }
}
