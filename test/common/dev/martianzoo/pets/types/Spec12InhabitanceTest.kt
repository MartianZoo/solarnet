package dev.martianzoo.pets.types

import dev.martianzoo.pets.Parsing.parseClasses
import dev.martianzoo.pets.api.Exceptions.ExpressionException
import dev.martianzoo.pets.api.Exceptions.InvalidGameConfigException
import dev.martianzoo.pets.api.Exceptions.InvalidPetDefinitionException
import dev.martianzoo.pets.api.SystemClasses.COMPONENT
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import kotlin.test.Test
import kotlin.test.assertSame

/** Section 12 of `docs/type-system-spec.md`: game universes and Type inhabitance. */
internal class Spec12InhabitanceTest {

  /** A master with two known milestones and a game view containing one of them. */
  private val master =
      loadTypes(
          """
          CLASS Player1 : Anyone
          ABSTRACT CLASS Milestone : Owned<Anyone> {
            CLASS Gardener
            CLASS Terraformer
          }
          CLASS ClaimMilestoneAction<Milestone>
          """
              .trimIndent()
      )

  private val view = gameView(master, "Player1", "Gardener", "ClaimMilestoneAction")

  // T12-1 Known and unknown names

  @Test
  internal fun `T12-1 known and unknown Class names have distinct lookup behavior`() {
    view.isInhabited(cn("Gardener")) shouldBe true
    view.isInhabited(cn("Terraformer")) shouldBe false
    view.findClass(cn("Terraformer")) shouldBe master.getClass(cn("Terraformer"))
    view.isInhabited(cn("Jackalope")) shouldBe false
    view.findClass(cn("Jackalope")) shouldBe null
    shouldThrow<ExpressionException> { view.resolve(te("Jackalope")) }
  }

  @Test
  internal fun `T12-1 a known Class retains its nominal meaning when its base Type is uninhabited`() {
    val terraformer = view.getClass(cn("Terraformer"))

    view.resolve(te("Terraformer")).expressionFull shouldBe te("Terraformer<Anyone>")
    terraformer.isSubtypeOf(view.getClass(cn("Milestone"))) shouldBe true
    view.resolve(te("Terraformer<Player1>")).isSubtypeOf(view.resolve(te("Milestone"))) shouldBe
        true
    view.resolve(te("Class<Terraformer>")).representedClass shouldBe terraformer
  }

  // T12-2 A view reuses the master universe

  @Test
  internal fun `T12-2 a view shares the master's classes and types`() {
    (view.getClass(cn("Gardener")) === master.getClass(cn("Gardener"))) shouldBe true
    view.resolve(te("Gardener")) shouldBe master.resolve(te("Gardener"))
    master.knows(view.resolve(te("Gardener"))) shouldBe true
    view.knows(master.resolve(te("Gardener"))) shouldBe true
  }

  @Test
  internal fun `T12-2 premises share compiled Classes but own their selected domains`() {
    val otherView = gameView(master, "Player1", "Terraformer", "ClaimMilestoneAction")

    assertSame(master.getClass(cn("Milestone")), view.getClass(cn("Milestone")))
    assertSame(master.getClass(cn("Milestone")), otherView.getClass(cn("Milestone")))
    view.isInhabited(cn("Gardener")) shouldBe true
    otherView.isInhabited(cn("Gardener")) shouldBe false
    view.isInhabited(cn("Terraformer")) shouldBe false
    otherView.isInhabited(cn("Terraformer")) shouldBe true
  }

  @Test
  internal fun `T12-2 resolution and subtyping use shared master identities`() {
    view.resolve(te("Terraformer")) shouldBe master.resolve(te("Terraformer"))
    view.resolve(te("Terraformer")).isSubtypeOf(view.resolve(te("Milestone"))) shouldBe
        master.resolve(te("Terraformer")).isSubtypeOf(master.resolve(te("Milestone")))
  }

  @Test
  internal fun `T12-2 master Types retain equality in an interpreting view`() {
    val master = loadTypes("CLASS MasterLeaf")

    val view =
        ClassLoader.forPremise(
            premiseTable = PremiseClassTable(master, parseClasses("CLASS LocalLeaf").toSet()),
            roots = emptySet(),
        )
    val masterType = master.resolve(te("Component(NOT MasterLeaf)"))
    val viewType = view.resolve(te("Component(NOT MasterLeaf)"))

    masterType shouldBe viewType
    masterType.hashCode() shouldBe viewType.hashCode()
    setOf(masterType, viewType).size shouldBe 1
  }

