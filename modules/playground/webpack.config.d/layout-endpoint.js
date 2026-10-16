// Write-only review snapshots of adjustments made in the mockup.
// The app always starts from its code-defined layout.
config.plugins.push(new (require("webpack")).DefinePlugin({
  PLAYGROUND_LAYOUT_ENABLED: JSON.stringify(!!process.env.WEBPACK_SERVE),
}));

if (process.env.WEBPACK_SERVE) {
  const fs = require("fs");
  const path = require("path");
  const argument = process.argv.find((arg) => arg.startsWith("playgroundLayout="));
  const layoutFile = argument && argument.substring("playgroundLayout=".length);
  if (!layoutFile) throw new Error("playgroundLayout argument missing from webpack command line");

  config.devServer = config.devServer || {};
  config.devServer.open = false;
  // A build must not discard an in-progress live game. Updates are picked up on manual refresh.
  config.devServer.hot = false;
  config.devServer.liveReload = false;
  config.devServer.client = false;
  config.devServer.webSocketServer = false;
  // Its own port, so it can run beside the viewer's development server on 8080.
  config.devServer.port = 8086;
  config.devServer.setupMiddlewares = (middlewares, devServer) => {
    const app = devServer.app;
    fs.mkdirSync(path.dirname(layoutFile), { recursive: true });
    app.put("/layout.json", (request, response) => {
      let body = "";
      request.setEncoding("utf8");
      request.on("data", (chunk) => (body += chunk));
      request.on("end", () => {
        fs.writeFileSync(layoutFile, body);
        response.status(204).end();
      });
    });
    return middlewares;
  };
}
