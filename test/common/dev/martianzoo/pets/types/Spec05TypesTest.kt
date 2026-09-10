package dev.martianzoo.pets.types

import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.types.Dependency.Key
import io.kotest.matchers.shouldBe
import kotlin.test.Test

/** Section 5 of `docs/type-system-spec.md`: what a type is, and how it is written down. */
internal class Spec05TypesTest {

  private val mars =
      loadTypes(
          """
          CLASS Player1 : Owner
          CLASS Player2 : Owner
          ABSTRACT CLASS Area {
            ABSTRACT CLASS MarsArea {
              ABSTRACT CLASS LandArea { CLASS Tharsis_2_2, Tharsis_2_3 }
              ABSTRACT CLASS WaterArea { CLASS Tharsis_1_1 }
            }
          }
          ABSTRACT CLASS Occupant<Area>
          ABSTRACT CLASS Tile : Occupant
          CLASS GreeneryTile : Tile<MarsArea>, Owned<Owner>
          CLASS Neighbor<Area, Area>
          """
              .trimIndent()
      )

  private fun type(s: String) = mars.resolve(te(s))

  // T5-1 What a type is

  @Test
  internal fun `T5-1 a type is a root class plus one bound per dependency key`() {
    val greenery = type("GreeneryTile<Tharsis_2_2, Player1>")

    greenery.rootClass shouldBe mars.getClass(cn("GreeneryTile"))
    greenery.className shouldBe cn("GreeneryTile")
    greenery.dependencies.keys shouldBe listOf(Key(cn("Occupant"), 0), Key(cn("Owned"), 0))
    greenery.refinement shouldBe null
    greenery.classTable shouldBe mars
  }

  @Test
  internal fun `T5-1 a type is identified by its class, bounds and refinement, not its spelling`() {
    type("GreeneryTile<Tharsis_2_2, Player1>") shouldBe type("GreeneryTile<Player1, Tharsis_2_2>")
    type("GreeneryTile<Tharsis_2_2, Player1>") shouldBe type("GreeneryTile<Tharsis_2_2, Player1>")
    (type("GreeneryTile<Tharsis_2_2>") == type("GreeneryTile<Tharsis_2_3>")) shouldBe false
  }

  // T5-2 Base type

  @Test
  internal fun `T5-2 a bare class name means that class's base type`() {
    type("GreeneryTile") shouldBe mars.getClass(cn("GreeneryTile")).baseType
    type("GreeneryTile").expressionFull shouldBe te("GreeneryTile<MarsArea, Owner>")
    type("GreeneryTile<MarsArea, Owner>") shouldBe type("GreeneryTile")
  }

  @Test
  internal fun `T5-2 an explicit empty argument list also means the base type`() {
    type("GreeneryTile<>") shouldBe type("GreeneryTile")
  }

  // T5-3 Abstractness

  @Test
  internal fun `T5-3 a type is abstract when its class is`() {
    type("Occupant<Tharsis_2_2>").abstract shouldBe true
    type("Tile<Tharsis_2_2>").abstract shouldBe true
  }

  @Test
  internal fun `T5-3 a type is abstract when any dependency bound is`() {
    type("GreeneryTile").abstract shouldBe true
    type("GreeneryTile<Tharsis_2_2>").abstract shouldBe true
    type("GreeneryTile<Player1>").abstract shouldBe true
    type("GreeneryTile<Tharsis_2_2, Player1>").abstract shouldBe false
  }

  @Test
  internal fun `T5-3 a type is abstract when it carries a refinement`() {
    type("GreeneryTile<Tharsis_2_2, Player1>(HAS Neighbor)").abstract shouldBe true
    type("Tharsis_2_2(NOT Tharsis_2_2)").abstract shouldBe true
  }

  @Test
  internal fun `T5-3 abstractness is structural and never consults a world`() {
    type("GreeneryTile<Tharsis_2_2, Player1>").isAbstract(emptyWorld) shouldBe false
    type("GreeneryTile").isAbstract(fullWorld) shouldBe true
  }

  // T5-4 Full form

  @Test
  internal fun `T5-4 the full form states every dependency in key order`() {
    type("GreeneryTile").expressionFull shouldBe te("GreeneryTile<MarsArea, Owner>")
    type("GreeneryTile<Player1>").expressionFull shouldBe te("GreeneryTile<MarsArea, Player1>")
    type("Neighbor<Tharsis_2_2>").expressionFull shouldBe te("Neighbor<Tharsis_2_2, Area>")
  }

  // T5-5 Minimal form

  @Test
  internal fun `T5-5 the minimal form omits every argument equal to the inherited bound`() {
    type("GreeneryTile<MarsArea, Owner>").expression shouldBe te("GreeneryTile")
    type("GreeneryTile<Area>").expression shouldBe te("GreeneryTile")
    type("GreeneryTile<Tharsis_2_2, Owner>").expression shouldBe te("GreeneryTile<Tharsis_2_2>")
  }

