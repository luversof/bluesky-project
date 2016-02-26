(function(app) {
	app.AppComponent = ng.core.Component({
		selector : 'display',
		template : 'Hello World!'
	}).Class({
		constructor : function() {
		}
	});
})(window.app || (window.app = {}));