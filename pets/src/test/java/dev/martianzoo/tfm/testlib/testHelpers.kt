package dev.martianzoo.tfm.testlib

import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.util.HashMultiset
import dev.martianzoo.util.Multiset
import org.junit.jupiter.api.assertThrows

public fun te(s: String): Expression = parse(s)

public fun assertFails(message: String, shouldFail: () -> Unit) =
    assertThrows<RuntimeException>(message, shouldFail)

public fun assertFails(shouldFail: () -> Unit) = assertThrows<RuntimeException>(shouldFail)

public fun <T> multiset(vararg pairs: Pair<Int, T>): Multiset<T> {
  val result = HashMultiset<T>()
  pairs.forEach { (count, element) -> result.add(element, count) }
  return result
}
