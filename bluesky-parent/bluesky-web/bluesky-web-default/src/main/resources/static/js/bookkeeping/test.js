// 가계부 조회
function getBookkeeping() {
	fetch("/bookkeeping.json").then(function(response) {
		return response.json();
	}).then(function(myJson) {
		console.log(myJson);
	});
}

function getBookkeeping2() {
	var request = new XMLHttpRequest();
	request.open("GET", "/bookkeeping.json", true);
	request.onreadystatechange = function() {
		if (request.readyState != 4 || request.status != 200)
			return;
		alert("Success: " + request.responseText);
	};
	request.send();
}

function createBookkeeping() {
	var request = new XMLHttpRequest();
	request.open("POST", "/bookkeeping.json", true);
	request.setRequestHeader('Content-Type', 'application/x-www-form-urlencoded');
	request.onreadystatechange = function() {
		if (request.readyState != 4 || request.status != 200)
			return;
		alert("Success: " + request.responseText);
	};
	request.send(encodeURI('name=test'));
}

//$.ajax({
//	url : "bookkeeping.json",
//	type : "post",
//	data : { "aaa" : "ccc" }
//})

function errorHandler(err) {
	console.log("ERROR ", err);
}


var Bookkeeping = function() {
	return {
		getBookkeeping : function() {
			return fetch("/bookkeeping.json", { 
				method: "GET",
				credentials: "same-origin",
				headers : {
					"Content-type" : "application/json"
				}
			}).then(response => {
				if (response.ok) {
					return response.json();
				} else {
					//console.log("err  : ", response.json());
					return Promise.reject({ response : response.json() })
				}
			}).catch(err => console.error("test ", err));
		}
	}
}
var bookkeeping = Bookkeeping();

