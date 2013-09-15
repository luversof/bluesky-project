String.prototype.format = function() {
    var args = arguments;
    return this.replace(/\{(\d+)\}/g, function() {
        return args[arguments[1]];
    });
};

/**
 * 현재 asset 관리 페이지의 편집기능을 위해 만들어짐
 * 기능이 추가되면 재정의 필요
 */
var asset = {
	config : {
		contextPath : "/",
		userId : null,
		dataKey : "data-asset",
		uiPosition : {
			target : "table tr",
			id : "td:eq(0)",
			name : "td:eq(1)",
			username : "td:eq(2)",
			amount : "td:eq(3)",
			enable : "td:eq(4)",
			assetGroup : {
				name : "td:eq(5)"
			},
			menu : {
				edit : "td:eq(6)",
				add : ".asset-menu-add"
			}
		},
		url : {
			add : "{0}user/{1}/asset.json",
			modify : "{0}user/{1}/asset/{2}.json",
			remove : "{0}user/{1}/asset/{2}.json"
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
		$("<div>").addClass("modal fade").html(
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
	 * ui에서 asset data를 획득
	 * @returns asset json
	 */
	getAssetDataFromUi : function() {
		return {
				id : this.currentTarget.find(this.config.uiPosition.id).text(),
				name : this.currentTarget.find(this.config.uiPosition.name).text(),
				username : this.currentTarget.find(this.config.uiPosition.username).text(),
				amount : this.currentTarget.find(this.config.uiPosition.amount).text(),
				enable : this.currentTarget.find(this.config.uiPosition.enable).text(),
				"assetGroup.name" : this.currentTarget.find(this.config.uiPosition.assetGroup.name).text()
		};
	},
	/* (e) util */
	/* (s) action */
	add : function() {
		
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
			console.log("not change");
			return;
		}
		changedAsset._method = "put";
		
		var assetObj = this;
		$.ajax({
			url : this.getUrlModify(),
			type : "post",
			data : changedAsset,
			success : function() {
				assetObj.showMessageModal("asset changed");
				assetObj.setAssetDataFromUi();
			}
		});
	},
	remove : function() {
		var assetObj = this;
		$.ajax({
			url : this.getUrlRemove(),
			type : "post",
			data : {_method : "delete"},
			success : function() {
				assetObj.showMessageModal("asset changed");
				assetObj.currentTarget.remove();
			}
		});
	},
	/* (e) action */
	/* (s) ui method */
	showMenuEdit : function() {
		var assetObj = this;
		
		this.currentTarget.find(this.config.uiPosition.menu.edit).empty().append(
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
		this.currentTarget.find(this.config.uiPosition.menu.edit).empty();	
	},
	/* (e) ui method */
	/* (s) event method */
	eventEnter : function() {
		assetObj = this;
		$(this.config.uiPosition.target).on("mouseenter", function(event) {
			assetObj.currentTarget = $(this);
			if (assetObj.currentTarget.data(assetObj.config.dataKey) == null) {
				console.log("data-asset set and get");
				assetObj.setAssetDataFromUi();
				assetObj.eventChange();
			}
			//수정 and 삭제 아이콘 활성화
			assetObj.showMenuEdit();
			event.preventDefault();
		});
	},
	eventChange : function() {
		assetObj = this;
		this.currentTarget.find(
			this.config.uiPosition.id + "," +
			this.config.uiPosition.name + "," +
			this.config.uiPosition.username + "," +
			this.config.uiPosition.amount + "," +
			this.config.uiPosition.enable + "," +
			this.config.uiPosition.assetGroup.name
		).on("focusout", function(event) {
			assetObj.currentTarget.find(assetObj.config.uiPosition.menu.edit).find(".glyphicon-refresh").fadeIn();
		});
	},
	eventLeave : function() {
		$(this.config.uiPosition.target).on("mouseleave", function(event) {
			assetObj.hideMenuEdit();
			event.preventDefault();
		});
	},
	eventMenuAdd : function() {
		$(this.config.uiPosition.menu.add).on("click", function() {
			console.log("eventMenuAdd");
			$(".asset-add-modal").modal();
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
		$(this.config.uiPosition.menu.add).css("cursor", "pointer");
	}
	
};