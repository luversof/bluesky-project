String.prototype.format = function() {
    var args = arguments;
    return this.replace(/\{(\d+)\}/g, function() {
        return args[arguments[1]];
    });
};

/**
 * 상단 메뉴 표시
 * contextPath에 대한 메뉴 url 변경 적용
 */
var navbar = {
	contextPath : "/",
	display : function() {
		if (location.pathname == this.contextPath) {
			$(".navbar .nav li:eq(0)").addClass("active");
			return;
		}
		$(".navbar .navbar-nav li").each(function() {
			console.log("pathname : %s", location.pathname);
			if (location.pathname.search($(this).text()) > 0) {
				$(this).addClass("active");
			} else {
				$(this).removeClass();
			}
		});
	}
};

/**
 * 상단 navbar scroll에 따른 hide 처리 
 */
$(function() {
	var $nav = $(".navbar"),
	_hideShowOffset = 20,
	_lastScroll = 0,
	_detachPoint = 50;
	
	$(window).scroll(function() {
	var t = $(window).scrollTop(),
		e = t > _lastScroll ? "down" : "up",
		i = Math.abs(t - _lastScroll);
	
	if (t >= _detachPoint || 0 >= t || t > -1) {
		if ("down" === e && i >= _hideShowOffset) {
			$nav.fadeOut();
		} else if("up" === e && i >= _hideShowOffset) {
			$nav.fadeIn();
		}
	}
	
	_lastScroll = t;
	});
});

/**
 * blog관련 script display를 별도 분리 처리 하지 않음
 */
var blog = {
	/**
	 * 포스트 삭제
	 * 
	 * @param blogId
	 */
	remove : function(blogId) {
		console.log("blog.remove");
		$("<form />").attr({
			"action" : "/blog/" + blogId,
			"method" : "post"
		}).append($("<input />").attr({
			"type" : "hidden",
			"name" : "_method"
		}).val("delete")).submit();
	},
	/**
	 * data-blog-content attribute가 선언된 태그의 안에 해당 content 삽입 처
	 */
	displayContext : function() {
		$("[data-blog-content]").each(function() {
			console.log($(this).attr("data-blog-content"));
			$(this).html($(this).attr("data-blog-content"));
		});
	}
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
				editMenu : "td:eq(6)",
				cancelMenu : "td:eq(7)",
			}
		},
		url : {
			modify : "{0}user/{1}/asset/{2}"
		}
	},
	/**
	 * event 대상 target
	 */
	currentTarget : null,
	
	/* (s) util */
	setContextPath : function(contextPath) {
		this.config.contextPath = contextPath;
	},
	setUserId : function(userId) {
		this.config.userId = userId;
	},
	/**
	 * modify url을 구함.
	 * argument 0 : contextPath
	 * argument 1 : userId
	 * argument 2 : assetId - from assetData 
	 */
	getUrlModify : function() {
		return this.config.url.modify.format(this.config.contextPath, this.config.userId, this.getAssetData().id);	
	},
	/**
	 * ui에서 획득한 asset data를 저장
	 */
	setAssetDataFromUi : function() {
		if (this.currentTarget.data(this.config.dataKey) != null) {
			return;
		}
		console.log("data-asset set and get");
		//해당 tr의 데이터 추출
		var asset = this.getAssetDataFromUi();
		//해당 데이터를 보관
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
			.find(this.config.uiPosition.assetGroup.name).text(asset.assetGroup.name);
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
				assetGroup : {
					name : this.currentTarget.find(this.config.uiPosition.assetGroup.name).text()
				}
		};
	},
	/* (e) util */
	/* (s) action */
	modify : function(asset) {
		
		console.log("modify : ", this.getUrlModify());
		asset._method = "put";
		console.log("test : ", this.getAssetData().assetGroup.name);
		this.getAssetData().assetGroup = null;
		$.ajax({
			url : this.getUrlModify(),
			type : "post",
			data : asset,
			success : function() {
				
			}
		});
	},
	/* (e) action */
	/* (s) ui method */
	showEditMenu : function() {
		var assetObj = this;
		
		this.currentTarget.find(this.config.uiPosition.menu.editMenu).empty().append(
			$("<span>").addClass("glyphicon glyphicon-edit").attr("title", "edit").css("cursor", "pointer").tooltip().on("click", function(event) {
				console.log("eventClickEdit");
				var asset = assetObj.getAssetData();
				var changedAsset = assetObj.getAssetDataFromUi();
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
				console.log("change");
				assetObj.modify(changedAsset);
			})
		).append(" ").append(
			$("<span>").addClass("glyphicon glyphicon-remove").attr("title", "remove").css("cursor", "pointer").tooltip()
		).append(" ").append(
			$("<span>").addClass("glyphicon glyphicon-refresh").attr("title", "reset").css("cursor", "pointer").tooltip().on("click", function(event) {
				assetObj.setUiFromAssetData();
			})
		);
	},
	hideEditMenu : function() {
		this.currentTarget.find(this.config.uiPosition.menu.editMenu).empty();	
	},
	changeNormalText : function() {
		
	},
	showCancelMenu : function() {
		
	},
	/* (e) ui method */
	/* (s) event method */
	eventEnter : function() {
		assetObj = this;
		$(this.config.uiPosition.target).on("mouseenter", function(event) {
			assetObj.currentTarget = $(this);
			assetObj.setAssetDataFromUi();
			//수정 and 삭제 아이콘 활성화
			assetObj.showEditMenu();
			event.preventDefault();
		});
	},
	eventLeave : function() {
		$(this.config.uiPosition.target).on("mouseleave", function(event) {
			assetObj.hideEditMenu();
			event.preventDefault();
		});
	},
	start : function() {
		this.eventEnter();
		this.eventLeave();
	}
	/* (s) event method */
};