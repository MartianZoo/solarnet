package dev.martianzoo.pets.api

import dev.martianzoo.pets.ast.ClassName.Companion.cn
import kotlin.test.Test
import kotlin.test.assertEquals

internal class CustomClassTest {
  @Test
  internal fun classNameDefaultsToKotlinSimpleName() {
    assertEquals(cn("AutomaticallyNamed"), AutomaticallyNamed.className)
  }

  private object AutomaticallyNamed : CustomClass()
}
