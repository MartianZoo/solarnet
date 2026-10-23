package dev.martianzoo.state

/** Why a transaction executed when it did, separately from what authorized its work. */
public enum class Motivation {
  /** A client submitted this work for its Actor. */
  CLIENT_REQUEST,

  /** Admin's eager handling policy performed this work. */
  ADMIN_EAGER_POLICY,

  /** The Actor's own configured handling policy performed this work. */
  PLAYER_POLICY,
}
