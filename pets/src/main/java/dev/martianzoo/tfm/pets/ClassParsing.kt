package dev.martianzoo.tfm.pets

import com.github.h0tk3y.betterParse.combinators.and
import com.github.h0tk3y.betterParse.combinators.map
import com.github.h0tk3y.betterParse.combinators.oneOrMore
import com.github.h0tk3y.betterParse.combinators.optional
import com.github.h0tk3y.betterParse.combinators.or
import com.github.h0tk3y.betterParse.combinators.separatedTerms
import com.github.h0tk3y.betterParse.combinators.skip
import com.github.h0tk3y.betterParse.combinators.zeroOrMore
import com.github.h0tk3y.betterParse.grammar.parser
import com.github.h0tk3y.betterParse.parser.Parser
import dev.martianzoo.tfm.data.ClassDeclaration
import dev.martianzoo.tfm.data.ClassDeclaration.DefaultsDeclaration
import dev.martianzoo.tfm.pets.ClassParsing.Body.BodyElement
import dev.martianzoo.tfm.pets.ClassParsing.Body.BodyElement.ActionElement
import dev.martianzoo.tfm.pets.ClassParsing.Body.BodyElement.DefaultsElement
import dev.martianzoo.tfm.pets.ClassParsing.Body.BodyElement.EffectElement
import dev.martianzoo.tfm.pets.ClassParsing.Body.BodyElement.InvariantElement
import dev.martianzoo.tfm.pets.ClassParsing.Body.BodyElement.NestedDeclGroup
import dev.martianzoo.tfm.pets.ClassParsing.BodyElements.bodyElementExceptNestedClasses
import dev.martianzoo.tfm.pets.ClassParsing.NestableDecl.IncompleteNestableDecl
import dev.martianzoo.tfm.pets.ClassParsing.Signatures.moreSignatures
import dev.martianzoo.tfm.pets.ClassParsing.Signatures.signature
import dev.martianzoo.tfm.pets.Parsing.parse
import dev.martianzoo.tfm.pets.Parsing.parseRepeated
import dev.martianzoo.tfm.pets.PureTransformers.actionListToEffects
import dev.martianzoo.tfm.pets.ast.Action
import dev.martianzoo.tfm.pets.ast.ClassName
import dev.martianzoo.tfm.pets.ast.ClassName.Parsing.classFullName
import dev.martianzoo.tfm.pets.ast.ClassName.Parsing.classShortName
import dev.martianzoo.tfm.pets.ast.Effect
import dev.martianzoo.tfm.pets.ast.Expression
import dev.martianzoo.tfm.pets.ast.HasClassName
import dev.martianzoo.tfm.pets.ast.PetNode.Companion.raw
import dev.martianzoo.tfm.pets.ast.Requirement
import dev.martianzoo.util.KClassMultimap
import dev.martianzoo.util.plus
import dev.martianzoo.util.toSetStrict

/** Parses the PETS language. */
public object ClassParsing : PetTokenizer() {
  /**
   * Parses a series of Pets class declarations. The syntax is currently not documented (sorry), but
   * examples can be reviewed in `components.pets` and `player.pets`.
   */
  public fun parseClassDeclarations(declarationsSource: String): List<ClassDeclaration> {
    val tokens = TokenCache.tokenize(stripLineComments(declarationsSource))
    return parseRepeated(Declarations.topLevelGroup, tokens).flatten()
  }

  /**
   * Parses a **single-line** class declaration; if it has a body, the elements within the body are
   * semicolon-separated.
   */
  public fun parseOneLiner(declarationSource: String): ClassDeclaration =
      parse(Declarations.oneLineDecl, declarationSource)

  // end public API

  private val nls = zeroOrMore(char('\n'))

  // TODO hack
  init {
    ClassName.Parsing.className
  }

  /*
   * These objects like [Signatures] are purely for grouping and to limit visibility of the
   * fine-grained details never needed again.
   */

  internal object Signatures {

    private val dependencies: Parser<List<Expression>> =
        optionalList(skipChar('<') and commaSeparated(Expression.parser()) and skipChar('>'))