  @Test
  internal fun `T5-5 the minimal form writes its arguments in dependency order`() {
    type("GreeneryTile<Player1, Tharsis_2_2>").expression shouldBe
        te("GreeneryTile<Tharsis_2_2, Player1>")
  }

  @Test
  internal fun `T5-5 the minimal form keeps the earliest arguments when sizes tie`() {
    // Both slots accept `Tharsis_2_2`, so one argument suffices, and it is read left to right.
    type("Neighbor<Tharsis_2_2, Area>").expression shouldBe te("Neighbor<Tharsis_2_2>")
    type("Neighbor<Area, Tharsis_2_2>").expression shouldBe te("Neighbor<Area, Tharsis_2_2>")
    type("Neighbor<Tharsis_2_2, Tharsis_2_3>").expression shouldBe
        te("Neighbor<Tharsis_2_2, Tharsis_2_3>")
  }

  @Test
  internal fun `T5-5 the minimal form may omit an argument another one already determines`() {
    val cards =
        loadTypes(
            "CLASS Player1 : Owner",
            "ABSTRACT CLASS CardFront : Owned<Owner> { CLASS Pets }",
            "ABSTRACT CLASS Cardbound<CardFront<Owner>> : Owned<Owner> { CLASS Animal }",
        )

    // The owner is implied by the card, because both positions hold one header variable.
    cards.resolve(te("Animal<Player1, Pets<Player1>>")).expression shouldBe
        te("Animal<Pets<Player1>>")
    cards.resolve(te("Animal<Player1, Pets<Player1>>")).expressionFull shouldBe
        te("Animal<Player1, Pets<Player1>>")
  }

  @Test
  internal fun `T5-5 a refinement is always rendered`() {
    type("GreeneryTile(HAS Neighbor)").expression shouldBe te("GreeneryTile(HAS Neighbor)")
    type("Area(NOT Tharsis_2_2)").expression shouldBe te("Area(NOT Tharsis_2_2)")
  }

  // T5-6 Round-tripping

  @Test
  internal fun `T5-6 both forms resolve back to the same type`() {
    listOf(
            "GreeneryTile",
            "GreeneryTile<Tharsis_2_2>",
            "GreeneryTile<Tharsis_2_2, Player1>",
            "Neighbor<Tharsis_2_2, Tharsis_2_3>",
            "Area(NOT Tharsis_2_2)",
            "Class<GreeneryTile>",
        )
        .forEach {
          val resolved = type(it)
          mars.resolve(resolved.expression) shouldBe resolved
          mars.resolve(resolved.expressionFull) shouldBe resolved
        }
  }

  @Test
  internal fun `T5-6 toString shows the minimal form`() {
    "${type("GreeneryTile<Player1, Tharsis_2_2>")}" shouldBe "GreeneryTile<Tharsis_2_2, Player1>"
  }

  // T5-7 Building a type directly

  @Test
  internal fun `T5-7 withAllDependencies needs every one of the class's own keys`() {
    val greenery = mars.getClass(cn("GreeneryTile"))

    greenery.withAllDependencies(type("GreeneryTile<Tharsis_2_2, Player1>").dependencies) shouldBe
        type("GreeneryTile<Tharsis_2_2, Player1>")
    // `Occupant` supplies no owner, and `GreeneryTile` needs one.
    shouldThrowIae { greenery.withAllDependencies(type("Occupant<Tharsis_2_2>").dependencies) }
  }

  @Test
  internal fun `T5-7 withAllDependencies ignores keys the class does not have`() {
    // This is what projects a type onto each of its supertypes: a greenery tile's owner is simply
    // not part of what an `Occupant` is.
    mars
        .getClass(cn("Occupant"))
        .withAllDependencies(type("GreeneryTile<Tharsis_2_2, Player1>").dependencies) shouldBe
        type("Occupant<Tharsis_2_2>")
  }

  @Test
  internal fun `T5-7 withAllDependencies enforces the class's declared bounds`() {
    val table =
        loadTypes(
            "ABSTRACT CLASS Area { CLASS Land, Water }",
            "ABSTRACT CLASS Holder<Area>",
            "ABSTRACT CLASS Narrow : Holder<Land>",
            "ABSTRACT CLASS Wide : Holder<Area>",
        )

    val waterDependencies = table.resolve(te("Wide<Water>")).dependencies
    shouldThrowIae { table.getClass(cn("Narrow")).withAllDependencies(waterDependencies) }
  }

  @Test
  internal fun `T5-7 specialize applies arguments to the base type`() {
    mars.getClass(cn("GreeneryTile")).specialize(listOf(te("Tharsis_2_2"))) shouldBe
        type("GreeneryTile<Tharsis_2_2>")
    mars.getClass(cn("GreeneryTile")).specialize(listOf()) shouldBe type("GreeneryTile")
  }

  // T5-8 Every type view

  @Test
  internal fun `T5-8 a ground type is its own structural view and has no type variable`() {
    val greenery = type("GreeneryTile")

    greenery.groundType shouldBe greenery
    (greenery.groundType === greenery) shouldBe true
    greenery.typeVariable shouldBe null
  }
}
