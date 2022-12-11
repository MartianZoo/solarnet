# Overview

Petaform is not a programming language (which tells a computer how to *do* something). Nor is it a markup language like HTML or other common sort of computer language. It's a "specification language", which is a fancy term for *it lets us say what we mean*. That is, two parties who both know the rules of a specification language gain the ability to communicate about that language's topic area *with precision*.

When I started playing *Terraforming Mars*, I loved many things about the game, and above all its rich and integrated theme. But I was downright *fascinated* by the iconographic grammar it uses on cards, milestones, map areas and so forth. I started to see dozens of ways in which it serves very effectively as a specification language, and dozens of ways in which it falls short (none of my discussion of which should ever be taken as critical of the game developers, whom I'm nothing but impressed with).

I realized that a suitable specification language *could* exist -- and if it did, then both the iconographic depiction and a reasonable pass at the textual explanation on each card (etc.) could be *derived* from it. Moreover, an actual running game engine would in theory not need to be *programmed* how to handle each card individually; it could simply read the specification and know what to do.

Then I discovered that I was already hard at work on it! For example, the many hours I sunk into [Terraforming Mars: The Spreadsheet] were an effort to "regularize" the cards and create a uniform way of looking at them that adhered to a few rules.

Now (December 2022) I've been working on it for over two years. It's been my main pursuit outside of work. Some of the effort has been writing actual computer programs to *test* that the language is suitable in the ways I want it to be. But most of it has been simply grappling with the *design* of the thing -- so far it's been a generous fountain of absolutely wicked design puzzles!

## Game state

The topic of this language is queries and manipulations of a Terraforming Mars game state, so we'd better start by understanding what game state is.

I have a [long answer] to that, but the short version is: it's everything you need to know about a game-in-progress in order to fully reconstruct a *functionally equivalent* game-in-progress at another time or place. It's like a photograph of the game, but also needs to include invisible state like whose turn it is.

Petaform's conception of a *Terraforming Mars* game state goes like this:

### Components

What makes up a game state is *a bunch of things*. Countable things, called components.

Anything that has no effect on the game, like a greenery/city tile waiting in a bowl to be used, simply does not exist. To play a tile is *create* it.

Examples, in order of ascending weirdness:

   * An OceanTile sitting on that two-card spot on the main board
   * Each Microbe sitting on your Psychrophiles card
   * Each unit of TerraformRating or plant production you have
   * Each step the temperature track has been raised (where we say "it's -20 degrees out", Petaform says "five TemperatureStep components")
   * Each map Area on the board (even though it never changes); there's even an Area called "FloatingInSpace" for use by Stanford Torus)
   * Each Adjacency between two tiles on the board
   * Each VictoryPoint doled out in the endgame
   * A ColonyTile in play (playing the first animal card of the game *creates* the Miranda component)
   * Each Tag you have in play, but also, each distinct *type* of tag you have in play (because two cards, one milestone, and one global event need 'em to be)
   * When a UNMI or Pristar player gains a TR, they also gain an invisible "I have raised my TR this generation" component if they don't already have it
   * A card is two separate components: a CardBack and a CardFront, where both can't exist at the same time (the play-a-card mechanism converts one to the other)
   * **You, yourself, are a component**

(They do get a bit weirder than this, too.)

Much of the power of the language comes from the way it treats everything in such a homogenized way -- and the idea came straight from the existing icon grammar, which depicts "raise the temperature 1 step" and "gain a plant" in entirely similar ways.

### Game state

A game state is nothing but a set of components -- actually a "multiset", since there can be multiple indistinguishable elements, like your Steel resources.

A change to that state is nothing but adding components, removing components, or transmuting some components into an equal number of other components.

(Well, this is almost true. There are also task queues, which keep track of what the game is waiting on each player to do. But this is a topic for later.)


