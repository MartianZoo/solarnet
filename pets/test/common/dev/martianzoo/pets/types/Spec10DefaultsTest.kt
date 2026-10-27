package dev.martianzoo.pets.types

import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.PetElaborator
import dev.martianzoo.pets.api.Exceptions.ExpressionException
import dev.martianzoo.pets.api.Exceptions.InvalidPetDefinitionException
import dev.martianzoo.pets.api.Exceptions.PetSyntaxException
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Instruction.Quantifier.AMAP
import dev.martianzoo.pets.ast.Instruction.Quantifier.MANDATORY
import dev.martianzoo.pets.ast.Instruction.Quantifier.OPTIONAL
import dev.martianzoo.pets.ast.InstructionTree
import dev.martianzoo.pets.data.ClassDeclaration
import dev.martianzoo.pets.data.ClassDeclaration.ClassKind.CONCRETE
import dev.martianzoo.pets.data.ClassDeclaration.DefaultsDeclaration
import dev.martianzoo.pets.data.ClassDeclaration.DefaultsDeclaration.OneDefault
import dev.martianzoo.pets.types.Dependency.Key
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
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
          ABSTRACT CLASS Tile<Area> : Owned {
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
  internal fun `T10-1 dependency defaults are gathered separately for all uses, gains and triggers`() {
    val tile = defaults("Tile")

    tile.allUsages.dependencies.keys.shouldBeEmpty()
    tile.gainOnly.dependencies.keys shouldContainExactly listOf(Key(cn("Tile"), 0))
    tile.triggerOnly.dependencies.keys.shouldBeEmpty()
  }

  @Test
  internal fun `T10-1 Owned supplies a bound without a dependency default`() {
    defaults("Plant").allUsages.dependencies.keys.shouldBeEmpty()
    mars.getClass(cn("Plant")).defaultType.expressionFull shouldBe te("Plant<Owner>")
  }

  @Test
  internal fun `T10-1 defaultType is the base type with the all-uses defaults applied`() {
    val table =
        loadTypes(
            """
            ABSTRACT CLASS CardLocation {
              CLASS Hand
              CLASS Discard
            }
            """,
            "ABSTRACT CLASS CardBack<CardLocation> { DEFAULT CardBack<Hand> }",
            "CLASS ProjectCard : CardBack",
        )
    val projectCard = table.getClass(cn("ProjectCard"))

    projectCard.baseType.expressionFull shouldBe te("ProjectCard<CardLocation>")
    projectCard.defaultExpression shouldBe te("ProjectCard<Hand>")
    projectCard.defaultType.expressionFull shouldBe te("ProjectCard<Hand>")
    table.resolve(te("ProjectCard")) shouldBe projectCard.baseType
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
            ABSTRACT CLASS GlobalParameter { DEFAULT +GlobalParameter* }
            ABSTRACT CLASS Optional { DEFAULT -Optional? }
            CLASS OceanTile : GlobalParameter, Optional
            ABSTRACT CLASS Mandatory { DEFAULT +Mandatory! }
            CLASS Fixed : Mandatory { DEFAULT +Fixed* }
            """
                .trimIndent()
        )

    table.getClass(cn("GlobalParameter")).defaults.gainOnly.quantifier shouldBe AMAP
    // `Component` supplies `!` for anything that does not override it.
    table.getClass(cn("GlobalParameter")).defaults.removalQuantifier shouldBe MANDATORY
    table.getClass(cn("OceanTile")).defaults.gainOnly.quantifier shouldBe AMAP
    table.getClass(cn("OceanTile")).defaults.removalQuantifier shouldBe OPTIONAL
    table.getClass(cn("Fixed")).defaults.gainOnly.quantifier shouldBe AMAP
  }

  @Test
  internal fun `T10-2 supertypes that disagree about a quantifier are an error`() {
    shouldThrow<InvalidPetDefinitionException> {
      loadTypes(
          "ABSTRACT CLASS Eager { DEFAULT +Eager* }",
          "ABSTRACT CLASS Choosy { DEFAULT +Choosy? }",
          "CLASS Both : Eager, Choosy",
      )
    }
  }

  // T10-3 A default names its own class

  @Test
  internal fun `T10-3 a DEFAULT clause must name the class that declares it`() {
    shouldThrow<PetSyntaxException> {
      loadTypes(
          "ABSTRACT CLASS Area { CLASS Tharsis_2_2 }",
          "CLASS Tile<Area> { DEFAULT Area<Tharsis_2_2> }",
      )
    }
    shouldThrow<PetSyntaxException> {
      ClassDeclaration(
          cn("Tile"),
          CONCRETE,
          defaultsDeclaration =
              DefaultsDeclaration(
                  gainOnly = OneDefault(quantifier = OPTIONAL),
                  forClass = cn("Other"),
              ),
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
  internal fun `T10-4 differing inherited defaults are rejected even when compatible`() {
    shouldThrow<InvalidPetDefinitionException> {
          loadTypes(
              """
              ABSTRACT CLASS Area {
                ABSTRACT CLASS MarsArea {
                  ABSTRACT CLASS LandArea { CLASS Tharsis_2_2 }
                }
              }
              ABSTRACT CLASS Tile<Area>
              ABSTRACT CLASS MarsTile : Tile { DEFAULT +MarsTile<MarsArea> }
              ABSTRACT CLASS LandTile : Tile { DEFAULT +LandTile<LandArea> }
              CLASS GreeneryTile : LandTile, MarsTile
              """
                  .trimIndent()
          )
        }
        .detail shouldContain "inherits conflicting defaults"
  }

  @Test
  internal fun `T10-4 narrowing a bound does not hide a conflict between inherited defaults`() {
    shouldThrow<InvalidPetDefinitionException> {
          loadTypes(
              """
            ABSTRACT CLASS Area {
              ABSTRACT CLASS LandArea {
                CLASS First
                CLASS Second
              }
              ABSTRACT CLASS WaterArea { CLASS Ocean }
            }
            ABSTRACT CLASS Tile<Area>
            ABSTRACT CLASS LandTile : Tile { DEFAULT +LandTile<First> }
            ABSTRACT CLASS WaterTile : Tile { DEFAULT +WaterTile<Ocean> }
            CLASS RestrictedTile : LandTile<LandArea>, WaterTile
            """
          )
        }
        .detail shouldContain "inherits conflicting defaults"
  }

  @Test
  internal fun `T10-4 equal defaults from separate inheritance paths agree`() {
    val table =
        loadTypes(
            """
        ABSTRACT CLASS Area { CLASS Land }
        ABSTRACT CLASS Tile<Area>
        ABSTRACT CLASS LeftTile : Tile { DEFAULT +LeftTile<Land> }
        ABSTRACT CLASS RightTile : Tile { DEFAULT +RightTile<Land> }
        CLASS CombinedTile : LeftTile, RightTile
        """
        )

    PetElaborator(table).elaborateInput(parse<InstructionTree>("CombinedTile<>")) shouldBe
        parse<InstructionTree>("CombinedTile<Land>!")
  }

  @Test
  internal fun `T10-4 a dependency default must narrow a declared bound`() {
    for (clause in listOf("DEFAULT Tile<Area>", "DEFAULT +Tile<Area>", "DEFAULT Tile<Area>:")) {
      shouldThrow<InvalidPetDefinitionException> {
            loadTypes(
                "ABSTRACT CLASS Area { CLASS First }",
                "ABSTRACT CLASS Tile<Area> { $clause }",
            )
          }
          .detail shouldContain "has no effect"
    }
  }

  @Test
  internal fun `T10-4 a bound-sized argument can reserve a position for another default`() {
    val table =
        loadTypes(
            """
            ABSTRACT CLASS Area {
              CLASS First
              CLASS Second
            }
            CLASS Pair<Area, Area> { DEFAULT +Pair<Area, Second> }
            """,
        )

    PetElaborator(table).elaborateInput(parse<InstructionTree>("Pair<First>")) shouldBe
        parse<InstructionTree>("Pair<First, Second>!")
  }

  @Test
  internal fun `T10-4 a quantifier does not hide ineffective dependency arguments`() {
    for (clauses in listOf("DEFAULT +Tile<Area>*", "DEFAULT +Tile<Area>; DEFAULT +Tile*")) {
      shouldThrow<InvalidPetDefinitionException> {
            loadTypes(
                "ABSTRACT CLASS Area { CLASS First }",
                "CLASS Tile<Area> { $clauses }",
            )
          }
          .detail shouldContain "has no effect"
    }
  }

  @Test
  internal fun `T10-4 supertypes with no common narrowing for one default are an error`() {
    shouldThrow<InvalidPetDefinitionException> {
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
    }
  }

  @Test
  internal fun `T10-4 an inapplicable override leaves the choice open without reviving an ancestor`() {
    // Synthetic precedence case; no Canon card requiring this combination is known.
    val table =
        loadTypes(
            """
            ABSTRACT CLASS Area {
              ABSTRACT CLASS LandArea {
                CLASS First
                CLASS Second
              }
              ABSTRACT CLASS WaterArea { CLASS Ocean }
            }
            ABSTRACT CLASS Tile<Area> { DEFAULT +Tile<First> }
            ABSTRACT CLASS WaterTile : Tile { DEFAULT +WaterTile<WaterArea> }
            CLASS RestrictedTile : WaterTile<LandArea>
            """
        )

    val elaborator = PetElaborator(table)
    elaborator.elaborateInput(parse<InstructionTree>("WaterTile<>")) shouldBe
        parse<InstructionTree>("WaterTile<WaterArea>!")
    elaborator.elaborateInput(parse<InstructionTree>("RestrictedTile<Second>")) shouldBe
        parse<InstructionTree>("RestrictedTile<Second>!")

    // WaterTile already overrode First. Its inapplicable default is omitted, leaving both
    // land areas available instead of silently restoring First.
    elaborator.elaborateInput(parse<InstructionTree>("RestrictedTile")) shouldBe
        parse<InstructionTree>("RestrictedTile!")
    table.getClass(cn("RestrictedTile")).defaults.gainOnly.dependencies.keys.shouldBeEmpty()
    shouldThrow<ExpressionException> {
          elaborator.elaborateInput(parse<InstructionTree>("RestrictedTile<>"))
        }
        .detail shouldContain "has no gain dependency defaults to accept"
  }

  // T10-5 Anyone is an ordinary default argument

  @Test
  internal fun `T10-5 a broader default is ineffective after intersection with its bound`() {
    shouldThrow<InvalidPetDefinitionException> {
          loadTypes(
              "ABSTRACT CLASS Player : Owner { CLASS Player1 }",
              "ABSTRACT CLASS Card : Owned<Player> { DEFAULT Card<Anyone> }",
          )
        }
        .detail shouldContain "has no effect"
  }
}
