$(document).ready(function() {
	
	
	var BattleNet = function() {
		
		var _getMyProfile = function() {
			var result = $.ajax({
				url : "/battleNet/d3/my/profile.json",
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
				
				console.log("json : ", json);
				console.log("template : ", template);
				
				$(".content-battleNet").html(Mustache.render(template, json));
			}
		}
	};
	
	var battleNet = BattleNet();
	
	battleNet.getMyProfile();
});