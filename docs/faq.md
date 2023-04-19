# FAQ

### Why no Colonies?

It actually looks surprisingly easy to add. But we have bigger problems.

### Why no Turmoil?

It'll be a pain in the ass to add. But, I think, totally possible.

### Could this engine be used for other games?

Maybe, but it probably won't be good.

It's true that the game engine itself basically doesn't know anything about plants, city tiles, action cards, etc. All that comes from `components.pets` and `player.pets`. There is a lot of custom code for converting data in the .json files into class declarations, but you just wouldn't use that.

However, everything is designed toward TfM's peculiarities. The deep mechanical nature of the game. I would expect many games would feel shoehorned in.

The perfect candidate would be a game that relies heavily on triggered effects and... counting things.

However, I really wouldn't recommend trying this now. Maybe in 2024.

### Why is using the REPL such a pain in the ass?

You're speaking directly to the engine API, and the engine is very low-level. It doesn't really care about being easy to use.

### Why do I see code doing things in such painfully slow ways?

Right now, as long as I can type stuff into the REPL and not be annoyed at the slowness of the response, there's no problem to solve. I also care a lot about keeping the code as easy to understand as possible, since the whole thing is so damned complicated by nature.

### Could I add my own fan cards?

That's part of the idea, for sure! However, a couple caveats:

* There's no actual provision for how to bring fan cards into the system, so for now you would just fork the project and edit the cards.json file. We can talk about a better way to do it, for sure.
* If your fan card mixes existing game mechanics in different ways you'll probably be fine, but if it does things far enough out of the ordinary that the engine/language doesn't support it, I'm not going to be inclined to add the features you want. It's so much more important to focus on getting the published cards working.

### If I want to help?

First... that would be amazing. I hope you'd feel welcome to get involved.

If you're going to fork and edit, I'd suggest talking to me about it, because I'm still doing some massive refactorings and not paying much attention to doing anything carefully. So you could have a bad experience.

### Can we please improve the error messages? They're almost mocking me.

Yeah. The more interest I hear in people messing around with this thing, the more effort I'll put into those error messages.

### What do the FryxFolk think of this project?

Don't know yet.

### How do you even know you're getting the rules right?

Well, right now we aren't. And I plan to keep a list of rules we'll *never* get right, although I really want to keep it small and limited to small discrepancies.

Anyway though, I've spent 3 years of my life studying the HELL out of the game rules. I'm almost certainly the only person on the planet whose ratio of studying the game vs. *playing* the game is so out of whack that my set of physical cards I keep at home is literally alphabetized.

### Why do I see bad code in the project?

Trying to build something complicated by yourself, it's just impossible to keep the code quality high without grinding to a halt. Having to share coding efforts with others would make me up my game.

### Why is it in Kotlin?

Several reasons

* I needed to learn it for my job
* It interoperates well with Java, Javascript, and other things
* It's an awesome language
* IntelliJ is an incredible product

