package dev.martianzoo.tfm.pets.ast

import dev.martianzoo.tfm.data.StateChange
import dev.martianzoo.tfm.data.StateChange.Cause
import dev.martianzoo.tfm.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.testlib.assertFails
import org.junit.jupiter.api.Test

private class StateChangeTest {

  @Test
  fun bad() {
    val valid = StateChange(42, cn("Foo").type, cn("Bar").type, Cause(cn("Qux").type, 3))

    assertFails { valid.copy(count = 0) }
    assertFails { valid.copy(gaining = null, removing = null) }
    assertFails {
      valid.copy(gaining = cn("Same").type, removing = cn("Same").type)
    }
    assertFails { valid.cause!!.copy(change = 0) }
  }
}
