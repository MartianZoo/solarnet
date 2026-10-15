package dev.martianzoo.pets.types

import dev.martianzoo.pets.Parsing.parseClasses
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class PremiseClassTableTest {
  @Test
  internal fun deepPremiseHierarchyDoesNotExhaustTheStack() {
    val declarations =
        (0..2000).joinToString("\n") { index ->
          if (index == 0) "ABSTRACT CLASS Level0"
          else "ABSTRACT CLASS Level$index : Level${index - 1}"
        }
    val table = PremiseClassTable(loadTypes("CLASS Unrelated"), parseClasses(declarations).toSet())
    assertFalse(table.isSubtypeOf(cn("Level2000"), cn("Unrelated")))
    assertTrue(table.isSubtypeOf(cn("Level2000"), cn("Level0")))
  }

  @Test
  internal fun sharedAncestorsAreNotRevisitedForEveryInheritancePath() {
    val declarations =
        (0..100).joinToString("\n") { index ->
          val parents = if (index == 0) "" else " : Left${index - 1}, Right${index - 1}"
          "ABSTRACT CLASS Left$index$parents\nABSTRACT CLASS Right$index$parents"
        }
    val table = PremiseClassTable(loadTypes("CLASS Unrelated"), parseClasses(declarations).toSet())
    assertFalse(table.isSubtypeOf(cn("Left100"), cn("Unrelated")))
    assertTrue(table.isSubtypeOf(cn("Left100"), cn("Right0")))
  }
}
