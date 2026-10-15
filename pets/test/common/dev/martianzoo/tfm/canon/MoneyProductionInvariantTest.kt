package dev.martianzoo.tfm.canon

import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.ast.Instruction
import dev.martianzoo.pets.ast.InstructionTree
import kotlin.test.Test
import kotlin.test.assertTrue

internal class MoneyProductionInvariantTest {
  @Test
  internal fun bareMoneyProductionDoesNotRepresentPrintedProduction() {
    assertAdjusted("Plant / PROD[MC]", expected = false)
  }

  @Test
  internal fun anEnclosingProductionTransformAlsoChangesTheMultiplier() {
    assertAdjusted("PROD[Plant / MC]", expected = false)
  }

  @Test
  internal fun scalingCorrectedProductionPreservesTheCorrection() {
    assertAdjusted("Plant / 2 (PROD[MC] - ProdOffset<Class<MC>>)", expected = true)
  }

  @Test
  internal fun explicitMoneyProductionAlsoNeedsTheOffset() {
    assertAdjusted("Plant / Production<Class<MC>>", expected = false)
  }

  @Test
  internal fun subtractingTheMatchingOffsetGivesPrintedProduction() {
    assertAdjusted("Plant / (PROD[MC] - ProdOffset<Class<MC>>)", expected = true)
  }

  @Test
  internal fun offsetInsideTheProductionTransformIsAlsoRecognized() {
    assertAdjusted("Plant / PROD[MC - ProdOffset<Class<MC>>]", expected = true)
  }

  @Test
  internal fun mentioningAnOffsetWithoutSubtractingItIsInsufficient() {
    assertAdjusted("Plant / (Production<Class<MC>> OR ProdOffset<Class<MC>>)", expected = false)
  }

  @Test
  internal fun anotherResourcesOffsetDoesNotCorrectMoneyProduction() {
    assertAdjusted("Plant / (PROD[MC] - ProdOffset<Class<Steel>>)", expected = false)
  }

  @Test
  internal fun anotherOwnersOffsetDoesNotCorrectMoneyProduction() {
    assertAdjusted(
        "Plant / (Production<Class<MC>, Player> - ProdOffset<Class<MC>, SoloOpponent>)",
        expected = false,
    )
  }

  @Test
  internal fun energyProductionNeedsNoMoneyOffset() {
    assertAdjusted("Plant / PROD[Energy]", expected = true)
  }

  @Test
  internal fun broadProductionCountsCanIncludeMoney() {
    assertAdjusted("Plant / Production<Class<StandardResource>>", expected = false)
  }

  @Test
  internal fun industrialComplexAdjustsTheTargetBeforeSubtractingStoredProduction() {
    assertAdjusted(
        "PROD[EACH Class<@StandardResource> { @StandardResource / (Class<@StandardResource> OR QuickStartVariant OR ProdOffset<Class<@StandardResource>>) - Production<Class<@StandardResource>> }]",
        expected = true,
    )
  }

  private fun assertAdjusted(source: String, expected: Boolean) {
    val table = Canon.classTable
    val lowered =
        table.transformDispatcher().transformInstructionTree(parse<InstructionTree>(source))
    val metrics = lowered.descendantsOfType<Instruction.Per>().map { it.metric }
    assertTrue(metrics.isNotEmpty(), "Fixture must contain a per instruction: $source")
    val offenders = metrics.flatMap { unadjustedMoneyProduction(it, table) }
    assertTrue(offenders.isEmpty() == expected, "$source; unadjusted counts: $offenders")
  }
}
