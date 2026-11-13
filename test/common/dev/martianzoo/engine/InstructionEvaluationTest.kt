package dev.martianzoo.engine

import dev.martianzoo.agenttestsupport.testAgent
import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.PetElaborator
import dev.martianzoo.pets.api.Exceptions.DependencyException
import dev.martianzoo.pets.api.Exceptions.ExpressionException
import dev.martianzoo.pets.api.Exceptions.GameplayException
import dev.martianzoo.pets.api.Exceptions.LimitsException
import dev.martianzoo.pets.api.Exceptions.RequirementException
import dev.martianzoo.pets.ast.Instruction
import dev.martianzoo.pets.ast.InstructionTree
import dev.martianzoo.testsupport.PLAYER1
import dev.martianzoo.tfm.engine.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class InstructionEvaluationTest {
  private val game: World = setUpGame(canonicalPremise())
  private val gameWorld = (game as WholeWorld).gameWorld
  private val elaborator = PetElaborator(game.classTable)
  private val effector =
      Effector(elaborator, CustomInstructionRuntime(gameWorld, elaborator)) {
        game.reader
      }
  private val instructor: Instructor =
      Instructor(
          game.reader,
          Limiter(game.classTable, gameWorld),
          Changer(game.reader, gameWorld, effector),
          effector,
          game.classTable,
          elaborator,
          TimelineImpl(
              gameWorld,
              Changer(game.reader, gameWorld, effector),
              Transactions(gameWorld, {}) {},
          ),
      )

  init {
    game.testAgent(PLAYER1).exMachina("Plant, 10 ProjectCard, PROD[-1 MC]")
  }

  private fun preprocess(instr: InstructionTree): InstructionTree {
    return elaborator.elaborateInput(instr, PLAYER1)
  }

  private fun preprocessAndEvaluate(input: String): InstructionTree {
    val preprocessed = preprocess(parse(input))
    return instructor.evaluate(preprocessed as Instruction)
  }

  private fun checkEvaluation(input: String, expected: String?) {
    val evaluated = preprocessAndEvaluate(input)
    evaluated.toString() shouldBe expected
  }

  @Test
  internal fun testResolveChange() {
    checkEvaluation("Ok", "Ok")
    checkEvaluation("2 Plant", "2! Plant<Player1>")
    checkEvaluation("2* Plant", "2! Plant<Player1>")
    checkEvaluation("2? Plant", "2? Plant<Player1>")
    checkEvaluation("-Plant", "-Plant<Player1>!")
    checkEvaluation("-9* Plant", "-Plant<Player1>!")
    checkEvaluation("-4* Heat", "-0! Heat<Player1>")
    checkEvaluation("-4? Heat", "-0? Heat<Player1>")
    checkEvaluation("-CardFront*", "Ok")
    checkEvaluation("Heat FROM Plant*", "Heat<Player1> FROM Plant<Player1>!")
    checkEvaluation("9? Heat FROM Plant", "Heat<Player1> FROM Plant<Player1>?")
    checkEvaluation("Plant FROM Heat*", "0! Plant<Player1> FROM Heat<Player1>")
    checkEvaluation("Plant FROM Heat?", "0? Plant<Player1> FROM Heat<Player1>")
    checkEvaluation("3* Microbe", "Ok")
    checkEvaluation("3? Microbe", "Ok")
    checkEvaluation("-3* Microbe", "Ok")
    checkEvaluation("-3? Microbe", "Ok")
    shouldThrow<DependencyException> { preprocessAndEvaluate("Microbe<Ants>*") }
    shouldThrow<DependencyException> { preprocessAndEvaluate("3! Microbe") }
    shouldThrow<LimitsException> { preprocessAndEvaluate("-3! Microbe") }
    shouldThrow<LimitsException> { preprocessAndEvaluate("-2 Plant") }
    shouldThrow<LimitsException> { preprocessAndEvaluate("Plant FROM Heat") }
    shouldThrow<LimitsException> { preprocessAndEvaluate("2 Heat FROM Plant") }
    shouldThrow<LimitsException> { preprocessAndEvaluate("2 Plant<Player2> FROM Plant<Player1>") }
    checkEvaluation(
        "OxygenStep FROM TerraformRating!",
        "OxygenStep FROM TerraformRating<Player1>!",
    )
    shouldThrow<ExpressionException> {
      preprocessAndEvaluate("2! OxygenStep FROM TerraformRating")
    }
  }

  @Test
  internal fun `evaluation retains an occurrence omitted by the compact resolved Type`() {
    checkEvaluation(
        "CityTile<@MarsArea> FROM GreeneryTile<@MarsArea>",
        "CityTile<Player1, @MarsArea> FROM GreeneryTile<Player1, @MarsArea>!",
    )
  }

  @Test
  internal fun `evaluation retains compact FROM until both projections are concrete`() {
    checkEvaluation(
        "Production<Player1 FROM Player2, Class<StandardResource>>?",
        "Production<Player1 FROM Player2, Class<StandardResource>>?",
    )
  }

  @Test
  internal fun reflexiveTransmutationIsRejectedRegardlessOfSourceCount() {
    shouldThrow<ExpressionException> { preprocessAndEvaluate("Plant FROM Plant") }
    shouldThrow<ExpressionException> { preprocessAndEvaluate("Heat FROM Heat") }
  }

  @Test
  internal fun reflexiveTransmutationIsRejectedRegardlessOfQuantifier() {
    shouldThrow<ExpressionException> { preprocessAndEvaluate("2! Plant FROM Plant") }
    shouldThrow<ExpressionException> { preprocessAndEvaluate("2? Plant FROM Plant") }
    shouldThrow<ExpressionException> { preprocessAndEvaluate("2* Plant FROM Plant") }
  }

  @Test
  internal fun testResolvePer() {
    checkEvaluation("Plant / TerraformRating", "20! Plant<Player1>")
    checkEvaluation("Plant / 3 TerraformRating", "6! Plant<Player1>")
    checkEvaluation("Plant / 3 TerraformRating MAX 2", "2! Plant<Player1>")
    checkEvaluation("Plant / Steel", "0! Plant<Player1>")
    checkEvaluation("Plant / 21 TerraformRating", "0! Plant<Player1>")
    checkEvaluation("-Plant* / TerraformRating", "-Plant<Player1>!")
    checkEvaluation("-Plant? / TerraformRating", "-Plant<Player1>?")
  }

  @Test
  internal fun testResolveGated() {
    checkEvaluation("10 TerraformRating: Plant", "Plant<Player1>!")
    checkEvaluation("10 TerraformRating: Plant / TerraformRating", "20! Plant<Player1>")
    // TODO I'm nervous about the <Anyone> disappearing
    checkEvaluation("10 TerraformRating: Plant<Anyone> / TerraformRating", "20! Plant")
    checkEvaluation(
        "10 TerraformRating: Titanium OR TerraformRating",
        "Titanium<Player1>! OR TerraformRating<Player1>!",
    )
    shouldThrow<RequirementException> { preprocessAndEvaluate("30 TerraformRating: Plant") }
  }

  @Test
  internal fun testFanoutGivesEachSelectionItsOwnBranch() {
    // The selected player, not the surrounding one, owns everything inside the braces.
    checkEvaluation("EACH Me@Player { Plant }", "Plant<Player1>!, Plant<Player2>!")
    checkEvaluation(
        "EACH Me@Player { 2 Plant, Heat }",
        "2! Plant<Player1>, Heat<Player1>!, 2! Plant<Player2>, Heat<Player2>!",
    )
  }

  @Test
  internal fun testFanoutRefinementChoosesWhichSelectionsTakePart() {
    // A selector refinement is evaluated against each candidate, so only Player1, who was given a
    // Plant, takes part. A gate inside the body behaves like any other gate and is not a filter.
    checkEvaluation("EACH Player(HAS 1 Plant) { Heat }", "Heat<Player1>!")
    checkEvaluation("EACH Player(HAS 99 Plant) { Heat }", "Ok")
    shouldThrow<RequirementException> { preprocessAndEvaluate("EACH Player { 99 Plant: Heat }") }
  }

  @Test
  internal fun `a selector concretizes a full transmutation before it evaluates`() {
    checkEvaluation(
        "EACH Selected@Player(HAS Plant) { " +
            "Heat<Selected@Player> FROM Plant<Selected@Player> }",
        "Heat<Player1> FROM Plant<Player1>!",
    )
  }

  @Test
  internal fun `only a named Me selection rebinds ownership in its branch`() {
    checkEvaluation("EACH Me@Player { Plant }", "Plant<Player1>!, Plant<Player2>!")
    checkEvaluation(
        "EACH @ProjectCard<Anyone> { -@ProjectCard, Plant }",
        List(10) { "-ProjectCard<Player1, Hand>!, Plant<Player1>!" }.joinToString(", "),
    )
    // An unnamed selector leaves the enclosing owner in force.
    checkEvaluation("EACH Owner { Plant }", "Plant<Player1>!, Plant<Player1>!")
    checkEvaluation(
        "EACH ProjectCard<Anyone> { Plant }",
        List(10) { "Plant<Player1>!" }.joinToString(", "),
    )
  }

  @Test
  internal fun testFanoutRangesOverOccurrences() {
    // Player1 holds ten indistinguishable ProjectCards, and each copy contributes one branch.
    checkEvaluation(
        "EACH @ProjectCard<Anyone> { -@ProjectCard }",
        List(10) { "-ProjectCard<Player1, Hand>!" }.joinToString(", "),
    )
    checkEvaluation(
        "EACH ProjectCard<Anyone> { StandardResource }",
        List(10) { "StandardResource<Player1>!" }.joinToString(", "),
    )
  }

  @Test
  internal fun testFanoutOverNothingIsNoOp() {
    checkEvaluation("EACH @CardFront<Anyone> { -@CardFront }", "Ok")
  }

  @Test
  internal fun testFanoutSelectorMayOnlySupplyRepetition() {
    checkEvaluation("EACH Player { OxygenStep }", "OxygenStep!, OxygenStep!")
    checkEvaluation("EACH CardFront<Anyone> { OxygenStep }", "Ok")
  }

  @Test
  internal fun testResolveOr() {
    checkEvaluation(
        "-2 Plant OR Plant FROM Heat " +
            "OR Ok OR 2 Heat FROM Plant OR 2 Plant<Player2> FROM Plant<Player1> OR (30 TerraformRating: Plant)",
        "Ok",
    )
    checkEvaluation(
        "-2 Plant OR Plant FROM Heat OR (TerraformRating: 8 Steel) OR " +
            "2 Heat FROM Plant OR 2 Plant<Player2> FROM Plant<Player1> OR (30 TerraformRating: Plant)",
        "8! Steel<Player1>",
    )

    checkEvaluation(
        "-2 Plant OR Plant FROM Heat OR -Plant* / TerraformRating OR 8 Steel OR " +
            "2 Heat FROM Plant OR 2 Plant<Player2> FROM Plant<Player1> OR (30 TerraformRating: Plant)",
        "-Plant<Player1>! OR 8! Steel<Player1>",
    )

    checkEvaluation("PROD[Plant OR (3 PlantTag: 4 Plant)]", "Production<Player1, Class<Plant>>!")
    checkEvaluation(
        "Steel / 2 ProjectCard OR -Titanium? OR (Plant: 5 Steel) OR Ok OR 5 Steel",
        "5! Steel<Player1> OR -0? Titanium<Player1> OR Ok",
    )
    shouldThrow<GameplayException> {
      preprocessAndEvaluate(
          "-2 Plant OR Plant FROM Heat OR 2 Heat FROM Plant " +
              "OR 2 Plant<Player2> FROM Plant<Player1> OR (30 TerraformRating: Plant)",
      )
    }
  }

  @Test
  internal fun `an unavailable choice preserves requirement failure when every option is gated`() {
    val failure =
        shouldThrow<RequirementException> {
          preprocessAndEvaluate("(30 TerraformRating: Plant) OR (15 OxygenStep: Steel)")
        }

    failure.message!!.contains("30 TerraformRating") shouldBe true
    failure.message!!.contains("15 OxygenStep") shouldBe true
  }

  @Test
  internal fun testResolveGroups() {
    checkEvaluation("TerraformRating: (Plant, Heat)", "Plant<Player1>!, Heat<Player1>!")
  }
}
