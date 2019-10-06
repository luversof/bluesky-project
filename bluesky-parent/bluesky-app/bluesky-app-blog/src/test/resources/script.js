var test = function(applicationContext, blogRepository) {
	var blogList = blogRepository.findAll();
	
	var blogList2 = applicationContext.getBean("blogRepository").findAll();
	
	print("blogList : ", blogList);
	print("blogList2 : ", blogList2);
	
	for each (var blog in blogList;) {
		print("blog", blog);
		print("blog id ", blog.getId());
		print("blog id check", blog.getId() == "b68f7647-6ddd-4b8c-aecf-352e82ad764e")
			
	}
	
	if (true) {
		throw ("ERROR Message")
	}
	
};

var test2 = function() {
	
};


var a = 12;