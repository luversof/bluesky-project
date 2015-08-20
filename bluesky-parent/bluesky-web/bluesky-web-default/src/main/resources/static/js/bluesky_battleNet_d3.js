$(document).ready(function() {
	
	
	var BattleNet = function() {
		var _profile = {};
		var _hero = {};
		
		var _getMyProfile = function() {
			var result = $.ajax({
				url : "/battleNet/d3/my/profile.json",
				async : false
			});
			_profile[result.responseJSON.battleTag] = result.responseJSON;
			return _profile[result.responseJSON.battleTag];
		}
		
		var _getProfile = function(battleTag) {
			if (_profile[battleTag] != undefined) {
				return _profile[battleTag];
			} 
			var result = $.ajax({
				url : "/battleNet/d3/profile/" + encodeURIComponent(battleTag) + ".jaon",
				async : false
			});
			return result.responseJSON;
		}
		
		var _getHeroProfile = function(battleTag, heroId) {
			if (_hero[battleTag] != undefined && _hero[battleTag][heroId] != undefined) {
				return _hero[battleTag][heroId];
			}
			var result = $.ajax({
				url : "/battleNet/d3/profile/" + encodeURIComponent(battleTag) + "/hero/" + heroId + ".jaon",
				async : false
			});
			if (_hero[battleTag] == undefined) {
				_hero[battleTag] = {};
			}
			_hero[battleTag][heroId] = result.responseJSON;
			
			return _hero[battleTag][heroId];
		}
		
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
		
		return { 
			getMyProfile : function() {
				var data = _getMyProfile();
				var template = _getProfileTemplate();
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
				data.getIconThumbnailUrl = function() {
					var classPath = this["class"].replace("-", "");
					if (this["class"] == "crusader") {
						classPath = "x1_" + classPath;
					}
					return "http://media.blizzard.com/d3/icons/portraits/21/" + classPath + "_" + (this.gender == 0 ? "male" : "female") + ".png";
				}
				
				//console.log("data : ", data);
				//console.log("template : ", template);
				
				$(".content-battleNet").html(Mustache.render(template, data));
			},
			getHeroProfile : function(battleTag, characterId) {
				var data = _getHeroProfile(battleTag, characterId);
				var template = _getHeroProfileTemplate();
				
				$("#character-" + characterId + " .panel-body").html(Mustache.render(template, data));
			}
		}
	};
	
	var battleNet = BattleNet();
	
	battleNet.getMyProfile();
	
	$(document).on("click", "[data-character-id]", function() {
		var battleTag = $(this).attr("data-battleTag");
		var characterId = $(this).attr("data-character-id");
		battleNet.getHeroProfile(battleTag, characterId);
	});
});