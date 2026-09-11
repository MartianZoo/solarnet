package dev.martianzoo.pets.types

import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.api.CustomClass
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.data.ClassSelection
import dev.martianzoo.pets.data.GamePremise
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

/**
 * Which classes a game premise activates. This is premise-construction policy rather than type
 * meaning, so `docs/type-system-spec.md` describes only what an uninhabited class *means* (section
 * 12), not how one comes to be uninhabited.
 */
internal class ActivationTest {

  @Test
  internal fun `custom class requirements load with the custom class only`() {
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

    val inactive = gameView(catalog)
    inactive.isActive(cn("RuntimeDependency")) shouldBe false

    val loaded = gameView(catalog, "DependencySource")
    loaded.isActive(cn("RuntimeDependency")) shouldBe true
  }

  @Test
  internal fun `dependency signatures activate available vocabulary`() {
    val catalog =
        testCatalog(
            """
            CLASS SelectedContent<AvailableVocabulary>
            CLASS AvailableVocabulary
            """
                .trimIndent()
        )
    val table = gameView(catalog, "SelectedContent")

    table.isActive(cn("AvailableVocabulary")) shouldBe true
  }

  @Test
  internal fun `excluding an inactive type does not activate it`() {
    val catalog =
        testCatalog(
            """
            ABSTRACT CLASS Domain
            ABSTRACT CLASS Holder<Domain>
            CLASS Inactive : Domain
            """
                .trimIndent()
        )
    val table = gameView(catalog, "Holder")

    table.isActive(table.resolve(te("Holder<Domain(NOT Inactive)>"))) shouldBe true
  }

  @Test
  internal fun `structural dependencies activate catalog-known classes`() {
    val catalog = testCatalog("CLASS Active<Inactive>\nCLASS Inactive")

    val table = gameView(catalog, "Active")

    table.isActive(cn("Inactive")) shouldBe true
  }

  @Test
  internal fun `premise rejects a structurally activated unrequested Module`() {
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

    shouldThrow<IllegalArgumentException> { ClassTable.forPremise(premise) }
  }

  @Test
  internal fun `premise rejects structural reactivation of an excluded class`() {
    val catalog = testCatalog("CLASS Active<Excluded>\nCLASS Excluded")
    val premise =
        GamePremise(
            catalog,
            emptySet(),
            setOf(ClassSelection(cn("Active")), ClassSelection(cn("Excluded"), included = false)),
            emptySet(),
        )

    shouldThrow<IllegalArgumentException> { ClassTable.forPremise(premise) }
  }

  @Test
  internal fun `premise rejects structural reactivation of a conditionally excluded class`() {
    val catalog =
        testCatalog(
            """
            ABSTRACT CLASS Module
            CLASS Requested : Module { This:: Active }
            CLASS Conditional { HAS Flag }
            CLASS Active<Conditional>
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

    shouldThrow<IllegalArgumentException> { ClassTable.forPremise(premise) }
  }

  @Test
  internal fun `class metrics do not activate the represented class`() {
    val catalog = testCatalog("CLASS Querying { HAS MAX 0 Class<Inactive> }\nCLASS Inactive")
    val table = gameView(catalog, "Querying")

    table.isActive(cn("Inactive")) shouldBe false
  }

  @Test
  internal fun `reachable constructive instructions activate their destination`() {
    val catalog =
        testCatalog(
            """
            CLASS Active { This:: Constructed }
            CLASS Constructed
            """
                .trimIndent()
        )

    val table = gameView(catalog, "Active")

    table.isActive(cn("Constructed")) shouldBe true
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

    val table = ClassTable.forPremise(premise)

    table.isActive(cn("Constructed")) shouldBe true
  }

  @Test
  internal fun `bare trigger does not activate its externally issued protocol`() {
    val catalog =
        testCatalog(
            """
            CLASS Active { Protocol: Constructed }
            CLASS Protocol
            CLASS Constructed
            """
                .trimIndent()
        )

    val table = gameView(catalog, "Active")

    table.isActive(cn("Protocol")) shouldBe false
    table.isActive(cn("Constructed")) shouldBe false
  }

  @Test
  internal fun `positive invariants activate their required inhabitants`() {
    val catalog = testCatalog("CLASS Active { HAS =1 Required }\nCLASS Required")

    val table = gameView(catalog, "Active")

    table.isActive(cn("Required")) shouldBe true
  }

  @Test
  internal fun `constructive instructions activate only when their trigger and gate can be reached`() {
    val catalog =
        testCatalog(
            """
            CLASS Active {
              InactiveTrigger<InactiveTriggerArgument>: Triggered
              This:: (Class<InactiveGate>: Gated)
            }
            CLASS InactiveTrigger<InactiveTriggerArgument>
            CLASS InactiveTriggerArgument
            CLASS Triggered
            CLASS InactiveGate
            CLASS Gated
            """
                .trimIndent()
        )

    val dormant = gameView(catalog, "Active")
    val reachable =
        gameView(catalog, "Active", "InactiveTrigger", "InactiveTriggerArgument", "InactiveGate")

    dormant.isActive(cn("Triggered")) shouldBe false
    dormant.isActive(cn("Gated")) shouldBe false
    dormant.isActive(cn("InactiveTrigger")) shouldBe false
    reachable.isActive(cn("Triggered")) shouldBe true
    reachable.isActive(cn("Gated")) shouldBe true
    reachable.isActive(cn("InactiveTrigger")) shouldBe true
  }

  @Test
  internal fun `structural supertypes become active`() {
    val catalog = testCatalog("CLASS Active : Inactive\nABSTRACT CLASS Inactive")

    val table = gameView(catalog, "Active")

    table.isActive(cn("Inactive")) shouldBe true
  }
}
