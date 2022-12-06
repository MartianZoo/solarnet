package dev.martianzoo.tfm.petaform.parser

import com.google.common.truth.Truth.assertThat
import dev.martianzoo.tfm.petaform.api.Action
import dev.martianzoo.tfm.petaform.api.Action.Cost.Spend
import dev.martianzoo.tfm.petaform.api.ComponentClassDeclaration
import dev.martianzoo.tfm.petaform.api.ComponentDecls
import dev.martianzoo.tfm.petaform.api.Effect
import dev.martianzoo.tfm.petaform.api.Expression
import dev.martianzoo.tfm.petaform.api.Instruction
import dev.martianzoo.tfm.petaform.api.Instruction.Gain
import dev.martianzoo.tfm.petaform.api.Instruction.Intensity
import dev.martianzoo.tfm.petaform.api.Instruction.Intensity.OPTIONAL
import dev.martianzoo.tfm.petaform.api.PetaformNode
import dev.martianzoo.tfm.petaform.parser.PetaformParser.ComponentClasses
import dev.martianzoo.tfm.petaform.parser.PetaformParser.parse
import org.junit.jupiter.api.Test

class ComponentDeclarationsTest {
  @Test
  fun bodyElements() {
    assertThat(parse(ComponentClasses.bodyElement, "default Foo?"))
        .isEqualTo(Gain(Expression("Foo"), 1, OPTIONAL))
    assertThat(parse(ComponentClasses.bodyElement, "Foo -> Bar"))
        .isEqualTo(Action(Spend(Expression("Foo")), Gain(Expression("Bar"))))
    assertThat(parse(ComponentClasses.bodyElement, "Foo: Bar")).isInstanceOf(Effect::class.java)
    assertThat(parse(ComponentClasses.bodyElement, "component Foo"))
        .isEqualTo(listOf(ComponentClassDeclaration(Expression("Foo"), complete=false)))
  }

  @Test
  fun body() {
    assertThat(
        parse(
            ComponentClasses.oneComponent,
            """
          component Bar : Qux { default Foo?
            Foo -> Bar


            Foo: Bar
            component Foo

          }
        """.trim()
        )
    ).hasSize(2)
  }

  @Test
  fun series() {
    assertThat(
        parse(
            ComponentClasses.components,
            """
        component Die {
        }
        component DieHard {
          // whatever
        }

        component Atomized

        component Generation

        """.trim()
        )
    )
  }


  @Test
  fun oneSimple() {
    val cs = parse<ComponentDecls>("""
        abstract One
    """)
    assertThat(cs.decls).containsExactly(ComponentClassDeclaration(Expression("One"), abstract=true))
  }

  @Test
  fun threeSimple() {
    val cs = parse<ComponentDecls>("""
        abstract One
        component Two

        abstract Three
    """)
    assertThat(cs.decls).containsExactly(
        ComponentClassDeclaration(Expression("One"), abstract=true),
        ComponentClassDeclaration(Expression("Two"), abstract=false),
        ComponentClassDeclaration(Expression("Three"), abstract=true),
    )
  }

  @Test fun withSupers() {
    val cs = parse<ComponentDecls>("""
        component One : Two, Three
    """)
    assertThat(cs.decls).containsExactly(
        ComponentClassDeclaration(
            Expression("One"),
            supertypes = setOf(
                Expression("Two"), Expression("Three"))
        )
    )
  }

  @Test fun complexExprs() {
    val cs = parse<ComponentDecls>("""
        component One<Two<Three>(HAS Four)> : Five(HAS 6 Seven), Eight<Nine>
    """)
    assertThat(cs.decls).containsExactly(
        ComponentClassDeclaration(
            parse("One<Two<Three>(HAS Four)>"),
            supertypes = setOf(
                parse("Five(HAS 6 Seven)"), parse("Eight<Nine>"))
        )
    )
  }

  @Test fun nested() {
    val cs = parse<ComponentDecls>("""
        component One { // comment
          abstract Two
          // comment
          component Three : Four
        }
    """)
    assertThat(cs.decls).containsExactly(
        ComponentClassDeclaration(parse("One")),
        ComponentClassDeclaration(parse("Two"), abstract = true, supertypes = setOf(parse("One"))),
        ComponentClassDeclaration(parse("Three"), supertypes = setOf(parse("One"), parse("Four"))),
    )
  }

  @Test fun default() {
    val instr: PetaformNode = parse(PetaformParser.ComponentClasses.default, "default -Component!")
    assertThat(instr).isEqualTo(Instruction.Remove(Expression("Component"), 1,  Intensity.MANDATORY))
  }

  @Test fun withDefaults() {
    val cs = parse<ComponentDecls>("""
        abstract Component {
           default Component!
           default -Component!

           component This   // comment


           abstract Phase { // comment
               // comment

               component End
           }
        }
    """)
    //  assertThat(cs.decls).containsExactly(
  }
}
