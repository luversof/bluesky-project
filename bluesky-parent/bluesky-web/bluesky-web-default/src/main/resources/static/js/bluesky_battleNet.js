$(document).ready(function() {
	
	
	var BattleNet = function() {
		
		var _getMyProfile = function() {
			$.ajax({
				url : "/battleNet/d3/my/profile.json",
				async : false
			});
		}
		
		var _getProfileTemplate = function() {
			$.ajax({
				url : "/html/battleNet/d3/profile.html",
				async : false
			})
		}
		
		return { 
			getMyProfile : function() {
				_getMyProfile();
				_getProfileTemplate();
			}
		}
	};
	
	var battleNet = BattleNet();
	
	battleNet.getMyProfile();
});