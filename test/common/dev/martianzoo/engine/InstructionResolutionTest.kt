package dev.martianzoo.engine

import dev.martianzoo.agenttestsupport.testTfm
import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.PetElaborator
import dev.martianzoo.pets.api.Exceptions.AbstractException
import dev.martianzoo.pets.api.Exceptions.DependencyException
import dev.martianzoo.pets.api.Exceptions.ExpressionException
import dev.martianzoo.pets.api.Exceptions.LimitsException
import dev.martianzoo.pets.api.Exceptions.NotNowException
import dev.martianzoo.pets.api.Exceptions.RequirementException
import dev.martianzoo.pets.api.Exceptions.abstractInstruction
import dev.martianzoo.pets.ast.Instruction
import dev.martianzoo.pets.ast.InstructionTree
import dev.martianzoo.testsupport.PLAYER1
import dev.martianzoo.tfm.engine.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class InstructionResolutionTest {
  private val game: World = setUpGame(canonicalPremise())
  private val gameWorld = (game as WholeWorld).gameWorld
  private val elaborator = PetElaborator(game.classTable)
  private val effector = Effector(elaborator) { game.reader }
  private val instructor: Instructor =
      Instructor(
          game.reader,
          Limiter(game.classTable, gameWorld),
          Changer(game.reader, gameWorld, effector),
          effector,
          game.classTable,
          elaborator,
          CustomInstructionRuntime(game.reader.catalog, elaborator),
      )

  init {
    game.testTfm(PLAYER1).sneak("Plant, 10 ProjectCard, PROD[-1 MC]")
  }

  private fun preprocess(instr: InstructionTree): InstructionTree {
    return elaborator.elaborateInput(instr, PLAYER1)
  }

  private fun preprocessAndResolve(unresolved: String): InstructionTree {
    val preprocessed = preprocess(parse(unresolved))
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
  internal fun `resolution retains an occurrence omitted by the compact resolved Type`() {
    checkResolution(
        "CityTile<MarsArea AS ThatArea> FROM GreeneryTile<ThatArea>",
        "CityTile<Player1, MarsArea AS ThatArea> FROM GreeneryTile<Player1, ThatArea>!",
    )
  }

  @Test
  internal fun `resolution retains compact FROM until both projections are concrete`() {
    checkResolution(
        "Production<Player1 FROM Player2, Class<StandardResource>>?",
        "Production<Player1 FROM Player2, Class<StandardResource>>?",
    )
  }

  @Test
  internal fun testResolvePer() {
    checkResolution("Plant / TerraformRating", "20 Plant<Player1>!")
    checkResolution("Plant / 3 TerraformRating", "6 Plant<Player1>!")
    checkResolution("Plant / 3 TerraformRating MAX 2", "2 Plant<Player1>!")
    checkResolution("Plant / Steel", "Ok")
    checkResolution("Plant / 21 TerraformRating", "Ok")
    checkResolution("-Plant. / TerraformRating", "-Plant<Player1>!")
    checkResolution("-Plant? / TerraformRating", "-Plant<Player1>?")
  }

  @Test
  internal fun testResolveGated() {
    checkResolution("10 TerraformRating: Plant", "Plant<Player1>!")
    checkResolution("10 TerraformRating: Plant / TerraformRating", "20 Plant<Player1>!")
    // TODO I'm nervous about the <Anyone> disappearing
    checkResolution("10 TerraformRating: Plant<Anyone> / TerraformRating", "20 Plant!")
    checkResolution(
        "10 TerraformRating: Titanium OR TerraformRating",
        "Titanium<Player1>! OR TerraformRating<Player1>!",
    )
    shouldThrow<RequirementException> { preprocessAndResolve("30 TerraformRating: Plant") }
  }

  @Test
  internal fun testFanoutGivesEachSelectionItsOwnBranch() {
    // The selected player, not the surrounding one, owns everything inside the braces.
    checkResolution("EACH Player { Plant }", "Plant<Player1>!, Plant<Player2>!")
    checkResolution(
        "EACH Player { 2 Plant, Heat }",
        "2 Plant<Player1>!, Heat<Player1>!, 2 Plant<Player2>!, Heat<Player2>!",
    )
  }

  @Test
  internal fun testFanoutRefinementChoosesWhichSelectionsTakePart() {
    // A selector refinement is evaluated against each candidate, so only Player1, who was given a
    // Plant, takes part. A gate inside the body behaves like any other gate and is not a filter.
    checkResolution("EACH Player(HAS 1 Plant) { Heat }", "Heat<Player1>!")
    checkResolution("EACH Player(HAS 99 Plant) { Heat }", "Ok")
    shouldThrow<RequirementException> { preprocessAndResolve("EACH Player { 99 Plant: Heat }") }
  }

  @Test
  internal fun testOnlyAnOwnerSelectionSuppliesTheOwnerOfItsBranch() {
    checkResolution("EACH Player { Plant }", "Plant<Player1>!, Plant<Player2>!")
    checkResolution(
        "EACH ProjectCard<Anyone> AS ThatCard { -ThatCard, Plant }",
        List(10) { "-ProjectCard<Player1, Hand>!, Plant<Player1>!" }.joinToString(", "),
    )
    // A selector reads its enclosing context, so `Owner` there is one component, not every owner.
    shouldThrow<ExpressionException> { preprocessAndResolve("EACH Owner { Plant }") }
    // ...and it concretizes dependencies in a selector rooted in the enclosing owner's context.
    shouldThrow<ExpressionException> { preprocessAndResolve("EACH ProjectCard<Owner> { Plant }") }
  }

  @Test
  internal fun testFanoutRangesOverOccurrences() {
    // Player1 holds ten indistinguishable ProjectCards, and each copy contributes one branch.
    checkResolution(
        "EACH ProjectCard<Anyone> AS ThatCard { -ThatCard }",
        List(10) { "-ProjectCard<Player1, Hand>!" }.joinToString(", "),
    )
    checkResolution(
        "EACH ProjectCard<Anyone> { StandardResource }",
        List(10) { "StandardResource<Player1>!" }.joinToString(", "),
    )
  }

  @Test
  internal fun testFanoutOverNothingIsNoOp() {
    checkResolution("EACH CardFront<Anyone> AS ThatCard { -ThatCard }", "Ok")
  }

  @Test
  internal fun testFanoutSelectorMayOnlySupplyRepetition() {
    checkResolution("EACH Player { OxygenStep }", "OxygenStep!, OxygenStep!")
    checkResolution("EACH CardFront<Anyone> { OxygenStep }", "Ok")
  }

  @Test
  internal fun testResolveOr() {
    checkResolution(
        "-2 Plant OR Plant FROM Heat " +
            "OR Ok OR 2 Heat FROM Plant OR 2 Plant<Player2> FROM Plant<Player1> OR (30 TerraformRating: Plant)",
        "Ok",
    )
    checkResolution(
        "-2 Plant OR Plant FROM Heat OR (TerraformRating: 8 Steel) OR " +
            "2 Heat FROM Plant OR 2 Plant<Player2> FROM Plant<Player1> OR (30 TerraformRating: Plant)",
        "8 Steel<Player1>!",
    )

    checkResolution(
        "-2 Plant OR Plant FROM Heat OR -Plant. / TerraformRating OR 8 Steel OR " +
            "2 Heat FROM Plant OR 2 Plant<Player2> FROM Plant<Player1> OR (30 TerraformRating: Plant)",
        "-Plant<Player1>! OR 8 Steel<Player1>!",
    )

    checkResolution("PROD[Plant OR (3 PlantTag: 4 Plant)]", "Production<Player1, Class<Plant>>!")
    checkResolution(
        "Steel / 2 ProjectCard OR -Titanium? OR (Plant: 5 Steel) OR Ok OR 5 Steel",
        "5 Steel<Player1>! OR Ok",
    )
    shouldThrow<NotNowException> {
      preprocessAndResolve(
          "-2 Plant OR Plant FROM Heat OR 2 Heat FROM Plant " +
              "OR 2 Plant<Player2> FROM Plant<Player1> OR (30 TerraformRating: Plant)",
      )
    }
  }

  @Test
  internal fun `an unavailable choice preserves requirement failure when every option is gated`() {
    val failure =
        shouldThrow<RequirementException> {
          preprocessAndResolve("(30 TerraformRating: Plant) OR (15 OxygenStep: Steel)")
        }

    failure.message!!.contains("30 TerraformRating") shouldBe true
    failure.message!!.contains("15 OxygenStep") shouldBe true
  }

  @Test
  internal fun testResolveGroups() {
    shouldThrow<AbstractException> { preprocessAndResolve("Plant, Heat") }
    shouldThrow<AbstractException> { preprocessAndResolve("(TerraformRating: Plant), Heat") }
    checkResolution("TerraformRating: (Plant, Heat)", "Plant<Player1>!, Heat<Player1>!")
  }
}
