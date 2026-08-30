package dev.martianzoo.pets

import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.Instruction
import dev.martianzoo.pets.ast.InstructionTree
import dev.martianzoo.pets.ast.PetNode
import dev.martianzoo.pets.data.Actor
import dev.martianzoo.pets.data.Catalog
import dev.martianzoo.pets.data.GameEvent
import dev.martianzoo.pets.data.GameEvent.ChangeEvent
import dev.martianzoo.pets.data.GameEvent.ChangeEvent.StateChange
import dev.martianzoo.pets.data.Task

/** Session-specific ASCII class-name input and presentation policy. */
public class Vocabulary
private constructor(
    public val locale: String,
    private val displayNames: Map<ClassName, String>,
    private val petsNames: Map<ClassName, ClassName>,
    private val inputOnlySynonyms: Map<ClassName, ClassName>,
    private val inputNames: Map<ClassName, ClassName>,
) {
  /** Resolves a localized Pets name or input-only synonym to its stable canonical name. */
  public fun canonicalName(name: ClassName): ClassName = inputNames[name] ?: name

  /** Returns the localized natural-language name used in ordinary UI. */
  public fun displayName(name: ClassName): String =
      displayNames[name] ?: defaultEnglishDisplayName(name)

  /** Returns the localized identifier used when rendering Pets source. */
  public fun petsName(name: ClassName): ClassName = petsNames[name] ?: name

  /** Returns the localized Pets name for [actor]. */
  public fun petsName(actor: Actor): ClassName = petsName(actor.className)

  /** Rewrites every recognized input name in [node] to its stable canonical name. */
  public fun canonicalize(node: Expression): Expression = canonicalizer.transformExpression(node)

  /** Rewrites every recognized input name while preserving one task-shaped instruction. */
  public fun canonicalize(node: Instruction): Instruction = canonicalizer.transformInstruction(node)

  /** Rewrites every recognized input name in a possibly grouped instruction tree. */
  public fun canonicalize(node: InstructionTree): InstructionTree =
      canonicalizer.transformInstructionTree(node)

  /** Renders [node] as localized, parseable Pets source without emitting input-only synonyms. */
  public fun renderPets(node: PetNode): String =
      petsRenderer.transformWithoutKindCheck(node).toString()

  /** Renders an engine state change using localized Pets-compatible class names. */
  public fun renderPets(change: StateChange): String {
    val count = if (change.count == 1) "" else "${change.count} "
    return when (val gaining = change.gaining) {
      null -> "-$count${renderPets(change.removing!!)}"
      else ->
          "+$count${renderPets(gaining)}" +
              (change.removing?.let { " FROM ${renderPets(it)}" } ?: "")
    }
  }

  /** Renders one event for a Pets-oriented event log. */
  public fun renderPets(event: GameEvent): String =
      when (event) {
        is GameEvent.TaskAddedEvent -> renderTaskEvent(event, "+") + event.agentSuffix()
        is GameEvent.TaskRemovedEvent ->
            "${event.ordinal}: -Task${event.task.id}" + event.agentSuffix()
        is GameEvent.TaskEditedEvent ->
            renderTaskEvent(event, "+") + " FROM Task${event.task.id}" + event.agentSuffix()
        is GameEvent.GameplayInputEvent -> event.toString()
        is ChangeEvent ->
            buildString {
              append("${event.ordinal}: ${renderPets(event.change)} BY ${petsName(event.actor)}")
              append(
                  event.cause?.let { " VIA ${renderPets(it.context)} BECAUSE ${it.triggerEvent}" }
                      ?: " (manual)"
              )
              append(event.agentSuffix())
            }
      }

  /** Renders one pending task using localized Pets-compatible class names. */
  public fun renderPets(
      task: Task,
      displayId: String? = task.id.toString(),
  ): String = buildString {
    if (displayId != null) append(displayId)
    append(if (task.selected) "* " else if (displayId != null) "  " else "")
    append("[${petsName(task.assignee)}] ")
    append(renderPets(task.instruction))
    task.then?.let { append(" (THEN ${renderPets(it)})") }
    task.whyPending?.let { append(" ($it)") }
  }

  private fun renderTaskEvent(event: GameEvent.TaskEvent, sign: String): String = buildString {
    append("${event.ordinal}: ${sign}Task${event.task.id} { ")
    append(renderPets(event.task.instruction))
    event.task.then?.let { append(" THEN ${renderPets(it)}") }
    append(" }")
    event.task.whyPending?.let { append(" ($it)") }
  }

  private val canonicalizer: PetTransformer = classNameTransformer(::canonicalName)
  private val petsRenderer: PetTransformer = classNameTransformer(::petsName)

  public companion object {
    public const val ENGLISH: String = "en"

    /** Builds one vocabulary from the names and language files in [catalog]. */
    public fun create(
        catalog: Catalog,
        locale: String = ENGLISH,
        inputOnlySynonyms: Iterable<Pair<String, String>> = emptyList(),
        activeClassNames: Set<ClassName>,
        petsNameAliases: Map<ClassName, ClassName> = emptyMap(),
    ): Vocabulary {
      val canonicalByAlias =
          petsNameAliases.entries.associate { (canonical, alias) -> alias to canonical }
      return create(
          canonicalNames = catalog.allClassNames,
          displayNamesByLanguage = catalog.displayNamesByLanguage,
          derivedPetsNameClassNames = catalog.derivedPetsNameClassNames intersect activeClassNames,
          locale = locale,
          inputOnlySynonyms =
              inputOnlySynonyms.map { (synonym, target) ->
                synonym to (canonicalByAlias[cn(target)] ?: cn(target)).toString()
              },
          petsNameAliases = petsNameAliases,
      )
    }

    /** Builds a vocabulary directly; useful for clients assembling their own class catalog. */
    public fun create(
        canonicalNames: Set<ClassName>,
        displayNamesByLanguage: Map<String, Map<ClassName, String>>,
        derivedPetsNameClassNames: Set<ClassName> = canonicalNames,
        locale: String = ENGLISH,
        inputOnlySynonyms: Iterable<Pair<String, String>> = emptyList(),
        petsNameAliases: Map<ClassName, ClassName> = emptyMap(),
    ): Vocabulary {
      val normalizedInputOnlySynonymPairs = inputOnlySynonyms.map { (synonym, canonical) ->
        cn(synonym) to cn(canonical)
      }
      require(
          normalizedInputOnlySynonymPairs.map { it.first }.distinct().size ==
              normalizedInputOnlySynonymPairs.size
      ) {
        "Duplicate class synonym"
      }
      val normalizedInputOnlySynonyms = normalizedInputOnlySynonymPairs.toMap()
      val normalizedLocale = locale.replace('_', '-').lowercase()
      val normalizedLanguages = displayNamesByLanguage.mapKeys { (language) ->
        language.replace('_', '-').lowercase()
      }
      val fallbackChain =
          generateSequence(normalizedLocale) { current ->
                current.substringBeforeLast('-', missingDelimiterValue = "").ifEmpty { null }
              }
              .toList() + ENGLISH
      val effectiveDisplayNames = buildMap {
        canonicalNames.forEach { canonical ->
          val displayName =
              fallbackChain.firstNotNullOfOrNull { normalizedLanguages[it]?.get(canonical) }
                  ?: defaultEnglishDisplayName(canonical)
          require(displayName.isNotBlank()) { "Blank display name for $canonical" }
          requireAscii(displayName)
          put(canonical, displayName)
        }
        petsNameAliases.forEach { (canonical, petsName) ->
          require(canonical in canonicalNames) {
            "Pets-name alias $petsName targets unknown class $canonical"
          }
          put(canonical, petsName.toString())
        }
      }
      require(canonicalNames.containsAll(derivedPetsNameClassNames)) {
        "Pets-name derivation requested for unknown classes: ${derivedPetsNameClassNames - canonicalNames}"
      }
      val effectivePetsNames = buildMap {
        effectiveDisplayNames
            .filterKeys { it in derivedPetsNameClassNames }
            .mapValuesTo(this) { (canonicalName, displayName) ->
              if (normalizedLocale.substringBefore('-') == ENGLISH) {
                canonicalName
              } else {
                val localizedName = petsClassName(displayName)
                if (localizedName in canonicalNames && localizedName != canonicalName) {
                  canonicalName
                } else {
                  localizedName
                }
              }
            }
        putAll(petsNameAliases)
      }

      val inputOwners = mutableMapOf<ClassName, ClassName>()
      fun register(
          input: ClassName,
          canonical: ClassName,
          kind: String,
      ) {
        require(canonical in canonicalNames) { "$kind $input targets unknown class $canonical" }
        val existing = inputOwners[input]
        require(existing == null || existing == canonical) {
          "Class-name collision for $input: $existing and $canonical"
        }
        if (input in canonicalNames) {
          require(input == canonical) {
            "Class-name collision for $input: canonical name and alternate for $canonical"
          }
        }
        inputOwners[input] = canonical
      }

      canonicalNames.forEach { register(it, it, "canonical name") }
      effectivePetsNames.forEach { (canonical, petsName) ->
        register(petsName, canonical, "localized Pets name")
      }
      normalizedInputOnlySynonyms
          .filterValues { it in canonicalNames }
          .forEach { (synonym, canonical) -> register(synonym, canonical, "input-only synonym") }

      return Vocabulary(
          normalizedLocale,
          effectiveDisplayNames,
          effectivePetsNames,
          normalizedInputOnlySynonyms,
          inputOwners.filter { (input, canonical) -> input != canonical },
      )
    }

    /** Derives ordinary English display text by separating the words in [className]. */
    public fun defaultEnglishDisplayName(className: ClassName): String {
      val source = className.toString()
      return buildString {
        source.forEachIndexed { index, character ->
          val previous = source.getOrNull(index - 1)
          val startsWord =
              index > 0 &&
                  character != '_' &&
                  previous != '_' &&
                  ((character.isUpperCase() && previous?.isLowerCase() == true) ||
                      (character.isDigit() && previous?.isDigit() == false) ||
                      (!character.isDigit() && previous?.isDigit() == true))
          if ((character == '_' || startsWord) && lastOrNull() != ' ') append(' ')
          if (character != '_') append(character)
        }
      }
    }

    /** Derives the sole Pets-compatible spelling allowed for the ASCII [displayName]. */
    internal fun petsClassName(displayName: String): ClassName {
      requireAscii(displayName)
      var atWordStart = true
      var wordStartedUppercase = false
      var previousWasLowercase = false
      val result = buildString {
        displayName.forEach { character ->
          val isUppercase = character in 'A'..'Z'
          val isLowercase = character in 'a'..'z'
          when {
            character == '\'' -> Unit
            isUppercase || isLowercase || character in '0'..'9' -> {
              if (!atWordStart && wordStartedUppercase && previousWasLowercase && isUppercase) {
                atWordStart = true
              }
              if (atWordStart) {
                wordStartedUppercase = isUppercase
              }
              append(if (atWordStart) character.uppercaseChar() else character.lowercaseChar())
              atWordStart = false
              previousWasLowercase = isLowercase
            }
            else -> {
              atWordStart = true
              previousWasLowercase = false
            }
          }
        }
      }
      require(result.isNotEmpty()) { "Display name has no identifier characters: $displayName" }
      val validResult =
          if (result.length > 6 && result[1] !in 'a'..'z' && result[1] != '_') {
            result.substring(0, 1) + "_" + result.substring(1)
          } else {
            result
          }
      return cn(validResult)
    }

    private fun requireAscii(value: String) {
      require(value.all { it.code < 128 }) { "Non-ASCII display name: $value" }
    }

    private fun classNameTransformer(transform: (ClassName) -> ClassName): PetTransformer =
        object : PetTransformer() {
          override fun transformNode(node: PetNode): PetNode {
            if (node is ClassName) {
              return transform(node)
            }
            return transformChildren(node)
          }
        }
  }
}
