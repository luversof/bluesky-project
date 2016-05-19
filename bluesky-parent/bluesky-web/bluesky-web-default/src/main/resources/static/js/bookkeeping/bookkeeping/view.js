$(document).ready(function() {

	$.BookkeepingView = Backbone.View.extend({
		el : "<tr>",	//기본은 div
		template : $("#template-bookkeeping-view").html(),
		events : {
			"click [data-menu=updateBookkeeping]" : "updateBookkeeping",
			"click [data-menu=deleteBookkeeping]" : "deleteBookkeeping",
			"keyup [data-key-name=name]" : "nameKeyUp",
			"keypress [data-key-name=name]" : "nameKeyPress",
			"change select[name=baseDate]" : "isChange",
		},
		initialize : function() {
			this.listenTo(this.model, 'change', this.render);
			this.listenTo(this.model, 'destroy', this.remove);
		},
		render : function() {
			this.$el.html(Mustache.render(this.template, this.model.toJSON()));
			this.$el.find("[data-menu=updateBookkeeping]").hide();
			this.$el.find("select[name=baseDate] > option[value=" + this.model.get("baseDate") + "]").attr("selected", "selected");
			return this;
		},
		updateBookkeeping : function() {
			this.model.save({
				name : this.$el.find("[data-key-name=name]").text(),
				baseDate : this.$el.find("select[name=baseDate] option:selected").val()
			});
			this.$el.find("[data-menu=updateBookkeeping]").hide(100);
		},
		deleteBookkeeping : function() {
			this.model.destroy();
		},
		// 변경된 내용이 있는지 여부 확인
		isChange : function() {
			if (this.$el.find("[data-key-name=name]").text() == this.model.get("name") 
					&& this.$el.find("select[name=baseDate] option:selected").val() == this.model.get("baseDate")) {
				this.$el.find("[data-menu=updateBookkeeping]").hide(100);
			} else {
				this.$el.find("[data-menu=updateBookkeeping]").show(100);
			}
		},
		nameKeyUp : function(event) {
			this.isChange();
			if (event.keyCode == 13) {
				this.updateBookkeeping();
			}
		},
		// enter 입력 처리 방지
		nameKeyPress : function(event) {
			return event.keyCode != 13;
		}
	});


	$.BookkeepingCollectionView = Backbone.View.extend({
		el : "#bookkeepingArea",
		template : $("#template-bookkeeping-collection-view").html(),
		events : {
			"click [data-menu=createBookkeeping]" : "createBookkeeping",
			"keyup [data-key-name=createBookkeepingName]" : "createBookkeepingNameKeyUp",
			"keypress [data-key-name=createBookkeepingName]" : "createBookkeepingNameKeyPress"
		},
		initialize : function() {
			//console.log("This collection view has been initialized.");
			this.collection = new $.BookkeepingCollection();
			
			this.listenTo(this.collection, "reset", this.render);
			this.listenTo(this.collection, "add", this.render);
			
			this.collection.fetch({reset : true});
		},
		render : function() {
			this.$el.html(Mustache.render(this.template));
			this.collection.each(function(bookkeeping) {
				var bookkeepingView = new $.BookkeepingView({ model : bookkeeping });
				this.$el.find("table tbody").append(bookkeepingView.render().el);
			}, this);
		},
//		renderBookkeeping : function(bookkeeping) {
//			var bookkeepingView = new $.BookkeepingView({model : bookkeeping});
//			this.$el.find("table tbody").append(bookkeepingView.render().el);
//		},
		createBookkeeping : function(event) {
			event.preventDefault();
			var bookkeeping = new $.Bookkeeping({
				name : this.$el.find("[data-key-name=createBookkeepingName]").text(),
				baseDate : this.$el.find("select[name=createBookkeepingBaseDate] option:selected").val()
			});
			if (!bookkeeping.isValid()) {
				return;
			}
			this.collection.create(bookkeeping);
			$(event.currentTarget).closest("tr")
			.find("[contenteditable=true]").text("");
		},
		createBookkeepingNameKeyUp : function(event) {
			if (event.keyCode === 13) {
				this.createBookkeeping(event);
			}
		},
		createBookkeepingNameKeyPress : function(event) {
			return event.keyCode !== 13;
		}
	});

	$.bookkeepingCollectionView = new $.BookkeepingCollectionView();
});