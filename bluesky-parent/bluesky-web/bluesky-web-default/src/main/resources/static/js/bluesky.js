/**
 * json list 의 정렬을 위해 제공되는 함수 (2차 정렬까지만 제공 - 다른 형태의 구현을 고민할 필요가 있음)
 * @param a
 * @param b
 * @param targetKey
 * @param targetArray
 * @param target2ndKey
 * @param target2ndArray
 * @returns
 */
function sortFunc(a, b, targetKey, targetArray, target2ndKey, target2ndArray) {
	var aCodeIndex = $.inArray(a[targetKey], targetArray);
	var bCodeIndex = $.inArray(b[targetKey], targetArray);
	var cCodeIndex = $.inArray(a[target2ndKey], target2ndArray);
	var dCodeIndex = $.inArray(b[target2ndKey], target2ndArray);
	if (aCodeIndex > -1 && bCodeIndex > -1) {
		if (aCodeIndex > bCodeIndex) return 1;
		else if (aCodeIndex < bCodeIndex) return -1;
		else {
			if (cCodeIndex > -1 && dCodeIndex > -1) {
				if (cCodeIndex > dCodeIndex) return 1;
				else if (cCodeIndex < dCodeIndex) return -1;
				else return 0;
			}
			else if (cCodeIndex > -1) return -1;
			else if (dCodeIndex > -1) return 1;

			return 0;
		}
	}
	else if (aCodeIndex > -1) return -1;
	else if (bCodeIndex > -1) return 1;

	return 0;
}

/**
 * 에러 메세지 정렬을 하는 경우 아래 값을 추가하여 호출함
 */
$.sortKey = null;
$.sortKeyOrder = [];
$.sort2ndKey = null;
$.sort2ndKeyOrder = [];

/**
 * String format 처리
 * 문자열에 "{0}.."와 같이 대치 예약어가 있는 경우 넘겨받은 argument로 치환 처리함 
 * @returns
 */
String.prototype.format = function() {
	var args = arguments;
	return this.replace(/\{(\d+)\}/g, function() {
		return args[arguments[1]];
	});
};
	
/**
 * 상단 메뉴 표시 contextPath에 대한 메뉴 url 변경 적용 - 사용하지 않음
 */
var navbar = {
	contextPath : "/",
	display : function() {
		if (location.pathname === this.contextPath) {
			$(".navbar .nav li:eq(0)").addClass("active");
			return;
		}
		$(".navbar .navbar-nav li").each(function() {
			if (location.pathname.search($(this).text()) > 0) {
				$(this).addClass("active");
			} else {
				$(this).removeClass();
			}
		});
	}
};

/**
 * jquery form serializeArray 를 json으로 변환
 * spring data rest
 */
var makeFormDataJSON = function(targetForm) {
	var json = {};
	$.each(targetForm.serializeArray(), function(idx, ele){
		json[ele.name] = ele.value;
	});
	return JSON.stringify(json);
}

/**
 * spring data rest 응답에서 id를 추출
 */
var getIdFromDataRest = function(data) {
	var parts = data._links.self.href.split("/");
	return parts[parts.length - 1];
}

/**
 * 유저 정보 관련 data
 */
var UserInfo = function(userId) {
	var _userId = userId;
	var _targetUserId;
	
	var _isLogin = function() {
		return _userId !== undefined && _userId != "";
	}
	return {
		getUserId : function() {
			return _userId;
		},
		setTargetUserId : function(targetUserId) {
			_targetUserId = targetUserId;
		},
		/**
		 * 로그인 여부 확인
		 */
		isLogin : function() {
			return _isLogin();
		},
		/**
		 * 로그인한 유저가 대상 유저와 일치하는지 확인
		 */
		isLoginUser : function(targetUserId) {
			console.log(targetUserId);
			return _isLogin() && _userId == targetUserId; 
		}
	}
}

$(document).ready(function() {
	
	/**
	 * 상단 navbar scroll에 따른 hide 처리 
	 */
	var $nav = $(".navbar"),
	_hideShowOffset = 20,
	_lastScroll = 0,
	_detachPoint = 50;
	
	$(window).scroll(function() {
		var t = $(window).scrollTop(),
			e = t > _lastScroll ? "down" : "up",
			i = Math.abs(t - _lastScroll);
		
		if (t >= _detachPoint || 0 >= t || t > -1) {
			if ("down" === e && i >= _hideShowOffset) {
				$nav.fadeOut();
			} else if("up" === e && i >= _hideShowOffset) {
				$nav.fadeIn();
			}
		}
		_lastScroll = t;
	});
	
	//$("body").tooltip({ selector : "[data-toggle=tooltip]" });
	 $('[data-toggle="tooltip"]').tooltip()
	
	/* (s) csrf */
	/* 
	var token = $("meta[name='_csrf']").attr("content");
	var header = $("meta[name='_csrf_header']").attr("content");
	$(document).ajaxSend(function(e, xhr, options) {
		xhr.setRequestHeader(header, token);
	});
	*/
	/* (s) csrf */
	
	/**
	 * ajax 전역 에러 처리
	 */
	$(document).ajaxError(function(event, jqXHR, ajaxSettings, thrownError) {
		var alertMessage = "에러가 발생하였습니다.";
		if (jqXHR.status == 401) {
			alert("로그인이 필요합니다.")
			return;
		}
		
		if (jqXHR.responseJSON === undefined) {
			//alert(alertMessage);
			return;
		}
		var result = jqXHR.responseJSON.result;
		if (result === undefined) {
			alert(alertMessage);
			return;
		}
		
		if ($.isArray(result) && result[0].displayableMessage) {
			function errorMessageSort(a, b) {
				return sortFunc(a, b, $.sortKey, $.sortKeyOrder, $.sort2ndKey, $.sort2ndKeyOrder);
			}
			result.sort(errorMessageSort);
			alertMessage = result[0].message;
		} else if (result.displayableMessage) {
			alertMessage = jqXHR.responseJSON.result.message;
		}
		alert(alertMessage);
	});
	
	
	/**
	 * https로 접근한 경우 http로 재이동 처리
	 */
	if (location.protocol === "https:") {
		location.href = location.href.replace("https:", "http:").replace(":8443", ":8082");
	}

	
	/**
	 * banner print
	 */
	if (window.console !== undefined) {
		setTimeout(console.log.bind(console, "%cBluesky","font: 8em Arial; color: #6799FF; font-weight:bold"), 0);
		setTimeout(console.log.bind(console, "%c - bluesky 프로젝트","font: 2em HY견고딕,sans-serif; color: #333;"), 0);
	}
	
	/**
	 * 취소 버튼 동작
	 */
	$("button.cancel").on("click", function() {
		history.back();
	});
	
	Vue.component("common-nav", {
		template : "#common-nav",
		props : ["page"],
		data : function() {
			return {
			};
		}
	});
});
