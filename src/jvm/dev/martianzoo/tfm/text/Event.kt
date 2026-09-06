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
              subject = NounPhrase.text("you"),
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
      private val activeVerb: String? = null,
      private val passiveVerb: String? = null,
  ) {
    PLAY(activeVerb = "play", passiveVerb = "is played"),
    BUY(activeVerb = "buy"),
    USE_ACTION(activeVerb = "use"),
    PLACE(activeVerb = "place", passiveVerb = "is placed"),
    CREATE(activeVerb = "create", passiveVerb = "is created"),
    INCREASE_PRODUCTION(activeVerb = "increase"),
    RAISE(activeVerb = "raise", passiveVerb = "is raised"),
    ADD(activeVerb = "add"),
    ;

    fun verb(voice: Voice): String? =
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
    verb: String,
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
