package dev.martianzoo.pets.testlib

import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.util.HashMultiset
import dev.martianzoo.pets.util.Multiset
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.withClue

internal fun te(s: String): Expression = parse(s)

internal fun assertFails(message: String, shouldFail: () -> Unit) =
    withClue(message) { assertFails(shouldFail) }

internal fun assertFails(shouldFail: () -> Unit) = shouldThrow<RuntimeException>(shouldFail)

internal fun <T> multiset(vararg pairs: Pair<Int, T>): Multiset<T> {
  val result = HashMultiset<T>()
  pairs.forEach { (count, element) -> result.add(element, count) }
  return result
}
