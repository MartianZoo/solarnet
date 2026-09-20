package dev.martianzoo.state

import dev.martianzoo.engine.testClassTable
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test
import kotlin.test.assertFails

internal class ComponentChangeTest {

  @Test
  internal fun componentTypeMustBeConcrete() {
    val abstractType = testClassTable("ABSTRACT CLASS Choice").getClass(cn("Choice")).baseType

    shouldThrow<IllegalArgumentException> { abstractType.toComponent() }.message shouldBe
        "component type must be concrete: `Choice`"
  }

  @Test
  internal fun changeKindsRejectInvalidValues() {
    val table = testClassTable("CLASS Foo, Bar, Same")
    val foo = table.getClass(cn("Foo")).baseType.toComponent()
    val bar = table.getClass(cn("Bar")).baseType.toComponent()
    val same = table.getClass(cn("Same")).baseType.toComponent()
    val gain = ComponentChange.Gain(42, foo)

    assertFails { gain.copy(count = 0) }
    val transmutation = ComponentChange.Transmute(42, foo, bar)
    transmutation.gaining shouldBe foo
    transmutation.removing shouldBe bar
    transmutation.toString() shouldBe "+42 Foo FROM Bar"
    val selfTransmutation = ComponentChange.Transmute(42, same, same)
    selfTransmutation.gaining shouldBe same
    selfTransmutation.removing shouldBe same
    selfTransmutation.reversed() shouldBe selfTransmutation
    selfTransmutation.toString() shouldBe "+42 Same FROM Same"
  }
}
