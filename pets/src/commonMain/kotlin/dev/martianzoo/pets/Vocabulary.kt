package dev.martianzoo.pets

import dev.martianzoo.data.GameEvent
import dev.martianzoo.data.GameEvent.ChangeEvent
import dev.martianzoo.data.GameEvent.ChangeEvent.StateChange
import dev.martianzoo.data.Ruleset
import dev.martianzoo.data.Task
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.PetNode

/** Session-specific ASCII class-name input and presentation policy. */
public class Vocabulary
private constructor(
    public val locale: String,
    private val displayNames: Map<ClassName, String>,
    private val petsNames: Map<ClassName, ClassName>,
    public val inputOnlySynonyms: Map<ClassName, ClassName>,
    private val inputNames: Map<ClassName, ClassName>,
) {
  /** Resolves a localized Pets name or input-only synonym to its stable canonical name. */
  public fun canonicalName(name: ClassName): ClassName = inputNames[name] ?: name

  /** Returns the localized natural-language name used in ordinary UI. */
  public fun displayName(name: ClassName): String = displayNames[name] ?: name.toString()

  /** Returns the localized identifier used when rendering Pets source. */
  public fun petsName(name: ClassName): ClassName = petsNames[name] ?: name

  /** Rewrites every recognized input name in [node] to its stable canonical name. */
  public fun <P : PetNode> canonicalize(node: P): P = canonicalizer.transform(node)

  /** Renders [node] as localized, parseable Pets source without emitting input-only synonyms. */
  public fun renderPets(node: PetNode): String = petsRenderer.transform(node).toString()

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
  public fun renderPets(event: GameEvent): String {
    val eventText =
        when (event) {
          is GameEvent.TaskAddedEvent -> renderTaskEvent(event, "+")
          is GameEvent.TaskRemovedEvent -> "${event.ordinal}: -Task${event.task.id}"
          is GameEvent.TaskEditedEvent -> renderTaskEvent(event, "+") + " FROM Task${event.task.id}"
          is ChangeEvent ->
              buildString {
                append("${event.ordinal}: ${renderPets(event.change)} BY ${event.actor}")
                append(
                    event.cause?.let {
                      " VIA ${renderPets(it.context)} BECAUSE ${it.triggerEvent}"
                    } ?: " (manual)"
                )
              }
        }
    return eventText + (event.comment?.let { " // $it" } ?: "")
  }

  /** Renders one pending task using localized Pets-compatible class names. */
  public fun renderPets(
      task: Task,
      queueAssignee: dev.martianzoo.data.Actor? = null,
      displayId: String = task.id.toString(),
  ): String = buildString {
    append(displayId)
    append(if (task.next) "* " else "  ")
    if (queueAssignee == null) {
      append("[${task.assignee}] ")
    } else {
      append("[queue: $queueAssignee, assignee: ${task.assignee}] ")
    }
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

    /** Builds one vocabulary from the names and language files in [ruleset]. */
    public fun create(
        ruleset: Ruleset,
        locale: String = ENGLISH,
        inputOnlySynonyms: Iterable<Pair<String, String>> = emptyList(),
    ): Vocabulary =
        create(
            canonicalNames = ruleset.allClassNames,
            displayNamesByLanguage = ruleset.displayNamesByLanguage,
            derivedPetsNameClassNames = ruleset.derivedPetsNameClassNames,
            locale = locale,
            inputOnlySynonyms = inputOnlySynonyms,
        )

    /** Builds a vocabulary directly; useful for clients assembling their own class catalog. */
    public fun create(
        canonicalNames: Set<ClassName>,
        displayNamesByLanguage: Map<String, Map<ClassName, String>>,
        derivedPetsNameClassNames: Set<ClassName> = canonicalNames,
        locale: String = ENGLISH,
        inputOnlySynonyms: Iterable<Pair<String, String>> = emptyList(),
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
          fallbackChain
              .firstNotNullOfOrNull { normalizedLanguages[it]?.get(canonical) }
              ?.let {
                require(it.isNotBlank()) { "Blank display name for $canonical" }
                requireAscii(it)
                put(canonical, it)
              }
        }
      }
      require(canonicalNames.containsAll(derivedPetsNameClassNames)) {
        "Pets-name derivation requested for unknown classes: ${derivedPetsNameClassNames - canonicalNames}"
      }
      val missingDisplayNames = derivedPetsNameClassNames - effectiveDisplayNames.keys
      require(missingDisplayNames.isEmpty()) {
        "No localized or English display name for structured classes: $missingDisplayNames"
      }
      val effectivePetsNames =
          effectiveDisplayNames
              .filterKeys { it in derivedPetsNameClassNames }
              .mapValues { (_, displayName) -> petsClassName(displayName) }

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
          .forEach { (synonym, canonical) ->
            register(synonym, canonical, "input-only synonym")
          }

      return Vocabulary(
          normalizedLocale,
          effectiveDisplayNames,
          effectivePetsNames,
          normalizedInputOnlySynonyms,
          inputOwners.filter { (input, canonical) -> input != canonical },
      )
    }

    /** Derives the sole Pets-compatible spelling allowed for the ASCII [displayName]. */
    public fun petsClassName(displayName: String): ClassName {
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
      return cn(result)
    }

    private fun requireAscii(value: String) {
      require(value.all { it.code < 128 }) { "Non-ASCII display name: $value" }
    }

    private fun classNameTransformer(transform: (ClassName) -> ClassName): PetTransformer =
        object : PetTransformer() {
          override fun <P : PetNode> transform(node: P): P {
            if (node is ClassName) {
              @Suppress("UNCHECKED_CAST")
              return transform(node) as P
            }
            return transformChildren(node)
          }
        }
  }
}
