package net.luversof.web.index.controller;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;


import lombok.Data;
import net.luversof.web.util.DevCheckUtil;

@Controller
@RequestMapping(value = "/_check", produces = MediaType.TEXT_HTML_VALUE)
public class DevCheckViewController {
	
	@Autowired
	private RequestMappingHandlerMapping requestMappingHandlerMapping;
	
	@GetMapping({"", "/index"})
	public String index(ModelMap modelMap) {
		Map<RequestMappingInfo, HandlerMethod> handlerMethodMap = requestMappingHandlerMapping.getHandlerMethods().entrySet().stream()
				.filter(handlerMapping -> handlerMapping.getValue().getBean().toString().toLowerCase().contains("devcheckcontroller") && handlerMapping.getKey().getPatternsCondition().getPatterns().stream().anyMatch(pattern -> pattern.startsWith("/_check")) 
						&& handlerMapping.getKey().getProducesCondition().getExpressions().stream().anyMatch(mediaTypeExpression -> mediaTypeExpression.getMediaType().equals(MediaType.APPLICATION_JSON)))
				.collect(Collectors.toMap(Entry::getKey, Entry::getValue));
		
		List<DevCheckInfo> devCheckInfoList = new ArrayList<>();
		handlerMethodMap.entrySet().forEach(map -> devCheckInfoList.add(new DevCheckInfo(map)));
		modelMap.addAttribute("devCheckInfoList", devCheckInfoList.stream().sorted(Comparator.comparing(DevCheckInfo::getBeanName).thenComparing(devCheckInfo-> devCheckInfo.getUrlList().get(0))).collect(Collectors.toList()));
		return "_check/index";
	}
	
	@Data
	public static class DevCheckInfo {
		public DevCheckInfo(Entry<RequestMappingInfo, HandlerMethod> handlerMethodMap) {
			this.beanName = handlerMethodMap.getValue().getBean().toString().replace("DevCheckController", "");
			this.urlList = new ArrayList<>();
			for (String url : handlerMethodMap.getKey().getPatternsCondition().getPatterns()) {
				urlList.add(DevCheckUtil.getUrlWithParameter(url, handlerMethodMap.getValue().getMethod()));
			}
			this.handlerMethodMap = handlerMethodMap;
		}
		
		private String beanName;
		private List<String> urlList;
		Entry<RequestMappingInfo, HandlerMethod> handlerMethodMap;
	}
}
