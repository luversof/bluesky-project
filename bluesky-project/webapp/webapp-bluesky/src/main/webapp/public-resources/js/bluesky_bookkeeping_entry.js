$(document).ready(function() {
	/**
	 * 1. view 설정
	 */
	var view = new View({
		template : $("#entry-template").text(),
		target : $(".table.table-hover tbody"),
		menuShow : function(event) {
			$(event.currentTarget).find("td:last").empty().append(
					$("<span>").addClass("glyphicon glyphicon-edit").attr("title", "edit").css("cursor", "pointer").tooltip()
				).append(" ").append(
					$("<span>").addClass("glyphicon glyphicon-remove").attr("title", "remove").css("cursor", "pointer").tooltip()
				).append(" ").append(
					$("<span>").addClass("glyphicon glyphicon-refresh").attr("title", "reset").css("cursor", "pointer").tooltip().hide()
				);
			this.menuResetDisplayCheck($(event.currentTarget).find(".glyphicon-refresh"));
		}
	});
	
	/**
	 * 2. controller 설정
	 */
	var controller = new Controller({
		url : "/user/1/bookkeeping/entry",
		id : "id",
		dataKey : "model",
		view : view,
		initialize : function() {
			console.log("우하하하하");
		},
		events : {
			"tr" : {
				"mouseover" : "menuShow",
				"mouseleave" : "menuHide"
			}
		},
		menuShow : function(event) {
			console.log("[controller] menuShow");
			$(event.currentTarget).find("td:last").empty().append(
					$("<span>").addClass("glyphicon glyphicon-edit").attr("title", "edit").css("cursor", "pointer").tooltip()
				).append(" ").append(
					$("<span>").addClass("glyphicon glyphicon-remove").attr("title", "remove").css("cursor", "pointer").tooltip()
				).append(" ").append(
					$("<span>").addClass("glyphicon glyphicon-refresh").attr("title", "reset").css("cursor", "pointer").tooltip().hide()
				);
			event.data.controller.menuResetDisplayCheck($(event.currentTarget).find(".glyphicon-refresh"));
		},
		menuHide : function(event) {
			$(event.currentTarget).find("td:last").empty();
		},
		menuResetDisplayCheck : function(displayTarget) {
			console.log("[controller] menuResetDisplayCheck");
			console.log(this);
			console.log(displayTarget);
//			displayTarget.hide();
//			if (!this.model.hasChanged()) {
//				return;
//			}
//			for (var key in this.model.changedAttributes()) {
//				if (this.model.get(key) != this.model.previous(key)) {
//					displayTarget.fadeIn();	
//				}
//			}
		}
 	});
	
	//var data = new Model({id : 52}, {controller : controller});
	//data.get();
	controller.getModelList();
	
//	var data2 = new Model({asset : {id : 1}, entryGroup : { id : 1}, amount : 123, memo : "test"}, {controller : controller});
//	data2.save();
});