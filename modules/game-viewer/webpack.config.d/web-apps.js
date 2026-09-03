const path = require("path");

if (process.env.WEBPACK_SERVE) {
  const apps = [
    {
      route: "gameviewer",
      title: "Solarnet Game Viewer",
      entry: path.resolve(__dirname, "kotlin/solarnet-game-viewer.js"),
      bundle: "game-viewer.js",
      resources: path.resolve(__dirname, "kotlin"),
    },
    {
      route: "webrepl",
      title: "REgo PLastics Web REPL",
      entry: path.resolve(__dirname, "../solarnet-web/kotlin/solarnet-web.js"),
      bundle: "web.js",
      resources: path.resolve(__dirname, "../solarnet-web/kotlin"),
    },
  ];

  config.entry = Object.fromEntries(apps.map((app) => [app.route, app.entry]));
  config.output.filename = ({ chunk }) => {
    const app = apps.find(({ route }) => route === chunk.name);
    return app ? `${app.route}/${app.bundle}` : "[name].js";
  };

  config.devServer.open = false;
  config.devServer.allowedHosts = ["newazure.local"];
  config.devServer.static = apps.map((app) => ({
    directory: app.resources,
    publicPath: `/${app.route}`,
    watch: false,
  }));

  const appDirectory = `<!doctype html>
<html lang="en">
  <head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <meta name="color-scheme" content="dark light">
    <title>Solarnet Apps</title>
    <style>
      body { font: 1rem system-ui, sans-serif; max-width: 42rem; margin: 4rem auto; padding: 0 1.5rem; }
      h1 { font-size: 1.5rem; }
      li { margin: 0.75rem 0; }
    </style>
  </head>
  <body>
    <main>
      <h1>Solarnet Apps</h1>
      <ul>
        ${apps.map((app) => `<li><a href="/${app.route}/">${app.title}</a></li>`).join("\n        ")}
      </ul>
    </main>
  </body>
</html>`;

  config.devServer.setupMiddlewares = (middlewares, devServer) => {
    devServer.app.get("/", (_request, response) => {
      response.type("html").send(appDirectory);
    });
    for (const app of apps) {
      devServer.app.get(new RegExp(`^/${app.route}$`), (_request, response) => {
        response.redirect(308, `/${app.route}/`);
      });
    }
    return middlewares;
  };
}
