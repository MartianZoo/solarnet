package dev.martianzoo.pets.types

import dev.martianzoo.pets.Parsing.parseClasses
import dev.martianzoo.pets.api.Exceptions.ExpressionException
import dev.martianzoo.pets.api.SystemClasses.COMPONENT
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.data.ClassSelection
import dev.martianzoo.pets.data.GamePremise
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlin.test.Test
import kotlin.test.assertSame

/**
 * Section 12 of `docs/type-system-spec.md`: what a game's view of the universe changes, and what it
 * does not.
 */
internal class Spec12InhabitanceTest {

  /**
   * A catalog with two milestones. Only one of them is used in the game below, so the other stays
   * known but uninhabited -- the "jackalope" case.
   */
  private val catalog =
      testCatalog(
          """
          CLASS Player1 : Owner
          ABSTRACT CLASS Milestone : Owned<Owner> {
            CLASS Gardener
            CLASS Terraformer
          }
          CLASS ClaimMilestoneAction<Milestone>
          """
              .trimIndent()
      )

  private val master = catalog.classTable

  private val view = gameView(catalog, "Player1", "Gardener", "ClaimMilestoneAction")

  // T12-1 The three states of a name

  @Test
  internal fun `T12-1 a name is active, uninhabited, or unknown`() {
    view.isActive(cn("Gardener")) shouldBe true
    view.isActive(cn("Terraformer")) shouldBe false
    view.findClass(cn("Terraformer")) shouldBe master.getClass(cn("Terraformer"))
    view.findClass(cn("Jackalope")) shouldBe null
    shouldThrow<ExpressionException> { view.resolve(te("Jackalope")) }
  }

  @Test
  internal fun `T12-1 an uninhabited class keeps its name, hierarchy and dependencies`() {
    val terraformer = view.getClass(cn("Terraformer"))

    view.resolve(te("Terraformer")).expressionFull shouldBe te("Terraformer<Owner>")
    terraformer.isSubtypeOf(view.getClass(cn("Milestone"))) shouldBe true
    view.resolve(te("Terraformer<Player1>")).isSubtypeOf(view.resolve(te("Milestone"))) shouldBe
        true
    view.resolve(te("Class<Terraformer>")).representedClass shouldBe terraformer
  }

  // T12-2 A view reuses the master universe

  @Test
  internal fun `T12-2 a view shares the master's classes and types`() {
    (view.getClass(cn("Gardener")) === master.getClass(cn("Gardener"))) shouldBe true
    (view.resolve(te("Gardener")) === master.resolve(te("Gardener"))) shouldBe true
    view.knows(master.resolve(te("Gardener"))) shouldBe true
  }

  @Test
  internal fun `T12-2 resolution and subtyping do not depend on the view`() {
    view.resolve(te("Terraformer")) shouldBe master.resolve(te("Terraformer"))
    view.resolve(te("Terraformer")).isSubtypeOf(view.resolve(te("Milestone"))) shouldBe
        master.resolve(te("Terraformer")).isSubtypeOf(master.resolve(te("Milestone")))
  }

  @Test
  internal fun `T12-2 an interpreting view does not change structural Type equality`() {
    val catalog = testCatalog("CLASS MasterLeaf")
    val master = catalog.classTable
    val view =
        GamePremise(
                catalog = catalog,
                modules = emptySet(),
                classSelections = emptySet(),
                initialComponentTypes = emptySet(),
                premiseClassDeclarations = parseClasses("CLASS LocalLeaf").toSet(),
            )
            .classTable
    val masterType = master.resolve(te("Component(NOT MasterLeaf)"))
    val viewType = view.resolve(te("Component(NOT MasterLeaf)"))

    masterType shouldBe viewType
    masterType.hashCode() shouldBe viewType.hashCode()
    setOf(masterType, viewType).size shouldBe 1
  }

