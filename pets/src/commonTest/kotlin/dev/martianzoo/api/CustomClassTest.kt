package dev.martianzoo.api

import dev.martianzoo.pets.ast.ClassName.Companion.cn
import kotlin.test.Test
import kotlin.test.assertEquals

internal class CustomClassTest {
  @Test
  fun classNameDefaultsToKotlinSimpleName() {
    assertEquals(cn("AutomaticallyNamed"), AutomaticallyNamed.className)
  }

  private object AutomaticallyNamed : CustomClass()
}
