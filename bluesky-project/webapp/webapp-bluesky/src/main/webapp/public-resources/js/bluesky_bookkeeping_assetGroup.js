String.prototype.format = function() {
    var args = arguments;
    return this.replace(/\{(\d+)\}/g, function() {
        return args[arguments[1]];
    });
};

/**
 * 현재 assetGroup 관리 페이지의 편집기능을 위해 만들어짐
 * 기능이 추가되면 재정의 필요
 * template 변환 관련 좋은 라이브러리가 있으면 좋은데..
 */
var assetGroup = {
	config : {
		contextPath : "/",
		userId : null,
		dataKey : "data-assetGroup",
		uiPosition : {
			target : "table tbody tr",
			addPosition : "table tbody",
			id : "td:eq(0)",
			name : "td:eq(1)",
			username : "td:eq(2)",
			assetType : "td:eq(3)",
			menu : {
				edit : { area : "td:eq(4)" },
				add : { area : ".assetGroup-menu-add", evnetTarget : ".assetGroup-add"}
			}
		},
		url : {
			add : "{0}user/{1}/bookkeeping/assetGroup.json",
			modify : "{0}user/{1}/bookkeeping/assetGroup/{2}.json",
			remove : "{0}user/{1}/bookkeeping/assetGroup/{2}.json"
		}
	},
	/**
	 * event 대상 target
	 */
	currentTarget : null,
	setContextPath : function(contextPath) {
		this.config.contextPath = contextPath;
	},
	setUserId : function(userId) {
		this.config.userId = userId;
	},
	
	/* (s) util */
	showMessageModal : function(message) {
		return $("<div>").addClass("modal fade").html(
			$("<div>").addClass("modal-dialog").html(
				$("<div>").addClass("modal-content").html(
					$("<div>").addClass("modal-body").text(message)
				)
			)
		).modal();
	},
	getUrlAdd : function() {
		return this.config.url.add.format(this.config.contextPath, this.config.userId);
	},
	getUrlModify : function() {
		return this.config.url.modify.format(this.config.contextPath, this.config.userId, this.getAssetGroupData().id);	
	},
	getUrlRemove : function() {
		return this.config.url.remove.format(this.config.contextPath, this.config.userId, this.getAssetGroupData().id);	
	},
	/**
	 * ui에서 획득한 assetGroup data를 저장
	 */
	setAssetGroupDataFromUi : function() {
		var assetGroup = this.getAssetGroupDataFromUi();
		//console.log("setAssetGroupDataFromUi assetGroup : " + assetGroup.id);
		this.setAssetGroupData(assetGroup);
	},
	setAssetGroupData : function(assetGroup) {
		this.currentTarget.data(this.config.dataKey, assetGroup);
	},
	setUiFromAssetGroupData : function() {
		var assetGroup = this.getAssetGroupData();
		this.currentTarget
			.find(this.config.uiPosition.id).text(assetGroup.id).end()
			.find(this.config.uiPosition.name).text(assetGroup.text).end()
			.find(this.config.uiPosition.username).text(assetGroup.username).end()
			.find(this.config.uiPosition.assetType).text(assetGroup.assetType);
	},
	getAssetGroupData : function() {
		return this.currentTarget.data(this.config.dataKey);
	},
	/**
	 * 추가 대상 assetGroup data 획득
	 */
	getAssetGroupDataAdd : function () {
		var assetGroup = {
			name : $("#name").val(),
			amount : $("#amount").val(),
			assetType : $("#assetType").val(),
			enable : $("#enable").val()
		};
		return assetGroup;
	},
	/**
	 * ui에서 assetGroup data를 획득
	 * @returns assetGroup json
	 */
	getAssetGroupDataFromUi : function() {
		return {
				id : this.currentTarget.find(this.config.uiPosition.id).text(),
				name : this.currentTarget.find(this.config.uiPosition.name).text(),
				username : this.currentTarget.find(this.config.uiPosition.username).text(),
				assetType : this.currentTarget.find(this.config.uiPosition.assetType).text()
		};
	},
	addUi : function(assetGroup) {
		return 	$("<tr>")
				.append($("<td>").text(assetGroup.id))
				.append($("<td>").text(assetGroup.name))
				.append($("<td>").text(assetGroup.username))
				.append($("<td>").text(assetGroup.assetType))
				.append($("<td>"))
				.appendTo($(this.config.uiPosition.addPosition));
	},
	/* (e) util */
	/* (s) action */
	add : function() {
		var assetGroupObj = this;
		var assetGroup = this.getAssetGroupDataAdd();
		$.ajax({
			url : this.getUrlAdd(),
			type : "post",
			data : assetGroup,
			success : function(data) {
				//최초 add인 경우 처리는?
				var target = assetGroupObj.addUi(data.assetGroup);
				$(".assetGroup-add-modal").modal("hide");
				$("html, body").animate({scrollTop : target.offset().top});
				target.hide().fadeIn(1500);
			}
		});
	},
	/**
	 * modify는 ui에서 가져온 변경된 정보만 수정 요청함
	 */
	modify : function() {
		var assetGroup = this.getAssetGroupData();
		var changedAssetGroup = this.getAssetGroupDataFromUi();
		var isChange = false;
		for (var key in assetGroup) {
			if (typeof assetGroup[key] != "object" && assetGroup[key] != changedAssetGroup[key]) {
				isChange = true;
			}
		}
		if (!isChange) {
			//console.log("not change");
			return;
		}
		changedAssetGroup._method = "put";
		
		var assetGroupObj = this;
		$.ajax({
			url : this.getUrlModify(),
			type : "post",
			data : changedAssetGroup,
			success : function(data) {
				assetGroupObj.showMessageModal("assetGroup changed");
				assetGroupObj.setAssetGroupDataFromUi();
			}
		});
	},
	remove : function() {
		var assetGroupObj = this;
		var target = this.currentTarget;
		$.ajax({
			url : this.getUrlRemove(),
			type : "post",
			data : {_method : "delete"},
			success : function() {
				assetGroupObj.showMessageModal("assetGroup changed").on("hidden.bs.modal", function() {
					target.remove();
				});
				
				//마지막 열이 삭제된 경우 처리는?
			}
		});
	},
	/* (e) action */
	/* (s) ui method */
	showMenuEdit : function() {
		var assetGroupObj = this;
		this.currentTarget.find(this.config.uiPosition.menu.edit.area).empty().append(
			$("<span>").addClass("glyphicon glyphicon-edit").attr("title", "edit").css("cursor", "pointer").tooltip().on("click", function(event) {
				assetGroupObj.modify();
			})
		).append(" ").append(
			$("<span>").addClass("glyphicon glyphicon-remove").attr("title", "remove").css("cursor", "pointer").tooltip().on("click", function(event) {
				assetGroupObj.remove();
			})
		).append(" ").append(
			$("<span>").addClass("glyphicon glyphicon-refresh").attr("title", "reset").css("cursor", "pointer").tooltip().on("click", function(event) {
				assetGroupObj.setUiFromassetGroupData();
			}).hide()
		);
	},
	hideMenuEdit : function() {
		this.currentTarget.find(this.config.uiPosition.menu.edit.area).empty();	
	},
	/**
	 * ui와 저장된 data가 다른 경우 reset 메뉴를 보여줌
	 */
	showMenuReset : function() {
		var assetGroupData = this.getAssetGroupData();
		var assetGroupDataFromUi = this.getAssetGroupDataFromUi();
		var checkData = true;
		for (key in assetGroupDataFromUi) {
			if (assetGroupData[key] != assetGroupDataFromUi[key]) {
				checkData = false;
				console.log("showMenuReset because : old [" + key + "] : " + typeof assetGroupData[key] + ", ui : "  + typeof assetGroupDataFromUi[key] + ", boolean : " + (assetGroupData[key] != assetGroupDataFromUi[key]));
			}
		}
		if (!checkData) {
			this.currentTarget.find(this.config.uiPosition.menu.edit.area).find(".glyphicon-refresh").fadeIn();
		}
	},
	/* (e) ui method */
	/* (s) event method */
	eventEnter : function() {
		var assetGroupObj = this;
		$(this.config.uiPosition.target).on("mouseenter", function(event) {
			assetGroupObj.currentTarget = $(this);
			if (assetGroupObj.currentTarget.data(assetGroupObj.config.dataKey) == null) {
				console.log("data-assetGroup set and get");
				var assetGroup = assetGroupObj.getAssetGroupDataFromUi();
				assetGroupObj.setAssetGroupDataFromUi();
				assetGroupObj.eventChange();
			}
			//수정 and 삭제 아이콘 활성화
			assetGroupObj.showMenuEdit();
			assetGroupObj.showMenuReset();
			event.preventDefault();
		});
	},
	eventChange : function() {
		var assetGroupObj = this;
		this.currentTarget.find(
			this.config.uiPosition.id + "," +
			this.config.uiPosition.name + "," +
			this.config.uiPosition.username + "," +
			this.config.uiPosition.assetType
		).on("focusout", function(event) {
			assetGroupObj.showMenuReset();
		});
	},
	eventLeave : function() {
		var assetGroupObj = this;
		$(this.config.uiPosition.target).on("mouseleave", function(event) {
			assetGroupObj.hideMenuEdit();
			event.preventDefault();
		});
	},
	eventMenuAdd : function() {
		var assetGroupObj = this;
		$(this.config.uiPosition.menu.add.area).on("click", function(event) {
			console.log("eventMenuAdd");
			$(".assetGroup-add-modal").modal();
		});
		$(this.config.uiPosition.menu.add.evnetTarget).on("click", function(){
			assetGroupObj.add();
		});
	},
	/* (s) event method */
	/**
	 * 사용할 페이지에서 호출하는 메소드 이 메소드 이외에는 closer로 숨겨도 될거 같긴하다.
	 */
	start : function(contextPath, userId) {
		this.setContextPath(contextPath);
		this.setUserId(userId);
		this.eventEnter();
		this.eventLeave();
		this.eventMenuAdd();
		$(this.config.uiPosition.menu.add.area).css("cursor", "pointer");
	}
};