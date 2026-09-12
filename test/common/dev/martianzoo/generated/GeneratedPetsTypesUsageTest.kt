package dev.martianzoo.generated

import dev.martianzoo.pets.HasExpression
import dev.martianzoo.pets.Parsing
import dev.martianzoo.pets.ast.Expression
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

internal class GeneratedPetsTypesUsageTest {
  @Test
  fun generatedTypesConstructTheirExpressionsAndEnforceTheirShapes() {
    assertExpression("Player1", Player1())
    assertExpression("Player2", Player2())
    assertExpression("SoloOpponent", SoloOpponent())
    assertExpression("Tharsis_4_4", Tharsis_4_4())
    assertExpression("Cimmeria_1_1", Cimmeria_1_1())
    assertExpression("OceanTile<Tharsis_4_4>", OceanTile<Tharsis_4_4>())

    val city = CityTile<Tharsis_4_4, Player1>()
    assertExpression("CityTile<Tharsis_4_4, Player1>", city)
    assertExpression("CityTile<Tharsis_4_4, Player1>", acceptOwnedLandTile(city))

    val greenery = GreeneryTile<Tharsis_4_4, Player1>()
    assertExpression("GreeneryTile<Tharsis_4_4, Player1>", greenery)
    assertExpression("GreeneryTile<Tharsis_4_4, Player1>", acceptOwnedLandTile(greenery))

    assertExpression("Terraformer35<Player1>", Terraformer35<Player1>())
    val aerialMappers: ActionCard<Player1, *> = AerialMappers()
    assertExpression("AerialMappers<Player1>", aerialMappers)
    assertExpression(
        "AerialMappers<Player1>",
        AerialMappers.fromExpression(Parsing.parse("AerialMappers<Player1>")),
    )
    assertExpression(
        "AerialMappers<Player1>",
        generatedPetsComponent(Parsing.parse("AerialMappers<Player1>")),
    )
    assertFailsWith<IllegalArgumentException> {
      AerialMappers.fromExpression(Player1().expression)
    }
    acceptPlayer1ActionCard(aerialMappers)
    assertExpression("Class<AerialMappers>", AerialMappers.c)
    assertEquals("AerialMappers", AerialMappers.className.toString())
    assertEquals(AerialMappers.className, AerialMappers.c.className)
    assertExpression(
        "Cathedral<Player1, CityTile<Tharsis_4_4, Player1>>",
        Cathedral<Player1, CityTile<Tharsis_4_4, Player1>>(),
    )
    assertExpression("Callisto", Callisto())
    acceptCallistoSelection(Callisto())

    assertEquals("TemperatureStep", TemperatureStep().toString())

    // Number properties retain ordinary Kotlin values.
    val birds = Birds<Player1>()
    assertEquals(11, PowerPlantProject().cost)
    assertEquals(10, birds.cost)
    assertEquals(1, Tharsis_1_1().row)
    assertEquals(1, Tharsis_1_1().column)

    // Stored Requirement and Metric syntax is parsed into its native Pets type.
    assertEquals("13 OxygenStep", birds.requirement.toString())
    assertEquals("3 GreeneryTile", Gardener<Player1>().requirement.toString())
    assertEquals("PROD[MC]", Banker().metric.toString())
    assertEquals(null, QuickStartVariant().premiseRequirement)

    // OwnedTile has separate covariant area and owner parameters:
    // acceptOwnedLandTile(CityTile<Cimmeria_1_1, Player1>()) // WaterArea is not LandArea.
    // acceptOwnedLandTile(CityTile<Tharsis_4_4, SoloOpponent>()) // Not owned by a Player.
    // acceptOwnedLandTile(OceanTile<Tharsis_4_4>()) // A Tile, but not an OwnedTile.
    // val wrongArity: OwnedTile<LandArea> // OwnedTile requires both area and owner arguments.

    // Linkage makes the Cathedral owner agree with the nested CityTile owner:
    // Cathedral<Player1, CityTile<Tharsis_4_4, Player2>>()

    // Bounds reject unrelated shapes before an Expression can be constructed:
    // Terraformer35<SoloOpponent>() // SoloOpponent is not a Player.
    // Aerial Mappers' Floater resource type is fixed by its Pets declaration, not caller-selected.
  }

  private fun acceptOwnedLandTile(tile: OwnedTile<LandArea, Player>): HasExpression = tile

  private fun acceptPlayer1ActionCard(card: ActionCard<Player1, *>): HasExpression = card

  private fun acceptCallistoSelection(
      selection: ColonyTileSelection<Class<Callisto>>
  ): HasExpression = selection

  private fun assertExpression(expected: String, actual: HasExpression) {
    assertEquals(Parsing.parse<Expression>(expected), actual.expression)
  }
}
