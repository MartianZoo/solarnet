package dev.martianzoo.pets.data

import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.types.testCatalog
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.Test

internal class CatalogTest {
  @Test
  internal fun missingClassDeclarationIsRejected() {
    val catalog = testCatalog("CLASS Known")

    shouldThrow<IllegalArgumentException> { catalog.classDeclaration(cn("Missing")) }
  }
}
