package com.example.demo.controller;

import com.example.demo.model.User;
import com.example.demo.repositories.UserRepository;
import com.example.demo.service.UserService;
import com.stripe.Stripe;
import com.stripe.model.Customer;
import com.stripe.model.EphemeralKey;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.EphemeralKeyCreateParams;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/stripe")
public class StripeController {

    private final UserService userService;

    private final UserRepository userRepository;

    @Value("${stripe.secret.key}")
    private String stripeSecretKey;

    public StripeController(UserService userService,UserRepository userRepository) {
        this.userService = userService;
        this.userRepository = userRepository;
    }
    @PostMapping("/create-ephemeral-key")
    public ResponseEntity<Map<String, String>> createEphemeralKey(
            Authentication authentication,
            @RequestBody Map<String, String> request
    ) {

        String apiVersion = request.get("apiVersion");
        if (apiVersion == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Missing Stripe API version"));
        }

        try {
            Stripe.apiKey = stripeSecretKey;

            String userEmail = authentication.getName();
            User user = userService.getUserEntityByEmail(userEmail);


            user = ensureStripeCustomerExists(user);


            EphemeralKeyCreateParams params = EphemeralKeyCreateParams.builder()
                    .setCustomer(user.getStripeCustomerId())
                    .setStripeVersion(apiVersion)
                    .build();

            EphemeralKey key = EphemeralKey.create(params);

            Map<String, String> response = new HashMap<>();
            response.put("ephemeralKey", key.getSecret());
            response.put("customerId", user.getStripeCustomerId());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    private User ensureStripeCustomerExists(User user) throws Exception {

        if (user.getStripeCustomerId() != null) {
            return user;
        }


        CustomerCreateParams params = CustomerCreateParams.builder()
                .setEmail(user.getEmail())
                .build();
        Customer stripeCustomer = Customer.create(params);
        user.setStripeCustomerId(stripeCustomer.getId());
        userRepository.save(user);

        return user;
    }

}
