$(document).ready(function() {

	$.BookkeepingView = Backbone.View.extend({
		el : "<tr>",	//기본은 div
		template : $("#template-bookkeeping-view").html(),
		events : {
			"click [data-menu=updateBookkeeping]" : "updateBookkeeping",
			"click [data-menu=deleteBookkeeping]" : "removeBookkeeping",
			"keyup [data-key=name]" : "changeNameKeyUp",
			"keypress [data-key=name]" : "changeNameKeyPress"
		},
		initialize : function() {
			this.listenTo(this.model, 'change', this.render);
			this.listenTo(this.model, 'destroy', this.remove);
		},
		render : function() {
			this.$el.html(Mustache.render(this.template, this.model.toJSON()));
			this.$el.find("[data-menu=updateBookkeeping]").hide();
			return this;
		},
		updateBookkeeping : function() {
			this.model.save({name : this.$el.find("[data-key=name]").text()});
			this.$el.find("[data-menu=updateBookkeeping]").hide(100);
		},
		removeBookkeeping : function() {
			this.model.destroy();
		},
		changeNameKeyUp : function(event) {
			if (this.$el.find("[data-key=name]").text() == this.model.get("name")) {
				this.$el.find("[data-menu=updateBookkeeping]").hide(100);
			} else {
				this.$el.find("[data-menu=updateBookkeeping]").show(100);
			}
			if (event.keyCode == 13) {
				this.updateBookkeeping();
			}
		},
		// enter 입력 처리 방지
		changeNameKeyPress : function(event) {
			return event.keyCode != 13;
		}
	});


	$.BookkeepingCollectionView = Backbone.View.extend({
		el : "#bookkeepingArea",
		template : $("#template-bookkeeping-list").html(),
		events : {
			"click [data-menu=createBookkeeping]" : "createBookkeeping",
			"keyup [data-key-name=createBookkeepingName]" : "createNameKeyUp",
			"keypress [data-key-name=createBookkeepingName]" : "createNameKeyPress"
		},
		initialize : function() {
			//console.log("This collection view has been initialized.");
			this.$el.html(Mustache.render(this.template));
			
			this.collection = new $.BookkeepingCollection();
			this.collection.fetch({reset : true});
			
			this.listenTo(this.collection, "reset", this.render);
			this.listenTo(this.collection, "add", this.renderBookkeeping);
		},
		render : function() {
			this.collection.each(function(bookkeeping) {
				var bookkeepingView = new $.BookkeepingView({model : bookkeeping});
				this.$el.find("table tbody").append(bookkeepingView.render().el);
			}, this);
		},
		renderBookkeeping : function(bookkeeping) {
			var bookkeepingView = new $.BookkeepingView({model : bookkeeping});
			this.$el.find("table tbody").append(bookkeepingView.render().el);
		},
		createBookkeeping : function(event) {
			event.preventDefault();
			this.collection.create({name : $("[data-key-name=createBookkeepingName]").text()});
			$(event.target).closest("tr")
			.find("[contenteditable=true]").text("");
		},
		createNameKeyUp : function(event) {
			if (event.keyCode === 13) {
				this.createBookkeeping(event);
			}
		},
		createNameKeyPress : function(event) {
			return event.keyCode !== 13;
		}
	});

	$.bookkeepingCollectionView = new $.BookkeepingCollectionView();
});