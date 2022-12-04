package dev.martianzoo.tfm.petaform.parser

import com.github.h0tk3y.betterParse.parser.parseToEnd
import com.google.common.truth.Truth.assertThat
import dev.martianzoo.tfm.petaform.api.ComponentDecl
import dev.martianzoo.tfm.petaform.api.ComponentDecls
import dev.martianzoo.tfm.petaform.api.Expression
import dev.martianzoo.tfm.petaform.api.Instruction
import dev.martianzoo.tfm.petaform.api.Instruction.Intensity
import dev.martianzoo.tfm.petaform.api.PetaformNode
import dev.martianzoo.tfm.petaform.parser.PetaformParser.parse
import org.junit.jupiter.api.Test

class ComponentDeclarationsTest {
  @Test
  fun oneSimple() {
    val cs = parse<ComponentDecls>("""
        abstract One
    """)
    assertThat(cs.decls).containsExactly(ComponentDecl(Expression("One"), true))
  }

  @Test
  fun threeSimple() {
    val cs = parse<ComponentDecls>("""
        abstract One
        abstract Two

        abstract Three
    """)
    assertThat(cs.decls).containsExactly(
        ComponentDecl(Expression("One"), true),
        ComponentDecl(Expression("Two"), true),
        ComponentDecl(Expression("Three"), true),
    )
  }

  @Test fun withSupers() {
    val cs = parse<ComponentDecls>("""
        component One : Two, Three
    """)
    assertThat(cs.decls).containsExactly(
        ComponentDecl(Expression("One"), supertypes = setOf(
            Expression("Two"), Expression("Three")))
    )
  }

  @Test fun complexExprs() {
    val cs = parse<ComponentDecls>("""
        component One<Two<Three>(HAS Four)> : Five(HAS 6 Seven), Eight<Nine>
    """)
    assertThat(cs.decls).containsExactly(
        ComponentDecl(parse("One<Two<Three>(HAS Four)>"), supertypes = setOf(
            parse("Five(HAS 6 Seven)"), parse("Eight<Nine>")))
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
        ComponentDecl(parse("One")),
        ComponentDecl(parse("Two"), supertypes = setOf(parse("One")), abstract = true),
        ComponentDecl(parse("Three"), supertypes = setOf(parse("One"), parse("Four"))),
    )
  }

  @Test fun default() {
    val instr: PetaformNode = PetaformParser.ComponentStuff.defaultSpec.parse("default -Component!")
    assertThat(instr).isEqualTo(Instruction.Remove(Expression("Component"), 1,  Intensity.MANDATORY))

    val node: PetaformNode = PetaformParser.ComponentStuff.componentContent.parse("default -Component!")
    assertThat(node).isEqualTo(instr)
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
