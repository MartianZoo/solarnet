package dev.martianzoo.tfm.text

import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.ast.Effect
import dev.martianzoo.pets.ast.InstructionTree
import dev.martianzoo.pets.ast.Requirement
import dev.martianzoo.tfm.canon.Canon
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class ColoniesExpansionEnglishTest {
  private val english = English(Canon.classTable, TerraformingMarsDescribers.descriptions)

  @Test
  internal fun describesColonies() {
    english.describe(parse<InstructionTree>("Colony<ColonyTile>")) shouldBe
        "Place a colony (may be placed where you already have a colony)."
    english.describe(parse<InstructionTree>("2 Colony<>")) shouldBe "Place 2 colonies."
    english.describe(parse<Requirement>("Colony")) shouldBe "Requires that you have a colony."
    english.describe(parse<Requirement>("MAX 1 Colony")) shouldBe
        "Requires that you have 1 or fewer colonies."
  }

  @Test
  internal fun describesColonyOperations() {
    english.describe(parse<InstructionTree>("2 AdvanceColonyTracks")) shouldBe
        "Increase all colony tile tracks 2 steps."
    english.describe(parse<InstructionTree>("GainColonyBonuses")) shouldBe
        "Gain all your colony bonuses."
    english.describe(parse<InstructionTree>("ColonyTileSelection")) shouldBe "Add 1 colony tile."
    english.describe(parse<InstructionTree>("2 Camp<This>")) shouldBe
        "Add 2 camp resources to this card."
  }

  @Test
  internal fun describesTrading() {
    english.describe(parse<InstructionTree>("2 TradeFleet")) shouldBe "Gain 2 Trade Fleets."
    english.describe(parse<InstructionTree>("UseAction<TradeAction>")) shouldBe
        "Use the Trade standard action."
    english.describe(parse<Effect>("Trade<ColonyTile>: ColonyProduction<ColonyTile>?")) shouldBe
        "When you trade, you may raise that colony tile track 1 step."
  }
}
