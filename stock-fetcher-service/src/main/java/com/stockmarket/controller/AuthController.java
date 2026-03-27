//package com.stockmarket.controller;
//
//import com.stockmarket.auth.MarketDataAuthenticationService;
//import com.stockmarket.auth.AuthenticationContext;
//import com.stockmarket.model.AuthTokenResponse;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//
//import java.io.IOException;
//import java.util.UUID;
//
//@Slf4j
//@RestController
//@RequestMapping("/api/auth")
//@RequiredArgsConstructor
//public class AuthController {
//
//    private final MarketDataAuthenticationService authService;
//
//    /**
//     * Endpoint to obtain authentication token (cookies + crumb)
//     * This should be called when the Angular app starts
//     */
//    @PostMapping("/token")
//    public ResponseEntity<AuthTokenResponse> getToken() {
//        try {
//            log.info("Token request received from client");
//
//            // Get or refresh authentication context
//            AuthenticationContext authContext = authService.getAuthContext();
//
//            // Generate a simple session token (UUID) for tracking
//            String sessionToken = UUID.randomUUID().toString();
//
//            // Calculate expiry time (in seconds)
//            long expiresIn = authContext.getRemainingValiditySeconds();
//
//            // Build response
//            AuthTokenResponse response = new AuthTokenResponse(
//                    sessionToken,
//                    authContext.getCookies(),
//                    authContext.getCrumb(),
//                    expiresIn,
//                    System.currentTimeMillis()
//            );
//
//            log.info("Token successfully generated. Expires in {} seconds", expiresIn);
//            return ResponseEntity.ok(response);
//
//        } catch (IOException e) {
//            log.error("Failed to generate authentication token", e);
//            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
//                    .body(null);
//        } catch (Exception e) {
//            log.error("Unexpected error during token generation", e);
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
//                    .body(null);
//        }
//    }
//
//    /**
//     * Endpoint to refresh authentication token
//     */
//    @PostMapping("/refresh")
//    public ResponseEntity<AuthTokenResponse> refreshToken() {
//        try {
//            log.info("Token refresh request received");
//
//            // Force refresh
//            authService.invalidate();
//            AuthenticationContext authContext = authService.refreshAuthContext();
//
//            String sessionToken = UUID.randomUUID().toString();
//            long expiresIn = authContext.getRemainingValiditySeconds();
//
//            AuthTokenResponse response = new AuthTokenResponse(
//                    sessionToken,
//                    authContext.getCookies(),
//                    authContext.getCrumb(),
//                    expiresIn,
//                    System.currentTimeMillis()
//            );
//
//            log.info("Token successfully refreshed. Expires in {} seconds", expiresIn);
//            return ResponseEntity.ok(response);
//
//        } catch (IOException e) {
//            log.error("Failed to refresh authentication token", e);
//            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
//                    .body(null);
//        }
//    }
//
//    /**
//     * Health check endpoint
//     */
//    @GetMapping("/health")
//    public ResponseEntity<String> health() {
//        boolean isValid = authService.isValid();
//        if (isValid) {
//            return ResponseEntity.ok("Authentication service is healthy");
//        } else {
//            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
//                    .body("Authentication context is invalid or expired");
//        }
//    }
//
//    /**
//     * Invalidate current authentication
//     */
//    @PostMapping("/invalidate")
//    public ResponseEntity<String> invalidate() {
//        log.info("Invalidation request received");
//        authService.invalidate();
//        return ResponseEntity.ok("Authentication context invalidated");
//    }
//}
