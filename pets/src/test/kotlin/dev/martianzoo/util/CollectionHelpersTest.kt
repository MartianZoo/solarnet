package dev.martianzoo.util

import com.google.common.collect.Multiset
import kotlin.math.PI
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.tan
import kotlin.random.Random

class CollectionHelpersTest {

  class RanGen(val scale: Double) {

    fun nextInt(limit: Int): Int {
      val d = Random.Default.nextDouble()

      val power: Double = tan((scale + 1.0) * PI / 4)
      return min(((1 - d.pow(power)) * limit).toInt(), limit - 1)
    }

    inline fun <reified E : Enum<E>> randomEnum() = choose(*enumValues<E>())

    fun <T : Any?> choose(vararg choices: T): T = choices[nextInt(choices.size)]
    fun <T : Any?> choose(choices: List<T>): T = choices[nextInt(choices.size)]

    fun <T : Any?> choose(choices: Multiset<T>) = getNth(choices, nextInt(choices.size))

    fun <T : Any?> getNth(choices: Multiset<T>, index: Int): T {
      var skip = index
      for (wc in choices.entrySet()) {
        skip -= wc.count
        if (skip < 0) {
          return wc.element
        }
      }
      error("")
    }

    fun <T : Any?> choose(vararg weightToChoice: Pair<Int, T>): T {
      val sum = weightToChoice.map { it.first }.sum()
      var skip = nextInt(sum)
      for (wc in weightToChoice) {
        skip -= wc.first
        if (skip < 0) {
          return wc.second
        }
      }
      error("")
    }

    fun <T : Any?> chooseS(vararg weightToChoiceSupplier: Pair<Int, () -> T>): T {
      val sum = weightToChoiceSupplier.map { it.first }.sum()
      var skip = nextInt(sum)
      for (wc in weightToChoiceSupplier) {
        skip -= wc.first
        if (skip < 0) {
          return wc.second()
        }
      }
      error("")
    }
  }
}
