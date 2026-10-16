package dev.martianzoo.pets

import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.api.Exceptions.ExpressionException
import dev.martianzoo.pets.api.Exceptions.InvalidPetDefinitionException
import dev.martianzoo.pets.api.Exceptions.PetSyntaxException
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.Instruction
import dev.martianzoo.pets.ast.InstructionTree
import dev.martianzoo.pets.types.RecordingWorld
import dev.martianzoo.pets.types.fullWorld
import dev.martianzoo.pets.types.loadTypes
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import kotlin.test.Test

/** Passing characterizations of known incorrect Pets behavior. */
internal class BugsTest {
  @Test
  internal fun `an equality-incompatible subclass incorrectly aborts concrete-type queries`() {
    // Synthetic subtype composition; only the order of the concrete declarations changes.
    for (sameFirst in listOf(false, true)) {
      val subclasses =
          listOf("CLASS Independent : Pair", "CLASS Same : Pair<@Area, @Area>").let {
            if (sameFirst) it.reversed() else it
          }
      val table =
          loadTypes(
              """
              ABSTRACT CLASS Area {
                CLASS First
                CLASS Second
              }
              ABSTRACT CLASS Pair<Area, Area>
              """,
              *subclasses.toTypedArray(),
          )
      val requested = table.resolve(parse<Expression>("Pair<First, Second>"))
      val independent = table.resolve(parse<Expression>("Independent<First, Second>"))
      independent.isSubtypeOf(requested) shouldBe true

      // T11-1/T11-3 require Independent alone. Same's equality excludes Same;
      // constructing the incompatible candidate incorrectly aborts the query.
      shouldThrow<ExpressionException> { table.allConcreteSubtypes(requested).toList() }
          .detail shouldContain "dependencies disagree in `Same<First, Second>`"
      shouldThrow<ExpressionException> { table.singleConcreteSubtype(requested, fullWorld) }
          .detail shouldContain "dependencies disagree in `Same<First, Second>`"

      // T12-4 requires true in either order. An early valid candidate hides the exception.
      if (sameFirst) {
        shouldThrow<ExpressionException> { table.isInhabited(requested) }.detail shouldContain
            "dependencies disagree in `Same<First, Second>`"
      } else {
        table.isInhabited(requested) shouldBe true
      }
    }
  }

  @Test
  internal fun `automatic narrowing incorrectly counts a subclass excluded by a dependency difference`() {
    val table =
        loadTypes(
            """
            ABSTRACT CLASS Area {
              CLASS Land
              CLASS Water
            }
            ABSTRACT CLASS Tile<Area> {
              CLASS Greenery : Tile<Land>
              CLASS Ocean : Tile<Water>
            }
            """
        )
    val requested = table.resolve(parse<Expression>("Tile<Area(NOT Land)>"))
    val ocean = table.resolve(parse<Expression>("Ocean"))
    ocean.isSubtypeOf(requested) shouldBe true
    table.allConcreteSubtypes(requested).toList() shouldBe listOf(ocean)

    // T11-3 requires Ocean. Land(NOT Land) has no candidate, yet Greenery counts as a rival.
    table.singleConcreteSubtype(requested, fullWorld) shouldBe null
  }

  @Test
  internal fun `class-literal narrowing incorrectly treats a changed bound as a different predicate`() {
    val table = loadTypes("ABSTRACT CLASS Tag { CLASS BuildingTag }")
    val general = table.resolve(parse<Expression>("Class<@Tag>(HAS @Tag)"))
    val specific = table.resolve(parse<Expression>("Class<@BuildingTag>(HAS @BuildingTag)"))
    val candidate = table.resolve(parse<Expression>("Class<BuildingTag>"))

    for (refined in listOf(general, specific)) {
      val world = RecordingWorld()
      candidate.narrows(refined, world) shouldBe true
      world.questions shouldBe listOf("BuildingTag")
    }
    specific.isSubtypeOf(table.resolve(parse<Expression>("Class<Tag>"))) shouldBe true

    // T8-1 and T8-10 require true: the domain narrows while the predicate still tests
    // the same candidate class. The implementation instead compares the original bounds.
    specific.isSubtypeOf(general) shouldBe false
  }

