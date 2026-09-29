package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.agenttestsupport.testTfm
import dev.martianzoo.pets.api.Exceptions.NarrowingException
import dev.martianzoo.pets.api.Exceptions.TaskException
import dev.martianzoo.pets.data.Player
import dev.martianzoo.tfm.tests.cards.cardnames.Flooding
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class FloodingTest : CardTest() {
  @Test
  internal fun `Can charge the first neighboring owner without charging the others`() {
    playFlooding("Player2", "-4 MC<Player2>, 0 MC<Player3>, 0 MC<Player4>")
  }

  @Test
  internal fun `Can charge the second neighboring owner without charging the others`() {
    playFlooding("Player3", "0 MC<Player2>, -4 MC<Player3>, 0 MC<Player4>")
  }

  @Test
  internal fun `Can charge no one`() {
    arrangeFlooding()
    p1.playProject(Flooding, 7) { placeTile(5, 4) }
        .expect("0 MC<Player2>, 0 MC<Player3>, 0 MC<Player4>")
  }

  @Test
  internal fun `Cannot charge anyone when no ocean area neighbors a tile`() {
    newGame(players = 4)
    val p2 = requireP2()
    admin.phase("Action")
    p1.runOperation("7 MC, ProjectCard")
    p2.runOperation("10 MC")

    p1.playProject(Flooding, 7) {
          shouldThrow<NarrowingException> { doTask("OceanTile<Tharsis_5_4>! THEN -4 MC<Player2>!") }
          placeTile(5, 4)
        }
        .expect("0 MC<Player2>")

    p2.count("MC") shouldBe 10
  }

  @Test
  internal fun `Can remove fewer than four MC from a neighboring player`() {
    arrangeFlooding()
    requireP2().runOperation("-8 MC")
    p1.playProject(Flooding, 7) {
          doTask("OceanTile<Tharsis_5_4>! THEN -2 MC<Player2>!")
        }
        .expect("-2 MC<Player2>, 0 MC<Player3>")
  }

  @Test
  internal fun `Cannot remove more than four MC or attack both neighbors`() {
    arrangeFlooding()
    p1.playProject(Flooding, 7) {
          shouldThrow<NarrowingException> { doTask("OceanTile<Tharsis_5_4>! THEN -5 MC<Player2>!") }
          doTask("OceanTile<Tharsis_5_4>! THEN -4 MC<Player2>!")
          shouldThrow<TaskException> { doTask("-4 MC<Player3>") }
        }
        .expect("-4 MC<Player2>, 0 MC<Player3>")
  }

  @Test
  internal fun `An unrelated ocean while Flooding is pending grants no extra attack`() {
    arrangeFlooding()
    p1.runOperation("$Flooding, OceanTile<Tharsis_1_2>") {
          shouldThrow<NarrowingException> { doTask("-4 MC<Player4>") }
          doTask("OceanTile<Tharsis_5_4>! THEN -4 MC<Player2>!")
          shouldThrow<TaskException> { doTask("-4 MC<Player4>") }
        }
        .expect("-4 MC<Player2>, 0 MC<Player3>, 0 MC<Player4>")
  }

  @Test
  internal fun `A later ocean grants no attack after Flooding resolves`() {
    arrangeFlooding()
    p1.playProject(Flooding, 7) { placeTile(5, 4) }
    p1.runOperation("OceanTile<Tharsis_1_2>") {
          shouldThrow<TaskException> { doTask("-4 MC<Player4>") }
        }
        .expect("0 MC<Player2>, 0 MC<Player3>, 0 MC<Player4>")
  }

  @Test
  internal fun `An adjacent special tile qualifies its owner`() {
    arrangeFlooding()
    requireP2().runOperation("-CityTile<Tharsis_4_3>, NaturalPreserve_SpecialTile<Tharsis_4_3>")
    p1.playProject(Flooding, 7) {
          doTask("OceanTile<Tharsis_5_4>! THEN -4 MC<Player2>!")
        }
        .expect("-4 MC<Player2>, 0 MC<Player3>, 0 MC<Player4>")
  }

  @Test
  internal fun `Can play at the ocean limit without placing an ocean or attacking`() {
    arrangeFlooding()
    admin.runOperation(
        "OceanTile<Tharsis_1_2>, OceanTile<Tharsis_1_4>, OceanTile<Tharsis_1_5>, " +
            "OceanTile<Tharsis_2_6>, OceanTile<Tharsis_4_8>, OceanTile<Tharsis_5_5>, " +
            "OceanTile<Tharsis_5_6>, OceanTile<Tharsis_6_7>, OceanTile<Tharsis_6_8>"
    )
    p1.playProject(Flooding, 7) {
          shouldThrow<TaskException> { doTask("-4 MC<Player2>") }
          shouldThrow<TaskException> { doTask("-4 MC<Player4>") }
        }
        .expect("0 OceanTile, 0 TerraformRating, 0 MC<Player2>, 0 MC<Player3>, 0 MC<Player4>")
  }

  @Test
  internal fun `Placing the last ocean still permits the attack`() {
    arrangeFlooding()
    admin.runOperation(
        "OceanTile<Tharsis_1_2>, OceanTile<Tharsis_1_4>, OceanTile<Tharsis_1_5>, " +
            "OceanTile<Tharsis_2_6>, OceanTile<Tharsis_4_8>, OceanTile<Tharsis_5_5>, " +
            "OceanTile<Tharsis_5_6>, OceanTile<Tharsis_6_7>"
    )
    p1.playProject(Flooding, 7) {
          p1.selectTask(tasks.ids().single())
          shouldThrow<NarrowingException> {
            doTask("OceanTile<Tharsis_5_4>! THEN -4 MC<Player4>!")
          }
          doTask("OceanTile<Tharsis_5_4>! THEN -4 MC<Player2>!")
        }
        .expect("OceanTile, TerraformRating, -4 MC<Player2>, 0 MC<Player4>")
  }

  @Test
  internal fun `Cannot discard ocean restrictions by choosing the victim first`() {
    arrangeFlooding()
    admin.runOperation("OceanTile<Tharsis_1_2>")
    p1.playProject(Flooding, 7) {
          p1.selectTask(tasks.ids().single())
          shouldThrow<NarrowingException> {
            p1.narrowTask("OceanTile<WaterArea>! THEN -4 MC<Player4>!")
          }
          shouldThrow<NarrowingException> {
            p1.narrowTask("OceanTile<WaterArea(HAS MAX 0 Tile)>! THEN -4 MC<Player4>!")
          }
          doTask("OceanTile<Tharsis_5_4>! THEN -4 MC<Player2>!")
        }
        .expect("-4 MC<Player2>, 0 MC<Player3>, 0 MC<Player4>")
  }

  @Test
  internal fun `Further narrowing cannot switch the chosen victim`() {
    arrangeFlooding()
    p1.playProject(Flooding, 7) {
          doTask("OceanTile<Tharsis_5_4>! THEN -4 MC<Player2>?")
          p1.selectTask(tasks.ids().single())
          p1.narrowTask("-4 MC<Player2>?")
          shouldThrow<NarrowingException> { doTask("-4 MC<Player4>!") }
          doTask("-2 MC<Player2>!")
        }
        .expect("-2 MC<Player2>, 0 MC<Player3>, 0 MC<Player4>")
  }

  @Test
  internal fun `Must place an ocean when the track is not complete`() {
    arrangeFlooding()
    p1.playProject(Flooding, 7) {
          shouldThrow<NarrowingException> { declineTask() }
          placeTile(5, 4)
        }
        .expect("0 MC<Player2>, 0 MC<Player3>, 0 MC<Player4>")
  }

  @Test
  internal fun `An occupied water area cannot substitute an existing ocean for a new placement`() {
    arrangeFlooding()
    admin.runOperation("OceanTile<Tharsis_1_2>")
    p1.playProject(Flooding, 7) {
          shouldThrow<NarrowingException> { doTask("OceanTile<Tharsis_1_2>! THEN -4 MC<Player4>!") }
          shouldThrow<NarrowingException> { doTask("-4 MC<Player4>") }
          doTask("OceanTile<Tharsis_5_4>! THEN -4 MC<Player2>!")
        }
        .expect("-4 MC<Player2>, 0 MC<Player3>, 0 MC<Player4>")
  }

  @Test
  internal fun `Another ocean can fill the track while Flooding is pending`() {
    arrangeFlooding()
    admin.runOperation(
        "OceanTile<Tharsis_1_4>, OceanTile<Tharsis_1_5>, OceanTile<Tharsis_2_6>, " +
            "OceanTile<Tharsis_4_8>, OceanTile<Tharsis_5_5>, OceanTile<Tharsis_5_6>, " +
            "OceanTile<Tharsis_6_7>, OceanTile<Tharsis_6_8>"
    )
    p1.runOperation("$Flooding, OceanTile<Tharsis_1_2>") {
          shouldThrow<TaskException> { doTask("-4 MC<Player2>") }
          shouldThrow<TaskException> { doTask("-4 MC<Player4>") }
        }
        .expect("0 MC<Player2>, 0 MC<Player3>, 0 MC<Player4>")
    admin.count("OceanTile") shouldBe 9
  }

  @Test
  internal fun `Cannot charge a non-neighboring owner`() {
    arrangeFlooding()
    p1.playProject(Flooding, 7) {
          shouldThrow<NarrowingException> {
            doTask("OceanTile<Tharsis_5_4>! THEN -4 MC<Player4>!")
          }
          doTask("OceanTile<Tharsis_5_4>! THEN -4 MC<Player2>!")
        }
        .expect("-4 MC<Player2>, 0 MC<Player3>, 0 MC<Player4>")
  }

  @Test
  internal fun `Cannot qualify the victim through a different ocean area`() {
    arrangeFlooding()
    p1.playProject(Flooding, 7) {
          shouldThrow<NarrowingException> {
            doTask("OceanTile<Tharsis_1_2>! THEN -4 MC<Player2>!")
          }
          doTask("OceanTile<Tharsis_1_2>! THEN -4 MC<Player4>!")
        }
        .expect("0 MC<Player2>, 0 MC<Player3>, -4 MC<Player4>")
  }

  @Test
  internal fun `Partial narrowing cannot discard the shared victim`() {
    arrangeFlooding()
    p1.playProject(Flooding, 7) {
          p1.selectTask(tasks.ids().single())
          shouldThrow<NarrowingException> {
            p1.narrowTask("OceanTile<Tharsis_5_4>! THEN -4 MC<Anyone>?")
          }
          shouldThrow<NarrowingException> {
            p1.narrowTask("OceanTile<WaterArea>! THEN -4 MC<Player2>?")
          }
          p1.count("OceanTile") shouldBe 0
          doTask("OceanTile<Tharsis_5_4>! THEN -4 MC<Player2>!")
        }
        .expect("-4 MC<Player2>, 0 MC<Player3>, 0 MC<Player4>")
  }

  @Test
  internal fun `Selecting an unresolved attack arm cannot start the placement`() {
    arrangeFlooding()
    p1.playProject(Flooding, 7) {
          p1.selectTask(tasks.ids().single())
          shouldThrow<NarrowingException> {
            p1.narrowTask(
                "OceanTile<WaterArea(HAS MAX 0 Tile, HAS Neighbor<OwnedTile<Anyone>>)>! " +
                    "THEN -4 MC<Anyone>?"
            )
          }
          p1.count("OceanTile") shouldBe 0
          shouldThrow<NarrowingException> {
            doTask("OceanTile<Tharsis_5_4>! THEN -4 MC<Player4>!")
          }
          doTask("OceanTile<Tharsis_5_4>! THEN -4 MC<Player2>!")
        }
        .expect("-4 MC<Player2>, 0 MC<Player3>, 0 MC<Player4>")
  }

  @Test
  internal fun `Can decline the loss in a complete placement choice`() {
    arrangeFlooding()
    p1.playProject(Flooding, 7) {
          doTask("OceanTile<Tharsis_5_4>! THEN Ok")
        }
        .expect("0 MC<Player2>, 0 MC<Player3>, 0 MC<Player4>")
  }

  @Test
  internal fun `Can choose a smaller optional loss while selecting the victim`() {
    arrangeFlooding()
    p1.playProject(Flooding, 7) {
          doTask("OceanTile<Tharsis_5_4>! THEN -2 MC<Player2>?")
          doTask("-2 MC<Player2>!")
        }
        .expect("-2 MC<Player2>, 0 MC<Player3>, 0 MC<Player4>")
  }

  private fun playFlooding(owner: String, expectedCharge: String) {
    arrangeFlooding()
    p1.playProject(Flooding, 7) {
          doTask("OceanTile<Tharsis_5_4>! THEN -4 MC<$owner>!")
        }
        .expect(expectedCharge)
  }

  private fun arrangeFlooding() {
    val game = newGame(players = 4)
    val p2 = requireP2()
    val players = Player.players(4)
    val p3 = game.testTfm(players[2])
    val p4 = game.testTfm(players[3])
    admin.phase("Action")
    p1.runOperation("7 MC, ProjectCard")
    p2.runOperation("10 MC, CityTile<Tharsis_4_3>")
    p3.runOperation("10 MC, CityTile<Tharsis_5_3>")
    p4.runOperation("10 MC, CityTile<Tharsis_1_1>")
  }
}
