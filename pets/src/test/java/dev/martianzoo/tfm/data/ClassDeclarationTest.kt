package dev.martianzoo.tfm.data

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import dev.martianzoo.api.SystemClasses.THIS
import dev.martianzoo.data.ClassDeclaration
import dev.martianzoo.pets.HasClassName.Companion.classNames
import dev.martianzoo.pets.Parsing
import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.Transforming.actionToEffect
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Effect
import dev.martianzoo.pets.ast.Instruction.Intensity
import dev.martianzoo.pets.ast.Requirement
import dev.martianzoo.pets.ast.ScaledExpression.Companion.scaledEx
import dev.martianzoo.tfm.testlib.te
import kotlin.test.Test

internal class ClassDeclarationTest {
  @Test
  fun testExample() {
    val declText =
        """
          ABSTRACT CLASS Foo<Bar> : Baz<Qux> {
            HAS =1 This
            DEFAULT +Foo<Abc>?
            DEFAULT Foo<Xyz>

            This: DoStuff
            Steel -> 5
          }
        """
            .trimIndent()

    val decl: ClassDeclaration = Parsing.parseClasses(declText).single()

    val foo = cn("Foo")
    val dep = cn("Bar").expression
    val sup = te("Baz<Qux>")

    val inv: Requirement = Requirement.Exact(scaledEx(1, THIS.expression))
    val eff: Effect = parse<Effect>("This: DoStuff")
    val act = actionToEffect(parse("Steel -> 5"), 1)
    val gain = cn("Abc").expression
    val univ = cn("Xyz").expression

    decl.shortName shouldBe foo
    decl.className shouldBe foo
    decl.abstract shouldBe true
    decl.dependencies.shouldContainExactlyInAnyOrder(dep)
    decl.supertypes.shouldContainExactlyInAnyOrder(sup)
    decl.invariants.shouldContainExactlyInAnyOrder(inv)
    decl.effects.shouldContainExactlyInAnyOrder(eff, act)
    decl.defaultsDeclaration.gainOnly.specs.shouldContainExactlyInAnyOrder(gain)
    decl.defaultsDeclaration.universal.specs.shouldContainExactlyInAnyOrder(univ)
    decl.defaultsDeclaration.gainOnly.intensity shouldBe Intensity.OPTIONAL
    decl.extraNodes.shouldBeEmpty()

    decl.supertypes.classNames().shouldContainExactlyInAnyOrder(cn("Baz"))

    decl.allNodes.shouldContainExactlyInAnyOrder(foo, dep, sup, inv, eff, act, gain, univ)
  }
}
