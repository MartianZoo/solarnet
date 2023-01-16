package dev.martianzoo.tfm.pets.ast

import dev.martianzoo.tfm.data.StateChange
import dev.martianzoo.tfm.data.StateChange.Cause
import dev.martianzoo.tfm.pets.ast.ClassName.Companion.cn
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

private class StateChangeTest {

  @Test
  fun bad() {
    val valid = StateChange(5, 42, cn("Foo").type, cn("Bar").type, Cause(cn("Qux").type, 3))

    assertThrows<RuntimeException> { valid.copy(ordinal = -1) }
    assertThrows<RuntimeException> { valid.copy(ordinal = 2) }
    assertThrows<RuntimeException> { valid.copy(ordinal = 3) }
    assertThrows<RuntimeException> { valid.copy(count = 0) }
    assertThrows<RuntimeException> { valid.copy(gaining = null, removing = null) }
    assertThrows<RuntimeException> { valid.copy(gaining = cn("Same").type, removing = cn("Same").type) }
    assertThrows<RuntimeException> { valid.copy(cause = valid.cause!!.copy(change = 5)) }
    assertThrows<RuntimeException> { valid.cause!!.copy(change = 0) }
  }
}
