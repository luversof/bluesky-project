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
			$(".entry-menu-add").css("cursor", "pointer");
		},
		events : {
			"tr" : {
				"mouseover" : ["menuShow", "menuResetDisplayCheck"],
				"mouseleave" : "menuHide"
			},
			"[contenteditable=true]" : { "keyup" : "menuResetDisplayCheck", "focusin" : "menuResetDisplayCheck"},
			"span.glyphicon-edit" : { "click" : "modify" },
			"span.glyphicon-remove" : { "click" : "remove" },
			"span.glyphicon-refresh" : { "click" : "reset" }
		},
		externalEvents : {
			".entry-menu-add" : { "click" : "meunAddDisplay" },
			".entry-menu-add .entry-add" : { "click" : "add" }
		},
		menuShow : function(event) {
			console.debug("[controller] menuShow");
			if (!$(event.currentTarget).closest("[data-id]").find("td:last").html() == "") {
				return;
			}
			$(event.currentTarget).closest("[data-id]").find("td:last").empty().append(
				$("<span>").addClass("glyphicon glyphicon-edit").attr("title", "edit").css("cursor", "pointer").tooltip()
			).append(" ").append(
				$("<span>").addClass("glyphicon glyphicon-remove").attr("title", "remove").css("cursor", "pointer").tooltip()
			).append(" ").append(
				$("<span>").addClass("glyphicon glyphicon-refresh").attr("title", "reset").css("cursor", "pointer").tooltip().hide()
			);
		},
		menuHide : function(event) {
			console.debug("[controller] menuHide");
			$(event.currentTarget).find("td:last").empty();
		},
		menuResetDisplayCheck : function(event) {
			console.debug("[controller] menuResetDisplayCheck");
			var targetRoot = $(event.currentTarget).closest("[data-id]");
			var controller = event.data.controller;
			var uiData = controller.getDataFromTemplate(targetRoot);
			var model = controller.getSavedModel(targetRoot.attr("data-id"));
			var resetArea = targetRoot.find(".glyphicon-refresh");
			controller.isChanged(uiData, model.data) ? resetArea.fadeIn() : resetArea.fadeOut();
		},
		/**
		 *  Template 에서 역으로 데이터를 추출하여 템플릿에 뿌려진 값을 json으로 원복한다.
		 */
		getDataFromTemplate : function(target) {
			console.debug("[controller] getDataFromTemplate");
			return {
				asset : { name : target.find("td:eq(0)").text() },
				entryGroup : { name : target.find("td:eq(1)").text() },
				amount : target.find("td:eq(2)").text(),
				date : target.find("td:eq(3)").text(),
				memo : target.find("td:eq(4)").text()
			};
		},
		isChanged : function(uiData, data) {
			for (key in uiData) {
				if (typeof uiData[key] == "object") {
					var result = this.isChanged(uiData[key], data[key]);
					if (result) {
						return result;
					}
				} else {
					if (uiData[key] != data[key]) {
						return true;
					}
				}
			}
			return false;
		},
		reset : function(event) {
			console.debug("[controller] reset");
			var targetRoot = $(event.currentTarget).closest("[data-id]");
			var controller = event.data.controller;
			var model = controller.getSavedModel(targetRoot.attr("data-id"));
			controller.view.render(model);
		},
		modify : function(event) {
			
		},
		
		/** (s) externalEvent **/	
		meunAddDisplay : function(event) {
			console.debug("[controller] menuAddDisplay");
			$(".entry-add-modal").modal();
		},
		add : function(event) {
			console.log("[controller] add");
			var entry = new Entry({
				asset : { id : $("[id='asset.id']").val()},
				entryGroup : {id : $("[id='entryGroup.id']").val()},
				amount : $("#amount").val(),
				date : $("#date").val(),
				memo : $("#memo").val(),
				transferEntry : $("#transferEntry").is(":checked")
			});
			var model = new Model(entry, {controller : event.data.controller});
			event.data.controller.saveModel(model);
		}
		/** (e) externalEvent **/
 	});
	
	//var data = new Model({id : 52}, {controller : controller});
	//data.get();
	controller.getModelList();
	
//	var data2 = new Model({asset : {id : 1}, entryGroup : { id : 1}, amount : 123, memo : "test"}, {controller : controller});
//	data2.save();
});