  @Test
  internal fun `T12-2 premise Classes extend the shared master universe`() {
    val master =
        loadTypes(
            """
            ABSTRACT CLASS Player
            ABSTRACT CLASS Feature
            ABSTRACT CLASS UnselectedBase
            CLASS Holder<Feature>
            """
                .trimIndent()
        )

    val premiseDeclarations =
        PremiseClassTable(
            master,
            parseClasses(
                    """
                    CLASS Player1 : Player
                    CLASS LocalFeature : Feature
                    CLASS UnselectedFeature : UnselectedBase
                    CLASS LocalRoot
                    """
                        .trimIndent()
                )
                .toSet(),
        )
    val view =
        ClassLoader.forPremise(
            premiseTable = premiseDeclarations,
            roots = setOf(cn("LocalFeature"), cn("Player1")),
        )

    assertSame(master, premiseDeclarations.master)
    master.findClass(cn("LocalFeature")) shouldBe null
    assertSame(master.getClass(cn("Holder")), view.getClass(cn("Holder")))
    view.getClass(cn("LocalFeature")).isSubtypeOf(master.getClass(cn("Feature"))) shouldBe true
    view.resolve(te("Holder<LocalFeature>")).classTable shouldBe view
    view.allSubclasses(master.getClass(cn("Feature"))).map {
      it.className
    } shouldContainExactlyInAnyOrder listOf(cn("Feature"), cn("LocalFeature"))
    val masterPlayer = master.getClass(cn("Player"))
    master.allSubclasses(masterPlayer).map { it.className } shouldContainExactly
        listOf(cn("Player"))
    view.allSubclasses(masterPlayer).map { it.className } shouldContainExactlyInAnyOrder
        listOf(cn("Player"), cn("Player1"))
    view.directSubclasses(masterPlayer).map { it.className } shouldContainExactly
        listOf(cn("Player1"))
    val world = RecordingWorld(answer = true)
    view
        .resolve(te("LocalFeature"))
        .narrows(view.resolve(te("Feature(HAS Holder<Feature>)")), world) shouldBe true
    world.questions shouldContainExactly listOf("Holder<LocalFeature>")

    view.findClass(cn("UnselectedFeature")) shouldNotBe null
    view.isInhabited(cn("UnselectedFeature")) shouldBe false
    view.isInhabited(cn("UnselectedBase")) shouldBe false
    premiseDeclarations.isSubtypeOf(cn("LocalRoot"), COMPONENT) shouldBe true
    view.getClass(cn("LocalRoot")).isSubtypeOf(view.componentClass) shouldBe true
  }

  @Test
  internal fun `T12-2 master dependencies validate premise-local targets`() {
    val master = loadTypes("ABSTRACT CLASS Target\nCLASS Holder<Target>")
    val view =
        ClassLoader.forPremise(
            premiseTable =
                PremiseClassTable(master, parseClasses("CLASS LocalTarget : Target").toSet()),
            roots = setOf(cn("Holder"), cn("LocalTarget")),
        )

    shouldThrow<InvalidPetDefinitionException> { view.componentLimits }.message shouldContain
        "`Holder` -> `LocalTarget`"
  }

  @Test
  internal fun `T12-2 glb is relative to the table interpreting the classes`() {
    val master =
        loadTypes(
            """
            ABSTRACT CLASS Left
            ABSTRACT CLASS Right
            ABSTRACT CLASS Third
            ABSTRACT CLASS MasterBoth : Left, Right
            """
                .trimIndent()
        )

    val view =
        ClassLoader.forPremise(
            premiseTable =
                PremiseClassTable(
                    master,
                    parseClasses(
                            """
                            ABSTRACT CLASS LocalBoth : Left, Right
                            ABSTRACT CLASS LocalOnly : Left, Third
                            """
                                .trimIndent()
                        )
                        .toSet(),
                ),
            roots = setOf(cn("LocalBoth"), cn("LocalOnly")),
        )
    val left = master.getClass(cn("Left"))
    val right = master.getClass(cn("Right"))
    val third = master.getClass(cn("Third"))

    master.glb(left, right) shouldBe master.getClass(cn("MasterBoth"))
    view.glb(left, right) shouldBe null
    master.glb(left, third) shouldBe null
    view.glb(left, third) shouldBe view.getClass(cn("LocalOnly"))
    master.glb(master.resolve(te("Left")), master.resolve(te("Right"))) shouldBe
        master.resolve(te("MasterBoth"))
    view.glb(master.resolve(te("Left")), master.resolve(te("Right"))) shouldBe null
  }

  @Test
  internal fun `T12-2 premise class names cannot replace master classes`() {
    val master = loadTypes("CLASS Existing")

    shouldThrow<InvalidGameConfigException> {
      ClassLoader.forPremise(
          premiseTable = PremiseClassTable(master, parseClasses("CLASS Existing").toSet()),
          roots = emptySet(),
      )
    }
  }

