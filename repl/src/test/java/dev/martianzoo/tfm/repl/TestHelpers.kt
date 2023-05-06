package dev.martianzoo.tfm.repl

import com.google.common.truth.Truth.assertThat
import dev.martianzoo.tfm.engine.PlayerSession
import dev.martianzoo.util.HashMultiset

object TestHelpers {
  fun PlayerSession.assertCounts(vararg pairs: Pair<Int, String>) =
      assertThat(pairs.map { count(it.second) })
          .containsExactlyElementsIn(pairs.map { it.first })
          .inOrder()

  /** Action just means "queue empty -> do anything -> queue empty again" */
  fun <T: Any> PlayerSession.action(startInstruction: String, block: Thing.() -> T?): T? {
    require(agent.tasks().none()) { agent.tasks() }
    val cp = game.checkpoint()
    initiate(startInstruction) // try task then try to drain

    val thing = Thing(this)
    val result = try {
      thing.block()
    } catch (e: Exception) {
      game.rollBack(cp)
      throw e
    }
    if (result == null) {
      game.rollBack(cp)
    } else {
      require(agent.tasks().none()) { agent.tasks() }
    }
    return result
  }

  class Thing(val session: PlayerSession) {
    fun doFirstTask(instr: String) { session.doFirstTask(instr) }
    fun tryMatchingTask(instr: String) { session.tryMatchingTask(instr) }
    fun tasks() = session.agent.tasks().values
    fun rollItBack() = null
    fun assertCounts(vararg pairs: Pair<Int, String>) = session.assertCounts(*pairs)

    val next = HashMultiset<String>()

    // TODO this stops checking when a value goes to zero...
    fun assertChanges(vararg pairs: Pair<Int, String>) {
      pairs.forEach { (delta, element) -> next.setCount(element, next.count(element) + delta) }
      val keys = next.entries.map { it.key }.union(pairs.map { it.second })
      assertThat(keys.map(session::count))
          .containsExactlyElementsIn(keys.map(next::count))
          .inOrder()
    }
  }
}
