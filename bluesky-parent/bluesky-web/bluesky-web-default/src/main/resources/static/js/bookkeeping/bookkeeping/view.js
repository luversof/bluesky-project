$(document).ready(function() {
	
	$.BookkeepingView = Backbone.View.extend({
		el : "<tr>",	//기본은 div
		//className : "testt",
		template : $("#template-bookkeeping-view").html(),
		events : {
			"click .glyphicon-edit" : "updateBookkeeping",
			"click .glyphicon-remove" : "removeBookkeeping",
			"keyup [data-key=name]" : "changeNameKeyUP",
			"keypress [data-key=name]" : "changeNameKeyPress"
		},
		initialize : function() {
			console.log("This view has been initialized.");
			this.listenTo(this.model, 'change', this.render);
			this.listenTo(this.model, 'destroy', this.remove);
		},
		render : function() {
			//console.log("BookkeepingView render", this.model, this.$el);
			this.$el.html(Mustache.render(this.template, this.model.toJSON()));
			this.$el.find(".glyphicon-edit").hide();
			return this;
		},
		edit : function() {
			console.log("edit");
		},
		updateBookkeeping : function() {
			this.model.save({name : this.$el.find("[data-key=name]").text()});
			this.$el.find(".glyphicon-edit").hide(100);
		},
		removeBookkeeping : function() {
			this.model.destroy();
		},
		changeNameKeyUP : function(e) {
			//console.log("data : ", this.$el.find("[data-key=name]").text());
			if (this.$el.find("[data-key=name]").text() != this.model.get("name")) {
				this.$el.find(".glyphicon-edit").show(100);
			} else {
				this.$el.find(".glyphicon-edit").hide(100);
			}
			if (e.keyCode == 13) {
				this.updateBookkeeping();
			}
		},
		// enter 입력 처리 방지
		changeNameKeyPress : function(e) {
			return e.keyCode != 13;
		}
	});
	
	
	$.BookkeepingCollectionView = Backbone.View.extend({
		el : "#bookkeepingArea table tbody",
		template : $("#template-bookkeeping-list").html(),
		events : {
			//"click .addBookkeeping" : "addBookkeeping"
		},
		initialize : function() {
			console.log("This collection view has been initialized.");
			
			this.collection = new $.BookkeepingCollection();
			this.collection.fetch({reset : true});
			
			this.listenTo(this.collection, "reset", this.render);
			this.listenTo(this.collection, "add", this.renderBookkeeping);
		},
		render : function() {
			console.log("BookkeepingCollectionView render, model : ", this.collection.toJSON());
			
			this.collection.each(function(bookkeeping) {
				var bookkeepingView = new $.BookkeepingView({model : bookkeeping});
				this.$el.append(bookkeepingView.render().el);
			}, this);
			
			//Mustache의 하위 참조 방식을 사용하지 않음.
			//this.$el.html(Mustache.render(this.bookkeepingCollectionTemplate, this.collection.toJSON(), {"bookkeeping-view" : $.BookkeepingView.prototype.bookkeepingTemplate}))
			//return this;
		},
		renderBookkeeping : function(bookkeeping) {
			var bookkeepingView = new $.BookkeepingView({model : bookkeeping});
			this.$el.append(bookkeepingView.render().el);
		},
		addBookkeeping : function() {
			
			console.log("addBookkeeping", this.collection);
	//		var formData = {}
	//		var bookkeeping = new $.Bookkeeping({ name : $("input[name=addBookkeepingName]").val()});
	//		var bookkeepingView = new $.BookkeepingView({model : bookkeeping});
	//		alert(1);
			this.collection.create({ name : $("input[name=addBookkeepingName]").val() });
			//this.$el.find("table tbody").append(bookkeepingView.render().el);
	//		alert(2);
		}
	});

	$("#bookkeepingArea").html(Mustache.render($("#template-bookkeeping-list").html()));
	
	$.bookkeepingCollectionView = new $.BookkeepingCollectionView();
	
	
	$(document).on("click", ".addBookkeeping", function(event) {
		event.preventDefault();
		$.bookkeepingCollectionView.addBookkeeping();
	});
});