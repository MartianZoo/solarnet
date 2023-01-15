package dev.martianzoo.tfm.pets

import com.github.h0tk3y.betterParse.combinators.map
import com.github.h0tk3y.betterParse.parser.ParseException
import com.github.h0tk3y.betterParse.parser.Parser
import com.github.h0tk3y.betterParse.parser.parseToEnd
import dev.martianzoo.tfm.data.ClassDeclaration
import dev.martianzoo.tfm.pets.ClassDeclarationParsers.oneLineDecl
import dev.martianzoo.tfm.pets.ClassDeclarationParsers.topLevelGroup
import dev.martianzoo.tfm.pets.ClassDeclarationParsers.tokenize
import dev.martianzoo.tfm.pets.ast.Action
import dev.martianzoo.tfm.pets.ast.Action.Cost
import dev.martianzoo.tfm.pets.ast.Effect
import dev.martianzoo.tfm.pets.ast.Effect.Trigger
import dev.martianzoo.tfm.pets.ast.Instruction
import dev.martianzoo.tfm.pets.ast.PetNode
import dev.martianzoo.tfm.pets.ast.ScalarAndType
import dev.martianzoo.tfm.pets.ast.Requirement
import dev.martianzoo.tfm.pets.ast.Script
import dev.martianzoo.tfm.pets.ast.TypeExpression
import dev.martianzoo.tfm.pets.ast.TypeExpression.TypeParsers
import dev.martianzoo.util.Debug
import dev.martianzoo.util.ParserGroup
import kotlin.reflect.KClass
import kotlin.reflect.cast

object Parsing {

  private val parserGroup by lazy {
    val pgb = ParserGroup.Builder<PetNode>()
    pgb.publish(TypeParsers.genericType)
    pgb.publish(TypeParsers.typeExpression)
    pgb.publish(ScalarAndType.parser())
    pgb.publish(Requirement.parser())
    pgb.publish(Instruction.parser())
    pgb.publish(Cost.parser())
    pgb.publish(Action.parser())
    pgb.publish(Trigger.parser())
    pgb.publish(Effect.parser())
    pgb.publish(Script.parser())
    pgb.finish()
  }

  /**
   * Parses the PETS element in [elementSource], expecting a construct of type [P], and returning
   * the parsed [P]. [P] can only be one of the major elemental types like [Effect], [Action],
   * [Instruction], [TypeExpression], etc.
   */
  inline fun <reified P : PetNode> parsePets(elementSource: String): P =
      parsePets(P::class, elementSource)

  /** Non-reified version of `parse(source)`. */
  fun <P : PetNode> parsePets(expectedType: KClass<P>, source: String): P {
    val pet = try {
      parserGroup.parse(expectedType, tokenize(source))
    } catch (e: ParseException) {
      throw IllegalArgumentException("""
          Expecting ${expectedType.simpleName} ...
          Input was:
          $source
      """.trimIndent(), e)
    }
    return expectedType.cast(pet)
  }

  // For java
  fun <P : PetNode> parsePets(expectedType: Class<P>, source: String) =
      parsePets(expectedType.kotlin, source)

  fun parseScript(scriptSource: String): Script {
    val scriptLines = try {
      val tokens = tokenize(stripLineComments(scriptSource))
      parseRepeated(Script.parser() map { listOf(it) }, tokens)
    } catch (e: Exception) {
      Debug.d("Script was:\n$scriptSource")
      throw e
    }
    return Script(scriptLines)
  }

  fun parseOneLineClassDeclaration(declarationSource: String): ClassDeclaration {
    return parse(oneLineDecl, declarationSource)
  }
  /**
   * Parses an entire PETS class declarations source file.
   */
  fun parseClassDeclarations(declarationsSource: String): List<ClassDeclaration> {
    val tokens = tokenize(stripLineComments(declarationsSource))
    return parseRepeated(topLevelGroup, tokens)
  }

  fun <T> parse(parser: Parser<T>, source: String): T {
    val tokens = tokenize(source)
    Debug.d(tokens.filterNot { it.type.ignored }.joinToString(" ") {
      it.type.name?.replace("\n", "\\n") ?: "NULL"
    })
    return parser.parseToEnd(tokens)
  }

  private val lineCommentRegex = Regex(""" *(//[^\n]*)*\n""")

  private fun stripLineComments(text: String) = lineCommentRegex.replace(text, "\n")
}
