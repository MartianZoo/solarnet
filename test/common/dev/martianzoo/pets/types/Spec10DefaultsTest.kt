package dev.martianzoo.pets.types

import dev.martianzoo.pets.api.Exceptions.PetException
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Instruction.Quantifier.AMAP
import dev.martianzoo.pets.ast.Instruction.Quantifier.MANDATORY
import dev.martianzoo.pets.ast.Instruction.Quantifier.OPTIONAL
import dev.martianzoo.pets.data.ClassDeclaration
import dev.martianzoo.pets.data.ClassDeclaration.ClassKind.CONCRETE
import dev.martianzoo.pets.data.ClassDeclaration.DefaultsDeclaration
import dev.martianzoo.pets.data.ClassDeclaration.DefaultsDeclaration.OneDefault
import dev.martianzoo.pets.types.Dependency.Key
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import kotlin.test.Test

/** Section 10 of `docs/type-system-spec.md`: defaults. */
internal class Spec10DefaultsTest {

  private val mars =
      loadTypes(
          """
          CLASS Player1 : Owner
          ABSTRACT CLASS Area {
            ABSTRACT CLASS MarsArea {
              ABSTRACT CLASS LandArea { CLASS Tharsis_2_2 }
              ABSTRACT CLASS WaterArea { CLASS Tharsis_1_1 }
            }
          }
          ABSTRACT CLASS Tile<Area> : Owned<Owner> {
            DEFAULT +Tile<LandArea>
          }
          CLASS GreeneryTile : Tile<MarsArea>
          CLASS OceanTile : Tile<MarsArea> {
            DEFAULT +OceanTile<WaterArea>
          }
          CLASS Plant : Owned
          """
              .trimIndent()
      )

  private fun defaults(name: String) = mars.getClass(cn(name)).defaults

  // T10-1 The three default sets

  @Test
  internal fun `T10-1 defaults are gathered separately for all uses, gains and removals`() {
    val tile = defaults("Tile")

    tile.allUsages.dependencies.keys shouldContainExactly listOf(Key(cn("Owned"), 0))
    tile.gainOnly.dependencies.keys shouldContainExactly listOf(Key(cn("Tile"), 0))
    tile.removeOnly.dependencies.keys.shouldBeEmpty()
  }

  @Test
  internal fun `T10-1 an all-uses default supplies a bound wherever the type is written`() {
    // The system class `Owned` declares `DEFAULT Owned<Owner>`.
    defaults("Plant").allUsages.dependencies.get(Key(cn("Owned"), 0)).expressionFull shouldBe
        te("Owner")
    mars.getClass(cn("Plant")).defaultType.expressionFull shouldBe te("Plant<Owner>")
  }

  @Test
  internal fun `T10-1 defaultType is the base type with the all-uses defaults applied`() {
    // Inherited dependencies come first, so `Owned_0` precedes `Tile_0`.
    mars.getClass(cn("GreeneryTile")).baseType.expressionFull shouldBe
        te("GreeneryTile<Owner, MarsArea>")
    mars.getClass(cn("GreeneryTile")).defaultType.expressionFull shouldBe
        te("GreeneryTile<Owner, MarsArea>")
  }

  @Test
  internal fun `T10-1 defaults never change which types exist`() {
    // The gain default names LandArea, but the type `GreeneryTile` still admits any MarsArea.
    mars.resolve(te("GreeneryTile<Tharsis_1_1>")).expressionFull shouldBe
        te("GreeneryTile<Owner, Tharsis_1_1>")
  }

  // T10-2 Quantifiers

  @Test
  internal fun `T10-2 gain and removal quantifiers are inherited independently`() {
    val table =
        loadTypes(
            """
            ABSTRACT CLASS GlobalParameter { DEFAULT +GlobalParameter. }
            ABSTRACT CLASS Optional { DEFAULT -Optional? }
            CLASS OceanTile : GlobalParameter, Optional
            ABSTRACT CLASS Mandatory { DEFAULT +Mandatory! }
            CLASS Fixed : Mandatory { DEFAULT +Fixed. }
            """
                .trimIndent()
        )

    table.getClass(cn("GlobalParameter")).defaults.gainOnly.quantifier shouldBe AMAP
    // `Component` supplies `!` for anything that does not override it.
    table.getClass(cn("GlobalParameter")).defaults.removeOnly.quantifier shouldBe MANDATORY
    table.getClass(cn("OceanTile")).defaults.gainOnly.quantifier shouldBe AMAP
    table.getClass(cn("OceanTile")).defaults.removeOnly.quantifier shouldBe OPTIONAL
    table.getClass(cn("Fixed")).defaults.gainOnly.quantifier shouldBe AMAP
  }

  @Test
  internal fun `T10-2 supertypes that disagree about a quantifier are an error`() {
    val table =
        loadTypes(
            "ABSTRACT CLASS Eager { DEFAULT +Eager. }",
            "ABSTRACT CLASS Choosy { DEFAULT +Choosy? }",
            "CLASS Both : Eager, Choosy",
        )

    shouldThrow<PetException> { table.getClass(cn("Both")).defaults }
  }

