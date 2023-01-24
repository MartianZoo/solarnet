package dev.martianzoo.tfm.data

import dev.martianzoo.tfm.data.StateChange.Cause
import dev.martianzoo.tfm.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.testlib.assertFails
import org.junit.jupiter.api.Test

private class StateChangeTest {

  @Test
  fun bad() {
    val valid = StateChange(42, cn("Foo").ptype, cn("Bar").ptype, Cause(cn("Qux").ptype, 3))

    assertFails { valid.copy(count = 0) }
    assertFails { valid.copy(gaining = null, removing = null) }
    assertFails {
      valid.copy(gaining = cn("Same").ptype, removing = cn("Same").ptype)
    }
    assertFails { valid.cause!!.copy(trigger = -1) }
  }
}
