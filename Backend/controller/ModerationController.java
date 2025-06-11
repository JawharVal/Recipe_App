package com.example.demo.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ModerationController {

    private static final Logger log = LoggerFactory.getLogger(ModerationController.class);

    @Value("${sightengine.api.user}")
    private String sightengineUser;

    @Value("${sightengine.api.secret}")
    private String sightengineSecret;

    @PostMapping("/moderate-image")
    public ResponseEntity<Map<String, Boolean>> moderateImage(
            @RequestParam("fileUrl") String fileUrl
    ) {
        log.info("Received request to /api/moderate-image with fileUrl={}", fileUrl);

        try {

            String endpoint = "https://api.sightengine.com/1.0/check.json";
            String models = "nudity-2.1,weapon,alcohol,recreational_drug," +
                    "medical,properties,type,quality,offensive-2.0," +
                    "faces,scam,text-content,face-attributes,gore-2.0," +
                    "text,qr-content,tobacco,genai,violence,self-harm," +
                    "money,gambling";

            String urlParams = String.format(
                    "models=%s&api_user=%s&api_secret=%s&url=%s",
                    URLEncoder.encode(models, StandardCharsets.UTF_8),
                    URLEncoder.encode(sightengineUser, StandardCharsets.UTF_8),
                    URLEncoder.encode(sightengineSecret, StandardCharsets.UTF_8),
                    URLEncoder.encode(fileUrl, StandardCharsets.UTF_8)
            );
            String fullUrl = endpoint + "?" + urlParams;
            log.debug("Sightengine GET URL => {}", fullUrl);

            HttpURLConnection connection = (HttpURLConnection) new java.net.URL(fullUrl).openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(15_000);
            connection.setReadTimeout(15_000);

            int responseCode = connection.getResponseCode();
            if (responseCode != 200) {
                log.error("Sightengine API returned HTTP {}", responseCode);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("isAppropriate", false));
            }

            try (InputStream in = connection.getInputStream()) {
                ObjectMapper mapper = new ObjectMapper();
                JsonNode root = mapper.readTree(in);
                log.debug("Sightengine raw response => {}", root.toPrettyString());

                boolean isAppropriate = true;

                // --- Nudity Check ---

                JsonNode nudityNode = root.path("nudity");
                double sexualActivity = nudityNode.path("sexual_activity").asDouble(0.0);
                double sexualDisplay = nudityNode.path("sexual_display").asDouble(0.0);
                double erotica = nudityNode.path("erotica").asDouble(0.0);
                double verySuggestive = nudityNode.path("very_suggestive").asDouble(0.0);
                double suggestive = nudityNode.path("suggestive").asDouble(0.0);
                double mildlySuggestive = nudityNode.path("mildly_suggestive").asDouble(0.0);

                // Define threshold for nudity-related probabilities (adjust as needed)
                double nudityThreshold = 0.3;
                if (sexualActivity > nudityThreshold || sexualDisplay > nudityThreshold ||
                        erotica > nudityThreshold || verySuggestive > nudityThreshold ||
                        suggestive > nudityThreshold || mildlySuggestive > nudityThreshold) {
                    isAppropriate = false;
                    log.warn("High nudity levels detected: sexual_activity={}, sexual_display={}, erotica={}, very_suggestive={}, suggestive={}, mildly_suggestive={}",
                            sexualActivity, sexualDisplay, erotica, verySuggestive, suggestive, mildlySuggestive);
                }

                // --- Weapon Check ---
                JsonNode weaponClasses = root.path("weapon").path("classes");
                double firearmProb = weaponClasses.path("firearm").asDouble(0.0);
                double firearmToyProb = weaponClasses.path("firearm_toy").asDouble(0.0);
                double firearmGestureProb = weaponClasses.path("firearm_gesture").asDouble(0.0);
                double knifeProb = weaponClasses.path("knife").asDouble(0.0);
                double weaponThreshold = 0.05;
                if (firearmProb > weaponThreshold || firearmToyProb > weaponThreshold ||
                        firearmGestureProb > weaponThreshold || knifeProb > weaponThreshold) {
                    isAppropriate = false;
                    log.warn("Detected a weapon! firearm={}, knife={}, toy={}, gesture={}",
                            firearmProb, knifeProb, firearmToyProb, firearmGestureProb);
                }

                // --- Offensive Check ---
                // Offensive object returned as an object; iterate over its fields.
                JsonNode offensiveNode = root.path("offensive");
                boolean offensiveFlag = false;
                for (JsonNode value : offensiveNode) {
                    if (value.asDouble(0.0) > 0.3) {
                        offensiveFlag = true;
                        break;
                    }
                }
                if (offensiveFlag) {
                    isAppropriate = false;
                    log.warn("High offensive content detected.");
                }

                // --- Violence and Gore Check ---
                double violenceProb = root.at("/violence/prob").asDouble(0.0);
                double goreProb = root.at("/gore/prob").asDouble(0.0);
                if (violenceProb > 0.3 || goreProb > 0.3) {
                    isAppropriate = false;
                    log.warn("High violence/gore detected: violence_prob={}, gore_prob={}",
                            violenceProb, goreProb);
                }

                log.info("Determined isAppropriate={} based on Sightengine data", isAppropriate);
                return ResponseEntity.ok(Map.of("isAppropriate", isAppropriate));
            }

        } catch (Exception e) {
            log.error("Exception in /api/moderate-image for fileUrl={}", fileUrl, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("isAppropriate", false));
        }
    }
}
