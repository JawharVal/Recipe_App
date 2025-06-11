
package com.example.demo.controller;

import com.example.demo.model.NewsletterSubscription;
import com.example.demo.repositories.NewsletterSubscriptionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;


@RestController
@RequestMapping("/api/newsletter")
public class NewsletterController {

    @Autowired
    private NewsletterSubscriptionRepository newsletterRepository;

    @Autowired
    private JavaMailSender mailSender;

    private static final String SUPPORT_EMAIL = "jawhar.maatouk1@gmail.com";

    @PostMapping("/subscribe")
    public ResponseEntity<String> subscribe(Authentication authentication) {
        String userEmail = authentication.getName();

        if (newsletterRepository.findByEmail(userEmail).isPresent()) {
            return new ResponseEntity<>("Already subscribed.", HttpStatus.CONFLICT);
        }

        NewsletterSubscription subscription = new NewsletterSubscription(userEmail);
        newsletterRepository.save(subscription);

        sendConfirmationEmail(userEmail);

        return new ResponseEntity<>("Subscribed successfully.", HttpStatus.CREATED);
    }
    @PreAuthorize("hasAuthority('ADMIN')")
    @GetMapping("/admin/subscribers")
    public ResponseEntity<List<NewsletterSubscription>> getAllSubscribers() {
        List<NewsletterSubscription> subs = newsletterRepository.findAll();
        return ResponseEntity.ok(subs);
    }

    @PreAuthorize("hasAuthority('ADMIN')")
    @PostMapping("/admin/bulkSend")
    public ResponseEntity<String> sendBulkNewsletter(@RequestBody Map<String, String> payload) {
        String messageBody = payload.getOrDefault("message", "(no content)");

        String signature = "\n\n---\nBest Regards,\nLeGourmand Team\n";

        String finalMessage = messageBody + signature;

        List<NewsletterSubscription> allSubs = newsletterRepository.findAll();
        int count = allSubs.size();

        for (NewsletterSubscription sub : allSubs) {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setTo(sub.getEmail());
            msg.setSubject("LeGourmand Newsletter");
            msg.setText(finalMessage);
            mailSender.send(msg);
        }

        return ResponseEntity.ok("Bulk message sent to " + count + " subscribers.");
    }

    @GetMapping("/isSubscribed")
    public ResponseEntity<Boolean> isSubscribed(Authentication authentication) {
        String userEmail = authentication.getName();
        boolean isSubscribed = newsletterRepository.findByEmail(userEmail).isPresent();
        return ResponseEntity.ok(isSubscribed);
    }

    @PostMapping("/unsubscribe")
    public ResponseEntity<String> unsubscribe(Authentication authentication) {
        String userEmail = authentication.getName();

        Optional<NewsletterSubscription> subscriptionOpt = newsletterRepository.findByEmail(userEmail);
        if (subscriptionOpt.isEmpty()) {
            return new ResponseEntity<>("Not subscribed.", HttpStatus.BAD_REQUEST);
        }

        newsletterRepository.delete(subscriptionOpt.get());

        sendUnsubscribeEmail(userEmail);

        return new ResponseEntity<>("Unsubscribed successfully.", HttpStatus.OK);
    }

    private void sendConfirmationEmail(String recipientEmail) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(recipientEmail);
        message.setSubject("Welcome to LeGourmand Newsletter!");
        message.setText("Thank you for subscribing to the LeGourmand newsletter. Stay tuned for the latest recipes and updates!");

        mailSender.send(message);
    }

    private void sendUnsubscribeEmail(String recipientEmail) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(recipientEmail);
        message.setSubject("Unsubscribed from LeGourmand Newsletter");
        message.setText("You have successfully unsubscribed from the LeGourmand newsletter. We're sorry to see you go!");

        mailSender.send(message);
    }
}