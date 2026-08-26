package dev.martianzoo.engine

import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.PetTransformer.Companion.chain
import dev.martianzoo.pets.Transforming.replaceOwnerWith
import dev.martianzoo.pets.api.Exceptions.AbstractException
import dev.martianzoo.pets.api.Exceptions.DependencyException
import dev.martianzoo.pets.api.Exceptions.ExpressionException
import dev.martianzoo.pets.api.Exceptions.LimitsException
import dev.martianzoo.pets.api.Exceptions.NotNowException
import dev.martianzoo.pets.api.Exceptions.RequirementException
import dev.martianzoo.pets.api.Exceptions.abstractInstruction
import dev.martianzoo.pets.ast.Instruction
import dev.martianzoo.pets.ast.InstructionTree
import dev.martianzoo.pets.data.Player.Companion.PLAYER1
import dev.martianzoo.tfm.engine.*
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class PrepareTest {
  private val game: World = setUpGame(canonicalPremise())
  private val instructor: Instructor =
      Instructor(
          game.reader,
          Limiter(game.classTable, game.components),
          game.classTable,
      )

  init {
    game.tfm(PLAYER1).godMode().sneak("Plant, 10 ProjectCard, PROD[-1 MC]")
  }

  private fun preprocess(instr: InstructionTree): InstructionTree {
    val xer =
        chain(
            Transformers(game.classTable).transformMarkedSyntax(),
            Transformers(game.classTable).insertDefaults(),
            replaceOwnerWith(PLAYER1),
        )
    return xer.transformInstructionTree(instr)
  }

  private fun preprocessAndPrepare(unprepared: String): InstructionTree {
    val preprocessed = preprocess(game.vocabulary.canonicalize(parse<InstructionTree>(unprepared)))
    return instructor.prepare(
        preprocessed as? Instruction ?: throw abstractInstruction(preprocessed)
    )
  }

  private fun checkPrepare(unprepared: String, expected: String?) {
    val prepared = preprocessAndPrepare(unprepared)
    prepared.toString() shouldBe expected
  }

  @Test
  internal fun testPrepareChange() {
    checkPrepare("Ok", "Ok")
    checkPrepare("2 Plant", "2 Plant<Player1>!")
    checkPrepare("2 Plant.", "2 Plant<Player1>!")
    checkPrepare("2 Plant?", "2 Plant<Player1>?")
    checkPrepare("-Plant", "-Plant<Player1>!")
    checkPrepare("-9 Plant.", "-Plant<Player1>!")
    checkPrepare("55 OxygenStep.", "14 OxygenStep!")
    checkPrepare("-4 Heat.", "Ok")
    checkPrepare("-4 Heat?", "Ok")
    checkPrepare("-CardFront.", "Ok")
    checkPrepare("Heat FROM Plant.", "Heat<Player1> FROM Plant<Player1>!")
    checkPrepare("9 Heat FROM Plant?", "Heat<Player1> FROM Plant<Player1>?")
    checkPrepare("Plant FROM Heat.", "Ok")
    checkPrepare("Plant FROM Heat?", "Ok")
    checkPrepare("3 Microbe.", "Ok")
    checkPrepare("3 Microbe?", "Ok")
    checkPrepare("-3 Microbe.", "Ok")
    checkPrepare("-3 Microbe?", "Ok")
    shouldThrow<DependencyException> { preprocessAndPrepare("Microbe<Ants>.") }
    shouldThrow<DependencyException> { preprocessAndPrepare("3 Microbe!") }
    shouldThrow<LimitsException> { preprocessAndPrepare("-3 Microbe!") }
    shouldThrow<LimitsException> { preprocessAndPrepare("15 OxygenStep!") }
    shouldThrow<LimitsException> { preprocessAndPrepare("-2 Plant") }
    shouldThrow<LimitsException> { preprocessAndPrepare("Plant FROM Heat") }
    shouldThrow<LimitsException> { preprocessAndPrepare("2 Heat FROM Plant") }
    shouldThrow<LimitsException> { preprocessAndPrepare("2 Plant<Player2> FROM Plant<Player1>") }
    checkPrepare(
        "OxygenStep FROM TerraformRating!",
        "OxygenStep FROM TerraformRating<Player1>!",
    )
    shouldThrow<ExpressionException> { preprocessAndPrepare("2 OxygenStep FROM TerraformRating!") }
  }

  @Test
  internal fun testPreparePer() {
    checkPrepare("Plant / TerraformRating", "20 Plant<Player1>!")
    checkPrepare("Plant / 3 TerraformRating", "6 Plant<Player1>!")
    checkPrepare("Plant / 3 TerraformRating MAX 2", "2 Plant<Player1>!")
    checkPrepare("Plant / Steel", "Ok")
    checkPrepare("Plant / 21 TerraformRating", "Ok")
    checkPrepare("-Plant. / TR", "-Plant<Player1>!")
    checkPrepare("-Plant? / TR", "-Plant<Player1>?")
  }

  @Test
  internal fun testPrepareGated() {
    checkPrepare("10 TR: Plant", "Plant<Player1>!")
    checkPrepare("10 TR: Plant / TerraformRating", "20 Plant<Player1>!")
    // TODO I'm nervous about the <Anyone> disappearing
    checkPrepare("10 TR: Plant<Anyone> / TerraformRating", "20 Plant!")
    checkPrepare(
        "10 TR: Titanium OR TerraformRating",
        "Titanium<Player1>! OR TerraformRating<Player1>!",
    )
    shouldThrow<RequirementException> { preprocessAndPrepare("30 TR: Plant") }
  }

  @Test
  internal fun testPrepareOr() {
    checkPrepare(
        "15 OxygenStep! OR -2 Plant OR Plant FROM Heat " +
            "OR Ok OR 2 Heat FROM Plant OR 2 Plant<Player2> FROM Plant<Player1> OR (30 TR: Plant)",
        "Ok",
    )
    checkPrepare(
        "15 OxygenStep! OR -2 Plant OR Plant FROM Heat OR (TR: 8 Steel) OR " +
            "2 Heat FROM Plant OR 2 Plant<Player2> FROM Plant<Player1> OR (30 TR: Plant)",
        "8 Steel<Player1>!",
    )

    checkPrepare(
        "15 OxygenStep! OR -2 Plant OR Plant FROM Heat OR -Plant. / TR OR 8 Steel OR " +
            "2 Heat FROM Plant OR 2 Plant<Player2> FROM Plant<Player1> OR (30 TR: Plant)",
        "-Plant<Player1>! OR 8 Steel<Player1>!",
    )

    checkPrepare("PROD[Plant OR (3 PlantTag: 4 Plant)]", "Production<Player1, Class<Plant>>!")
    checkPrepare(
        "Steel / 2 ProjectCard OR -Titanium? OR (Plant: 5 Steel) OR Ok OR 5 Steel",
        "5 Steel<Player1>! OR Ok",
    )
    shouldThrow<NotNowException> {
      preprocessAndPrepare(
          "15 OxygenStep! OR -2 Plant OR Plant FROM Heat OR 2 Heat FROM Plant " +
              "OR 2 Plant<Player2> FROM Plant<Player1> OR (30 TR: Plant)",
      )
    }
  }

  @Test
  internal fun `an unavailable choice preserves requirement failure when every option is gated`() {
    val failure =
        shouldThrow<RequirementException> {
          preprocessAndPrepare("(30 TR: Plant) OR (15 OxygenStep: Steel)")
        }

    failure.message!!.contains("30 TerraformRating") shouldBe true
    failure.message!!.contains("15 OxygenStep") shouldBe true
  }

  @Test
  internal fun testPrepareGroups() {
    shouldThrow<AbstractException> { preprocessAndPrepare("Plant, Heat") }
    shouldThrow<AbstractException> { preprocessAndPrepare("(TR: Plant), Heat") }
    checkPrepare("TR: (Plant, Heat)", "Plant<Player1>!, Heat<Player1>!")
  }
}
