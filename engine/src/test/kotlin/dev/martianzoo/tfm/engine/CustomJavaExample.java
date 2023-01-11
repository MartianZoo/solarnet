package dev.martianzoo.tfm.engine;

import static com.google.common.collect.Iterables.getOnlyElement;
import static dev.martianzoo.tfm.api.HelpersKt.lookUpProductionLevels;
import static java.util.stream.Collectors.joining;

import dev.martianzoo.tfm.api.CustomInstruction;
import dev.martianzoo.tfm.api.ReadOnlyGameState;
import dev.martianzoo.tfm.pets.PetParser;
import dev.martianzoo.tfm.pets.ast.Instruction;
import dev.martianzoo.tfm.pets.ast.TypeExpression;
import java.util.Collections;
import java.util.List;

public class CustomJavaExample {

  public static class GainLowestProduction extends CustomInstruction {
    public GainLowestProduction() {
      super("gainLowestProduction");
    }

    @Override public Instruction translate(
        ReadOnlyGameState game, List<? extends TypeExpression> arguments) {
      var player = getOnlyElement(arguments);
      var prods = lookUpProductionLevels(game, player);
      int lowest = Collections.min(prods.values());
      String lowestProds = prods.keySet().stream()
          .filter(key -> prods.get(key) == lowest)
          .map(key -> key + "<" + player + ">")
          .collect(joining(" OR "));
      return PetParser.INSTANCE.parsePets(Instruction.class, lowestProds);
    }
  }
}
