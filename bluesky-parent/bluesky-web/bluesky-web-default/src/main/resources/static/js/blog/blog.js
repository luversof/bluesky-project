$(document).ready(function() {
	var blog = function() {
		return {
			create : function() {
				var form = $("[name=blog-create]");
				$.ajax({
					type : form.attr("method"),
					url : form.attr("action"),
					dataType : "json",
					success : function(data) {
						location.href = $.i18n.prop("url.blog.view.list", data.id);
					}
				});
			}
		};
	}();
	
	$("[name=blog-create] :submit").on("click", function(e) {
		e.preventDefault();
		blog.create();
	});
});