import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import javax.imageio.ImageIO;

/** Generates the Playground's bundled icons from the Martian Zoo site's 30px PNGs. */
public class BuildPlaygroundIconAtlas {
  private static final int WIDTH = 1024;
  private static final int GAP = 2;

  private record Icon(String name, BufferedImage image, int x, int y) {}

  public static void main(String[] args) throws IOException {
    if (args.length != 1) {
      throw new IllegalArgumentException(
          "Usage: java scripts/BuildPlaygroundIconAtlas.java /path/to/site/images/classes/30px");
    }
    Path source = Path.of(args[0]);
    Path output = Path.of("modules/playground/resources/files/icons");
    List<Path> files;
    try (var entries = Files.list(source)) {
      files =
          entries.filter(path -> path.getFileName().toString().endsWith(".png"))
              .sorted(Comparator.comparing(path -> path.getFileName().toString()))
              .toList();
    }

    var icons = new java.util.ArrayList<Icon>();
    var index = new StringBuilder("name\tx\ty\twidth\theight\n");
    int x = GAP;
    int y = GAP;
    for (Path file : files) {
      BufferedImage image = ImageIO.read(file.toFile());
      if (image == null || image.getHeight() != 30 || image.getWidth() + 2 * GAP > WIDTH) {
        throw new IllegalArgumentException("Invalid 30px icon: " + file);
      }
      if (x + image.getWidth() + GAP > WIDTH) {
        x = GAP;
        y += 30 + GAP;
      }
      String name = file.getFileName().toString().replaceFirst("\\.png$", "");
      icons.add(new Icon(name, image, x, y));
      index.append(name).append('\t').append(x).append('\t').append(y).append('\t')
          .append(image.getWidth()).append('\t').append(image.getHeight()).append('\n');
      x += image.getWidth() + GAP;
    }

    int height = y + 30 + GAP;
    var atlas = new BufferedImage(WIDTH, height, BufferedImage.TYPE_INT_ARGB);
    Graphics2D graphics = atlas.createGraphics();
    for (Icon icon : icons) {
      graphics.drawImage(icon.image(), icon.x(), icon.y(), null);
    }
    graphics.dispose();
    Files.createDirectories(output);
    ImageIO.write(atlas, "png", output.resolve("atlas.png").toFile());
    Files.writeString(output.resolve("index.tsv"), index, StandardCharsets.UTF_8);
    System.out.printf("Packed %d icons into %dx%d atlas%n", icons.size(), WIDTH, height);
  }
}
