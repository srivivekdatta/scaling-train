package com.example.lookup;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class LookupController {
    private final LookupRepository repository;

    public LookupController(LookupRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/")
    public String form(HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store");
        return "lookup";
    }

    @PostMapping("/")
    public String search(@RequestParam(name = "id", defaultValue = "") String id,
                         Model model, HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store");
        String lookupId = id.strip();
        if (lookupId.isEmpty() || lookupId.length() > 128) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            model.addAttribute("error", "Enter an ID between 1 and 128 characters.");
            return "lookup";
        }

        model.addAttribute("id", lookupId);
        try {
            var groups = repository.find(lookupId);
            model.addAttribute("groups", groups);
            model.addAttribute("hasResults", groups.stream().anyMatch(group -> !group.rows().isEmpty()));
        } catch (DataAccessException ex) {
            response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            model.addAttribute("error", "The database lookup is unavailable. Try again shortly.");
        }
        return "lookup";
    }
}
