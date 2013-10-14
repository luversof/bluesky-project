function showMessageModal(message) {
	return $("<div>").addClass("modal fade").html(
		$("<div>").addClass("modal-dialog").html(
			$("<div>").addClass("modal-content").html(
				$("<div>").addClass("modal-body").text(message)
			)
		)
	).modal();
};

$(document).ready(function() {
	/**
	 * 1. view 설정
	 */
	var view = new View({
		template : $("#entry-template").text(),
		target : $(".table.table-hover tbody")
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
			".entry-add-modal .entry-add" : { "click" : "add" }
		},
		
		/** (s) event **/
		menuShow : function(event) {
			console.debug("[controller] menuShow");
			var dataIdKey = event.data.controller.view.dataIdKey;
			if (!$(event.currentTarget).closest("[" + dataIdKey + "]").find("td:last").html() == "") {
				return;
			}
			$(event.currentTarget).closest("[" + dataIdKey + "]").find("td:last").empty().append(
				$("<span>").addClass("glyphicon glyphicon-remove").attr("title", "remove").css("cursor", "pointer").tooltip()
			).append(" ").append(
				$("<span>").addClass("glyphicon glyphicon-edit").attr("title", "edit").css("cursor", "pointer").tooltip().hide()
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
			var controller = event.data.controller;
			var dataIdKey = controller.view.dataIdKey;
			var targetRoot = $(event.currentTarget).closest("[" + dataIdKey +"]");
			var uiData = controller.getDataFromTemplate(targetRoot);
			var model = controller.getSavedModel(targetRoot.attr(dataIdKey));
			var resetArea = targetRoot.find(".glyphicon-refresh");
			var editArea = targetRoot.find(".glyphicon-edit");
			var isChanged = controller.isChanged(uiData, model.data);
			if (isChanged) {
				resetArea.fadeIn();
				editArea.fadeIn();
			} else {
				resetArea.fadeOut();
				editArea.fadeOut();
			}
		},
		reset : function(event) {
			console.debug("[controller] reset");
			var controller = event.data.controller;
			var dataIdKey = controller.view.dataIdKey;
			var targetRoot = $(event.currentTarget).closest("[" + dataIdKey +"]");
			var model = controller.getSavedModel(targetRoot.attr(controller.view.dataIdKey));
			controller.view.render(model);
		},
		modify : function(event) {
			console.debug("[controller] modify");
			var controller = event.data.controller;
			var dataIdKey = controller.view.dataIdKey;
			var targetRoot = $(event.currentTarget).closest("[" + dataIdKey +"]");
			var uiData = controller.getDataFromTemplate(targetRoot);
			var model = controller.getSavedModel(targetRoot.attr(dataIdKey));
			$.extend(true, model.data, uiData);
			controller.modifyModel(model).success(function() {
				showMessageModal("asset changed");
			});
			
		},
		remove : function(event) {
			console.debug("[controller] remove");
			var controller = event.data.controller;
			var view = controller.view;
			var dataIdKey = controller.view.dataIdKey;
			var targetRoot = $(event.currentTarget).closest("[" + dataIdKey +"]");
			var model = controller.getSavedModel(targetRoot.attr(dataIdKey));
			controller.removeModel(model).success(function() {
				showMessageModal("asset removed");
				view.target.find("[" + view.dataIdKey + "=" + model.getId() + "]").fadeOut();
			});
		},
		/** (e) event **/
		
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
					if (uiData[key] != data[key] && !(uiData[key] == "" && data[key] == null)) {
						return true;
					}
				}
			}
			return false;
		},
		
		/** (s) externalEvent **/	
		meunAddDisplay : function(event) {
			console.debug("[controller] menuAddDisplay");
			$(".entry-add-modal").modal();
		},
		add : function(event) {
			console.log("[controller] add");
			var controller = event.data.controller;
			var view = controller.view;
			var entry = {
				asset : { id : $("[id='asset.id']").val()},
				entryGroup : {id : $("[id='entryGroup.id']").val()},
				amount : $("#amount").val(),
				date : $("#date").val(),
				memo : $("#memo").val(),
				transferEntry : $("#transferEntry").is(":checked")
			};
			var model = new Model(entry, {controller : event.data.controller});
			controller.addModel(model).success(function() {
				$(".entry-add-modal").modal("hide");
				var target = view.target.find("[" + view.dataIdKey + "=" + model.getId() + "]");
				$("html, body").animate({scrollTop : target.offset().top});
				target.hide().fadeIn(1500);
			});
		}
		/** (e) externalEvent **/
 	});
	
	//var data = new Model({id : 52}, {controller : controller});
	//data.get();
	controller.getModelList();
	
//	var data2 = new Model({asset : {id : 1}, entryGroup : { id : 1}, amount : 123, memo : "test"}, {controller : controller});
//	data2.add();
});