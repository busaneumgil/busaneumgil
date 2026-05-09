package com.ssafy.e102.global.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AdminWebController {

	@GetMapping({"/admin-web", "/admin-web/"})
	public String adminWeb() {
		return "forward:/admin-web/index.html";
	}
}
