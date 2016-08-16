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
 * 상단 메뉴 표시 contextPath에 대한 메뉴 url 변경 적용
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
	
	$("body").tooltip({ selector : "[data-toggle=tooltip]" });
	
	/* (s) csrf */
	var token = $("meta[name='_csrf']").attr("content");
	var header = $("meta[name='_csrf_header']").attr("content");
	$(document).ajaxSend(function(e, xhr, options) {
		xhr.setRequestHeader(header, token);
	});
	/* (s) csrf */
	
	$(document).ajaxError(function(event, jqXHR, ajaxSettings, thrownError) {
		if (ajaxSettings.dataType === "json") {
			//var b = "";
			//for (var a in jqXHR) b+=a + "\n;";
			//console.log(b);
			//console.log(jqXHR.responseJSON);
		}
	});
	
	
	/**
	 * https로 접근한 경우 http로 재이동 처리
	 */
	if (location.protocol === "https:") {
		location.href = location.href.replace("https:", "http:").replace(":8443", ":8082");
	}

	
	if (window.console !== undefined) {
		setTimeout(console.log.bind(console, "%cBluesky","font: 8em Arial; color: #6799FF; font-weight:bold"), 0);
		setTimeout(console.log.bind(console, "%c - bluesky 프로젝트","font: 2em HY견고딕,sans-serif; color: #333;"), 0);
	}
});