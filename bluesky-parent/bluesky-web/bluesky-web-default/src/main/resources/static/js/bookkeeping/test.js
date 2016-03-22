$(document).ready(function() {
/*	var Bookkeeping = function() {

		var _getMyBookkeeping = function() {

			$.ajax({
				url : "/bookkeeping.json",
				dataType : "json"
			}).success(function(data) {
				console.log("data : ", data);
			});
		}

		return {
			getMyBookkeeping : function() {
				_getMyBookkeeping();
			}
		}
	};*/

	// var bookkeeping = Bookkeeping();
	// bookkeeping.getMyBookkeeping();
	
	$.Bookkeeping = Backbone.Model.extend({
		defaults : {
			id : null,
			name : null,
			userId : null
		},
		initialize : function() {
			console.log("This model has been initialized.");
			this.on("change", function() {
				console.log("this model has changed.");
			});
			this.on("change:name", function() {
				console.log("name value for this model has changed.");
			});
			this.on("invalid", function(model, error) {
				console.log("invalid : ", model, error);
			})
		},
		validate : function(attrs, options) {
			console.log("validate : ", attrs, options);
			if (attrs.name === undefined) {
				return "Remember to set a name for your bookkeeping";
			}
		}
	});
	/*var bookkeeping = new Bookkeeping({name : "test"});
	console.log("bookkeeping : ", bookkeeping);
	bookkeeping.set({name : "test22"}, {validate : true});
	console.log("bookkeeping2 : ", bookkeeping);
	bookkeeping.unset("name", {validate : true, mode : "write"});
	console.log("bookkeeping3 : ", bookkeeping);
	bookkeeping.set({"name" : "test55"}, {validate : true, mode2 : "modify"});
	console.log("bookkeeping4 : ", bookkeeping);*/
	
	$.BookkeepingCollection = Backbone.Collection.extend({
		model : $.Bookkeeping,
		url : "/bookkeeping",
		initialize : function() {
			console.log("BookkeepingCollection initialized.");
		}
	});
	
	/*	
	var bookkeepingList = [];
	for (var i = 0 ; i < 10 ; i++) {
		var bookkeeping = new Bookkeeping({name : "bookkeeping" + i});
		bookkeepingList.push(bookkeeping);
	}
	
	var bookkeepingCollection = new BookkeepingCollection(bookkeepingList);
	console.log("bookkeepingCollection length : ", bookkeepingCollection.length, bookkeepingCollection);
	*/
	//$.bookkeepingCollection = new $.BookkeepingCollection();
	//$.bookkeepingCollection.fetch();
	
	$.BookkeepingView = Backbone.View.extend({
		el : "#bookkeepingArea",	//기본은 div
		//className : "testt",
		bookkeepingTemplate : $("#template-bookkeeping-view").html(),
		events : {
			"dblclick label" : "edit",
			"keypress .edit" : "updateOnEnter",
			"focus .edit" : "updateOnEnter",
			"blur .edit" : "close"
		},
		initialize : function() {
			console.log("This view has been initialized.");
			//this.listenTo(this.model, "change", this.render);
			//this.model.on({"change" : this.render})
			this.render();
			
		},
		render : function() {
			console.log("BookkeepingView render");
			this.$el.html( Mustache.render(this.bookkeepingTemplate, this.model.toJSON()));
			return this;
		},
		edit : function() {
			console.log("edit");
		},
		close : function() {
			console.log("close");
		},
		updateOnEnter : function() {
			console.log("updateOnEnter");
		}
	});

	
	//var bookkeepingView = new BookkeepingView({model : bookkeeping})
	//bookkeepingView.render();
	//console.log(bookkeepingView.el);
//	var bookkeepingView = new BookkeepingView();
//	bookkeepingView.render();
	
	/* 이건 잘못된 호출
	for (var i = 0 ; i < bookkeepingCollection.length ; i++) {
		console.log("bookkeepingCollection[i] : ", bookkeepingCollection[i]);
		var bookkeepingView = new BookkeepingView(bookkeepingCollection[i]);
		bookkeepingView.render();
	}*/
	
	$.BookkeepingCollectionView = Backbone.View.extend({
		el : "#bookkeepingArea",
		bookkeepingCollectionTemplate : $("#template-bookkeeping-list").html(),
		events : {
			"click" : "test",
			"click #add" : "addBookkeeping"
		},
		initialize : function() {
			console.log("This collection view has been initialized.");
			
			this.collection = new $.BookkeepingCollection();
			this.collection.fetch({reset : true});
			//this.render();
			
			this.listenTo(this.collection, "reset", this.render);
			this.listenTo(this.collection, "add", this.renderBookkeeping);
			//this.model.on({"change" : this.render})
		},
		render : function() {
			console.log("BookkeepingCollectionView render, model : ", this.collection.toJSON());
			this.$el.html(Mustache.render(this.bookkeepingCollectionTemplate, this.collection.toJSON(), {"bookkeeping-view" : $.BookkeepingView.prototype.bookkeepingTemplate}))
		},
		renderBookkeeping : function(bookkeeping) {
			var bookkeepingView = $.BookkeepingView({model : bookkeeping});
			this.$el.find("tbody").append(bookkeepingView.render().el);
		},
		test : function() {
			console.log("테스트");
		},
		addBookkeeping : function(e) {
			e.preventDefault();
			var formData = {}
			var bookkeepingView = $.BookkeepingView({model : bookkeeping});
			this.$el.append(bookkeepingView.render().el);
		}
	});
	
	$.bookkeepingCollectionView = new $.BookkeepingCollectionView();
	//bookkeepingCollectionView.render();
	//console.log(bookkeepingCollectionView.el);
	
	
	
	$.BookkeepingRouter = Backbone.Router.extend({
		routes : {
			"/bookkeeping/index" : "bookkeepingList",
			"*other" : "defaultRoute"
		},
		bookkeepingList : function() {
			console.log("route bookkeepingList");
		},
		defaultRoute : function(other) {
			console.log("Invalid. You attempted to reach : ", other);
		}
	});	
	var bookkeepingRouter = new $.BookkeepingRouter();
	Backbone.history.start();
	// 우선 잘되는지 테스트
	/*var bookkeepingList = [];
	for (var i = 0 ; i < 10 ; i++) {
		var bookkeeping = {
			id : i,
			name : "bookkeeping" + i
		}
		bookkeepingList.push(bookkeeping);
	}
	console.log("bookkeepingList : ", bookkeepingList);
	$("#bookkeepingArea").html(Mustache.render($("#template-bookkeeping-list").html(), bookkeepingList, { "bookkeeping-view" : $("#template-bookkeeping-view").html()}));
	*/
});