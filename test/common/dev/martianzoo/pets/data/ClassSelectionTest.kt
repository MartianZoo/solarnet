package dev.martianzoo.pets.data

import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.api.Exceptions.PetException
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.types.PremiseClassTable
import dev.martianzoo.pets.types.testCatalog
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.Test

internal class ClassSelectionTest {
  @Test
  internal fun moduleConditionsMustCountOneSimpleClass() {
    val catalog = testCatalog("CLASS One\nCLASS Two\nCLASS Holder<One>")
    val declarations = PremiseClassTable(catalog.classTable, emptySet())

    shouldThrow<PetException> {
      ClassSelection(cn("One"), requirement = parse("=1 (One OR Two)"))
          .appliesTo(emptySet(), declarations)
    }
    shouldThrow<PetException> {
      ClassSelection(cn("One"), requirement = parse("Holder<One>"))
          .appliesTo(emptySet(), declarations)
    }
  }
}
