package dev.martianzoo.agent

/**
 * Signals that an Agent call needs information from its client before it can finish. For example, a
 * card movement may need externally supplied card identities. This is not a game-rule rejection or
 * a promise that pending Admin work may be left for a Player to resolve.
 *
 * Admin retries after other Admin progress. If its pass stalls, this signal propagates before
 * Player work; the enclosing call restores its game changes so it can be retried with the input.
 *
 * @constructor Creates a missing-input signal with the caller-facing [message]. Subclasses may
 *   carry the specific information the client must supply; constructing it changes no game state.
 */
public open class AgentInputRequiredException(message: String) : RuntimeException(message)
