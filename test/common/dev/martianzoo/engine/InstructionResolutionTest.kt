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

internal class InstructionResolutionTest {
  private val game: World = setUpGame(canonicalPremise())
  private val instructor: Instructor =
      Instructor(
          game.reader,
          Limiter(game.classTable, game.components),
          game.classTable,
      )

  init {
    game.tfm(PLAYER1).sneak("Plant, 10 ProjectCard, PROD[-1 MC]")
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

  private fun preprocessAndResolve(unresolved: String): InstructionTree {
    val preprocessed = preprocess(game.vocabulary.canonicalize(parse<InstructionTree>(unresolved)))
    return instructor.resolve(
        preprocessed as? Instruction ?: throw abstractInstruction(preprocessed)
    )
  }

  private fun checkResolution(unresolved: String, expected: String?) {
    val resolved = preprocessAndResolve(unresolved)
    resolved.toString() shouldBe expected
  }

  @Test
  internal fun testResolveChange() {
    checkResolution("Ok", "Ok")
    checkResolution("2 Plant", "2 Plant<Player1>!")
    checkResolution("2 Plant.", "2 Plant<Player1>!")
    checkResolution("2 Plant?", "2 Plant<Player1>?")
    checkResolution("-Plant", "-Plant<Player1>!")
    checkResolution("-9 Plant.", "-Plant<Player1>!")
    checkResolution("55 OxygenStep.", "14 OxygenStep!")
    checkResolution("-4 Heat.", "Ok")
    checkResolution("-4 Heat?", "Ok")
    checkResolution("-CardFront.", "Ok")
    checkResolution("Heat FROM Plant.", "Heat<Player1> FROM Plant<Player1>!")
    checkResolution("9 Heat FROM Plant?", "Heat<Player1> FROM Plant<Player1>?")
    checkResolution("Plant FROM Heat.", "Ok")
    checkResolution("Plant FROM Heat?", "Ok")
    checkResolution("3 Microbe.", "Ok")
    checkResolution("3 Microbe?", "Ok")
    checkResolution("-3 Microbe.", "Ok")
    checkResolution("-3 Microbe?", "Ok")
    shouldThrow<DependencyException> { preprocessAndResolve("Microbe<Ants>.") }
    shouldThrow<DependencyException> { preprocessAndResolve("3 Microbe!") }
    shouldThrow<LimitsException> { preprocessAndResolve("-3 Microbe!") }
    shouldThrow<LimitsException> { preprocessAndResolve("15 OxygenStep!") }
    shouldThrow<LimitsException> { preprocessAndResolve("-2 Plant") }
    shouldThrow<LimitsException> { preprocessAndResolve("Plant FROM Heat") }
    shouldThrow<LimitsException> { preprocessAndResolve("2 Heat FROM Plant") }
    shouldThrow<LimitsException> { preprocessAndResolve("2 Plant<Player2> FROM Plant<Player1>") }
    checkResolution(
        "OxygenStep FROM TerraformRating!",
        "OxygenStep FROM TerraformRating<Player1>!",
    )
    shouldThrow<ExpressionException> { preprocessAndResolve("2 OxygenStep FROM TerraformRating!") }
  }

  @Test
  internal fun testResolvePer() {
    checkResolution("Plant / TerraformRating", "20 Plant<Player1>!")
    checkResolution("Plant / 3 TerraformRating", "6 Plant<Player1>!")
    checkResolution("Plant / 3 TerraformRating MAX 2", "2 Plant<Player1>!")
    checkResolution("Plant / Steel", "Ok")
    checkResolution("Plant / 21 TerraformRating", "Ok")
    checkResolution("-Plant. / TR", "-Plant<Player1>!")
    checkResolution("-Plant? / TR", "-Plant<Player1>?")
  }

  @Test
  internal fun testResolveGated() {
    checkResolution("10 TR: Plant", "Plant<Player1>!")
    checkResolution("10 TR: Plant / TerraformRating", "20 Plant<Player1>!")
    // TODO I'm nervous about the <Anyone> disappearing
    checkResolution("10 TR: Plant<Anyone> / TerraformRating", "20 Plant!")
    checkResolution(
        "10 TR: Titanium OR TerraformRating",
        "Titanium<Player1>! OR TerraformRating<Player1>!",
    )
    shouldThrow<RequirementException> { preprocessAndResolve("30 TR: Plant") }
  }

  @Test
  internal fun testResolveOr() {
    checkResolution(
        "15 OxygenStep! OR -2 Plant OR Plant FROM Heat " +
            "OR Ok OR 2 Heat FROM Plant OR 2 Plant<Player2> FROM Plant<Player1> OR (30 TR: Plant)",
        "Ok",
    )
    checkResolution(
        "15 OxygenStep! OR -2 Plant OR Plant FROM Heat OR (TR: 8 Steel) OR " +
            "2 Heat FROM Plant OR 2 Plant<Player2> FROM Plant<Player1> OR (30 TR: Plant)",
        "8 Steel<Player1>!",
    )

    checkResolution(
        "15 OxygenStep! OR -2 Plant OR Plant FROM Heat OR -Plant. / TR OR 8 Steel OR " +
            "2 Heat FROM Plant OR 2 Plant<Player2> FROM Plant<Player1> OR (30 TR: Plant)",
        "-Plant<Player1>! OR 8 Steel<Player1>!",
    )

    checkResolution("PROD[Plant OR (3 PlantTag: 4 Plant)]", "Production<Player1, Class<Plant>>!")
    checkResolution(
        "Steel / 2 ProjectCard OR -Titanium? OR (Plant: 5 Steel) OR Ok OR 5 Steel",
        "5 Steel<Player1>! OR Ok",
    )
    shouldThrow<NotNowException> {
      preprocessAndResolve(
          "15 OxygenStep! OR -2 Plant OR Plant FROM Heat OR 2 Heat FROM Plant " +
              "OR 2 Plant<Player2> FROM Plant<Player1> OR (30 TR: Plant)",
      )
    }
  }

  @Test
  internal fun `an unavailable choice preserves requirement failure when every option is gated`() {
    val failure =
        shouldThrow<RequirementException> {
          preprocessAndResolve("(30 TR: Plant) OR (15 OxygenStep: Steel)")
        }

    failure.message!!.contains("30 TerraformRating") shouldBe true
    failure.message!!.contains("15 OxygenStep") shouldBe true
  }

  @Test
  internal fun testResolveGroups() {
    shouldThrow<AbstractException> { preprocessAndResolve("Plant, Heat") }
    shouldThrow<AbstractException> { preprocessAndResolve("(TR: Plant), Heat") }
    checkResolution("TR: (Plant, Heat)", "Plant<Player1>!, Heat<Player1>!")
  }
}
