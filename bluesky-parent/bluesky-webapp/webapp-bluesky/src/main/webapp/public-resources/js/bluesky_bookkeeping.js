$(document).ready(function() {
	
	function addBookkeeping() {
		var parameter = {
				
		};
		
		$.ajax({
			url : "/bookkeeping",
			type : "post",
			data : parameter,
			success : function() {
				
			}
		});
	}
	
	$(".add-bookkeeping").on("click", addBookkeeping);
});
