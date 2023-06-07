package dev.martianzoo.tfm.engine

import com.google.common.truth.Truth.assertThat
import dev.martianzoo.tfm.data.Player.Companion.players
import dev.martianzoo.tfm.data.TaskResult
import dev.martianzoo.tfm.engine.Gameplay.Companion.parse
import dev.martianzoo.tfm.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.pets.ast.Instruction
import dev.martianzoo.tfm.pets.ast.Instruction.Gain
import dev.martianzoo.tfm.pets.ast.Instruction.Remove

object TestHelpers {
  fun TfmGameplay.assertCounts(vararg pairs: Pair<Int, String>) =
      assertThat(pairs.map { this.count(it.second) })
          .containsExactlyElementsIn(pairs.map { it.first })
          .inOrder()

  fun Gameplay.assertCounts(vararg pairs: Pair<Int, String>) =
      assertThat(pairs.map { count(it.second) })
          .containsExactlyElementsIn(pairs.map { it.first })
          .inOrder()

  fun TfmGameplay.assertProds(vararg pairs: Pair<Int, String>) =
      assertThat(pairs.map { production(cn(it.second)) })
          .containsExactlyElementsIn(pairs.map { it.first })
          .inOrder()

  fun TfmGameplay.nextGeneration(vararg cardsBought: Int) {
    phase("Production")
    phase("Research") {
      for ((cards, player) in cardsBought.zip(players(5))) {
        asPlayer(player).doTask(if (cards > 0) "$cards BuyCard" else "Ok")
      }
    }
    phase("Action")
  }

  fun TaskResult.expect(p: TfmGameplay, x: String) {
    val exp =
        Instruction.split(p.parse<Instruction>(x)).instructions.map {
          when (it) {
            is Gain -> "+${it.copy(intensity = null)}"
            is Remove -> "${it.copy(intensity = null)}"
            else -> error("not allowed")
          }
        }
    val act = net().map { "$it" }
    assertThat(act).containsAtLeastElementsIn(exp)
  }
}
