package dev.martianzoo.tfm.petaform

import com.google.common.truth.Truth.assertThat
import dev.martianzoo.tfm.petaform.Action.Cost.Spend
import dev.martianzoo.tfm.petaform.Instruction.Gain
import dev.martianzoo.tfm.petaform.Instruction.Intensity
import dev.martianzoo.tfm.petaform.Instruction.Intensity.OPTIONAL
import dev.martianzoo.tfm.petaform.PetaformParser.Components
import dev.martianzoo.tfm.petaform.PetaformParser.Components.Count
import dev.martianzoo.tfm.petaform.PetaformParser.QEs
import dev.martianzoo.tfm.petaform.PetaformParser.parse
import dev.martianzoo.tfm.petaform.PetaformParser.parseComponents
import org.junit.jupiter.api.Test

class ComponentTest {
  @Test
  fun bodyElements() {
    assertThat(parse(Components.repeatableElement, "default Foo?"))
        .isEqualTo(Gain(TypeExpression("Foo"), null, OPTIONAL))
    assertThat(parse(Components.repeatableElement, "Foo -> Bar"))
        .isEqualTo(Action(Spend(TypeExpression("Foo")), Gain(TypeExpression("Bar"))))
    assertThat(parse(Components.repeatableElement, "Foo: Bar")).isInstanceOf(Effect::class.java)
    assertThat(parse(Components.repeatableElement, "class Foo"))
        .isEqualTo(listOf(Component(TypeExpression("Foo"), complete=false)))
  }

  @Test
  fun body() {
    assertThat(parseComponents("""
          class Bar : Qux { default Foo?
            Foo -> Bar


            Foo: Bar
            class Foo

          }
        """.trim())).hasSize(2)
  }

  @Test
  fun series() {
    assertThat(parseComponents("""
        class Die {
        }
        class DieHard {
          // whatever
        }

        class Atomized

        class Generation

        """.trim()
        )
    )
  }


  @Test
  fun oneSimple() {
    val cs = parseComponents("""
        abstract class One
    """)
    assertThat(cs).containsExactly(Component(TypeExpression("One"), abstract=true))
  }

  @Test
  fun threeSimple() {
    val cs = parseComponents("""
        abstract class One
        class Two

        abstract class Three
    """)
    assertThat(cs).containsExactly(
        Component(TypeExpression("One"), abstract=true),
        Component(TypeExpression("Two"), abstract=false),
        Component(TypeExpression("Three"), abstract=true),
    )
  }

  @Test fun withSupers() {
    val cs = parseComponents("""
        class One : Two, Three
    """)
    assertThat(cs).containsExactly(
        Component(
            TypeExpression("One"),
            supertypes = setOf(
                TypeExpression("Two"), TypeExpression("Three")
            )
        )
    )
  }

  @Test fun complexExprs() {
    val cs = parseComponents("""
        class One<Two<Three>(HAS Four)> : Five(HAS 6 Seven), Eight<Nine>
    """)
    assertThat(cs).containsExactly(
        Component(
            parse("One<Two<Three>(HAS Four)>"),
            supertypes = setOf(
                parse("Five(HAS 6 Seven)"), parse("Eight<Nine>"))
        )
    )
  }

  @Test fun nested() {
    val cs = parseComponents("""
        class One { // comment
          abstract class Two
          // comment
          class Three : Four
        }
    """)
    assertThat(cs).containsExactly(
        Component(parse("One")),
        Component(parse("Two"), abstract = true, supertypes = setOf(parse("One"))),
        Component(parse("Three"), supertypes = setOf(parse("One"), parse("Four"))),
    )
  }

  @Test fun oneLiner() {
    val cs = parseComponents("""
      class One { This: That }
    """)
  }

  @Test fun nestedOneLiner() {
    val cs = parseComponents("""
      class One {
        class Two { This: That }
        class Three { This: That }
      }
    """)
  }

  @Test fun withCount1() {
    assertThat(parse(QEs.scalar, "0")).isEqualTo(0)
    assertThat(parse(QEs.scalar, "2")).isEqualTo(2)
    assertThat(parse(Components.upper, "2")).isEqualTo(2)
    assertThat(parse(Components.upper, "*")).isNull()
    assertThat(parse(Components.twoDots, "..")).isNotNull()
    assertThat(parse(Components.twoDots, " .. ")).isNotNull()
  }

  @Test fun default() {
    val instr: PetaformNode = parse(PetaformParser.Components.default, "default -Component!")
    assertThat(instr).isEqualTo(Instruction.Remove(TypeExpression("Component"), null, Intensity.MANDATORY))
  }

  @Test fun withDefaults() {
    val cs = parseComponents("""
        abstract class Component {
           default Component!
           default -Component!

           class This   // comment


           abstract class Phase { // comment
               // comment

               class End
           }
        }
    """)
  }
}
