package com.example.demo.controller;

import com.example.demo.service.UserService;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.net.Webhook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payment")
public class StripeWebhookController {

    private final UserService userService;

    @Value("${stripe.secret.key}")
    private String stripeSecretKey;

    @Value("${stripe.webhook.secret}")
    private String stripeWebhookSecret;

    public StripeWebhookController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/webhook")
    public ResponseEntity<String> handleStripeWebhook(@RequestBody String payload, @RequestHeader("Stripe-Signature") String sigHeader) {
        try {
            Event event = Webhook.constructEvent(
                    payload, sigHeader, stripeWebhookSecret
            );

            if ("payment_intent.succeeded".equals(event.getType())) {
                PaymentIntent paymentIntent = (PaymentIntent) event.getDataObjectDeserializer().getObject().orElse(null);

                if (paymentIntent != null) {
                    String userEmail = paymentIntent.getMetadata().get("userEmail");
                    String planType = paymentIntent.getMetadata().get("planType");


                    userService.updateSubscription(userEmail, planType, 1);

                    return ResponseEntity.ok("Subscription updated");
                }
            }
            return ResponseEntity.ok("Event received");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Webhook error: " + e.getMessage());
        }
    }
}
