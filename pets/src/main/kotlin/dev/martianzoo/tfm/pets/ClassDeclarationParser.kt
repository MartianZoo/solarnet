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
import dev.martianzoo.tfm.data.ClassDeclaration.DependencyDeclaration
import dev.martianzoo.tfm.pets.ClassDeclarationParser.Sigs.Signature
import dev.martianzoo.tfm.pets.ClassDeclarationParser.Sigs.moreSignatures
import dev.martianzoo.tfm.pets.PetParser.Instructions.intensity
import dev.martianzoo.tfm.pets.PetParser.Types
import dev.martianzoo.tfm.pets.PetParser.action
import dev.martianzoo.tfm.pets.PetParser.effect
import dev.martianzoo.tfm.pets.PetParser.genericType
import dev.martianzoo.tfm.pets.PetParser.nls
import dev.martianzoo.tfm.pets.PetParser.requirement
import dev.martianzoo.tfm.pets.PetParser.typeExpression
import dev.martianzoo.tfm.pets.PetTokenizer._abstract
import dev.martianzoo.tfm.pets.PetTokenizer._class
import dev.martianzoo.tfm.pets.PetTokenizer._default
import dev.martianzoo.tfm.pets.PetTokenizer._has
import dev.martianzoo.tfm.pets.PetTokenizer.char
import dev.martianzoo.tfm.pets.PetTokenizer.commaSeparated
import dev.martianzoo.tfm.pets.PetTokenizer.optionalList
import dev.martianzoo.tfm.pets.PetTokenizer.skipChar
import dev.martianzoo.tfm.pets.SpecialClassNames.COMPONENT
import dev.martianzoo.tfm.pets.SpecialClassNames.THIS
import dev.martianzoo.tfm.pets.ast.Action
import dev.martianzoo.tfm.pets.ast.ClassName
import dev.martianzoo.tfm.pets.ast.Effect
import dev.martianzoo.tfm.pets.ast.Requirement
import dev.martianzoo.tfm.pets.ast.TypeExpression.GenericTypeExpression
import dev.martianzoo.util.KClassMultimap
import dev.martianzoo.util.onlyElement
import dev.martianzoo.util.plus
import dev.martianzoo.util.toSetStrict
import kotlin.reflect.KClass

/** Parses the Petaform language. */
object ClassDeclarationParser {
  private val toke = PetTokenizer.toke
  /**
   * Parses an entire PETS class declarations source file.
   */
  fun parseClassDeclarations(text: String): List<ClassDeclaration> {
    val tokens = toke.tokenize(stripLineComments(text))
    return parseRepeated(topLevelDeclsGroup, tokens)
  }

  // TODO move
  internal fun stripLineComments(text: String) = Regex(""" *(//[^\n]*)*\n""").replace(text, "\n")

  internal object Sigs {
    private val dependency: Parser<DependencyDeclaration> =
        typeExpression map ::DependencyDeclaration
    private val dependencies: Parser<List<DependencyDeclaration>> =
        optionalList(skipChar('<') and commaSeparated(dependency) and skipChar('>'))

    private val supertypes: Parser<List<GenericTypeExpression>> =
        optionalList(skipChar(':') and commaSeparated(parser { genericType }))

    data class Signature(val asClassDecl: ClassDeclaration) {
      constructor(
        className: ClassName,
        dependencies: List<DependencyDeclaration>,
        topInvariant: Requirement?,
        supertypes: List<GenericTypeExpression>,
      ) : this(ClassDeclaration(
          abstract = true,
          name = className,
          id = className,
          dependencies = dependencies,
          topInvariant = topInvariant,
          supertypes = supertypes.toSetStrict()
      ))
    }

    val signature: Parser<Signature> =
        parser { Types.classFullName } and
        dependencies and
        parser { Types.optlRefinement } and
        // optional(skipChar('[') and parser {Types.classShortName} and skipChar(']')) and
        supertypes map { (name, deps, refin, supes) ->
          Signature(name, deps, refin, supes)
        }

    val moreSignatures: Parser<MoreSignatures> = zeroOrMore(skipChar(',') and signature) map ::MoreSignatures
  }

  private val isAbstract: Parser<Boolean> = optional(_abstract) and skip(_class) map { it != null }

  internal sealed class BodyOrMoreSignatures {
    abstract fun convert(abstract: Boolean, firstSignature: Signature): NestableDeclGroup
  }

  internal class Body(val elements: KClassMultimap<BodyElement>) : BodyOrMoreSignatures() {
    constructor(list: List<BodyElement> = listOf()) : this(KClassMultimap(list))

    val haveBeenExtracted = mutableSetOf<KClass<*>>()

    override fun convert(abstract: Boolean, firstSignature: Signature) =
        NestableDeclGroup(abstract, firstSignature, this)

    inline fun <reified E : BodyElement> getAll() = elements.get<E>()
  }

  internal class MoreSignatures(val moreSignatures: List<Signature>) : BodyOrMoreSignatures() {
    override fun convert(abstract: Boolean, firstSignature: Signature) =
        NestableDeclGroup(
            (firstSignature plus moreSignatures).map {
              IncompleteNestableDecl(abstract, it)
            })
  }

  sealed class BodyElement
  internal class InvariantElement(val invariant: Requirement) : BodyElement()
  internal class DefaultsElement(val defaults: DefaultsDeclaration) : BodyElement()
  internal class EffectElement(val effect: Effect) : BodyElement()
  internal class ActionElement(val action: Action) : BodyElement()

  internal class NestedDeclGroup(val declGroup: NestableDeclGroup) : BodyElement()

  internal class NestableDeclGroup(val declList: List<NestableDecl>) {

    constructor(abstract: Boolean, signature: Signature, body: Body) :
        this(createNestableDecls(abstract, signature, body))

