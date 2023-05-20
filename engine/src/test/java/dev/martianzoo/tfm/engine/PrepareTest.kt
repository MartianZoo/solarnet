package dev.martianzoo.tfm.engine

import com.google.common.truth.Truth.assertThat
import dev.martianzoo.tfm.api.Exceptions.LimitsException
import dev.martianzoo.tfm.api.Exceptions.RequirementException
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.data.Player.Companion.PLAYER1
import dev.martianzoo.tfm.engine.Game.GameWriterImpl
import dev.martianzoo.tfm.engine.PlayerSession.Companion.session
import dev.martianzoo.tfm.pets.Parsing.parse
import dev.martianzoo.tfm.pets.PetTransformer
import dev.martianzoo.tfm.pets.Transforming.replaceOwnerWith
import dev.martianzoo.tfm.pets.ast.Instruction
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

private class PrepareTest {

  val game = Game.create(Canon.SIMPLE_GAME)
  val p1 = game.session(PLAYER1)

  init {
    p1.writer.unsafe().sneak("Plant, 10 ProjectCard, PROD[-1]")
  }

  fun preprocess(instr: Instruction): Instruction {
    return PetTransformer.chain(
        game.transformers.deprodify(),
        game.transformers.insertDefaults(),
        replaceOwnerWith(PLAYER1),
    )
        .transform(instr)
  }

  fun preprocessAndPrepare(unprepared: String): Instruction {
    val writer = game.writer(PLAYER1) as GameWriterImpl
    return writer.instructor.prepare(preprocess(parse(unprepared)))
  }

  fun checkPrepare(unprepared: String, expected: String?) {
    val prepared = preprocessAndPrepare(unprepared)
    assertThat("$prepared").isEqualTo("$expected")
  }

  @Test
  fun testPrepareChange() {
    checkPrepare("Ok", "Ok")
    checkPrepare("2 Plant", "2 Plant<Player1>!")
    checkPrepare("2 Plant.", "2 Plant<Player1>!")
    checkPrepare("2 Plant?", "2 Plant<Player1>?")
    checkPrepare("-Plant", "-Plant<Player1>!")
    checkPrepare("-9 Plant.", "-Plant<Player1>!")
    checkPrepare("55 OxygenStep.", "14 OxygenStep!")
    checkPrepare("-4 Heat.", "Ok")
    checkPrepare("-4 Heat?", "Ok")
    checkPrepare("Heat FROM Plant.", "Heat<Player1> FROM Plant<Player1>!")
    checkPrepare("9 Heat FROM Plant?", "Heat<Player1> FROM Plant<Player1>?")
    checkPrepare("Plant FROM Heat.", "Ok")
    checkPrepare("Plant FROM Heat?", "Ok")
    assertThrows<LimitsException>("1") { preprocessAndPrepare("15 OxygenStep!") }
    assertThrows<LimitsException>("2") { preprocessAndPrepare("-2 Plant") }
    assertThrows<LimitsException>("3") { preprocessAndPrepare("Plant FROM Heat") }
    assertThrows<LimitsException>("4") { preprocessAndPrepare("2 Heat FROM Plant") }
    assertThrows<LimitsException>("5") { preprocessAndPrepare("2 Plant<Player2 FROM Player1>") }
  }

  @Test
  fun testPreparePer() {
    checkPrepare("Plant / TerraformRating", "20 Plant<Player1>!")
    checkPrepare("Plant / 3 TerraformRating", "6 Plant<Player1>!")
    checkPrepare("Plant / 3 TerraformRating MAX 2", "2 Plant<Player1>!")
    checkPrepare("Plant / Steel", "Ok")
    checkPrepare("Plant / 21 TerraformRating", "Ok")
    checkPrepare("-Plant. / TR", "-Plant<Player1>!")
    checkPrepare("-Plant? / TR", "-Plant<Player1>?")
    // checkPrepare("Plant / 6 TR / 7 TR", "6 Plant<Player1>!") // TODO these should work
    // checkPrepare("(10 TR: Plant) / TerraformRating", "20 Plant<Player1>!")
  }

  @Test
  fun testPrepareGated() {
    checkPrepare("10 TR: Plant", "Plant<Player1>!")
    checkPrepare("10 TR: Plant / TerraformRating", "20 Plant<Player1>!")
    // TODO I'm nervous about the <Anyone> disappearing
    checkPrepare("10 TR: Plant<Anyone> / TerraformRating", "20 Plant!")
    checkPrepare(
        "10 TR: (Titanium OR TerraformRating)",
        "Titanium<Player1>! OR TerraformRating<Player1>!",
    )
    assertThrows<RequirementException>("1") { preprocessAndPrepare("30 TR: Plant") }
  }

  @Test
  fun testPrepareOr() {
    checkPrepare(
        "15 OxygenStep! OR -2 Plant OR Plant FROM Heat " +
            "OR Ok OR 2 Heat FROM Plant OR 2 Plant<Player2 FROM Player1> OR 30 TR: Plant",
        "Ok",
    )
    checkPrepare(
        "15 OxygenStep! OR -2 Plant OR Plant FROM Heat OR (TR: 8 Steel) OR " +
            "2 Heat FROM Plant OR 2 Plant<Player2 FROM Player1> OR 30 TR: Plant",
        "8 Steel<Player1>!",
    )

    checkPrepare(
        "15 OxygenStep! OR -2 Plant OR Plant FROM Heat OR -Plant. / TR OR 8 Steel OR " +
            "2 Heat FROM Plant OR 2 Plant<Player2 FROM Player1> OR 30 TR: Plant",
        "-Plant<Player1>! OR 8 Steel<Player1>!",
    )

    checkPrepare("PROD[Plant OR 3 PlantTag: 4 Plant]", "Production<Player1, Class<Plant>>!")
    checkPrepare(
        "Steel / 2 ProjectCard OR -Titanium? OR Plant: 5 Steel OR Ok OR 5 Steel",
        "5 Steel<Player1>! OR Ok",
    )
    assertThrows<Exception>("1") { // TODO what exception type is appropriate?
      preprocessAndPrepare(
          "15 OxygenStep! OR -2 Plant OR Plant FROM Heat OR 2 Heat FROM Plant " +
              "OR 2 Plant<Player2 FROM Player1> OR 30 TR: Plant",
      )
    }
  }

  @Test
  fun testPrepareMulti() {
    // TODO exception types
    assertThrows<IllegalStateException>("1") { preprocessAndPrepare("Plant, Heat") }
    assertThrows<IllegalStateException>("2") { preprocessAndPrepare("(TR: Plant), Heat") }
    assertThrows<IllegalStateException>("3") { preprocessAndPrepare("TR: (Plant, Heat)") }
  }
}