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
	
	var Bookkeeping = Backbone.Model.extend({
		defaults : {
			id : null,
			name : null,
			userId : null
		}
	});
	var bookkeeping = new Bookkeeping({name : "test"});
	
	var BookkeepingView = Backbone.View.extend({
		tagName : "li",
		bookKeepingTpl : $("#item-template").html(),
		events : {
			"dblclick label" : "edit",
			"keypress .edit" : "updateOnEnter",
			"blur .edit" : "close"
		},
		initialize : function() {
			this.$el = $("#bookkeepingArea");
		},
		render : function() {
			this.$el.html( Mustache.render(this.bookKeepingTpl, this.model.toJSON()));
			this.input = this.$(".edit");
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
	
	var bookkeepingView = new BookkeepingView({model : bookkeeping})
	bookkeepingView.render();

});