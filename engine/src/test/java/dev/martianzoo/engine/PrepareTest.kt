package dev.martianzoo.engine

import dev.martianzoo.api.Exceptions.LimitsException
import dev.martianzoo.api.Exceptions.NotNowException
import dev.martianzoo.api.Exceptions.RequirementException
import dev.martianzoo.data.Player.Companion.PLAYER1
import dev.martianzoo.engine.Engine.newGame
import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.PetTransformer.Companion.chain
import dev.martianzoo.pets.Transforming.replaceOwnerWith
import dev.martianzoo.pets.ast.Instruction
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.engine.Prod.deprodify
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class PrepareTest {
  val game: Game = newGame(Canon.SIMPLE_GAME)
  val instructor: Instructor =
      Instructor(game.reader, Limiter(game.classes, game.components), null, null, game.classes)

  init {
    game.tfm(PLAYER1).godMode().sneak("Plant, 10 ProjectCard, PROD[-1]")
  }

  fun preprocess(instr: Instruction): Instruction {
    val xer =
        chain(
            deprodify(Transformers(game.classes).classes),
            Transformers(game.classes).insertDefaults(),
            replaceOwnerWith(PLAYER1),
        )
    return xer.transform(instr)
  }

  fun preprocessAndPrepare(unprepared: String): Instruction {
    return instructor.prepare(preprocess(parse(unprepared)))
  }

  fun checkPrepare(unprepared: String, expected: String?) {
    val prepared = preprocessAndPrepare(unprepared)
    prepared.toString() shouldBe expected
  }

  @Test
  fun testPrepareChange() {
    checkPrepare("Ok", "Ok")
    checkPrepare("2 Plant", "2 Plant<Player1>!")
    checkPrepare("2 Plant.", "2 Plant<Player1>!")
    checkPrepare("2 Plant?", "2 Plant<Player1>?")
    checkPrepare("-Plant", "-Plant<Player1>!")
    checkPrepare("-9 Plant.", "-Plant<Player1>!")
    // checkPrepare("55 OxygenStep.", "14 OxygenStep!")
    checkPrepare("-4 Heat.", "Ok")
    checkPrepare("-4 Heat?", "Ok")
    checkPrepare("Heat FROM Plant.", "Heat<Player1> FROM Plant<Player1>!")
    checkPrepare("9 Heat FROM Plant?", "Heat<Player1> FROM Plant<Player1>?")
    checkPrepare("Plant FROM Heat.", "Ok")
    checkPrepare("Plant FROM Heat?", "Ok")
    // shouldThrow<LimitsException> { preprocessAndPrepare("15 OxygenStep!") }
    shouldThrow<LimitsException> { preprocessAndPrepare("-2 Plant") }
    shouldThrow<LimitsException> { preprocessAndPrepare("Plant FROM Heat") }
    shouldThrow<LimitsException> { preprocessAndPrepare("2 Heat FROM Plant") }
    shouldThrow<LimitsException> { preprocessAndPrepare("2 Plant<Player2 FROM Player1>") }
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
    shouldThrow<RequirementException> { preprocessAndPrepare("30 TR: Plant") }
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
    shouldThrow<NotNowException> {
      preprocessAndPrepare(
          "15 OxygenStep! OR -2 Plant OR Plant FROM Heat OR 2 Heat FROM Plant " +
              "OR 2 Plant<Player2 FROM Player1> OR 30 TR: Plant",
      )
    }
  }

  @Test
  fun testPrepareMulti() {
    shouldThrow<IllegalStateException> { preprocessAndPrepare("Plant, Heat") }
    shouldThrow<IllegalStateException> { preprocessAndPrepare("(TR: Plant), Heat") }
    shouldThrow<IllegalStateException> { preprocessAndPrepare("TR: (Plant, Heat)") }
  }
}
