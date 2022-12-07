package dev.martianzoo.tfm.petaform.parser

import com.google.common.collect.Multiset
import dev.martianzoo.tfm.petaform.api.PetaformException
import kotlin.math.PI
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.tan
import kotlin.random.Random
import kotlin.random.Random.Default.nextInt
import kotlin.reflect.KClass

abstract class RandomGenerator<B : Any>(val registry: Registry<B>, val scaling: (Int) -> Double) {
  abstract class Registry<B : Any> {
    val map = mutableMapOf<KClass<out B>, (RandomGenerator<B>) -> B>()

    inline fun <reified P : B> register(
        noinline creator: RandomGenerator<B>.() -> P) = register(P::class, creator)

    fun <P : B> register(type: KClass<P>, creator: RandomGenerator<B>.() -> P) {
      map[type] = creator
    }

    @Suppress("UNCHECKED_CAST")
    operator fun <N : B> get(type: KClass<N>) = map[type]!! as RandomGenerator<B>.() -> N
  }

  var depth: Int? = null

  inline fun <reified N : B> makeRandomNode() = makeRandomNode(N::class)

  fun <N : B> makeRandomNode(type: KClass<N>): N {
    depth = 0
    return recurse(type).also { depth = null  }
  }

  inline fun <reified N : B> recurse() = recurse(N::class)

  fun <N : B> recurse(type: KClass<N>): N {
    val d = depth!!
    depth = d + 1
    //println("$depth ${type.simpleName}")
    if (depth!! > 100) error("")
    while (true) {
      return try {
        registry[type].invoke(this)
      } catch (ignore: PetaformException) { // TODO
        continue
      }.also {
        //println("$depth $it")
        depth = depth!! - 1
        require(depth == d)
      }
    }
  }

  // Helpers

  fun nextInt(limit: Int): Int {
    val d = Random.Default.nextDouble()
    val x = scaling(depth!!)
    require(x in -1.0..1.0)

    val power: Double = tan((x + 1.0) * PI / 4)
    return min(((1 - d.pow(power)) * limit).toInt(), limit - 1)
  }

  inline fun <reified E : Enum<E>> randomEnum() = choose(*enumValues<E>())

  inline fun <reified P : B> listOfSize(size: Int): List<P> =
      mutableListOf<P>().also {
        while (it.size < size) {
          it.add(recurse())
        }
      }

  inline fun <reified P : B> setOfSize(size: Int): Set<P> =
      mutableSetOf<P>().also {
        while (it.size < size) {
          it.add(recurse())
        }
      }

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
    val sum =  weightToChoice.map { it.first }.sum()
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
    val sum =  weightToChoiceSupplier.map { it.first }.sum()
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
