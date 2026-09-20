package dev.martianzoo.generated

import dev.martianzoo.pets.HasExpression
import dev.martianzoo.pets.Parsing
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Expression
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

internal class GeneratedPetsTypesUsageTest {
  @Test
  fun gameConfigsAreBuiltFromRichClasses() {
    val config =
        gameConfig(
            modules = listOf(Class.of(HellasMap), Class.of(PreludeExpansion)),
            milestones = listOf(Class.of(Gardener)),
            awards = listOf(Class.of(Banker)),
            colonyTiles = listOf(Class.of(Ceres)),
            cardFronts = listOf(Class.of(AerialMappers)),
            extra = "-WorldGovernmentRule",
            playerNames = listOf("Blue", "Yellow"),
        )

    assertEquals(
        setOf(
            cn("HellasMap"),
            cn("PreludeExpansion"),
            cn("Gardener"),
            cn("Banker"),
            cn("AerialMappers"),
            cn("Ceres"),
        ),
        config.includedClassNames,
    )
    assertEquals(setOf(cn("WorldGovernmentRule")), config.excludedClassNames)
    assertEquals(listOf(cn("Blue"), cn("Yellow")), config.playerNames)
  }

  @Test
  fun generatedTypesConstructTheirExpressionsAndEnforceTheirShapes() {
    assertExpression("SoloOpponent", SoloOpponent())
    assertExpression("Tharsis_4_4", Tharsis_4_4())
    assertExpression("Cimmeria_1_1", Cimmeria_1_1())
    assertExpression("OceanTile<Tharsis_4_4>", OceanTile<Tharsis_4_4>())

    val city = CityTile<Player, Tharsis_4_4>()
    assertExpression("CityTile<Player, Tharsis_4_4>", city)
    assertExpression("CityTile<Player, Tharsis_4_4>", acceptOwnedLandTile(city))

    val greenery = GreeneryTile<Player, Tharsis_4_4>()
    assertExpression("GreeneryTile<Player, Tharsis_4_4>", greenery)
    assertExpression("GreeneryTile<Player, Tharsis_4_4>", acceptOwnedLandTile(greenery))

    val plant: Plant<Kevin> = Plant<Kevin>()
    assertExpression("Plant<Kevin>", plant)
    assertExpression("Kevin", Kevin())
    assertExpression("Class<Kevin>", Class.of(Kevin))

    assertExpression("Terraformer35<Player>", Terraformer35<Player>())
    val aerialMappers: ActionCard<Player, *> = AerialMappers()
    assertExpression("AerialMappers<Player>", aerialMappers)
    assertExpression(
        "AerialMappers<Player>",
        AerialMappers.fromExpression(Parsing.parse("AerialMappers<Player>")),
    )
    assertExpression(
        "AerialMappers<Player>",
        generatedPetsComponent(Parsing.parse("AerialMappers<Player>")),
    )
    assertFailsWith<IllegalArgumentException> {
      AerialMappers.fromExpression(SoloOpponent().expression)
    }
    acceptPlayerActionCard(aerialMappers)
    val classClass: Class<Class<*>> = Class.of(Class)
    assertExpression("Class<Class>", classClass)
    val projectCardClass: Class<ProjectCard<*, *>> = Class.of(ProjectCard)
    assertExpression("Class<ProjectCard>", projectCardClass)
    val aerialMappersClass: Class<AerialMappers<*>> = Class.of(AerialMappers)
    assertExpression("Class<AerialMappers>", aerialMappersClass)
    assertEquals("AerialMappers", AerialMappers.name.toString())
    assertEquals(AerialMappers.name, aerialMappersClass.className)
    assertExpression(
        "Cathedral<Player, CityTile<Player, Tharsis_4_4>>",
        Cathedral<Player, CityTile<Player, Tharsis_4_4>>(),
    )
    assertExpression("Callisto", Callisto())
    acceptCallistoSelection(Callisto())

    assertEquals("TemperatureStep", TemperatureStep().toString())

    // Number properties retain ordinary Kotlin values.
    val birds = Birds<Player>()
    assertEquals(11, PowerPlantProject().cost)
    assertEquals(10, birds.cost)
    assertEquals(1, Tharsis_1_1().row)
    assertEquals(1, Tharsis_1_1().column)

    // Stored Requirement and Metric syntax is parsed into its native Pets type.
    assertEquals("13 OxygenStep", birds.requirement.toString())
    assertEquals("3 GreeneryTile", Gardener<Player>().requirement.toString())
    assertEquals("PROD[MC]", Banker().metric.toString())
    assertEquals(null, QuickStartVariant().premiseRequirement)

    // OwnedTile has separate covariant area and owner parameters:
    // acceptOwnedLandTile(CityTile<Player, Cimmeria_1_1>()) // WaterArea is not LandArea.
    // acceptOwnedLandTile(CityTile<SoloOpponent, Tharsis_4_4>()) // Not owned by a Player.
    // acceptOwnedLandTile(OceanTile<Tharsis_4_4>()) // A Tile, but not an OwnedTile.
    // val wrongArity: OwnedTile<LandArea> // OwnedTile requires both area and owner arguments.

    // Linkage makes the Cathedral owner agree with the nested CityTile owner:
    // Cathedral<Player, CityTile<SoloOpponent, Tharsis_4_4>>()

    // Bounds reject unrelated shapes before an Expression can be constructed:
    // Terraformer35<SoloOpponent>() // SoloOpponent is not a Player.
    // Aerial Mappers' Floater resource type is fixed by its Pets declaration, not caller-selected.
  }

  private fun acceptOwnedLandTile(tile: OwnedTile<Player, LandArea>): HasExpression = tile

  private fun acceptPlayerActionCard(card: ActionCard<Player, *>): HasExpression = card

  private fun acceptCallistoSelection(
      selection: ColonyTileSelection<Class<Callisto>>
  ): HasExpression = selection

  private class Kevin private constructor(override val expression: Expression) : Player {
    override fun toString(): String = expression.toString()

    companion object : Class.Root<Kevin> {
      override val name = cn("Kevin")

      operator fun invoke(): Kevin = Kevin(name.of())
    }
  }

  private fun assertExpression(expected: String, actual: HasExpression) {
    assertEquals(Parsing.parse<Expression>(expected), actual.expression)
  }
}
