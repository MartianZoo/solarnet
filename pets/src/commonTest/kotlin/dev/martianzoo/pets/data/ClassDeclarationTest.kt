package dev.martianzoo.pets.data

import dev.martianzoo.pets.HasClassName.Companion.classNames
import dev.martianzoo.pets.Parsing
import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.api.SystemClasses.THIS
import dev.martianzoo.pets.ast.Action
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Effect
import dev.martianzoo.pets.ast.Instruction.Intensity
import dev.martianzoo.pets.ast.Requirement
import dev.martianzoo.pets.ast.ScaledExpression.Companion.scaledEx
import dev.martianzoo.tfm.testlib.te
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class ClassDeclarationTest {
  @Test
  internal fun rendersAsParseablePets() {
    val source =
        """
        "A useful class"
        ABSTRACT CLASS Foo<Bar, Qux> : Baz, Eep {
          HAS =1 This
          DEFAULT Foo<Xyz>
          DEFAULT +Foo<Abc>?
          DEFAULT -Foo<Def>!
          DEFAULT Foo<Trigger>:
          row = Number
          column = 2
          This: DoStuff
        }
        """
            .trimIndent()
    val declaration = Parsing.parseClasses(source).single()

    Parsing.parseClasses(declaration.toString()).single() shouldBe declaration
    declaration.toString() shouldBe source
  }

  @Test
  internal fun rendersOneLineBodiesForDeclarationFiles() {
    val declaration =
        Parsing.parseClasses("CLASS Foo {\n  row = 1\n  column = 2\n  This: Bar\n}").single()
    val oneLine = "CLASS Foo { row = 1; column = 2; This: Bar }"

    declaration.toString(oneLine = true) shouldBe oneLine
    Parsing.parseClasses(oneLine).single() shouldBe declaration
  }

  @Test
  internal fun duplicateEffectsArePreserved() {
    val effects =
        Parsing.parseClasses(
                """
                CLASS Foo {
                  This: Bar
                  This: Bar
                }
                """
                    .trimIndent(),
            )
            .single()
            .effects

    effects.shouldContainExactly(parse<Effect>("This: Bar"), parse<Effect>("This: Bar"))
  }

  @Test
  internal fun testExample() {
    val declText =
        """
        ABSTRACT CLASS Foo<Bar> : Baz<Qux> {
          HAS =1 This
          DEFAULT +Foo<Abc>?
          DEFAULT Foo<Xyz>
          DEFAULT Foo<Trigger>:

          This: DoStuff
          Steel -> 5
        }
        """
            .trimIndent()

    val decl: ClassDeclaration = Parsing.parseClasses(declText).single()

    val foo = cn("Foo")
    val dep = cn("Bar").expression
    val sup = te("Baz<Qux>")

    val inv: Requirement = Requirement.Exact(scaledEx(THIS.expression, 1))
    val eff: Effect = parse<Effect>("This: DoStuff")
    val invoice =
        parse<Effect>(
            "UseAction<This, First>: Owed<Class<Steel>> THEN " +
                "Invoice<This, First, Class<Steel>>"
        )
    val paid = parse<Effect>("-Invoice<This, First>: 5")
    val gain = cn("Abc").expression
    val univ = cn("Xyz").expression
    val trigger = cn("Trigger").expression
    val first = cn("First")

    decl.className shouldBe foo
    decl.abstract shouldBe true
    decl.dependencies.shouldContainExactlyInAnyOrder(dep)
    decl.supertypes.shouldContainExactlyInAnyOrder(sup)
    decl.invariants.shouldContainExactlyInAnyOrder(inv)
    decl.authoredEffects.shouldContainExactly(eff)
    decl.authoredActions.shouldContainExactly(parse<Action>("Steel -> 5"))
    decl.effects.shouldContainExactlyInAnyOrder(eff, invoice, paid)
    decl.defaultsDeclaration.gainOnly.specs.shouldContainExactlyInAnyOrder(gain)
    decl.defaultsDeclaration.universal.specs.shouldContainExactlyInAnyOrder(univ)
    decl.defaultsDeclaration.triggerOnly.specs.shouldContainExactlyInAnyOrder(trigger)
    decl.defaultsDeclaration.gainOnly.intensity shouldBe Intensity.OPTIONAL
    decl.extraNodes.shouldContainExactlyInAnyOrder(first)

    decl.supertypes.classNames().shouldContainExactlyInAnyOrder(cn("Baz"))

    decl.allNodes.shouldContainExactlyInAnyOrder(
        foo,
        dep,
        sup,
        inv,
        eff,
        invoice,
        paid,
        gain,
        univ,
        trigger,
        first,
    )
  }
}
