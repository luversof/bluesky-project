$(document).ready(function() {
	
	function addBookkeeping() {
		var parameter = {
			name : $(".add-bookkeeping input[name=name]").val()
		};
		
		$.ajax({
			url : "/bookkeeping.json",
			type : "post",
			data : parameter,
			success : function(data) {
				console.log(data);
			}
		});
	}
	
	$(".add-bookkeeping button").on("click", addBookkeeping);
});
