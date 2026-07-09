package dev.martianzoo.tfm.data

import dev.martianzoo.data.GameEvent.ChangeEvent.StateChange
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.testlib.assertFails
import kotlin.test.Test

internal class StateChangeTest {

  @Test
  fun bad() {
    val valid = StateChange(42, cn("Foo").expression, cn("Bar").expression)

    assertFails { valid.copy(count = 0) }
    assertFails { valid.copy(gaining = null, removing = null) }
    assertFails { valid.copy(gaining = cn("Same").expression, removing = cn("Same").expression) }
  }
}
