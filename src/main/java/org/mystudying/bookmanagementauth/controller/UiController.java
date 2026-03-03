package org.mystudying.bookmanagementauth.controller;

import org.mystudying.bookmanagementauth.config.UserPrincipal;
import org.mystudying.bookmanagementauth.services.UserService;
import org.mystudying.bookmanagementauth.services.VerificationService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Controller for serving Thymeleaf UI templates.
 * Data fetching is primarily handled by client-side JavaScript via REST APIs.
 */
@Controller
public class UiController {

    private final UserService userService;
    private final VerificationService verificationService;

    public UiController(UserService userService, VerificationService verificationService) {
        this.userService = userService;
        this.verificationService = verificationService;
    }

    @GetMapping("/")
    public String index() {
        return "index";
    }

    @GetMapping("/books")
    public String books(Model model) {
        model.addAttribute("showSidebar", true);
        model.addAttribute("sidebarType", "books");
        return "books";
    }

    @GetMapping("/books/{id}")
    public String bookDetails(@PathVariable long id, Model model) {
        // We only need to check if the book exists to show the page shell.
        // Data is fetched via /api/books/{id} in book-details.js
        model.addAttribute("bookId", id);
        return "book";
    }

    @GetMapping("/authors")
    public String authors(Model model) {
        model.addAttribute("showSidebar", true);
        model.addAttribute("sidebarType", "authors");
        return "authors";
    }

    @GetMapping("/authors/{id}")
    public String authorDetails(@PathVariable long id, Model model) {
        model.addAttribute("authorId", id);
        return "author";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/register")
    public String register() {
        return "register";
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public String profile(@AuthenticationPrincipal UserPrincipal principal, Model model) {
        if (principal != null) {
            var user = userService.findById(principal.getId()).orElse(null);
            model.addAttribute("user", user);
        }
        model.addAttribute("showSidebar", true);
        model.addAttribute("sidebarType", "user_profile");
        return "user";
    }

    @GetMapping("/users/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public String userDetails(@PathVariable Long id, Model model) {
        var user = userService.findById(id).orElse(null);
        model.addAttribute("user", user);
        model.addAttribute("showSidebar", true);
        model.addAttribute("sidebarType", "user_profile");
        return "user";
    }

    @GetMapping("/reports")
    @PreAuthorize("hasRole('ADMIN')")
    public String reports(Model model) {
        model.addAttribute("showSidebar", true);
        model.addAttribute("sidebarType", "reports");
        return "reports";
    }

    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public String users() {
        return "users";
    }

    @GetMapping("/reset-password")
    public String resetPassword(@RequestParam("token") String token, Model model) {
        model.addAttribute("token", token);
        return "reset-password";
    }

    @GetMapping("/verify")
    public String verifyAccount(@RequestParam("token") String token, Model model) {
        try {
            verificationService.verifyToken(token);
            model.addAttribute("message", "Your account has been successfully verified! You can now log in.");
            model.addAttribute("isSuccess", true);
        } catch (Exception e) {
            model.addAttribute("message", "Account verification failed: " + e.getMessage());
            model.addAttribute("isSuccess", false);
        }
        return "verification-result"; // A new Thymeleaf template to display the result
    }
}
