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
import dev.martianzoo.tfm.pets.PetParser.Actions.action
import dev.martianzoo.tfm.pets.PetParser.Effects.effect
import dev.martianzoo.tfm.pets.PetParser.Instructions
import dev.martianzoo.tfm.pets.PetParser.Requirements.requirement
import dev.martianzoo.tfm.pets.PetParser.Types
import dev.martianzoo.tfm.pets.PetParser.Types.typeExpression
import dev.martianzoo.tfm.pets.PetParser._abstract
import dev.martianzoo.tfm.pets.PetParser._class
import dev.martianzoo.tfm.pets.PetParser._default
import dev.martianzoo.tfm.pets.PetParser._has
import dev.martianzoo.tfm.pets.PetParser.char
import dev.martianzoo.tfm.pets.PetParser.commaSeparated
import dev.martianzoo.tfm.pets.PetParser.nls
import dev.martianzoo.tfm.pets.PetParser.optionalList
import dev.martianzoo.tfm.pets.PetParser.skipChar
import dev.martianzoo.tfm.pets.PetParser.tokenizer
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

  /**
   * Parses an entire PETS class declarations source file.
   */
  fun parseClassDeclarations(text: String): List<ClassDeclaration> {
    val tokens = tokenizer.tokenize(stripLineComments(text))
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
        optionalList(skipChar(':') and commaSeparated(parser { Types.genericType }))

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

    val moreSignatures: Parser<MoreSignatures> = zeroOrMore(skipChar(',') and Sigs.signature) map ::MoreSignatures
  }

  private val isAbstract: Parser<Boolean> = optional(_abstract) and skip(_class) map { it != null }

  internal sealed class BodyOrMoreSignatures {
    abstract fun convert(abstract: Boolean, firstSignature: Signature): NestableDeclGroup
  }

  internal class Body(val elements: KClassMultimap<BodyElement<*>>) : BodyOrMoreSignatures() {
    constructor(list: List<BodyElement<*>> = listOf()) : this(KClassMultimap(list))

    val haveBeenExtracted = mutableSetOf<KClass<*>>()

    override fun convert(abstract: Boolean, firstSignature: Signature) =
        NestableDeclGroup(abstract, firstSignature, this)

    inline fun <reified T, reified E : BodyElement<T>> getAllOfType(): Set<T> {
      println("tried to extract ${T::class}")
      return elements.get<E>().map { it.cargo }.toSetStrict()
    }
  }
  internal class MoreSignatures(val moreSignatures: List<Signature>) : BodyOrMoreSignatures() {
    override fun convert(abstract: Boolean, firstSignature: Signature) =
        NestableDeclGroup(
            (firstSignature plus moreSignatures).map {
              NestableDecl(abstract, it)
            })
  }

  sealed class BodyElement<T>(open val cargo: T)
  internal class InvariantElement(req: Requirement) : BodyElement<Requirement>(req)
  internal class DefaultsElement(def: DefaultsDeclaration) : BodyElement<DefaultsDeclaration>(def)
  internal class EffectElement(eff: Effect) : BodyElement<Effect>(eff)
  internal class ActionElement(act: Action) : BodyElement<Action>(act)

  internal class NestedDeclGroup(declGroup: NestableDeclGroup) :
      BodyElement<NestableDeclGroup>(declGroup)

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
        val effects: Set<Effect> = body.getAllOfType<Effect, EffectElement>()

        val newDecl = signature.asClassDecl.copy(
            abstract = abstract,
            otherInvariants = body.getAllOfType<Requirement, InvariantElement>(),
            effectsRaw = effects + actionsToEffects(body.getAllOfType<Action, ActionElement>()),
            defaultsDeclaration = merge(body.getAllOfType<DefaultsDeclaration, DefaultsElement>()),
        )

        val thisDecl = NestableDecl(newDecl, false)
        val nestableGroups = body.getAllOfType<NestableDeclGroup, NestedDeclGroup>()
        val asSiblings = nestableGroups.flatMap { it.unnestAllFrom(signature.asClassDecl.name) }
        return thisDecl plus asSiblings
      }
    }  }

  object Bodies {
    private val gainOnlyDefaults: Parser<DefaultsDeclaration> =
        skipChar('+') and Types.genericType and Instructions.intensity map { (type, int) ->
          require(type.className == THIS)
          require(type.refinement == null)
          DefaultsDeclaration(gainOnlySpecs = type.specs, gainIntensity = int)
        }

    private val allCasesDefault: Parser<DefaultsDeclaration> = Types.genericType map {
      require(it.className == THIS)
      require(it.refinement == null)
      DefaultsDeclaration(universalSpecs = it.specs)
    }

    private val default: Parser<DefaultsDeclaration> =
        skip(_default) and (gainOnlyDefaults or allCasesDefault)

    private val invariant: Parser<Requirement> =
        skip(_has) and requirement

    val bodyElementNoClass: Parser<BodyElement<*>> =
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

  private val bodyElement: Parser<BodyElement<*>> =
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

internal data class NestableDecl(
    private val maybeCompleteDecl: ClassDeclaration,
    internal val complete: Boolean,
) {
  constructor(abstract: Boolean, signature: Signature) :
      this(signature.asClassDecl.copy(abstract = abstract), false)

  init {
    if (complete) maybeCompleteDecl.validate()
  }

  // This returns a new NestableDecl that looks like it could be a sibling to containingClass
  // instead of nested inside it
  fun unnestOneFrom(container: ClassName): NestableDecl {
    return when {
      // it's already complete, so just lift it up as-is
      complete -> this

      // if nested inside Component, there's no supertype to add
      container == COMPONENT -> this

      // the class name we'd insert is already there (TODO be even smarter)
      maybeCompleteDecl.supertypes.any { it.className == container } -> copy(complete = true)

      // jam the superclass in and mark it complete
      else -> copy(prependSuperclass(container), complete = true)
    }
  }

  private fun prependSuperclass(superclassName: ClassName): ClassDeclaration {
    val allSupertypes = superclassName.type plus maybeCompleteDecl.supertypes
    return maybeCompleteDecl.copy(supertypes = allSupertypes.toSetStrict())
  }

  internal fun finishAtTopLevel(): ClassDeclaration { // TODO
    val supes = maybeCompleteDecl.supertypes
    return when {
      maybeCompleteDecl.name == COMPONENT -> maybeCompleteDecl.also { require(supes.isEmpty()) }
      supes.isEmpty() -> maybeCompleteDecl.copy(supertypes = setOf(COMPONENT.type))
      else -> maybeCompleteDecl.also {
        require(COMPONENT.type !in supes) {
          "${maybeCompleteDecl.name}: ${maybeCompleteDecl.supertypes}"
        }
      }
    }
  }
}
