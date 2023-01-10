window.onload = function() {
	//<editor-fold desc="Changeable Configuration Block">

	// the following lines will be replaced by docker/configurator, when it runs in a docker-container
	window.ui = SwaggerUIBundle({
		urls: [
			{ "name": "bluesky-api-gate", "url": "http://gate.api.bluesky.local/v3/api-docs" },
			{ "name": "bluesky-api-bookkeeping", "url": "http://bookkeeping.api.bluesky.local/v3/api-docs" },
			{ "name": "bluesky-api-user", "url": "http://user.api.bluesky.local/v3/api-docs" }
		],
		operationsSorter: "alpha",
		withCredentials: true,
		queryConfigEnabled: true,
		dom_id: '#swagger-ui',
		deepLinking: true,
		presets: [
			SwaggerUIBundle.presets.apis,
			SwaggerUIStandalonePreset
		],
		plugins: [
			SwaggerUIBundle.plugins.DownloadUrl
		],
		layout: "StandaloneLayout"
	});

	//</editor-fold>
};
