package dev.martianzoo.tfm.text

internal data class Event(
    val kind: Kind,
    val actorConstraint: ActorConstraint,
    val objectPhrase: NounPhrase,
    val complements: List<Modifier> = emptyList(),
) {
  fun renderTrigger(): Clause.Simple? {
    val voice =
        when (actorConstraint) {
          ActorConstraint.YOU -> Voice.ACTIVE
          ActorConstraint.UNRESTRICTED -> Voice.PASSIVE
        }
    val verb = kind.verb(voice) ?: return null
    return when (voice) {
      Voice.ACTIVE ->
          eventTrigger(
              subject = NounPhrase.you(),
              verb = verb,
              objectPhrase = objectPhrase,
              modifiers = complements,
          )
      Voice.PASSIVE ->
          eventTrigger(
              subject = objectPhrase,
              verb = verb,
              modifiers = complements,
          )
    }
  }

  enum class ActorConstraint {
    YOU,
    UNRESTRICTED,
  }

  enum class Kind(
      private val activeVerb: Verb? = null,
      private val passiveVerb: Verb? = null,
  ) {
    PLAY(activeVerb = Verb("plays", "play"), passiveVerb = Verb("is played", "are played")),
    BUY(activeVerb = Verb("buys", "buy")),
    USE_ACTION(activeVerb = Verb("uses", "use")),
    PLACE(activeVerb = Verb("places", "place"), passiveVerb = Verb("is placed", "are placed")),
    CREATE(
        activeVerb = Verb("creates", "create"),
        passiveVerb = Verb("is created", "are created"),
    ),
    INCREASE_PRODUCTION(activeVerb = Verb("increases", "increase")),
    RAISE(activeVerb = Verb("raises", "raise"), passiveVerb = Verb("is raised", "are raised")),
    ADD(activeVerb = Verb("adds", "add")),
    ;

    fun verb(voice: Voice): Verb? =
        when (voice) {
          Voice.ACTIVE -> activeVerb
          Voice.PASSIVE -> passiveVerb
        }
  }

  enum class Voice {
    ACTIVE,
    PASSIVE,
  }
}

internal fun eventTrigger(
    subject: NounPhrase,
    verb: Verb,
    objectPhrase: NounPhrase? = null,
    modifiers: List<Modifier> = emptyList(),
): Clause.Simple =
    Clause.Simple(
        predicate =
            Predicate(
                verb,
                objectPhrase?.let { Coordination.one(it) },
                modifiers,
            ),
        subject = subject,
    )
