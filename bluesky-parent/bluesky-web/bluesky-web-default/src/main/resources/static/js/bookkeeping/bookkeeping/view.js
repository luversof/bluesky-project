$(document).ready(function() {

	$.BookkeepingView = Backbone.View.extend({
		el : "<tr>",	//기본은 div
		template : $("#template-bookkeeping-view").html(),
		events : {
			"click [data-menu=updateBookkeeping]" : "updateBookkeeping",
			"click [data-menu=deleteBookkeeping]" : "deleteBookkeeping",
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
		deleteBookkeeping : function() {
			this.model.destroy();
		},
		isChange : function() {
			if (this.$el.find("[data-key=name]").text() == this.model.get("name")) {
				this.$el.find("[data-menu=updateBookkeeping]").hide(100);
			} else {
				this.$el.find("[data-menu=updateBookkeeping]").show(100);
			}
		},
		changeNameKeyUp : function(event) {
			this.isChange();
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
			this.collection = new $.BookkeepingCollection();
			
			this.listenTo(this.collection, "reset", this.render);
			this.listenTo(this.collection, "add", this.renderBookkeeping);
			
			this.collection.fetch({reset : true});
		},
		render : function() {
			this.$el.html(Mustache.render(this.template));
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
			var bookkeeping = new $.Bookkeeping({name : $("[data-key-name=createBookkeepingName]").text()});
			if (!bookkeeping.isValid()) {
				return;
			}
			this.collection.create(bookkeeping);
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