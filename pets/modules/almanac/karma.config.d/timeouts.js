// Canon loading and elaboration run inside the browser while checking the query examples.
config.client.mocha = Object.assign({}, config.client.mocha, { timeout: 60000 });
config.browserNoActivityTimeout = 90000;
// Synchronous Canon queries can block browser socket pings on loaded CI runners.
config.pingTimeout = 60000;
config.browserDisconnectTimeout = 10000;