  @Test
  internal fun `T12-2 premise declarations cannot add broad Signal subscriptions`() {
    val master = loadTypes("CLASS Result")

    shouldThrow<InvalidPetDefinitionException> {
      ClassLoader.forPremise(
          premiseTable =
              PremiseClassTable(
                  master,
                  parseClasses("CLASS LocalListener { Signal(NOT Ok): Result }").toSet(),
              ),
          roots = emptySet(),
      )
    }
  }

  @Test
  internal fun `T12-2 premise declarations cannot add Signal dependency targets`() {
    val master = loadTypes("CLASS Result")

    shouldThrow<InvalidPetDefinitionException> {
      ClassLoader.forPremise(
          premiseTable =
              PremiseClassTable(master, parseClasses("ABSTRACT CLASS Local<Signal>").toSet()),
          roots = emptySet(),
      )
    }
  }

  @Test
  internal fun `T12-2 sibling premise class tables are distinct universes`() {
    val master = loadTypes("ABSTRACT CLASS Feature\nCLASS Holder<Feature>")
    val declaration = parseClasses("CLASS LocalFeature : Feature").toSet()
    fun gameView(): ClassTable =
        ClassLoader.forPremise(
            premiseTable = PremiseClassTable(master, declaration),
            roots = setOf(cn("LocalFeature")),
        )
    val left = gameView()
    val right = gameView()

    left.getClass(cn("LocalFeature")) shouldNotBe right.getClass(cn("LocalFeature"))
    shouldThrowIae {
      left.getClass(cn("LocalFeature")).isSubtypeOf(right.getClass(cn("LocalFeature")))
    }
    shouldThrowIae {
      left.glb(
          left.resolve(te("Holder<LocalFeature>")),
          right.resolve(te("Holder<LocalFeature>")),
      )
    }
  }

  // T12-3 View-relative enumeration

  @Test
  internal fun `T12-3 subclass enumeration follows the premise closure`() {
    master.allSubclasses(master.getClass(cn("Milestone"))).map {
      "$it"
    } shouldContainExactlyInAnyOrder listOf("Gardener", "Terraformer", "Milestone")
    view.allSubclasses(view.getClass(cn("Milestone"))).map { "$it" } shouldContainExactlyInAnyOrder
        listOf("Gardener", "Milestone")
    view.directSubclasses(view.getClass(cn("Milestone"))).map { "$it" } shouldContainExactly
        listOf("Gardener")
  }

