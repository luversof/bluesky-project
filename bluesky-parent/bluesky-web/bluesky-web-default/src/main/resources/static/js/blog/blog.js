$(document).ready(function() {
	 
	
	/*var */blog = function() {
		var _link = {
			get : function(blogId) {
				return "/api/blogs/{0}".format(blogId);
			}
		}
		
		
		var _get = function(blogId) {
			return $.getJSON({
				url : _link.get(blogId)
			});
		} 
		
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
			},
			get : function(blogId) {
				return _get(blogId);
			}
		};
	}();
	
	$("[name=blog-create] :submit").on("click", function(e) {
		e.preventDefault();
		blog.create();
	});
	
});