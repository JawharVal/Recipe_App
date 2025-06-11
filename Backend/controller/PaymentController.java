package com.example.demo.controller;

import com.example.demo.model.User;
import com.example.demo.repositories.UserRepository;
import com.example.demo.service.UserService;
import com.example.demo.model.SubscriptionRequest;
import com.google.protobuf.ByteString;
import com.stripe.Stripe;
import com.stripe.model.Customer;
import com.stripe.model.EphemeralKey;
import com.stripe.model.PaymentIntent;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.EphemeralKeyCreateParams;
import com.stripe.param.PaymentIntentCreateParams;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import com.google.cloud.vision.v1.AnnotateImageRequest;
import com.google.cloud.vision.v1.AnnotateImageResponse;
import com.google.cloud.vision.v1.BatchAnnotateImagesResponse;
import com.google.cloud.vision.v1.Feature;
import com.google.cloud.vision.v1.Feature.Type;
import com.google.cloud.vision.v1.Image;
import com.google.cloud.vision.v1.ImageAnnotatorClient;
import com.google.cloud.vision.v1.Likelihood;
import com.google.cloud.vision.v1.SafeSearchAnnotation;
import com.google.protobuf.ByteString;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/payment")
public class PaymentController {
    private final UserService userService;

    private final UserRepository userRepository;

    @Value("${stripe.secret.key}")
    private String stripeSecretKey;

    public PaymentController(UserService userService,UserRepository userRepository) {
        this.userService = userService;
        this.userRepository = userRepository;
    }


    @PostMapping("/create-intent")
    public ResponseEntity<Map<String, String>> createPaymentIntent(
            @RequestParam String plan,
            Authentication authentication
    ) {
        Stripe.apiKey = stripeSecretKey;

        String userEmail = authentication.getName();
        User user = userService.getUserEntityByEmail(userEmail);

        if (user.getStripeCustomerId() == null || user.getStripeCustomerId().isEmpty()) {
            try {
                CustomerCreateParams customerParams = CustomerCreateParams.builder()
                        .setEmail(user.getEmail())
                        .build();
                Customer customer = Customer.create(customerParams);
                user.setStripeCustomerId(customer.getId());
                userService.save(user);
            } catch (com.stripe.exception.StripeException e) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "Failed to create Stripe Customer: " + e.getMessage()
                ));
            }
        }

        long amount = switch (plan.toUpperCase()) {
            case "PLUS" -> 44900;
            case "PRO"  -> 79900;
            default     -> throw new IllegalArgumentException("Invalid plan type");
        };

        try {
            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                    .setAmount(amount)
                    .setCurrency("rub")
                    .addPaymentMethodType("card")
                    .setCustomer(user.getStripeCustomerId())
                    .setSetupFutureUsage(PaymentIntentCreateParams.SetupFutureUsage.OFF_SESSION)
                    .putMetadata("userEmail", userEmail)
                    .putMetadata("planType", plan)
                    .build();

            PaymentIntent intent = PaymentIntent.create(params);

            Map<String, String> response = new HashMap<>();
            response.put("clientSecret", intent.getClientSecret());
            response.put("paymentIntentId", intent.getId());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
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
    @PostMapping("/confirm")
    public ResponseEntity<String> confirmPayment(@RequestBody Map<String, String> request, Authentication authentication) {
        String paymentIntentId = request.get("paymentIntentId");
        String planType = request.get("planType");

        if (paymentIntentId == null || paymentIntentId.isBlank()) {
            return ResponseEntity.status(400).body("Missing paymentIntentId");
        }

        try {

            PaymentIntent intent = PaymentIntent.retrieve(paymentIntentId);
            if ("succeeded".equals(intent.getStatus())) {

                String userEmail = authentication.getName();


                userService.updateSubscription(userEmail, planType, 1);

                return ResponseEntity.ok("Subscription updated successfully");
            } else {
                return ResponseEntity.status(400).body("Payment not completed.");
            }
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error confirming payment: " + e.getMessage());
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