  @Test
  internal fun `rendering a class-literal meet incorrectly loses its represented-class binding`() {
    val table =
        loadTypes(
            """
            CLASS Player1 : Owner
            CLASS Player2 : Owner
            ABSTRACT CLASS Tag : Owned {
              CLASS BuildingTag
              CLASS SpaceTag
            }
            """
        )
    val refined =
        table.resolve(parse<Expression>("Class<@Tag>(HAS @Tag<Player1>, HAS Tag<Player2>)"))
    val building = table.resolve(parse<Expression>("Class<BuildingTag>"))
    val intersection = table.glb(refined, building)!!

    for (expression in listOf(intersection.expression, intersection.expressionFull)) {
      table.resolve(expression) shouldBe intersection
      val retainedWorld = RecordingWorld()
      building.narrows(table.resolve(expression), retainedWorld) shouldBe true
      retainedWorld.questions shouldBe listOf("BuildingTag<Player1>", "Tag<Player2>")

      val reparsed = table.resolve(parse<Expression>(expression.toString()))
      val reparsedWorld = RecordingWorld()
      building.narrows(reparsed, reparsedWorld) shouldBe true
      // T5-6 requires the same type and world questions. Text instead loses the T8-10
      // candidate binding; the unmarked Player2 predicate stays unchanged.
      reparsed shouldNotBe intersection
      reparsedWorld.questions shouldBe listOf("Tag<Player1>", "Tag<Player2>")
    }
  }

  @Test
  internal fun `a meet incorrectly ignores dependencies that identify a greatest common type`() {
    // Simplified Pets and Ants: keep only their distinct resource kinds.
    val table =
        loadTypes(
            """
            ABSTRACT CLASS CardResource {
              CLASS Animal
              CLASS Microbe
            }
            ABSTRACT CLASS ResourceHolder<Class<CardResource>>
            ABSTRACT CLASS CardFront
            CLASS Pets : CardFront, ResourceHolder<Class<Animal>>
            CLASS Ants : CardFront, ResourceHolder<Class<Microbe>>
            """
        )
    val card = table.resolve(parse<Expression>("CardFront"))
    val animalHolder = table.resolve(parse<Expression>("ResourceHolder<Class<Animal>>"))
    val pets = table.resolve(parse<Expression>("Pets"))

    pets.isSubtypeOf(card) shouldBe true
    pets.isSubtypeOf(animalHolder) shouldBe true
    table.resolve(parse<Expression>("Ants")).isSubtypeOf(animalHolder) shouldBe false
    table.allConcreteSubtypes(card).filter { it.isSubtypeOf(animalHolder) }.toList() shouldBe
        listOf(pets)

    // T7-1 requires Pets, but the implementation stops at the ambiguous root classes.
    table.glb(card, animalHolder) shouldBe null
    table.glb(animalHolder, card) shouldBe null
  }

  @Test
  internal fun `an abstract box incorrectly permits identical concrete shared arguments in a transmutation`() {
    val table =
        loadTypes(
            """
            ABSTRACT CLASS AbstractThing
            CLASS ConcreteThing : AbstractThing
            ABSTRACT CLASS Box<AbstractThing>
            """
        )
    val elaborator = PetElaborator(table)
    val authored =
        elaborator.elaborateInput(
            parse<Instruction>("Box<@AbstractThing> FROM Box<@AbstractThing>")
        )
    val proposed =
        elaborator.elaborateInput(parse<Instruction>("Box<ConcreteThing> FROM Box<ConcreteThing>"))

    // The requested rule rejects the identical concrete argument even while Box remains abstract.
    proposed.narrows(authored, TableWorld(table)) shouldBe true
  }

  @Test
  internal fun `an owner-local class incorrectly repeats an argument fixed by specialization`() {
    val table =
        loadTypes(
            """
                ABSTRACT CLASS Party
                ABSTRACT CLASS Policy<Party>
                CLASS MarsFirst : Party {
                  This: Policy<This> {}
                }
                """
        )

    // The generated Policy already fixes its Party to MarsFirst; the gain needs no argument.
    val error =
        shouldThrow<InvalidPetDefinitionException> {
          PetElaborator(table).classEffects(table.getClass(cn("MarsFirst")))
        }
    error.detail shouldContain "MarsFirst_Policy<This>"
    error.detail shouldContain "does not match an available dependency"
  }

  @Test
  internal fun `a type-variable marker on a DEFAULT root is incorrectly discarded`() {
    // This declaration should be rejected, not silently treated as DEFAULT +Piece<First>.
    val table =
        loadTypes(
            """
                ABSTRACT CLASS Area { CLASS First }
                CLASS Piece<Area> { DEFAULT +@Piece<First> }
                """
        )

    PetElaborator(table).elaborateInput(parse<InstructionTree>("Piece<>")).toString() shouldBe
        "Piece<First>!"
  }

  @Test
  internal fun `an invalid character in an argument list incorrectly blames the opening bracket`() {
    val error = shouldThrow<PetSyntaxException> { parse<Expression>("Foo<~ Bar>") }

    // The diagnostic should point to the unrecognized '~' in column 5.
    error.sourceLocation?.column shouldBe 4
    error.sourceLocation?.text shouldBe "<"
    error.detail shouldContain "found `<`"
  }
}
