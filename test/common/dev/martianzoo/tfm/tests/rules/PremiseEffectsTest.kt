package dev.martianzoo.tfm.tests.rules

import dev.martianzoo.catalog.GameConfig
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Effect.Trigger.IfTrigger
import dev.martianzoo.pets.types.ClassTable
import dev.martianzoo.tfm.canon.Canon
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import kotlin.test.Test
import kotlin.test.assertSame

/** Premise facts simplify effects; live conditions and authored declarations remain intact. */
internal class PremiseEffectsTest {
  @Test
  internal fun `Vitor loses its solo award effect without selecting awards or scoring places`() {
    val solo = gameTable("prelude:cards", "Player1")
    val vitor = solo.getClass(cn("Vitor"))

    solo.isInhabited(cn("Vitor")) shouldBe true
    solo.allClassNames.filter { "award" in it.toString().lowercase() }.shouldBeEmpty()
    solo.allClassNames.shouldNotContain(cn("FirstPlace"))
    solo.allClassNames.shouldNotContain(cn("SecondPlace"))
    vitor.declaration.effects.count { it.trigger is IfTrigger } shouldBe 1
    solo.effects(vitor).count { it.trigger is IfTrigger } shouldBe 0
    solo.effects(vitor).size shouldBe vitor.declaration.effects.size - 1
  }

  @Test
  internal fun `premise effects omit or unwrap a class-literal guard without changing its backing declaration`() {
    val withoutTurmoil = gameTable("AmazonisMap", "Player1", "Player2")
    val withTurmoil = gameTable("AmazonisMap, TurmoilExpansion", "Player1", "Player2")
    val area = Canon.classTable.getClass(cn("Amazonis_02_02"))

    area.declaration.effects.single().toString() shouldBe
        "Placement<This> IF Class<PartyDelegate>: PartyDelegate"
    withoutTurmoil.effects(area).shouldBeEmpty()
    withTurmoil.effects(area).single().toString() shouldBe "Placement<This>: PartyDelegate"
  }

  @Test
  internal fun `premise effects retain a dynamic milestone gate`() {
    val table = gameTable("AmazonisMap", "Player1", "Player2")
    val milestone = table.getClass(cn("Milestone"))

    table.effects(milestone).size shouldBe milestone.declaration.effects.size
    milestone.declaration.effects.zip(table.effects(milestone)).forEach { (backing, executable) ->
      assertSame(backing, executable)
    }
  }

  @Test
  internal fun `premise effects specialize the selected game mode`() {
    val solo = gameTable("", "Player1")
    val multiplayer = gameTable("", "Player1", "Player2")
    val modulesReady = Canon.classTable.getClass(cn("ModulesReady"))

    (modulesReady.declaration.effects.single().trigger is IfTrigger) shouldBe true
    solo.effects(modulesReady).shouldBeEmpty()
    (multiplayer.effects(modulesReady).single().trigger is IfTrigger) shouldBe false
  }

  @Test
  internal fun `premise effects retain a live player threshold`() {
    val twoPlayers = gameTable("", "Player1", "Player2")
    val threePlayers = gameTable("", "Player1", "Player2", "Player3")
    val measureAward = Canon.classTable.getClass(cn("MeasureAward"))
    val conditional = measureAward.declaration.effects.single { it.trigger is IfTrigger }

    measureAward.declaration.effects.count { it.trigger is IfTrigger } shouldBe 1
    twoPlayers.effects(measureAward).count { it.trigger is IfTrigger } shouldBe 1
    twoPlayers.effects(measureAward).size shouldBe measureAward.declaration.effects.size
    threePlayers.effects(measureAward).count { it.trigger is IfTrigger } shouldBe 1
    threePlayers.effects(measureAward).size shouldBe measureAward.declaration.effects.size
    assertSame(conditional, twoPlayers.effects(measureAward).single { it.trigger is IfTrigger })
    assertSame(conditional, threePlayers.effects(measureAward).single { it.trigger is IfTrigger })
  }

  @Test
  internal fun `premise effects specialize selected module presence`() {
    val withoutColonies = gameTable("CimmeriaMap", "Player1", "Player2")
    val withColonies = gameTable("CimmeriaMap, ColoniesExpansion, Callisto", "Player1", "Player2")
    val bonus = Canon.classTable.getClass(cn("CimmeriaPlacementBonus"))

    (bonus.declaration.effects.single().trigger is IfTrigger) shouldBe true
    withoutColonies.effects(bonus).shouldBeEmpty()
    (withColonies.effects(bonus).single().trigger is IfTrigger) shouldBe false
    withColonies.effects(bonus).single().instruction.toString() shouldBe "Colony<>, -5 MC"
  }

  @Test
  internal fun `premise effects remove a selected module from a mixed condition`() {
    val table = gameTable("ColoniesExpansion, Callisto", "Player1")
    val colonies = table.getClass(cn("ColoniesExpansion"))

    colonies.declaration.effects.mapNotNull { (it.trigger as? IfTrigger)?.condition }.size shouldBe
        2
    table.effects(colonies).mapNotNull {
      (it.trigger as? IfTrigger)?.condition?.toString()
    } shouldBe listOf("SelectedColonyTile")
  }

  private fun gameTable(config: String, vararg players: String): ClassTable =
      Canon.gamePremise(GameConfig(config, *players)).classTable
}
