package dev.martianzoo.data

import dev.martianzoo.pets.PetTransformer
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.PetNode

internal fun renamedAuthority(
    authority: Authority,
    aliases: Map<ClassName, ClassName>,
): Authority = if (aliases.isEmpty()) authority else RenamedAuthority(authority, aliases)

internal fun classNameRenamer(aliases: Map<ClassName, ClassName>): PetTransformer =
    object : PetTransformer() {
      override fun <P : PetNode> transform(node: P): P {
        if (node is ClassName) {
          @Suppress("UNCHECKED_CAST")
          return (aliases[node] ?: node) as P
        }
        return transformChildren(node)
      }
    }

private class RenamedAuthority(
    private val source: Authority,
    private val aliases: Map<ClassName, ClassName>,
) : Authority by source {
  private val rename = classNameRenamer(aliases)

  init {
    require(aliases.keys.all { it in source.allClassNames }) {
      "cannot rename unknown classes: ${aliases.keys - source.allClassNames}"
    }
    require(aliases.values.distinct().size == aliases.size) {
      "class-name aliases must have distinct destinations"
    }
    val untouchedNames = source.allClassNames - aliases.keys
    require(aliases.values.none { it in untouchedNames }) {
      "class-name alias collides with an existing class: ${aliases.values intersect untouchedNames}"
    }
  }

  override val allClassDeclarations: Map<ClassName, ClassDeclaration> by lazy {
    source.allClassDeclarations.values.associate { declaration ->
      val renamed = declaration.rename(rename)
      renamed.className to renamed
    }
  }

  override val allClassNames: Set<ClassName>
    get() = allClassDeclarations.keys

  override fun classDeclaration(name: ClassName): ClassDeclaration =
      allClassDeclarations[name]
          ?: throw IllegalArgumentException("no class declaration by name $name")

  override val explicitClassDeclarations: Set<ClassDeclaration> by lazy {
    source.explicitClassDeclarations.mapTo(linkedSetOf()) { it.rename(rename) }
  }

  override val bootstrapValidations by lazy {
    source.bootstrapValidations.map { alternatives ->
      alternatives.mapTo(linkedSetOf(), rename::transform)
    }
  }

  override val modules by lazy {
    source.modules
        .map { (name, selections) ->
          rename.transform(name) to
              selections.mapTo(linkedSetOf()) { selection ->
                selection.copy(
                    className = rename.transform(selection.className),
                    requirement = selection.requirement?.let(rename::transform),
                )
              }
        }
        .toMap()
  }

  override val displayNamesByLanguage by lazy {
    source.displayNamesByLanguage.mapValues { (_, names) ->
      names.mapKeys { (name) -> rename.transform(name) }
    }
  }

  override val derivedPetsNameClassNames by lazy {
    source.derivedPetsNameClassNames.mapTo(linkedSetOf(), rename::transform)
  }
}

private fun ClassDeclaration.rename(rename: PetTransformer): ClassDeclaration =
    copy(
        className = rename.transform(className),
        dependencies = dependencies.map(rename::transform),
        supertypes = supertypes.mapTo(linkedSetOf(), rename::transform),
        invariants = invariants.mapTo(linkedSetOf(), rename::transform),
        effects = effects.map(rename::transform),
        defaultsDeclaration =
            defaultsDeclaration.copy(
                universal =
                    defaultsDeclaration.universal.copy(
                        specs = defaultsDeclaration.universal.specs.map(rename::transform)
                    ),
                gainOnly =
                    defaultsDeclaration.gainOnly.copy(
                        specs = defaultsDeclaration.gainOnly.specs.map(rename::transform)
                    ),
                removeOnly =
                    defaultsDeclaration.removeOnly.copy(
                        specs = defaultsDeclaration.removeOnly.specs.map(rename::transform)
                    ),
                forClass = defaultsDeclaration.forClass?.let(rename::transform),
            ),
        extraNodes = extraNodes.mapTo(linkedSetOf(), rename::transform),
    )
