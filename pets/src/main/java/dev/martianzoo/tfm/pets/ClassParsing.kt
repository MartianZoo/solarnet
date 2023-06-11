package dev.martianzoo.tfm.pets

import com.github.h0tk3y.betterParse.combinators.and
import com.github.h0tk3y.betterParse.combinators.asJust
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
import dev.martianzoo.tfm.data.ClassDeclaration.ClassKind
import dev.martianzoo.tfm.data.ClassDeclaration.ClassKind.ABSTRACT
import dev.martianzoo.tfm.data.ClassDeclaration.ClassKind.CONCRETE
import dev.martianzoo.tfm.data.ClassDeclaration.DefaultsDeclaration
import dev.martianzoo.tfm.data.ClassDeclaration.DefaultsDeclaration.OneDefault
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
import dev.martianzoo.tfm.pets.Transforming.actionListToEffects
import dev.martianzoo.tfm.pets.ast.Action
import dev.martianzoo.tfm.pets.ast.ClassName
import dev.martianzoo.tfm.pets.ast.ClassName.Parsing.classFullName
import dev.martianzoo.tfm.pets.ast.ClassName.Parsing.classShortName
import dev.martianzoo.tfm.pets.ast.Effect
import dev.martianzoo.tfm.pets.ast.Expression
import dev.martianzoo.tfm.pets.ast.Requirement
import dev.martianzoo.util.KClassMultimap
import dev.martianzoo.util.plus
import dev.martianzoo.util.toSetStrict

internal object ClassParsing : PetTokenizer() {
  private val nls = zeroOrMore(char('\n'))

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
              gainOnly = OneDefault(expr.arguments, int),
              forClass = expr.className,
          )
        }

    private val removeOnlyDefaults: Parser<DefaultsDeclaration> =
        skipChar('-') and
        Expression.parser() and
        intensity map { (expr, int) ->
          require(expr.refinement == null)
          DefaultsDeclaration(
              removeOnly = OneDefault(expr.arguments, int),
              forClass = expr.className,
          )
        }

    private val allCasesDefault: Parser<DefaultsDeclaration> by lazy {
      Expression.parser() map {
        require(it.refinement == null)
        DefaultsDeclaration(universal = OneDefault(it.arguments), forClass = it.className)
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
    private val kind: Parser<ClassKind> =
        (_abstract and _class asJust ABSTRACT) or (_class asJust CONCRETE)

    private val bodyElement = parser { bodyElementExceptNestedClasses or nestedGroup }

    private val multilineBodyInterior: Parser<Body> =
        separatedTerms(bodyElement, oneOrMore(char('\n')), acceptZero = true) map ::Body

    private val multilineBody: Parser<Body> =
        skipChar('{') and skip(nls) and multilineBodyInterior and skip(nls) and skipChar('}')

    private val nestableGroup: Parser<NestableDeclGroup> =
        skip(nls) and
        kind and
        signature and
        (multilineBody or moreSignatures) map { (kind, sig, bodyOrSigs) ->
          bodyOrSigs.convert(kind, sig)
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
        kind and
        signature and
        optional(oneLineBody) map { (kind, sig, body) ->
          NestableDeclGroup(kind, sig, body ?: Body()).finishOnlyDecl()
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
            kind = ABSTRACT, // needs to be overwritten!
            dependencies = dependencies,
            supertypes = supertypes.toSetStrict(),
        ),
    )
  }

  internal sealed class MoreSignaturesOrBody {
    abstract fun convert(kind: ClassKind, firstSignature: Signature): NestableDeclGroup
  }

  internal class MoreSignatures(private val moreSignatures: List<Signature>) :
      MoreSignaturesOrBody() {
    override fun convert(kind: ClassKind, firstSignature: Signature) =
        NestableDeclGroup(
            (firstSignature plus moreSignatures).map { IncompleteNestableDecl(kind, it) },
        )
  }

  internal class Body(private val elements: KClassMultimap<BodyElement>) : MoreSignaturesOrBody() {
    constructor(list: List<BodyElement> = listOf()) : this(KClassMultimap(list))

    override fun convert(kind: ClassKind, firstSignature: Signature) =
        NestableDeclGroup(kind, firstSignature, this)

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
        kind: ClassKind,
        signature: Signature,
        body: Body,
    ) : this(create(kind, signature, body))

    fun unnestAllFrom(container: ClassName): List<NestableDecl> =
        declList.map { it.unnestOneFrom(container) }

    fun finishOnlyDecl() = declList.single().decl

    fun finishAll() = declList.map { it.decl }

    private companion object {
      fun create(kind: ClassKind, signature: Signature, body: Body): List<NestableDecl> {
        val mergedDefaults = DefaultsDeclaration.merge(body.defaultses)
        require(mergedDefaults.forClass in setOf(null, signature.className))
        val newDecl =
            signature.asDeclaration.copy(
                kind = kind,
                invariants = body.invariants.toSetStrict(),
                effects = (body.effects + actionListToEffects(body.actions)).toSetStrict(),
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
          kind: ClassKind,
          signature: Signature
      ) : this(signature.asDeclaration.copy(kind = kind))

      // This returns a new NestableDecl that looks like it could be a sibling to containingClass
      // instead of nested inside it
      override fun unnestOneFrom(container: ClassName): NestableDecl {
        return if (decl.supertypes.any { it.className == container }) {
          CompleteNestableDecl(decl)
        } else {
          val supertypes = (container.expression plus decl.supertypes).toSetStrict()
          CompleteNestableDecl(decl.copy(supertypes = supertypes))
        }
      }
    }
  }
}
