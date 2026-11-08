package com.peopledesk.config;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * When the Angular build is bundled into the jar (Docker image), deep links such as /employees
 * must return index.html so the Angular router can take over. API and file paths are excluded.
 */
@Controller
public class SpaController {

    @GetMapping({"/login", "/employees", "/reports"})
    public String forwardToIndex() {
        return "forward:/index.html";
    }
}
