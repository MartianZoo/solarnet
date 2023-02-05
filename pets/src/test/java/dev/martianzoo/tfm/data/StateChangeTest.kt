package dev.martianzoo.tfm.data

import dev.martianzoo.tfm.data.ChangeLogEntry.StateChange
import dev.martianzoo.tfm.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.testlib.assertFails
import org.junit.jupiter.api.Test

private class StateChangeTest {

  @Test
  fun bad() {
    val valid = StateChange(42, cn("Foo").type, cn("Bar").type)

    assertFails { valid.copy(count = 0) }
    assertFails { valid.copy(gaining = null, removing = null) }
    assertFails { valid.copy(gaining = cn("Same").type, removing = cn("Same").type) }
  }
}
