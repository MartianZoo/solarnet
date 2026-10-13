package dev.martianzoo.engine

/** An Agent command needs client information that has not yet been supplied. */
public class AgentInputRequiredException(message: String) : RuntimeException(message)
