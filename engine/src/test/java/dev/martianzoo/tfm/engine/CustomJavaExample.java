package dev.martianzoo.tfm.engine;

import static com.google.common.collect.Iterables.getOnlyElement;
import static java.util.stream.Collectors.joining;

import dev.martianzoo.tfm.api.CustomInstruction;
import dev.martianzoo.tfm.api.ReadOnlyGameState;
import dev.martianzoo.tfm.api.ResourceUtils;
import dev.martianzoo.tfm.api.Type;
import dev.martianzoo.tfm.pets.Parsing;
import dev.martianzoo.tfm.pets.ast.Instruction;
import java.util.Collections;
import java.util.List;

public class CustomJavaExample {

  public static class GainLowestProduction extends CustomInstruction {

    public GainLowestProduction() {
      super("gainLowestProduction");
    }

    @Override
    public Instruction translate( // TODO null annotations
        ReadOnlyGameState game, List<? extends Type> arguments) {
      var player = getOnlyElement(arguments).getTypeExpr();
      var prods = ResourceUtils.INSTANCE.lookUpProductionLevels(game, player);
      int lowest = Collections.min(prods.values());
      String lowestProds = prods.keySet().stream()
          .filter(key -> prods.get(key) == lowest)
          .map(key -> key + "<" + player + ">")
          .collect(joining(" OR "));
      return Parsing.INSTANCE.parseElement(Instruction.class, lowestProds);
    }
  }
}
