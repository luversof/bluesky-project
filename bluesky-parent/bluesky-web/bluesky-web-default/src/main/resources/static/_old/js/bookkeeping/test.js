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

function commonErrorHandler(response) {
	console.log("ERR ", response);
	response.json().then((data) => {
		console.log("error body", data.result);
	});
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
					throw response;
				}
			}).then(data => {
				if (data == null) {
					if (confirm("현재 소유한 가계부가 없습니다. 가계부를 생성하시겠습니까?")) {
						// 가계부 생성
					}
				} else {
					// 해당 가계부 노출?
				}
				
			}).catch(commonErrorHandler);
		}
	}
}
var bookkeeping = Bookkeeping();

