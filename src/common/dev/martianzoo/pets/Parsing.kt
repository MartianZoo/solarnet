package dev.martianzoo.pets

import com.github.h0tk3y.betterParse.lexer.Token
import com.github.h0tk3y.betterParse.lexer.TokenMatchesSequence
import com.github.h0tk3y.betterParse.parser.ParseException
import com.github.h0tk3y.betterParse.parser.Parser
import com.github.h0tk3y.betterParse.parser.completionAtEnd
import com.github.h0tk3y.betterParse.parser.parseToEnd
import dev.martianzoo.pets.ClassParsing.Declarations
import dev.martianzoo.pets.PetTokenizer.TokenCache
import dev.martianzoo.pets.api.Exceptions.NoNewClassDeclarationsException
import dev.martianzoo.pets.api.Exceptions.PetSyntaxException
import dev.martianzoo.pets.ast.Action
import dev.martianzoo.pets.ast.Action.Cost
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.Effect
import dev.martianzoo.pets.ast.Effect.Trigger
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.Expression.Refinement
import dev.martianzoo.pets.ast.FromExpression
import dev.martianzoo.pets.ast.Instruction
import dev.martianzoo.pets.ast.InstructionTree
import dev.martianzoo.pets.ast.Metric
import dev.martianzoo.pets.ast.PetElement
import dev.martianzoo.pets.ast.PetNode
import dev.martianzoo.pets.ast.Property
import dev.martianzoo.pets.ast.PropertyName
import dev.martianzoo.pets.ast.PropertyValue
import dev.martianzoo.pets.ast.Requirement
import dev.martianzoo.pets.ast.ScaledExpression
import dev.martianzoo.pets.ast.ScaledExpression.Scalar
import dev.martianzoo.pets.data.ClassDeclaration
import dev.martianzoo.pets.util.ParserGroup
import kotlin.reflect.KClass

/**
 * Various functions for parsing [PetElement]s or [ClassDeclaration]s from text. The source syntax
 * is
 * [section 1](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#1-source-and-declarations)
 * of the Pets language specification.
 */
public object Parsing {
  /**
   * Parses a series of Pets class declarations, returning one [ClassDeclaration] per declared
   * class, in source order ([rule
   * L1-1](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#1-source-and-declarations)).
   * Nested declarations follow their container, recursively ([rule
   * L1-5](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#1-source-and-declarations)),
   * and owner-local classes are lowered to ordinary declarations ([section
   * 11](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#11-owner-local-classes)).
   * Examples can be reviewed in `global.pets` and `player.pets`.
   *
   * A source containing only whitespace and comments declares nothing. There is no partial success:
   * an incomplete final declaration is rejected.
   *
   * @throws PetSyntaxException if [declarationsSource] is not a complete sequence of declarations
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
   * Parses exactly one class declaration, with an optional semicolon-separated body ([rule
   * L1-12](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#1-source-and-declarations)).
   * This is how a declaration embedded in structured card data is read; syntax examples can be seen
   * in `"components"` fields of `cards.json`.
   *
   * Owner-local class syntax is rejected here, since that syntax is available only where a
   * declaration file is being read ([rule
   * L11-7](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#11-owner-local-classes)).
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
   * [InstructionTree], [Expression], etc.
   *
   * Owner-local derived Class syntax is parsed and validated, then rejected with
   * [NoNewClassDeclarationsException], because a submitted element has no definition owner and a
   * live game's class table is frozen ([rule
   * L11-7](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#11-owner-local-classes)).
   * Parsing that far is what keeps the specific error distinct from malformed syntax.
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

  // TODO: Contract this temporary tfm-canon seam.
  public fun <P : PetNode> parse(
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
      return parser.parseToEnd(matches).also(::rejectUnsupportedSyntax)
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

  private fun rejectUnsupportedSyntax(parsed: Any?) {
    when (parsed) {
      is ClassDeclaration -> parsed.allNodes.forEach(::rejectUnsupportedSyntax)
      is PetNode ->
          parsed.visitDescendants {
            (it as? Expression)?.let(ScaledExpression::rejectIfDenominationless)
            true
          }
      is Iterable<*> -> parsed.forEach(::rejectUnsupportedSyntax)
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
    pgb.publish(Refinement::class, Expression.refinementParser())
    pgb.publish(FromExpression.parser())
    pgb.publish(InstructionTree.parser())
    pgb.publish(Instruction.parser())
    pgb.publish(Metric.parser())
    pgb.publish(Property.parser())
    pgb.publish(PropertyName.parser())
    pgb.publish(PropertyValue.parser())
    pgb.publish(Requirement.parser())
    pgb.publish(Scalar::class, ScaledExpression.scalar())
    pgb.publish(ScaledExpression.parser())
    pgb.publish(Trigger.parser())

    pgb.finish()
  }
}