  @Test
  internal fun `T12-2 premise classes extend the master without recompiling it`() {
    val catalog =
        testCatalog(
            """
            ABSTRACT CLASS Feature
            ABSTRACT CLASS DormantBase
            CLASS Holder<Feature>
            """
                .trimIndent()
        )
    val master = catalog.classTable
    val premise =
        GamePremise(
            catalog = catalog,
            modules = emptySet(),
            classSelections = setOf(ClassSelection(cn("LocalFeature"))),
            initialComponentTypes = emptySet(),
            premiseClassDeclarations =
                parseClasses(
                        """
                        CLASS LocalFeature : Feature
                        CLASS DormantFeature : DormantBase
                        CLASS LocalRoot
                        """
                            .trimIndent()
                    )
                    .toSet(),
        )
    val view = premise.classTable

    master.findClass(cn("LocalFeature")) shouldBe null
    assertSame(master.getClass(cn("Holder")), view.getClass(cn("Holder")))
    view.getClass(cn("LocalFeature")).isSubtypeOf(master.getClass(cn("Feature"))) shouldBe true
    view.resolve(te("Holder<LocalFeature>")).classTable shouldBe view
    view.allSubclasses(master.getClass(cn("Feature"))).map { it.className } shouldContainExactly
        listOf(cn("Feature"), cn("LocalFeature"))
    val world = RecordingWorld(answer = true)
    view
        .resolve(te("LocalFeature"))
        .narrows(view.resolve(te("Feature(HAS Holder<Feature>)")), world) shouldBe true
    world.questions shouldContainExactly listOf("Holder<LocalFeature>")

    view.findClass(cn("DormantFeature")) shouldNotBe null
    view.isActive(cn("DormantFeature")) shouldBe false
    view.isActive(cn("DormantBase")) shouldBe false
    premise.premiseClassTable.isSubtypeOf(cn("LocalRoot"), COMPONENT) shouldBe true
  }

  @Test
  internal fun `T12-2 premise class names cannot replace master classes`() {
    val catalog = testCatalog("CLASS Existing")

    shouldThrowIae {
      GamePremise(
          catalog = catalog,
          modules = emptySet(),
          classSelections = emptySet(),
          initialComponentTypes = emptySet(),
          premiseClassDeclarations = parseClasses("CLASS Existing").toSet(),
      )
    }
  }

  @Test
  internal fun `T12-2 sibling premise class tables are distinct universes`() {
    val catalog = testCatalog("ABSTRACT CLASS Feature\nCLASS Holder<Feature>")
    val declaration = parseClasses("CLASS LocalFeature : Feature").toSet()
    fun projection(): ClassTable =
        GamePremise(
                catalog = catalog,
                modules = emptySet(),
                classSelections = setOf(ClassSelection(cn("LocalFeature"))),
                initialComponentTypes = emptySet(),
                premiseClassDeclarations = declaration,
            )
            .classTable
    val left = projection()
    val right = projection()

    left.getClass(cn("LocalFeature")) shouldNotBe right.getClass(cn("LocalFeature"))
    shouldThrowIae {
      left.getClass(cn("LocalFeature")).isSubtypeOf(right.getClass(cn("LocalFeature")))
    }
    shouldThrowIae {
      left.resolve(te("Holder<LocalFeature>")) glb right.resolve(te("Holder<LocalFeature>"))
    }
  }

  // T12-3 What the view does change

  @Test
  internal fun `T12-3 subclass enumeration is view-relative`() {
    master.allSubclasses(master.getClass(cn("Milestone"))).map { "$it" } shouldContainExactly
        listOf("Gardener", "Terraformer", "Milestone")
    view.allSubclasses(view.getClass(cn("Milestone"))).map { "$it" } shouldContainExactly
        listOf("Gardener", "Milestone")
    view.directSubclasses(view.getClass(cn("Milestone"))).map { "$it" } shouldContainExactly
        listOf("Gardener")
  }

  @Test
  internal fun `T12-3 concrete enumeration is view-relative`() {
    master
        .allConcreteSubtypes(master.resolve(te("Milestone")))
        .map { "$it" }
        .toList() shouldContainExactly listOf("Gardener<Player1>", "Terraformer<Player1>")
    view
        .allConcreteSubtypes(view.resolve(te("Milestone")))
        .map { "$it" }
        .toList() shouldContainExactly listOf("Gardener<Player1>")
    view
        .allConcreteSubtypes(view.resolve(te("Class<Milestone>")))
        .map { "$it" }
        .toList() shouldContainExactly listOf("Class<Gardener>")
  }

  @Test
  internal fun `T12-3 an uninhabited type enumerates nothing`() {
    view.allConcreteSubtypes(view.resolve(te("Terraformer"))).toList() shouldBe listOf()
    view.allConcreteSubtypes(view.resolve(te("Class<Terraformer>"))).toList() shouldBe listOf()
    view.concreteSubtypesSameClass(view.resolve(te("Terraformer"))).toList() shouldBe listOf()
  }

  @Test
  internal fun `T12-3 automatic narrowing can succeed in a view where the master is undecided`() {
    master.singleConcreteSubtype(master.resolve(te("Milestone")), fullWorld) shouldBe null
    view.singleConcreteSubtype(view.resolve(te("Milestone")), fullWorld) shouldBe
        view.resolve(te("Gardener<Player1>"))
  }

  // T12-4 Active types

