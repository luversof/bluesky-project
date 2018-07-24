$(document).ready(function() {
	 
	
	var blog = function() {
		var _link = {
			get : function(blogId) {
				return $.i18n.prop("url.blog.api.get", blogId);
			}
		}
		
		
		var _get = function(blogId) {
			return $.getJSON({
				url : _link.get(blogId)
			});
		} 
		
		return {
			create : function() {
				
			},
			get : function(blogId) {
				return _get(blogId);
			}
		};
	}();
	
	/*$(".blog-create").on("click", function(e) {
		e.preventDefault();
		blog.create();
	});*/
	
	var blogVue = new Vue({
		el : "#blogContent",
		components : {
			"blog-create" : {
				methods : {
					create : function() {
						$.ajax({
							type : "POST",
							url : $.i18n.prop("url.blog.api.create"),
							contentType:"application/hal+json; charset=utf-8",
							data : JSON.stringify({}),
							success : function(data) {
								location.href = $.i18n.prop("url.blog.view.list", data.id);
							}
						});
					}
				}
			}
		}
	});
});