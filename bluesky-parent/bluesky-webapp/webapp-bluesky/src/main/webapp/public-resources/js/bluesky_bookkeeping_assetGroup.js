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
		template : $("#assetGroup-template").text(),
		target : $(".table.table-hover tbody")
	});
	/**
	 * 2. controller 설정
	 */
	var controller = new Controller({
		url : "/user/1/bookkeeping/assetGroup",
		view : view,
		initialize : function() {
			$(".assetGroup-menu-add").css("cursor", "pointer");
		},
		events : {
			"tr" : {
				"mouseover" : ["menuShow", "menuModifyDisplayCheck"],
				"mouseleave" : "menuHide"
			},
			"[contenteditable=true]" : { "keyup" : "menuModifyDisplayCheck", "focusin" : "menuModifyDisplayCheck"},
			"span.glyphicon-edit" : { "click" : "modify" },
			"span.glyphicon-remove" : { "click" : "remove" },
			"span.glyphicon-refresh" : { "click" : "reset" }
		},
		externalEvents : {
			".assetGroup-menu-add" : { "click" : "meunAddDisplay" },
			".assetGroup-add-modal .assetGroup-add" : { "click" : "add" }
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
		menuModifyDisplayCheck : function(event) {
			console.debug("[controller] menuModifyDisplayCheck");
			var controller = event.data.controller;
			var dataIdKey = controller.view.dataIdKey;
			var targetRoot = $(event.currentTarget).closest("[" + dataIdKey +"]");
			var uiData = controller.getDataFromTemplate(targetRoot);
			var model = controller.getSavedModel(targetRoot.attr(dataIdKey));
			var resetArea = targetRoot.find(".glyphicon-refresh");
			var editArea = targetRoot.find(".glyphicon-edit");
			if (controller.isChanged(uiData, model.data)) {
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
			controller.view.add(model);
		},
		modify : function(event) {
			console.debug("[controller] modify");
			var controller = event.data.controller;
			var dataIdKey = controller.view.dataIdKey;
			var targetRoot = $(event.currentTarget).closest("[" + dataIdKey +"]");
			var uiData = controller.getDataFromTemplate(targetRoot);
			var model = controller.getSavedModel(targetRoot.attr(dataIdKey));
			$.extend(true, model.data, uiData);
			model.modify().success(function() {
				showMessageModal("assetGroup changed");
			});
		},
		remove : function(event) {
			console.debug("[controller] remove");
			var controller = event.data.controller;
			var dataIdKey = controller.view.dataIdKey;
			var targetRoot = $(event.currentTarget).closest("[" + dataIdKey +"]");
			var model = controller.getSavedModel(targetRoot.attr(dataIdKey));
			model.remove().success(function() {
				showMessageModal("assetGroup removed");
			});
		},
		/** (e) event **/
		/**
		 *  Template 에서 역으로 데이터를 추출하여 템플릿에 뿌려진 값을 json으로 원복한다.
		 */
		getDataFromTemplate : function(target) {
			console.debug("[controller] getDataFromTemplate");
			return {
				name : target.find("td:eq(0)").text(),
				assetType : target.find("td:eq(1)").text(),
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
					if (uiData[key].isChanged(data[key])) {
						return true;
					}
				}
			}
			return false;
		},
		/** (s) externalEvent **/	
		meunAddDisplay : function(event) {
			console.debug("[controller] menuAddDisplay");
			$(".assetGroup-add-modal").modal();
		},
		add : function(event) {
			console.log("[controller] add");
			var controller = event.data.controller;
			var view = controller.view;
			var assetGroup = {
				name : $("#name").val(),
				assetType : $("#assetType").val(),
			};
			var model = new Model(assetGroup, {controller : event.data.controller});
			model.add().success(function() {
				$(".assetGroup-add-modal").modal("hide");
				var targetView = view.get(model.getId());
				$("html, body").animate({scrollTop : targetView.offset().top});
				targetView.hide().fadeIn(1500);
			});
		}
		/** (e) externalEvent **/
 	});
	
	//var data = new Model({id : 52}, {controller : controller});
	//data.get();
	controller.getModelList();
	
//	var data2 = new Model({assetGroup : {id : 1}, assetGroupGroup : { id : 1}, amount : 123, memo : "test"}, {controller : controller});
//	data2.add();
});