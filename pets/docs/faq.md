# Pets FAQ

Engine goals, gameplay, and the REPL are covered by the [Solarnet FAQ](../../docs/faq.md).

### Could I add my own fan cards?

That's part of the idea, for sure! However, a couple caveats:

* There's no user-facing provision for bringing fan cards into the system, so for now you would fork the project and edit a bundle's `cards.json5` file. The build generates the corresponding Pets declarations. We can talk about a better way to do it, for sure.
* This will work fine if your fan cards remix existing game mechanics. Further-out behavior may
  need a bounded custom instruction, metric, or class after a genuine attempt to express it in
  ordinary Pets. A new general feature is not justified merely because one published or fan card
  needs it.

### Can we please improve the error messages? They're almost mocking me.

Yes. A parser's responsibility is not merely to accept correct Pets; it should precisely highlight
why incorrect Pets is incorrect, especially for commonly encountered mistakes. I value that highly,
but it is not a current program of work.
