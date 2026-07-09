package dev.martianzoo.tfm.testlib

import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.util.HashMultiset
import dev.martianzoo.util.Multiset
import io.kotest.assertions.withClue
import io.kotest.assertions.throwables.shouldThrow

public fun te(s: String): Expression = parse(s)

public fun assertFails(message: String, shouldFail: () -> Unit) =
    withClue(message) { assertFails(shouldFail) }

public fun assertFails(shouldFail: () -> Unit) = shouldThrow<RuntimeException>(shouldFail)

public fun <T> multiset(vararg pairs: Pair<Int, T>): Multiset<T> {
  val result = HashMultiset<T>()
  pairs.forEach { (count, element) -> result.add(element, count) }
  return result
}
