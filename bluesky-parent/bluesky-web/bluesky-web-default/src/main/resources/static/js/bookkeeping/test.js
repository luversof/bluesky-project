$(document).ready(function() {
	var Bookkeeping = function() {
		
		var _getMyBookkeeping = function() {
			
			$.ajax({
				url : "/bookkeeping.json",
				dataType : "json"
			}).success(function(data) {
				console.log("data : ", data);
			});
		}
		
		return {
			getMyBookkeeping : function() {
				_getMyBookkeeping();
			}
		}
	};
	
	
	var bookkeeping = Bookkeeping();
	bookkeeping.getMyBookkeeping();
});