  // T10-3 A default names its own class

  @Test
  internal fun `T10-3 a DEFAULT clause must name the class that declares it`() {
    shouldThrow<PetException> {
      loadTypes(
          "ABSTRACT CLASS Area { CLASS Tharsis_2_2 }",
          "CLASS Tile<Area> { DEFAULT Area<Tharsis_2_2> }",
      )
    }
    shouldThrow<IllegalArgumentException> {
      ClassDeclaration(
          cn("Tile"),
          CONCRETE,
          defaultsDeclaration = DefaultsDeclaration(forClass = cn("Other")),
      )
    }
    shouldThrow<IllegalArgumentException> {
      DefaultsDeclaration(gainOnly = OneDefault(quantifier = OPTIONAL))
    }
  }

  // T10-4 Inheriting dependency defaults

  @Test
  internal fun `T10-4 the nearest declaring superclass supplies the default`() {
    defaults("GreeneryTile").gainOnly.dependencies.get(Key(cn("Tile"), 0)).expressionFull shouldBe
        te("LandArea")
    defaults("OceanTile").gainOnly.dependencies.get(Key(cn("Tile"), 0)).expressionFull shouldBe
        te("WaterArea")
  }

  @Test
  internal fun `T10-4 an inherited default is intersected with the class's own bound`() {
    val table =
        loadTypes(
            """
            ABSTRACT CLASS Area {
              ABSTRACT CLASS MarsArea {
                ABSTRACT CLASS LandArea { CLASS Tharsis_2_2 }
              }
              ABSTRACT CLASS RemoteArea { CLASS Phobos }
            }
            ABSTRACT CLASS Tile<Area> { DEFAULT +Tile<MarsArea> }
            CLASS CityTile : Tile<LandArea>
            """
                .trimIndent()
        )

    table
        .getClass(cn("CityTile"))
        .defaults
        .gainOnly
        .dependencies
        .get(Key(cn("Tile"), 0))
        .expressionFull shouldBe te("LandArea")
  }

  @Test
  internal fun `T10-4 a default that merely restates the declared bound records nothing`() {
    val table =
        loadTypes(
            "ABSTRACT CLASS Area { CLASS Tharsis_2_2 }",
            "ABSTRACT CLASS Tile<Area> { DEFAULT +Tile<Area> }",
        )

    table.getClass(cn("Tile")).defaults.gainOnly.dependencies.keys.shouldBeEmpty()
  }

  @Test
  internal fun `T10-4 supertypes with no common narrowing for one default are an error`() {
    val table =
        loadTypes(
            """
            ABSTRACT CLASS Area {
              ABSTRACT CLASS LandArea
              ABSTRACT CLASS WaterArea
            }
            ABSTRACT CLASS Tile<Area>
            ABSTRACT CLASS LandTile : Tile { DEFAULT +LandTile<LandArea> }
            ABSTRACT CLASS WaterTile : Tile { DEFAULT +WaterTile<WaterArea> }
            CLASS Impossible : LandTile, WaterTile
            """
                .trimIndent()
        )

    shouldThrow<PetException> { table.getClass(cn("Impossible")).defaults }
  }

  // T10-5 `Owner` stays contextual

  @Test
  internal fun `T10-5 Owner written in a default is kept as written, not resolved to the bound`() {
    val table =
        loadTypes(
            "ABSTRACT CLASS Player : Owner { CLASS Player1 }",
            "ABSTRACT CLASS Card : Owned<Player> { DEFAULT Card<Owner> \n CLASS ProjectCard }",
        )

    table.getClass(cn("Card")).baseType.expressionFull shouldBe te("Card<Player>")
    // The default deliberately keeps the wider `Owner`, so that a context can supply the value.
    table
        .getClass(cn("Card"))
        .defaults
        .allUsages
        .dependencies
        .get(Key(cn("Owned"), 0))
        .expressionFull shouldBe te("Owner")
    table.getClass(cn("ProjectCard")).defaultExpression shouldBe te("ProjectCard<Owner>")
    table.getClass(cn("ProjectCard")).defaultType.expressionFull shouldBe te("ProjectCard<Player>")
  }

  @Test
  internal fun `T10-5 the default template is not a constructed type outside its class bounds`() {
    val table =
        loadTypes(
            "ABSTRACT CLASS Player : Owner { CLASS Player1 }",
            "ABSTRACT CLASS Card : Owned<Player> { DEFAULT Card<Owner> \n CLASS ProjectCard }",
        )
    val projectCard = table.getClass(cn("ProjectCard"))

    projectCard.defaultExpression shouldBe te("ProjectCard<Owner>")
    projectCard.defaultType.isSubtypeOf(projectCard.baseType) shouldBe true
  }
}
