package dev.martianzoo.pets

import dev.martianzoo.pets.ClassBody.Element.ActionElement
import dev.martianzoo.pets.ClassBody.Element.DefaultsElement
import dev.martianzoo.pets.ClassBody.Element.EffectElement
import dev.martianzoo.pets.ClassBody.Element.InvariantElement
import dev.martianzoo.pets.ClassBody.Element.NestedDeclaration
import dev.martianzoo.pets.ClassBody.Element.PropertyElement
import dev.martianzoo.pets.Transforming.actionSelectors
import dev.martianzoo.pets.api.Exceptions.PetSyntaxException
import dev.martianzoo.pets.ast.Action
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.Effect
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.PropertyName
import dev.martianzoo.pets.ast.PropertyValue
import dev.martianzoo.pets.ast.Requirement
import dev.martianzoo.pets.data.ClassDeclaration
import dev.martianzoo.pets.data.ClassDeclaration.ClassKind.CONCRETE
import dev.martianzoo.pets.data.ClassDeclaration.DefaultsDeclaration

/**
 * An authored class body, before its enclosing declaration supplies a name and signature.
 *
 * Members of each kind retain source order; duplicate property assignments are rejected.
 * Declaration construction checks invariants and defaults and attaches nested declarations to their
 * immediate container. Owner-local expressions carry this body until extraction supplies their
 * generated class name; scope resolution follows extraction.
 */
internal class ClassBody(elements: List<Element> = emptyList()) {
  private val invariants = elements.filterIsInstance<InvariantElement>().map { it.invariant }
  private val defaults = elements.filterIsInstance<DefaultsElement>().map { it.defaults }
  private val effects = elements.filterIsInstance<EffectElement>().map { it.effect }
  private val actions = elements.filterIsInstance<ActionElement>().map { it.action }
  // Rule L11-9: reject a name assigned twice in one body, so declaration order
  // never becomes an accidental override rule.
  private val properties = buildMap {
    elements.filterIsInstance<PropertyElement>().forEach { element ->
      val (name, value) = element.property
      if (name in this)
          throw PetSyntaxException(
              "property `$name` is assigned twice: `${get(name)}` and `$value`",
              sourceLocation = name.sourceLocation,
          )
      put(name, value)
    }
  }
  private val nestedDeclarations = elements.filterIsInstance<NestedDeclaration>()

  /** Builds the concrete declaration of an owner-local class. */
  public fun asDerivedDeclaration(className: ClassName, supertype: Expression): ClassDeclaration =
      toDeclarations(ClassDeclaration(className, CONCRETE, supertypes = setOf(supertype))).single()

  /** Container first, followed by its descendants in source order. */
  public fun toDeclarations(
      header: ClassDeclaration,
      docstring: String? = null,
  ): List<ClassDeclaration> {
    val mergedDefaults = DefaultsDeclaration.merge(defaults)
    val declaration =
        header.copy(
            invariants =
                buildSet {
                  invariants.forEach { invariant ->
                    if (!add(invariant))
                        throw PetSyntaxException(
                            "duplicate invariant `HAS $invariant` on `${header.className}`",
                            sourceLocation = invariant.sourceLocation,
                        )
                  }
                },
            authoredEffects = effects,
            authoredActions = actions,
            defaultsDeclaration = mergedDefaults,
            properties = properties,
            extraNodes = actionSelectors(actions),
            docstring = docstring,
        )
    return buildList {
      add(declaration)
      nestedDeclarations.forEach { nested ->
        val child = nested.declarations.first()
        // L11-6: attach only the immediate child to this container. Its descendants already
        // name their own containers; preserve an explicitly supplied parent specialization.
        add(
            if (child.supertypes.any { it.className == header.className }) child
            else child.copy(supertypes = setOf(header.className.expression) + child.supertypes)
        )
        addAll(nested.declarations.drop(1))
      }
    }
  }

  /** One declaration-body member. */
  public sealed class Element {
    public class InvariantElement(public val invariant: Requirement) : Element()

    public class DefaultsElement(public val defaults: DefaultsDeclaration) : Element()

    public class PropertyElement(public val property: Pair<PropertyName, PropertyValue>) : Element()

    public class EffectElement(public val effect: Effect) : Element()

    public class ActionElement(public val action: Action) : Element()

    public class NestedDeclaration(public val declarations: List<ClassDeclaration>) : Element()
  }
}
