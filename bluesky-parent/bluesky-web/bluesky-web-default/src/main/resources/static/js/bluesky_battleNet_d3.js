$(document).ready(function() {
	
	
	var BattleNet = function() {
		
		var _getMyProfile = function() {
			var result = $.ajax({
				url : "/battleNet/d3/my/profile.json",
				async : false
			});
			return result.responseJSON;
		}
		
		var _getProfile = function(profile) {
			var result = $.ajax({
				url : "/battleNet/d3/profile/" + encodeURIComponent(profile),
				async : false
			});
			return result.responseJSON;
		}
		
		var _getHeroProfile = function(profile, heroId) {
			var result = $.ajax({
				url : "/battleNet/d3/profile/" + encodeURIComponent(profile) + "/hero/" + heroId,
				async : false
			});
			return result.responseJSON;
		}
		
		var _getProfileTemplate = function() {
			var result = $.ajax({
				url : "/html/battleNet/d3/profile.html",
				async : false
			})
			return result.responseText;
		}
		
		return { 
			getMyProfile : function() {
				var json = _getMyProfile();
				var template = _getProfileTemplate();
				json.getLinkBattleTag = function() {
					return this.battleTag.replace("#", "-");
				}
				json.getClassName = function() {
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
				json.getIconThumbnailUrl = function() {
					var classPath = this["class"].replace("-", "");
					if (this["class"] == "crusader") {
						classPath = "x1_" + classPath;
					}
					return "http://media.blizzard.com/d3/icons/portraits/21/" + classPath + "_" + (this.gender == 0 ? "male" : "female") + ".png";
				}
				
				console.log("json : ", json);
				console.log("template : ", template);
				
				$(".content-battleNet").html(Mustache.render(template, json));
			}
		}
	};
	
	var battleNet = BattleNet();
	
	battleNet.getMyProfile();
});