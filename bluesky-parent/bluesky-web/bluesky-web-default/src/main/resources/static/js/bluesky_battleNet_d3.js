$(document).ready(function() {
	
	
	var BattleNet = function() {
		var _profile = {};
		var _hero = {};
		var _item = {};
		
		var _getMyProfile = function() {
			$.ajax({
				url : "/battleNet/d3/my/profile.json"
			}).success(function(data) {
				_profile[data.battleTag] = data;
				_addLambdaCareerProfile(data.battleTag);
				$(".content-battleNet").html(Mustache.render(_getProfileTemplate(), data));
			});
		}
		
		var _addLambdaCareerProfile = function(battleTag) {
			var data = _profile[battleTag];
			data.getLinkBattleTag = function() {
				return this.battleTag.replace("#", "-");
			}
			data.getClassName = function() {
				if (this["class"] == "monk") {
					return "수도사";
				} else if (this["class"] == "demon-hunter") {
					return "악마사냥꾼";
				} else if (this["class"] == "barbarian") {
					return "야만용사";
				} else if (this["class"] == "wizard") {
					return "마법사";
				} else if (this["class"] == "witch-doctor") {
					return "부두술사";
				} else if (this["class"] == "crusader") {
					return "성전사";
				} else {
					return "테스트";
				}
			}
		}
		
		/**
		 * careerProfile template
		 */
		var _profileTemplate = null;
		var _getProfileTemplate = function() {
			if (_profileTemplate != null) {
				return _profileTemplate;
			}
			var result = $.ajax({
				url : "/html/battleNet/d3/profile.html",
				async : false
			})
			_profileTemplate = result.responseText;
			return _profileTemplate;
		}
		
		var _getCareerProfile = function(battleTag) {
			if (_profile[battleTag] != undefined) {
				_addLambdaCareerProfile(battleTag);
				$(".content-battleNet").html(Mustache.render(_getProfileTemplate(), _profile[battleTag]));
				return;
			} 
			$.ajax({
				url : "/battleNet/d3/profile/" + encodeURIComponent(battleTag) + ".json",
				beforeSend : function() {
					$("form[name=battleTagForm]").append('<div class="text-center"><i class="fa fa-spinner fa-pulse"></i></div>');
				},
				complete : function() {
					$("form[name=battleTagForm]").find(".fa-spinner").parent().remove();
				}
			}).success(function(data) {
				_profile[battleTag] = data;
				_addLambdaCareerProfile(battleTag);
				$(".content-battleNet").html(Mustache.render(_getProfileTemplate(), data));
			});
		}
		
		var _getHeroProfile = function(battleTag, heroId) {
			if (_hero[battleTag] != undefined && _hero[battleTag][heroId] != undefined) {
				_displayHeroProfile(battleTag, heroId);
				return ;
			}
			$.ajax({
				url : "/battleNet/d3/profile/" + encodeURIComponent(battleTag) + "/hero/" + heroId + ".json",
				beforeSend : function() {
					$("#hero-" + heroId + " .panel-body").html('<div class="text-center"><i class="fa fa-spinner fa-pulse"></i></div>');
				},
				success : function(data) {
					if (_hero[battleTag] == undefined) {
						_hero[battleTag] = {};
					}
					_hero[battleTag][heroId] = data;
					_displayHeroProfile(battleTag, heroId);
				}
			});
		}
		
		var _displayHeroProfile = function(battleTag, heroId) {
			var data = _hero[battleTag][heroId];
			var template = _getHeroProfileTemplate();
			
			var partials = {"heroProfileItem" : _getHeroProfileItemTemplate()}
			
			for (var key in data.items) {
				data.items[key].getKey = key;
			}
			data.getKeyName = function() {
				switch (this.getKey) {
				case "head":
					return "머리";
				case "shoulders":
					return "어깨";
				case "torso":
					return "몸통";
				case "waist":
					return "허리";
				case "bracers":
					return "손목";
				case "hands":
					return "손";
				case "legs":
					return "다리";
				case "feet":
					return "발";
				case "mainHand":
					return "주 무기";
				case "offHand":
					return "보조 무기";
				case "neck":
					return "목";
				case "rightFinger":
					return "오른쪽 손가락";
				case "leftFinger":
					return "왼쪽 손가락";
				default:
					return "테스트";
				}
				return "";
			};
			
			$("#hero-" + heroId + " .panel-body").html(Mustache.render(template, data, partials));
		}
		
		var _getItemData = function(tooltipParams) {
			if (_item[tooltipParams] != undefined) {
				_displayItemData(tooltipParams);
				return;
			}
			$.ajax({
				url : "/battleNet/d3/data/" + tooltipParams + ".json",
				beforeSend : function() {$("#d3-itemData").html('<div class="text-center"><i class="fa fa-spinner fa-pulse fa-2x"></i></div>');},
				success : function(data) {
					if (_item[tooltipParams] == undefined) {
						_item[tooltipParams] = {};
					}
					_item[tooltipParams] = data;
					var template = _getItemDataTemplate();
					_displayItemData(tooltipParams);
				}
			});
		}
		
		var _displayItemData = function(tooltipParams) {
			var data = _item[tooltipParams];
			var template = _getItemDataTemplate();
			
			data.isSetItemsEquipped = function() {
				for (var key in data.setItemsEquipped) {
					if (this.id == data.setItemsEquipped[key]) {
						return true;
					} 
				}
				return false;
			}
			data.isDisplayGemName = function() {
				return this.attributes.passive.length > 0;
			}
			data.getGemNameColor = function() {
				return this.attributes.passive[0].color;
			}
			
			$("#d3-itemData").html(Mustache.render(template, data));
		}
		

		
		var _heroProfileTemplate = null;
		var _getHeroProfileTemplate = function() {
			if (_heroProfileTemplate != null) {
				return _heroProfileTemplate;
			}
			var result = $.ajax({
				url : "/html/battleNet/d3/heroProfile.html",
				async : false
			})
			_heroProfileTemplate = result.responseText;
			return _heroProfileTemplate;
		}
		
		var _heroProfileItemTemplate = null;
		var _getHeroProfileItemTemplate = function() {
			if (_heroProfileItemTemplate != null) {
				return _heroProfileItemTemplate;
			}
			var result = $.ajax({
				url : "/html/battleNet/d3/heroProfileItem.html",
				async : false
			})
			_heroProfileItemTemplate = result.responseText;
			return _heroProfileItemTemplate;
		}
		
		var _itemDataTemplate = null;
		var _getItemDataTemplate = function() {
			if (_itemDataTemplate != null) {
				return _itemDataTemplate;
			}
			var result = $.ajax({
				url : "/html/battleNet/d3/itemData.html",
				async : false
			})
			_itemDataTemplate = result.responseText;
			return _itemDataTemplate;
		}
		return { 
			getMyProfile : function() {
				_getMyProfile();
			},
			getCareerProfile : function(battleTag) {
				_getCareerProfile(battleTag);
			},
			getHeroProfile : function(battleTag, heroId) {
				_getHeroProfile(battleTag, heroId);
			},
			getItemData : function(tooltipParams) {
				_getItemData(tooltipParams);
			}
		}
	};
	
	var battleNet = BattleNet();
	
	//battleNet.getMyProfile();
	
	$(document).on("click", "[data-hero-id]", function(event) {
		event.preventDefault();
		var battleTag = $(this).attr("data-battleTag");
		var heroId = $(this).attr("data-hero-id");
		battleNet.getHeroProfile(battleTag, heroId);
	});
	
	$(document).on("click", "[data-d3-itemData]", function() {
		event.preventDefault();
		var tooltipParams = $(this).attr("data-d3-itemData");
		battleNet.getItemData(tooltipParams);
		$(".sidebar-offcanvas").offset({top : $(document).scrollTop() + 10});
	});
	
	/**
	 * mobile 에서 우측 사이드바 토글 처리
	 */
	$(document).on("click", "[data-toggle-offcanvas=on]", function () {
		$(".row-offcanvas").addClass("active");
	});
	
	$(document).on("click", "[data-toggle-offcanvas=off]", function () {
		$(".row-offcanvas").removeClass("active");
	});
	
	$(document).on("click", ".row-offcanvas.active", function() {
		$(".row-offcanvas").removeClass("active");
	});
	
	$(document).on("submit", "form[name=battleTagForm]", function() {
		var battleTag = $(this).find("#battleTag").val(); 
		console.log("test ", battleTag);
		if (battleTag == "") {
			alert("")
		};
		battleNet.getCareerProfile(battleTag);
		return false;
	})
	
	$("body").css("padding-top", "0px");
	$(".navbar").remove();
	$(".breadcrumb").hide();
	$("footer").hide();
	$(document).on("scroll", function() {
		$(".sidebar-offcanvas").offset({top : $(document).scrollTop() + 10});
	});
});