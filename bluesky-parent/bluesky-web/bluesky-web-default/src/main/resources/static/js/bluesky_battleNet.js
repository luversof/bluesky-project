$(document).ready(function() {
	
	
	var BattleNet = function() {
		
		var _getMyProfile = function() {
			$.ajax({
				url : "/battleNet/d3/my/profile.json",
				async : false
			});
		}
		
		return { 
			getMyProfile : function() {
				_getMyProfile();
			}
		}
	};
	
	var battleNet = BattleNet();
	
	battleNet.getMyProfile();
});