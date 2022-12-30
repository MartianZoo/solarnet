package dev.martianzoo.tfm.pets.ast

import dev.martianzoo.tfm.pets.ast.StateChange.Cause
import dev.martianzoo.tfm.pets.ast.TypeExpression.Companion.te
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class StateChangeTest {

  @Test
  fun bad() {
    val valid = StateChange(5, 42, te("Foo"), te("Bar"), Cause(te("Qux"), 3))

    assertThrows<RuntimeException> { valid.copy(ordinal = -1) }
    assertThrows<RuntimeException> { valid.copy(ordinal = 2) }
    assertThrows<RuntimeException> { valid.copy(ordinal = 3) }
    assertThrows<RuntimeException> { valid.copy(count = 0) }
    assertThrows<RuntimeException> { valid.copy(gaining = null, removing = null) }
    assertThrows<RuntimeException> { valid.copy(gaining = te("Same"), removing = te("Same")) }
    assertThrows<RuntimeException> { valid.copy(cause = valid.cause!!.copy(change = 5)) }
    assertThrows<RuntimeException> { valid.cause!!.copy(change = 0) }
  }
}