    private val supertypeList: Parser<List<Expression>> =
        optionalList(skipChar(':') and commaSeparated(Expression.parser()))

    val signature: Parser<Signature> =
        classFullName and
        dependencies and
        optional(skipChar('[') and parser { classShortName } and skipChar(']')) and
        supertypeList map { (name, deps, short, supes) ->
          Signature(name, short, deps, supes)
        }

    // This should only be included in the bodiless case
    val moreSignatures: Parser<MoreSignatures> =
        zeroOrMore(skipChar(',') and signature) map ::MoreSignatures
  }

  internal object BodyElements {
    private val invariant: Parser<Requirement> = skip(_has) and Requirement.parser()

    private val gainOnlyDefaults: Parser<DefaultsDeclaration> =
        skipChar('+') and
        Expression.parser() and
        intensity map { (expr, int) ->
          require(expr.refinement == null)
          DefaultsDeclaration(
              gainOnlySpecs = expr.arguments,
              gainIntensity = int,
              forClass = expr.className,
          )
        }

    private val removeOnlyDefaults: Parser<DefaultsDeclaration> =
        skipChar('-') and
        Expression.parser() and
        intensity map { (expr, int) ->
          require(expr.refinement == null)
          DefaultsDeclaration(
              removeOnlySpecs = expr.arguments,
              removeIntensity = int,
              forClass = expr.className,
          )
        }

    private val allCasesDefault: Parser<DefaultsDeclaration> by lazy {
      Expression.parser() map {
        require(it.refinement == null)
        DefaultsDeclaration(universalSpecs = it.arguments, forClass = it.className)
      }
    }

    private val default: Parser<DefaultsDeclaration> =
        skip(_default) and (gainOnlyDefaults or removeOnlyDefaults or allCasesDefault)

    val bodyElementExceptNestedClasses: Parser<BodyElement> =
        (invariant map ::InvariantElement) or
        (default map ::DefaultsElement) or
        (Effect.parser() map { EffectElement(it) }) or
        (Action.parser() map { ActionElement(it) })
  }

  internal object Declarations {
    private val isAbstract: Parser<Boolean> =
        optional(_abstract) and skip(_class) map { it != null }

    private val bodyElement = parser { bodyElementExceptNestedClasses or nestedGroup }

    private val multilineBodyInterior: Parser<Body> =
        separatedTerms(bodyElement, oneOrMore(char('\n')), acceptZero = true) map ::Body

    private val multilineBody: Parser<Body> =
        skipChar('{') and skip(nls) and multilineBodyInterior and skip(nls) and skipChar('}')

    private val nestableGroup: Parser<NestableDeclGroup> =
        skip(nls) and
        isAbstract and
        signature and
        (multilineBody or moreSignatures) map { (abs, sig, bodyOrSigs) ->
          bodyOrSigs.convert(abs, sig)
        }

    // a declaration group that can be nested, that in this case *IS* nested
    private val nestedGroup: Parser<NestedDeclGroup> = nestableGroup map ::NestedDeclGroup

    // a declaration group that could've been nested but is *NOT*
    val topLevelGroup: Parser<List<ClassDeclaration>> = nestableGroup map { it.finishAll() }

    // For CardDefinition

    private val oneLineBody: Parser<Body> =
        skipChar('{') and
        separatedTerms(bodyElementExceptNestedClasses, char(';')) and
        skipChar('}') map ::Body

    val oneLineDecl: Parser<ClassDeclaration> =
        isAbstract and
        signature and
        optional(oneLineBody) map { (abs, sig, body) ->
          NestableDeclGroup(abs, sig, body ?: Body()).finishOnlyDecl()
        }
  }

  // The rest of the file is temporary types used only during parsing.

  internal data class Signature(val asDeclaration: ClassDeclaration) :
      HasClassName by asDeclaration {
    constructor(
        className: ClassName,
        shortName: ClassName?,
        dependencies: List<Expression>,
        supertypes: List<Expression>,
    ) : this(
        ClassDeclaration(
            className = className,
            shortName = shortName ?: className,
            dependencies = dependencies,
            supertypes = supertypes.toSetStrict()))
  }

