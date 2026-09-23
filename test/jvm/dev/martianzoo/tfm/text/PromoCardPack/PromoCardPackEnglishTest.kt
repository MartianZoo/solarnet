package dev.martianzoo.tfm.text

import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.ast.Effect
import dev.martianzoo.pets.ast.InstructionTree
import dev.martianzoo.tfm.canon.Canon
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class PromoCardPackEnglishTest {
  private val english = English(Canon.classTable, TerraformingMarsDescribers.descriptions)

  @Test
  internal fun describesPromoResources() {
    english.describe(parse<InstructionTree>("2 Disease<This>")) shouldBe
        "Add 2 disease resources to this card."
    english.describe(parse<InstructionTree>("Graphene<This>")) shouldBe
        "Add 1 graphene resource to this card."
    english.describe(parse<InstructionTree>("Hydroelectric<This>")) shouldBe
        "Add 1 hydroelectric resource to this card."
  }

  @Test
  internal fun describesPromoProceduresAndMarker() {
    english.describe(parse<InstructionTree>("NomadsMarker")) shouldBe "Place a nomads marker."
    english.describe(parse<InstructionTree>("CopyPrelude")) shouldBe
        "Copy your other Prelude's direct effect."
    english.describe(parse<InstructionTree>("ChooseOceanArea")) shouldBe "Choose an ocean area."
  }

  @Test
  internal fun describesInteractionRecords() {
    english.describe(
        parse<Effect>(
            "MyResourceWasRemoved<Anyone> OR MyProductionWasDecreased<Anyone>: " +
                "3 MC<Anyone FROM Owner>."
        )
    ) shouldBe
        "When any player has their resources removed by another player, or has their production " +
            "decreased by another player, pay 3 M€ to that player, or as much as possible."
  }
}
