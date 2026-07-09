package com.roze.thundercall.controller;

import com.roze.thundercall.security.JwtTokenProvider;
import com.roze.thundercall.utils.BaseResponse;
import com.roze.thundercall.utils.BaseController;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequiredArgsConstructor
@RequestMapping("/deeplink")
public class DeepLinkController extends BaseController {
    private final JwtTokenProvider jwtTokenProvider;

    @GetMapping("/auth")
    public ResponseEntity<BaseResponse<Void>> handleDeepLink(@RequestParam String token,
                                                             @RequestParam(required = false) String platform,
                                                             HttpServletResponse response) throws IOException {
        if (!jwtTokenProvider.validateToken(token)) {
            return error("Token not valid", HttpStatus.UNAUTHORIZED);
        }

        String redirectUrl = switch (platform != null ? platform : "desktop") {
            case "mac" -> "app://auth?token=" + token;
            case "linux" -> "app://auth?token=" + token;
            case "windows" -> "app://auth?token=" + token;
            default -> "http://localhost:8084/auth_success?token=" + token;
        };
        response.sendRedirect(redirectUrl);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/auth_success")
    public String authSuccess(@RequestParam String token) {
        return """
                <html>
                <body>
                <h1>Authentication Successful</h1>
                <p>You can now close this window and return to the application</p>
                <script>window.close()</script>
                </body>
                </html>
                """;

    }
}
