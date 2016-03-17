(function(app) {
  app.AppComponent =
    ng.core.Component({
      selector: 'display',
      template: '<h1>TEST</h1>'
    })
    .Class({
      constructor: function() {}
    });
})(window.app || (window.app = {}));