  internal sealed class MoreSignaturesOrBody {
    abstract fun convert(abstract: Boolean, firstSignature: Signature): NestableDeclGroup
  }

  internal class MoreSignatures(private val moreSignatures: List<Signature>) :
      MoreSignaturesOrBody() {
    override fun convert(abstract: Boolean, firstSignature: Signature) =
        NestableDeclGroup(
            (firstSignature plus moreSignatures).map { IncompleteNestableDecl(abstract, it) })
  }

  internal class Body(private val elements: KClassMultimap<BodyElement>) : MoreSignaturesOrBody() {
    constructor(list: List<BodyElement> = listOf()) : this(KClassMultimap(list))

    override fun convert(abstract: Boolean, firstSignature: Signature) =
        NestableDeclGroup(abstract, firstSignature, this)

    private inline fun <reified E : BodyElement> getAll() = elements.get<E>()

    val invariants = getAll<InvariantElement>().map { it.invariant }
    val defaultses = getAll<DefaultsElement>().map { it.defaults }
    val effects = getAll<EffectElement>().map { it.effect }
    val actions = getAll<ActionElement>().map { it.action }
    val nestedGroups = getAll<NestedDeclGroup>().map { it.declGroup }

    sealed class BodyElement {
      class InvariantElement(val invariant: Requirement) : BodyElement()
      class DefaultsElement(val defaults: DefaultsDeclaration) : BodyElement()
      class EffectElement(val effect: Effect) : BodyElement()
      class ActionElement(val action: Action) : BodyElement()
      class NestedDeclGroup(val declGroup: NestableDeclGroup) : BodyElement()
    }
  }

  internal class NestableDeclGroup(private val declList: List<NestableDecl>) {
    constructor(
        abstract: Boolean,
        signature: Signature,
        body: Body,
    ) : this(create(abstract, signature, body))

    fun unnestAllFrom(container: ClassName): List<NestableDecl> =
        declList.map { it.unnestOneFrom(container) }

    fun finishOnlyDecl() = declList.single().decl

    fun finishAll() = declList.map { it.decl }

    private companion object {
      fun create(abstract: Boolean, signature: Signature, body: Body): List<NestableDecl> {
        val mergedDefaults = DefaultsDeclaration.merge(body.defaultses)
        require(mergedDefaults.forClass in setOf(null, signature.className))
        val newDecl =
            signature.asDeclaration.copy(
                abstract = abstract,
                invariants = body.invariants.toSetStrict { it.raw() },
                effectsIn =
                    (body.effects + actionListToEffects(body.actions)).toSetStrict { it.raw() },
                defaultsDeclaration = mergedDefaults,
            )
        val unnested = body.nestedGroups.flatMap { it.unnestAllFrom(signature.className) }
        return IncompleteNestableDecl(newDecl) plus unnested
      }
    }
  }

  internal sealed class NestableDecl {
    abstract val decl: ClassDeclaration
    abstract fun unnestOneFrom(container: ClassName): NestableDecl

    data class CompleteNestableDecl(override val decl: ClassDeclaration) : NestableDecl() {
      override fun unnestOneFrom(container: ClassName) = this
    }

    data class IncompleteNestableDecl(override val decl: ClassDeclaration) : NestableDecl() {
      constructor(
          abstract: Boolean,
          signature: Signature,
      ) : this(signature.asDeclaration.copy(abstract = abstract))
      // This returns a new NestableDecl that looks like it could be a sibling to containingClass
      // instead of nested inside it
      override fun unnestOneFrom(container: ClassName): NestableDecl {
        return if (decl.supertypes.any { it.className == container }) {
          CompleteNestableDecl(decl)
        } else {
          val supertypes = (container.expr plus decl.supertypes).toSetStrict()
          CompleteNestableDecl(decl.copy(supertypes = supertypes))
        }
      }
    }
  }

  private val lineCommentRegex = Regex(""" *(//[^\n]*)*\n""")

  private fun stripLineComments(text: String) = lineCommentRegex.replace(text, "\n")
}
