String.prototype.format = function() {
    var args = arguments;
    return this.replace(/\{(\d+)\}/g, function() {
        return args[arguments[1]];
    });
};

/**
 * 현재 entryGroup 관리 페이지의 편집기능을 위해 만들어짐
 * 기능이 추가되면 재정의 필요
 * template 변환 관련 좋은 라이브러리가 있으면 좋은데..
 */
var entryGroup = {
	config : {
		contextPath : "/",
		userId : null,
		dataKey : "data-entryGroup",
		uiPosition : {
			target : "table tbody tr",
			addPosition : "table tbody",
			id : "td:eq(0)",
			name : "td:eq(1)",
			username : "td:eq(2)",
			entryType : "td:eq(3)",
			menu : {
				edit : { area : "td:eq(4)" },
				add : { area : ".entryGroup-menu-add", evnetTarget : ".entryGroup-add"}
			}
		},
		url : {
			add : "{0}user/{1}/bookkeeping/entryGroup.json",
			modify : "{0}user/{1}/bookkeeping/entryGroup/{2}.json",
			remove : "{0}user/{1}/bookkeeping/entryGroup/{2}.json"
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
		return this.config.url.modify.format(this.config.contextPath, this.config.userId, this.getEntryGroupData().id);	
	},
	getUrlRemove : function() {
		return this.config.url.remove.format(this.config.contextPath, this.config.userId, this.getEntryGroupData().id);	
	},
	/**
	 * ui에서 획득한 entryGroup data를 저장
	 */
	setEntryGroupDataFromUi : function() {
		var entryGroup = this.getEntryGroupDataFromUi();
		//console.log("setEntryGroupDataFromUi entryGroup : " + entryGroup.id);
		this.setEntryGroupData(entryGroup);
	},
	setEntryGroupData : function(entryGroup) {
		this.currentTarget.data(this.config.dataKey, entryGroup);
	},
	setUiFromEntryGroupData : function() {
		var entryGroup = this.getEntryGroupData();
		this.currentTarget
			.find(this.config.uiPosition.id).text(entryGroup.id).end()
			.find(this.config.uiPosition.name).text(entryGroup.text).end()
			.find(this.config.uiPosition.username).text(entryGroup.username).end()
			.find(this.config.uiPosition.entryType).text(entryGroup.entryType);
	},
	getEntryGroupData : function() {
		return this.currentTarget.data(this.config.dataKey);
	},
	/**
	 * 추가 대상 entryGroup data 획득
	 */
	getEntryGroupDataAdd : function () {
		var entryGroup = {
			name : $("#name").val(),
			amount : $("#amount").val(),
			entryType : $("#entryType").val(),
			enable : $("#enable").val()
		};
		return entryGroup;
	},
	/**
	 * ui에서 entryGroup data를 획득
	 * @returns entryGroup json
	 */
	getEntryGroupDataFromUi : function() {
		return {
				id : this.currentTarget.find(this.config.uiPosition.id).text(),
				name : this.currentTarget.find(this.config.uiPosition.name).text(),
				username : this.currentTarget.find(this.config.uiPosition.username).text(),
				entryType : this.currentTarget.find(this.config.uiPosition.entryType).text()
		};
	},
	addUi : function(entryGroup) {
		return 	$("<tr>")
				.append($("<td>").text(entryGroup.id))
				.append($("<td>").text(entryGroup.name))
				.append($("<td>").text(entryGroup.username))
				.append($("<td>").text(entryGroup.entryType))
				.append($("<td>"))
				.appendTo($(this.config.uiPosition.addPosition));
	},
	/* (e) util */
	/* (s) action */
	add : function() {
		var entryGroupObj = this;
		var entryGroup = this.getEntryGroupDataAdd();
		$.ajax({
			url : this.getUrlAdd(),
			type : "post",
			data : entryGroup,
			success : function(data) {
				//최초 add인 경우 처리는?
				var target = entryGroupObj.addUi(data.entryGroup);
				$(".entryGroup-add-modal").modal("hide");
				$("html, body").animate({scrollTop : target.offset().top});
				target.hide().fadeIn(1500);
			}
		});
	},
	/**
	 * modify는 ui에서 가져온 변경된 정보만 수정 요청함
	 */
	modify : function() {
		var entryGroup = this.getEntryGroupData();
		var changedEntryGroup = this.getEntryGroupDataFromUi();
		var isChange = false;
		for (var key in entryGroup) {
			if (typeof entryGroup[key] != "object" && entryGroup[key] != changedEntryGroup[key]) {
				isChange = true;
			}
		}
		if (!isChange) {
			//console.log("not change");
			return;
		}
		changedEntryGroup._method = "put";
		
		var entryGroupObj = this;
		$.ajax({
			url : this.getUrlModify(),
			type : "post",
			data : changedEntryGroup,
			success : function(data) {
				entryGroupObj.showMessageModal("entryGroup changed");
				entryGroupObj.setEntryGroupDataFromUi();
			}
		});
	},
	remove : function() {
		var entryGroupObj = this;
		var target = this.currentTarget;
		$.ajax({
			url : this.getUrlRemove(),
			type : "post",
			data : {_method : "delete"},
			success : function() {
				entryGroupObj.showMessageModal("entryGroup changed").on("hidden.bs.modal", function() {
					target.fadeOut();
				});
				
				//마지막 열이 삭제된 경우 처리는?
			}
		});
	},
	/* (e) action */
	/* (s) ui method */
	showMenuEdit : function() {
		var entryGroupObj = this;
		this.currentTarget.find(this.config.uiPosition.menu.edit.area).empty().append(
			$("<span>").addClass("glyphicon glyphicon-edit").attr("title", "edit").css("cursor", "pointer").tooltip().on("click", function(event) {
				entryGroupObj.modify();
			})
		).append(" ").append(
			$("<span>").addClass("glyphicon glyphicon-remove").attr("title", "remove").css("cursor", "pointer").tooltip().on("click", function(event) {
				entryGroupObj.remove();
			})
		).append(" ").append(
			$("<span>").addClass("glyphicon glyphicon-refresh").attr("title", "reset").css("cursor", "pointer").tooltip().on("click", function(event) {
				entryGroupObj.setUiFromentryGroupData();
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
		var entryGroupData = this.getEntryGroupData();
		var entryGroupDataFromUi = this.getEntryGroupDataFromUi();
		var checkData = true;
		for (key in entryGroupDataFromUi) {
			if (entryGroupData[key] != entryGroupDataFromUi[key]) {
				checkData = false;
				console.log("showMenuReset because : old [" + key + "] : " + typeof entryGroupData[key] + ", ui : "  + typeof entryGroupDataFromUi[key] + ", boolean : " + (entryGroupData[key] != entryGroupDataFromUi[key]));
			}
		}
		if (!checkData) {
			this.currentTarget.find(this.config.uiPosition.menu.edit.area).find(".glyphicon-refresh").fadeIn();
		}
	},
	/* (e) ui method */
	/* (s) event method */
	eventEnter : function() {
		var entryGroupObj = this;
		$(this.config.uiPosition.target).on("mouseenter", function(event) {
			entryGroupObj.currentTarget = $(this);
			if (entryGroupObj.currentTarget.data(entryGroupObj.config.dataKey) == null) {
				console.log("data-entryGroup set and get");
				var entryGroup = entryGroupObj.getEntryGroupDataFromUi();
				entryGroupObj.setEntryGroupDataFromUi();
				entryGroupObj.eventChange();
			}
			//수정 and 삭제 아이콘 활성화
			entryGroupObj.showMenuEdit();
			entryGroupObj.showMenuReset();
			event.preventDefault();
		});
	},
	eventChange : function() {
		var entryGroupObj = this;
		this.currentTarget.find(
			this.config.uiPosition.id + "," +
			this.config.uiPosition.name + "," +
			this.config.uiPosition.username + "," +
			this.config.uiPosition.entryType
		).on("focusout", function(event) {
			entryGroupObj.showMenuReset();
		});
	},
	eventLeave : function() {
		var entryGroupObj = this;
		$(this.config.uiPosition.target).on("mouseleave", function(event) {
			entryGroupObj.hideMenuEdit();
			event.preventDefault();
		});
	},
	eventMenuAdd : function() {
		var entryGroupObj = this;
		$(this.config.uiPosition.menu.add.area).on("click", function(event) {
			console.log("eventMenuAdd");
			$(".entryGroup-add-modal").modal();
		});
		$(this.config.uiPosition.menu.add.evnetTarget).on("click", function(){
			entryGroupObj.add();
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