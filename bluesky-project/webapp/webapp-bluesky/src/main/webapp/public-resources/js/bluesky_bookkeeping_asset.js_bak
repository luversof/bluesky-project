String.prototype.format = function() {
    var args = arguments;
    return this.replace(/\{(\d+)\}/g, function() {
        return args[arguments[1]];
    });
};

/**
 * 현재 asset 관리 페이지의 편집기능을 위해 만들어짐
 * 기능이 추가되면 재정의 필요
 * template 변환 관련 좋은 라이브러리가 있으면 좋은데..
 */
var asset = {
	config : {
		contextPath : "/",
		userId : null,
		dataKey : "data-asset",
		uiPosition : {
			target : "table tbody tr",
			addPosition : "table tbody",
			id : "td:eq(0)",
			name : "td:eq(1)",
			username : "td:eq(2)",
			amount : "td:eq(3)",
			enable : "td:eq(4)",
			assetGroup : {
				name : "td:eq(5)"
			},
			menu : {
				edit : { area : "td:eq(6)" },
				add : { area : ".asset-menu-add", evnetTarget : ".asset-add"}
			}
		},
		url : {
			add : "{0}user/{1}/bookkeeping/asset.json",
			modify : "{0}user/bookkeeping/{1}/bookkeeping/asset/{2}.json",
			remove : "{0}user/{1}/bookkeeping/asset/{2}.json"
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
		return this.config.url.modify.format(this.config.contextPath, this.config.userId, this.getAssetData().id);	
	},
	getUrlRemove : function() {
		return this.config.url.remove.format(this.config.contextPath, this.config.userId, this.getAssetData().id);	
	},
	/**
	 * ui에서 획득한 asset data를 저장
	 */
	setAssetDataFromUi : function() {
		var asset = this.getAssetDataFromUi();
		//console.log("setAssetDataFromUi asset : " + asset.id);
		this.setAssetData(asset);
	},
	setAssetData : function(asset) {
		this.currentTarget.data(this.config.dataKey, asset);
	},
	setUiFromAssetData : function() {
		var asset = this.getAssetData();
		this.currentTarget
			.find(this.config.uiPosition.id).text(asset.id).end()
			.find(this.config.uiPosition.name).text(asset.text).end()
			.find(this.config.uiPosition.username).text(asset.username).end()
			.find(this.config.uiPosition.amount).text(asset.amount).end()
			.find(this.config.uiPosition.enable).text(asset.enable).end()
			.find(this.config.uiPosition.assetGroup.name).text(asset["assetGroup.name"]);
	},
	getAssetData : function() {
		return this.currentTarget.data(this.config.dataKey);
	},
	/**
	 * 추가 대상 asset data 획득
	 */
	getAssetDataAdd : function () {
		var asset = {
			name : $("#name").val(),
			amount : $("#amount").val(),
			"assetGroup.id" : $("[id='assetGroup.id']").val(),
			enable : $("#enable").val()
		};
		return asset;
	},
	/**
	 * ui에서 asset data를 획득
	 * @returns asset json
	 */
	getAssetDataFromUi : function() {
		return {
				id : this.currentTarget.find(this.config.uiPosition.id).text(),
				name : this.currentTarget.find(this.config.uiPosition.name).text(),
				username : this.currentTarget.find(this.config.uiPosition.username).text(),
				amount : this.currentTarget.find(this.config.uiPosition.amount).text(),
				enable : eval(this.currentTarget.find(this.config.uiPosition.enable).text()),
				"assetGroup.name" : this.currentTarget.find(this.config.uiPosition.assetGroup.name).text()
		};
	},
	addUi : function(asset) {
		return 	$("<tr>")
				.append($("<td>").text(asset.id))
				.append($("<td>").text(asset.name))
				.append($("<td>").text(asset.username))
				.append($("<td>").text(asset.amount))
				.append($("<td>").text(asset.enable))
				.append($("<td>").text(asset.assetGroup.name))
				.append($("<td>"))
				.appendTo($(this.config.uiPosition.addPosition));
	},
	/* (e) util */
	/* (s) action */
	add : function() {
		var assetObj = this;
		var asset = this.getAssetDataAdd();
		$.ajax({
			url : this.getUrlAdd(),
			type : "post",
			data : asset,
			success : function(data) {
				//최초 add인 경우 처리는?
				var target = assetObj.addUi(data.asset);
				$(".asset-add-modal").modal("hide");
				$("html, body").animate({scrollTop : target.offset().top});
				target.hide().fadeIn(1500);
			}
		});
	},
	/**
	 * modify는 ui에서 가져온 변경된 정보만 수정 요청함
	 */
	modify : function() {
		var asset = this.getAssetData();
		var changedAsset = this.getAssetDataFromUi();
		var isChange = false;
		for (var key in asset) {
			if (typeof asset[key] != "object" && asset[key] != changedAsset[key]) {
				isChange = true;
			}
		}
		if (!isChange) {
			//console.log("not change");
			return;
		}
		changedAsset._method = "put";
		
		var assetObj = this;
		$.ajax({
			url : this.getUrlModify(),
			type : "post",
			data : changedAsset,
			success : function(data) {
				assetObj.showMessageModal("asset changed");
				assetObj.setAssetDataFromUi();
			}
		});
	},
	remove : function() {
		var assetObj = this;
		var target = this.currentTarget;
		$.ajax({
			url : this.getUrlRemove(),
			type : "post",
			data : {_method : "delete"},
			success : function() {
				assetObj.showMessageModal("asset changed").on("hidden.bs.modal", function() {
					target.fadeOut();
				});
				
				//마지막 열이 삭제된 경우 처리는?
			}
		});
	},
	/* (e) action */
	/* (s) ui method */
	showMenuEdit : function() {
		var assetObj = this;
		this.currentTarget.find(this.config.uiPosition.menu.edit.area).empty().append(
			$("<span>").addClass("glyphicon glyphicon-edit").attr("title", "edit").css("cursor", "pointer").tooltip().on("click", function(event) {
				assetObj.modify();
			})
		).append(" ").append(
			$("<span>").addClass("glyphicon glyphicon-remove").attr("title", "remove").css("cursor", "pointer").tooltip().on("click", function(event) {
				assetObj.remove();
			})
		).append(" ").append(
			$("<span>").addClass("glyphicon glyphicon-refresh").attr("title", "reset").css("cursor", "pointer").tooltip().on("click", function(event) {
				assetObj.setUiFromAssetData();
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
		var assetData = this.getAssetData();
		var assetDataFromUi = this.getAssetDataFromUi();
		var checkData = true;
		for (key in assetDataFromUi) {
			if (assetData[key] != assetDataFromUi[key]) {
				checkData = false;
				console.log("showMenuReset because : old [" + key + "] : " + typeof assetData[key] + ", ui : "  + typeof assetDataFromUi[key] + ", boolean : " + (assetData[key] != assetDataFromUi[key]));
			}
		}
		if (!checkData) {
			this.currentTarget.find(this.config.uiPosition.menu.edit.area).find(".glyphicon-refresh").fadeIn();
		}
	},
	/* (e) ui method */
	/* (s) event method */
	eventEnter : function() {
		var assetObj = this;
		$(this.config.uiPosition.target).on("mouseenter", function(event) {
			assetObj.currentTarget = $(this);
			if (assetObj.currentTarget.data(assetObj.config.dataKey) == null) {
				console.log("data-asset set and get");
				var asset = assetObj.getAssetDataFromUi();
				assetObj.setAssetDataFromUi();
				assetObj.eventChange();
			}
			//수정 and 삭제 아이콘 활성화
			assetObj.showMenuEdit();
			assetObj.showMenuReset();
			event.preventDefault();
		});
	},
	eventChange : function() {
		var assetObj = this;
		this.currentTarget.find(
			this.config.uiPosition.id + "," +
			this.config.uiPosition.name + "," +
			this.config.uiPosition.username + "," +
			this.config.uiPosition.amount + "," +
			this.config.uiPosition.enable + "," +
			this.config.uiPosition.assetGroup.name
		).on("focusout", function(event) {
			assetObj.showMenuReset();
		});
	},
	eventLeave : function() {
		var assetObj = this;
		$(this.config.uiPosition.target).on("mouseleave", function(event) {
			assetObj.hideMenuEdit();
			event.preventDefault();
		});
	},
	eventMenuAdd : function() {
		var assetObj = this;
		$(this.config.uiPosition.menu.add.area).on("click", function(event) {
			console.log("eventMenuAdd");
			$(".asset-add-modal").modal();
		});
		$(this.config.uiPosition.menu.add.evnetTarget).on("click", function() {
			assetObj.add();
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