    fun unnestAllFrom(container: ClassName): List<NestableDecl> =
        declList.map { it.unnestOneFrom(container) }

    fun finishOnlyDecl() = declList.onlyElement().finishAtTopLevel()

    fun finishAll() = declList.map { it.finishAtTopLevel() }

    private companion object {
      private fun createNestableDecls(
          abstract: Boolean, signature: Signature, body: Body,
      ): List<NestableDecl> {
        val effects = body.getAll<EffectElement>().map { it.effect }
        val actions = body.getAll<ActionElement>().map { it.action }

        val newDecl = signature.asClassDecl.copy(
            abstract = abstract,
            otherInvariants = body.getAll<InvariantElement>().map { it.invariant }.toSetStrict(),
            effectsRaw = (effects + actionsToEffects(actions)).toSetStrict(),
            defaultsDeclaration = merge(body.getAll<DefaultsElement>().map { it.defaults }),
        )

        val nestableGroups = body.getAll<NestedDeclGroup>().map { it.declGroup }
        val asSiblings = nestableGroups.flatMap { it.unnestAllFrom(signature.asClassDecl.name) }
        return IncompleteNestableDecl(newDecl) plus asSiblings
      }
    }  }

  object Bodies {
    private val gainOnlyDefaults: Parser<DefaultsDeclaration> =
        skipChar('+') and genericType and intensity map { (type, int) ->
          require(type.className == THIS)
          require(type.refinement == null)
          DefaultsDeclaration(gainOnlySpecs = type.specs, gainIntensity = int)
        }

    private val allCasesDefault: Parser<DefaultsDeclaration> = parser { Types.genericType } map {
      require(it.className == THIS)
      require(it.refinement == null)
      DefaultsDeclaration(universalSpecs = it.specs)
    }

    private val default: Parser<DefaultsDeclaration> =
        skip(_default) and (gainOnlyDefaults or allCasesDefault)

    private val invariant: Parser<Requirement> =
        skip(_has) and requirement

    val bodyElementNoClass: Parser<BodyElement> =
        (invariant map ::InvariantElement) or
        (default map ::DefaultsElement) or
        (effect map ::EffectElement) or
        (action map ::ActionElement)
  }

  private val oneLineBody: Parser<Body> =
      skipChar('{') and
      separatedTerms(Bodies.bodyElementNoClass, char(';')) and
      skipChar('}') map ::Body

  val singleDecl: Parser<ClassDeclaration> =
      isAbstract and Sigs.signature and optional(oneLineBody) map { (abs, sig, body) ->
        NestableDeclGroup(abs, sig, body ?: Body()).finishOnlyDecl()
      }

  private val bodyElement: Parser<BodyElement> =
      Bodies.bodyElementNoClass or parser { nestedDecls }

  private val multilineBodyInterior: Parser<Body> =
      separatedTerms(bodyElement, oneOrMore(char('\n')), acceptZero = true) map ::Body

  private val multilineBody: Parser<Body> =
      skipChar('{') and skip(nls) and
      multilineBodyInterior and
      skip(nls) and skipChar('}')

  private val nestableDeclGroup: Parser<NestableDeclGroup> =
      skip(nls) and
      isAbstract and
      Sigs.signature and
      (multilineBody or moreSignatures) map {
        (abs, sig, bodyOrSigs) -> bodyOrSigs.convert(abs, sig)
      }

  // they not just *can* be nested, they *are* nested
  private val nestedDecls: Parser<NestedDeclGroup> = nestableDeclGroup map ::NestedDeclGroup

  // they *can* be nested, but they are *not*
  val topLevelDeclsGroup: Parser<List<ClassDeclaration>> = nestableDeclGroup map { it.finishAll() }
}

private fun merge(defs: Collection<DefaultsDeclaration>) =
    DefaultsDeclaration(
        universalSpecs = defs.firstNotNullOfOrNull { it.universalSpecs } ?: listOf(),
        gainOnlySpecs = defs.firstNotNullOfOrNull { it.gainOnlySpecs } ?: listOf(),
        gainIntensity = defs.firstNotNullOfOrNull { it.gainIntensity },
    )

/** A declaration that might be nested, so we don't know if we have all its supertypes yet. */
sealed class NestableDecl {
  abstract val decl: ClassDeclaration
  abstract fun unnestOneFrom(container: ClassName): NestableDecl

  fun finishAtTopLevel(): ClassDeclaration { // TODO
    if (decl.name != COMPONENT && decl.supertypes.isEmpty()) {
      return decl.copy(supertypes = setOf(COMPONENT.type)).also { it.validate() }
    }
    return decl.also { it.validate() }
  }
}

internal data class CompleteNestableDecl(override val decl: ClassDeclaration) : NestableDecl() {
  init { decl.validate() }
  override fun unnestOneFrom(container: ClassName) = this
}

internal data class IncompleteNestableDecl(override val decl: ClassDeclaration) : NestableDecl() {
  constructor(abstract: Boolean, signature: Signature) :
      this(signature.asClassDecl.copy(abstract = abstract))

  // This returns a new NestableDecl that looks like it could be a sibling to containingClass
  // instead of nested inside it
  override fun unnestOneFrom(container: ClassName): NestableDecl {
    return when {
      // the class name we'd insert is already there (TODO be even smarter)
      decl.supertypes.any { it.className == container } -> CompleteNestableDecl(decl)

      // jam the superclass in and mark it complete
      else -> CompleteNestableDecl(prependSuperclass(container))
    }
  }

  private fun prependSuperclass(superclassName: ClassName): ClassDeclaration {
    val allSupertypes = superclassName.type plus decl.supertypes
    return decl.copy(supertypes = allSupertypes.toSetStrict())
  }
}