  @Test
  internal fun `T12-3 concrete enumeration returns the view's inhabited Types`() {
    master
        .allConcreteSubtypes(master.resolve(te("Milestone")))
        .map { "$it" }
        .toList() shouldContainExactlyInAnyOrder listOf("Gardener<Player1>", "Terraformer<Player1>")
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
  internal fun `T12-3 uninhabited Types and their class literals enumerate nothing`() {
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

  // T12-4 Concrete-domain inhabitance

  @Test
  internal fun `T12-4 inhabitance follows available concrete narrowings`() {
    view.isInhabited(view.resolve(te("Gardener<Player1>"))) shouldBe true
    view.isInhabited(view.resolve(te("Milestone"))) shouldBe true
    view.isInhabited(view.resolve(te("Terraformer"))) shouldBe false
    view.isInhabited(view.resolve(te("ClaimMilestoneAction<Gardener>"))) shouldBe true
    view.isInhabited(view.resolve(te("ClaimMilestoneAction<Terraformer>"))) shouldBe false
  }

  @Test
  internal fun `T12-4 abstract and dependent Types can have empty concrete domains`() {
    val master = loadTypes("ABSTRACT CLASS Empty\nCLASS Holder<Empty>\nCLASS Live")
    val view = gameView(master, "Empty", "Holder", "Live")

    view.isIncluded(cn("Empty")) shouldBe true
    view.isIncluded(cn("Holder")) shouldBe true
    view.isInhabited(view.resolve(te("Empty"))) shouldBe false
    view.isInhabited(view.resolve(te("Class<Empty>"))) shouldBe false
    view.isInhabited(view.resolve(te("Holder<Empty>"))) shouldBe false
    view.isInhabited(view.resolve(te("Class<Holder>"))) shouldBe false
    view.isInhabited(view.resolve(te("Live"))) shouldBe true
  }

  @Test
  internal fun `T12-4 a Class literal can make its represented Class's base Type inhabited`() {
    val table =
        loadTypes(
            "ABSTRACT CLASS Link<Class<Component>>",
            "CLASS SelfLink : Link<Class<SelfLink>>",
        )

    table.isInhabited(table.resolve(te("SelfLink"))) shouldBe true
    table.isInhabited(table.resolve(te("Class<SelfLink>"))) shouldBe true
  }

  @Test
  internal fun `T12-4 missing inhabitants propagate through represented-class dependencies`() {
    val table =
        loadTypes(
            "ABSTRACT CLASS CardResource : Owned<Anyone> { CLASS Animal }",
            "ABSTRACT CLASS ResourceHolder<Class<CardResource>>",
            "CLASS Pets : ResourceHolder<Class<Animal>>",
        )

    // ResourceHolder omits canon's Owned superclass so only its resource dependency excludes Pets.
    // With no player, no animal can exist. Its class representative is then absent, so Pets
    // cannot exist either; the representative of Pets must disappear in turn.
    table.isInhabited(table.resolve(te("Animal"))) shouldBe false
    table.isInhabited(table.resolve(te("Pets"))) shouldBe false
    table.isInhabited(table.resolve(te("Class<Pets>"))) shouldBe false
  }

  @Test
  internal fun `T12-4 a structurally empty difference is uninhabited`() {
    val table = loadTypes("CLASS Rabbit")

    table.isInhabited(table.resolve(te("Rabbit(NOT Rabbit)"))) shouldBe false
  }

  @Test
  internal fun `T12-4 a Type from another universe is not known in this universe`() {
    val other = loadTypes("ABSTRACT CLASS Milestone { CLASS Gardener }")

    view.knows(other.resolve(te("Gardener"))) shouldBe false
    view.isInhabited(other.resolve(te("Gardener"))) shouldBe false
  }

  // T12-5 Structural meaning is universe-wide

  @Test
  internal fun `T12-5 nested differences see premise-only realizations`() {
    val master = loadTypes("ABSTRACT CLASS Player : Anyone\nCLASS Holder<Anyone>")
    val view =
        ClassLoader.forPremise(
            premiseTable =
                PremiseClassTable(master, parseClasses("CLASS Player1 : Player").toSet()),
            roots = setOf(cn("Player1")),
        )

    val otherHolder = view.resolve(te("Holder<Anyone(NOT Player)>"))

    otherHolder.expressionFull shouldBe te("Holder<Anyone(NOT Player)>")
    otherHolder.classTable shouldBe view
    view.resolve(te("Holder<Player1>")).isSubtypeOf(otherHolder) shouldBe false
  }

  @Test
  internal fun `T12-5 master candidates use the shared universe for premise differences`() {
    val master = loadTypes("ABSTRACT CLASS Player : Anyone\nCLASS SoloOpponent : Anyone")

    val view =
        ClassLoader.forPremise(
            premiseTable =
                PremiseClassTable(master, parseClasses("CLASS Player1 : Player").toSet()),
            roots = setOf(cn("Player1")),
        )
    val otherThanPlayer1 = view.resolve(te("Anyone(NOT Player1)"))

    master.resolve(te("SoloOpponent")).isSubtypeOf(otherThanPlayer1) shouldBe true
    view.resolve(te("Player1")).isSubtypeOf(otherThanPlayer1) shouldBe false
  }

  @Test
  internal fun `T12-5 structural overlap includes every premise Class`() {
    val master = loadTypes("ABSTRACT CLASS Left\nABSTRACT CLASS Right")
    val view =
        ClassLoader.forPremise(
            premiseTable =
                PremiseClassTable(
                    master,
                    parseClasses(
                            """
                            CLASS LeftOnly : Left
                            CLASS Overlap : Left, Right
                            """
                                .trimIndent()
                        )
                        .toSet(),
                ),
            roots = setOf(cn("LeftOnly")),
        )

    view.isInhabited(cn("Overlap")) shouldBe false
    view.resolve(te("Left(NOT Right)")).refinement shouldBe te("Left(NOT Right)").refinement
    view.resolve(te("Left")).isSubtypeOf(view.resolve(te("Left(NOT Right)"))) shouldBe false
  }

  @Test
  internal fun `T12-5 structural overlap includes every master Class`() {
    val overlaps =
        loadTypes(
            """
            CLASS Root
            ABSTRACT CLASS Left
            ABSTRACT CLASS Right
            CLASS Overlap : Left, Right
            CLASS LeftOnly : Left
            """
                .trimIndent()
        )
    val view = gameView(overlaps, "Root", "LeftOnly", "Right")

    view.isInhabited(cn("Overlap")) shouldBe false
    view.resolve(te("Left(NOT Right)")).refinement shouldBe te("Left(NOT Right)").refinement
    view.resolve(te("Left")).isSubtypeOf(view.resolve(te("Left(NOT Right)"))) shouldBe false
    view
        .allConcreteSubtypes(view.resolve(te("Left(NOT Right)")))
        .map { "$it" }
        .toList() shouldContainExactly listOf("LeftOnly")
  }
}