  @Test
  internal fun `T12-4 a type is active when its class and every dependency bound are`() {
    view.isActive(view.resolve(te("Gardener<Player1>"))) shouldBe true
    view.isActive(view.resolve(te("Terraformer"))) shouldBe false
    view.isActive(view.resolve(te("ClaimMilestoneAction<Gardener>"))) shouldBe true
    view.isActive(view.resolve(te("ClaimMilestoneAction<Terraformer>"))) shouldBe false
  }

  @Test
  internal fun `T12-4 a type from another catalog is not known, let alone active`() {
    val other = testCatalog("ABSTRACT CLASS Milestone { CLASS Gardener }").classTable

    view.knows(other.resolve(te("Gardener"))) shouldBe false
    view.isActive(other.resolve(te("Gardener"))) shouldBe false
  }

  // T12-5 Structural meaning is universe-wide

  @Test
  internal fun `T12-5 nested differences see premise-only realizations`() {
    val catalog = testCatalog("ABSTRACT CLASS Player : Owner\nCLASS Holder<Owner>")
    val view =
        GamePremise(
                catalog = catalog,
                modules = emptySet(),
                classSelections = setOf(ClassSelection(cn("Player1"))),
                initialComponentTypes = emptySet(),
                playerNames = listOf(cn("Player1")),
                premiseClassDeclarations = parseClasses("CLASS Player1 : Player").toSet(),
            )
            .classTable

    val otherOwner = view.resolve(te("Holder<Owner(NOT Player)>"))

    otherOwner.expressionFull shouldBe te("Holder<Owner(NOT Player)>")
    otherOwner.classTable shouldBe view
    view.resolve(te("Holder<Player1>")).isSubtypeOf(otherOwner) shouldBe false
  }

  @Test
  internal fun `T12-5 master candidates use the shared universe for premise differences`() {
    val catalog = testCatalog("ABSTRACT CLASS Player : Owner\nCLASS SoloOpponent : Owner")
    val master = catalog.classTable
    val view =
        GamePremise(
                catalog = catalog,
                modules = emptySet(),
                classSelections = setOf(ClassSelection(cn("Player1"))),
                initialComponentTypes = emptySet(),
                playerNames = listOf(cn("Player1")),
                premiseClassDeclarations = parseClasses("CLASS Player1 : Player").toSet(),
            )
            .classTable
    val otherThanPlayer1 = view.resolve(te("Owner(NOT Player1)"))

    master.resolve(te("SoloOpponent")).isSubtypeOf(otherThanPlayer1) shouldBe true
    view.resolve(te("Player1")).isSubtypeOf(otherThanPlayer1) shouldBe false
  }

  @Test
  internal fun `T12-5 inactive premise classes still determine structural overlap`() {
    val catalog = testCatalog("ABSTRACT CLASS Left\nABSTRACT CLASS Right")
    val premise =
        GamePremise(
            catalog = catalog,
            modules = emptySet(),
            classSelections = setOf(ClassSelection(cn("ActiveLeft"))),
            initialComponentTypes = emptySet(),
            premiseClassDeclarations =
                parseClasses(
                        """
                        CLASS ActiveLeft : Left
                        CLASS DormantOverlap : Left, Right
                        """
                            .trimIndent()
                    )
                    .toSet(),
        )
    val view = premise.classTable

    view.isActive(cn("DormantOverlap")) shouldBe false
    view.resolve(te("Left(NOT Right)")).refinement shouldBe te("Left(NOT Right)").refinement
    view.resolve(te("Left")).isSubtypeOf(view.resolve(te("Left(NOT Right)"))) shouldBe false
  }

  @Test
  internal fun `T12-5 inactive master classes still determine structural overlap`() {
    val overlaps =
        testCatalog(
            """
            CLASS Root
            ABSTRACT CLASS Left
            ABSTRACT CLASS Right
            CLASS Overlap : Left, Right
            CLASS LeftOnly : Left
            """
                .trimIndent()
        )
    val restricted = gameView(overlaps, "Root", "LeftOnly", "Right")

    restricted.isActive(cn("Overlap")) shouldBe false
    // `Left` still overlaps `Right` in the catalog, so the exclusion is still meaningful...
    restricted.resolve(te("Left(NOT Right)")).refinement shouldBe te("Left(NOT Right)").refinement
    restricted.resolve(te("Left")).isSubtypeOf(restricted.resolve(te("Left(NOT Right)"))) shouldBe
        false
    // ...while enumeration, which is view-relative, still lists only what this game can hold.
    restricted
        .allConcreteSubtypes(restricted.resolve(te("Left(NOT Right)")))
        .map { "$it" }
        .toList() shouldContainExactly listOf("LeftOnly")
  }
}
