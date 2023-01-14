package dev.martianzoo.tfm.pets

import com.github.h0tk3y.betterParse.combinators.map
import dev.martianzoo.tfm.data.ClassDeclaration
import dev.martianzoo.tfm.pets.ClassDeclarationParser.tokenize
import dev.martianzoo.tfm.pets.ClassDeclarationParser.topLevelDeclsGroup
import dev.martianzoo.tfm.pets.ast.Action
import dev.martianzoo.tfm.pets.ast.Effect
import dev.martianzoo.tfm.pets.ast.Instruction
import dev.martianzoo.tfm.pets.ast.PetNode
import dev.martianzoo.tfm.pets.ast.Script
import dev.martianzoo.tfm.pets.ast.TypeExpression
import dev.martianzoo.util.Debug
import kotlin.reflect.KClass

object Parsing {

  /**
   * Parses the PETS element in [elementSource], expecting a construct of type [P], and returning
   * the parsed [P]. [P] can only be one of the major elemental types like [Effect], [Action],
   * [Instruction], [TypeExpression], etc.
   */
  inline fun <reified P : PetNode> parsePets(elementSource: String): P =
      parsePets(P::class, elementSource)

  /** Non-reified version of `parse(source)`. */
  fun <P : PetNode> parsePets(expectedType: KClass<P>, source: String): P =
      ElementParsers.parsePets(expectedType, source)

  // For java
  fun <P : PetNode> parsePets(expectedType: Class<P>, source: String) =
      ElementParsers.parsePets(expectedType.kotlin, source)

  fun parseScript(scriptSource: String): Script {
    val scriptLines = try {
      val tokens = ElementParsers.tokenize(stripLineComments(scriptSource))
      parseRepeated(ElementParsers.scriptLine map { listOf(it) }, tokens)
    } catch (e: Exception) {
      Debug.d("Script was:\n$scriptSource")
      throw e
    }
    return Script(scriptLines)
  }

  /**
   * Parses an entire PETS class declarations source file.
   */
  fun parseClassDeclarations(declarationsSource: String): List<ClassDeclaration> {
    val tokens = tokenize(stripLineComments(declarationsSource))
    return parseRepeated(topLevelDeclsGroup, tokens)
  }

  val lineCommentRegex = Regex(""" *(//[^\n]*)*\n""")
  private fun stripLineComments(text: String) = lineCommentRegex.replace(text, "\n")
}
