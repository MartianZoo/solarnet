package dev.martianzoo.pets

import com.github.h0tk3y.betterParse.lexer.Token
import com.github.h0tk3y.betterParse.lexer.TokenMatchesSequence
import com.github.h0tk3y.betterParse.parser.ParseException
import com.github.h0tk3y.betterParse.parser.Parser
import com.github.h0tk3y.betterParse.parser.completionAtEnd
import com.github.h0tk3y.betterParse.parser.parseToEnd
import dev.martianzoo.api.Exceptions.NoNewClassDeclarationsException
import dev.martianzoo.api.Exceptions.PetSyntaxException
import dev.martianzoo.data.ClassDeclaration
import dev.martianzoo.pets.ClassParsing.Declarations
import dev.martianzoo.pets.PetTokenizer.TokenCache
import dev.martianzoo.pets.ast.Action
import dev.martianzoo.pets.ast.Action.Cost
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.Effect
import dev.martianzoo.pets.ast.Effect.Trigger
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.Instruction
import dev.martianzoo.pets.ast.InstructionTree
import dev.martianzoo.pets.ast.Metric
import dev.martianzoo.pets.ast.PetElement
import dev.martianzoo.pets.ast.PetNode
import dev.martianzoo.pets.ast.PropertyName
import dev.martianzoo.pets.ast.PropertyValue
import dev.martianzoo.pets.ast.Requirement
import dev.martianzoo.pets.ast.ScaledExpression
import dev.martianzoo.util.ParserGroup
import kotlin.reflect.KClass

/** Various functions for parsing [PetElement]s or [ClassDeclaration]s from text. */
public object Parsing {
  /**
   * Parses a series of Pets class declarations. The syntax is currently not documented (sorry), but
   * examples can be reviewed in `global.pets` and `player.pets`.
   */
  public fun parseClasses(declarationsSource: String): List<ClassDeclaration> {
    val declarations =
        parse(
            Declarations.declarationFile,
            declarationsSource,
            expectedTypeDesc = "Pets class declarations",
        )
    return declarations.flatMap { declaration ->
      DerivedClassLowerer(declaration.className).lowerDeclaration(declaration)
    }
  }

  /**
   * Parses a *single-line* class declaration. If it has a body with multiple elements, they are
   * semicolon-separated. Syntax examples can be seen in `"components"` fields of `cards.json`.
   */
  public fun parseOneLinerClass(declarationSource: String): ClassDeclaration =
      rejectOwnerLocalClasses(listOf(parse(Declarations.oneLineDecl, declarationSource))).single()

  private fun rejectOwnerLocalClasses(
      declarations: List<ClassDeclaration>
  ): List<ClassDeclaration> {
    val hasOwnerLocalClass = declarations.any { declaration ->
      declaration.allNodes.any { node ->
        (node as? Expression)?.derivedClassBody != null ||
            node.descendantsOfType<Expression>().any { it.derivedClassBody != null }
      }
    }
    if (hasOwnerLocalClass) {
      throw PetSyntaxException("Owner-local Classes are not allowed inside Class declarations")
    }
    return declarations
  }

  /**
   * Parses the Pets element of type [P] from [elementSource], and returns it *not* surrounded by a
   * `RAW` block. [P] can only be one of the published node kinds like [Effect], [Action],
   * [InstructionTree], [Expression], etc. Owner-local derived Class syntax is fully parsed, then
   * rejected because this API has no definition owner or mutable Class Table.
   */
  public inline fun <reified P : PetNode> parse(elementSource: String): P =
      parse(P::class, elementSource)

  /** Non-reified form of [parse]. */
  public fun <P : PetNode> parse(expectedType: KClass<P>, elementSource: String): P {
    val lowerer = DerivedClassLowerer(ClassName.cn("Submitted"))
    val pet = parse(expectedType, elementSource, lowerer)
    if (lowerer.declarations.isNotEmpty()) throw NoNewClassDeclarationsException()
    return pet
  }

  internal fun <P : PetNode> parse(
      expectedType: KClass<P>,
      elementSource: String,
      derivedClasses: DerivedClassLowerer,
  ): P {
    val group = parserGroup
    val matches: TokenMatchesSequence = TokenCache.tokenize(elementSource)
    require(expectedType != PetNode::class) { "missing type info" }

    val parsed = group.parse(expectedType, elementSource, matches)
    val lowered = derivedClasses.transformWithoutKindCheck(parsed)
    check(expectedType.isInstance(lowered)) {
      "Expected ${expectedType.simpleName} kind, got ${lowered.kind.simpleName}"
    }
    @Suppress("UNCHECKED_CAST")
    return lowered as P
  }

  internal fun <T> parse(
      parser: Parser<T>,
      source: String,
      matches: TokenMatchesSequence,
      expectedTypeDesc: String? = null,
  ): T {
    try {
      return parser.parseToEnd(matches)
    } catch (e: ParseException) {
      val tokenDesc =
          matches
              .filterNot { it.type.ignored }
              .joinToString(" ") { it.type.name?.replace("\n", "\\n") ?: "NULL" }

      throw PetSyntaxException(
          """
            Expecting: $expectedTypeDesc
            Token stream: $tokenDesc
            Input was:
            ${source.replaceIndent("  ")}
          """
              .trimIndent(),
          e,
      )
    } catch (e: RuntimeException) {
      throw PetSyntaxException("Invalid Pets syntax: $source", e)
    }
  }

  internal fun <T> parse(parser: Parser<T>, source: String, expectedTypeDesc: String? = null): T =
      parse(parser, source, TokenCache.tokenize(source), expectedTypeDesc)

  public fun acceptsNextToken(
      expectedType: KClass<out PetNode>,
      source: String,
      candidate: String,
  ): Boolean {
    return expectedTokens(expectedType, source).any { it.match(candidate, 0) > 0 }
  }

  private fun expectedTokens(expectedType: KClass<out PetNode>, source: String): Set<Token> {
    require(expectedType != PetNode::class) { "missing type info" }
    return parserGroup
        .parser(expectedType)
        .completionAtEnd(TokenCache.tokenize(source))
        .expectedTokens
  }

  private val parserGroup by lazy {
    val pgb = ParserGroup.Builder<PetNode>()
    pgb.publish(Action.parser())
    pgb.publish(ClassName.parser())
    pgb.publish(Cost.parser())
    pgb.publish(Effect.parser())
    pgb.publish(Expression.parser())
    pgb.publish(InstructionTree.parser())
    pgb.publish(Instruction.parser())
    pgb.publish(Metric.parser())
    pgb.publish(PropertyName.parser())
    pgb.publish(PropertyValue.parser())
    pgb.publish(Requirement.parser())
    pgb.publish(ScaledExpression.parser())
    pgb.publish(Trigger.parser())

    pgb.finish()
  }
}
