// Canon loading and elaboration run inside the browser while checking the query examples.
config.client.mocha = Object.assign({}, config.client.mocha, { timeout: 60000 });
config.browserNoActivityTimeout = 90000;
