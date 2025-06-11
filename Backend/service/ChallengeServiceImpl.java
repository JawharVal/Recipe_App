package com.example.demo.service;

import com.example.demo.dto.ChallengeDTO;
import com.example.demo.dto.RecipeDTO;
import com.example.demo.model.*;
import com.example.demo.repositories.*;
import com.example.demo.service.ChallengeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ChallengeServiceImpl implements ChallengeService {

    @Autowired
    private ChallengeRepository challengeRepository;

    @Autowired
    private RecipeRepository recipeRepository;
    @Autowired
    private RecipeService recipeService;


    @Autowired
    private RecipeSubmissionRepository recipeSubmissionRepository;
    @Autowired
    private GlobalLeaderboardRepository leaderboardRepository ;

    @Autowired
    private FeaturedWinnerRepository featuredWinnerRepository;


    @Autowired
    private UserRepository userRepository;

    @Lazy
    @Autowired
    private UserService userService;
    @Autowired
    private FeaturedChallengeRepository featuredChallengeRepository;

    @Override
    public List<ChallengeDTO> getAllChallenges() {
        List<Challenge> challenges = challengeRepository.findAll();
        challenges.forEach(challenge -> {
            boolean isActive = !LocalDate.now().isAfter(challenge.getDeadline());
            challenge.setActive(isActive);
        });
        return challenges.stream().map(this::mapToDTO).collect(Collectors.toList());
    }
    @Override
    public List<ChallengeDTO> getFeaturedChallenges() {
        List<FeaturedChallenge> featuredChallenges = featuredChallengeRepository.findAll();
        return featuredChallenges.stream()
                .map(fc -> mapToDTO(fc.getChallenge()))
                .collect(Collectors.toList());
    }

    @Override
    public ChallengeDTO featureChallenge(Long challengeId) {
        Challenge challenge = challengeRepository.findById(challengeId)
                .orElseThrow(() -> new RuntimeException("Challenge not found"));

        if (featuredChallengeRepository.findByChallenge(challenge).isPresent()) {
            throw new IllegalStateException("Challenge is already featured.");
        }

        FeaturedChallenge featuredChallenge = new FeaturedChallenge(challenge);
        featuredChallengeRepository.save(featuredChallenge);
        return mapToDTO(challenge);
    }

    @Override
    public void unfeatureChallenge(Long challengeId) {
        Challenge challenge = challengeRepository.findById(challengeId)
                .orElseThrow(() -> new RuntimeException("Challenge not found"));

        featuredChallengeRepository.findByChallenge(challenge)
                .ifPresent(featuredChallengeRepository::delete);
    }
    //@Scheduled(fixedRate = 15000)
    @Scheduled(cron = "0 0 0 * * ?") // Runs daily at midnight
    @Transactional
    public void resetExpiredChallengesAndLeaderboard() {
        LocalDate today = LocalDate.now();
        List<Challenge> challenges = challengeRepository.findAll();


        for (Challenge challenge : challenges) {
            if (!challenge.isFeatured() && challenge.getDeadline().isBefore(today)) {
                List<RecipeSubmission> submissionsToDelete = challenge.getRecipeSubmissions();

                if (!submissionsToDelete.isEmpty()) {
                    recipeSubmissionRepository.deleteAll(submissionsToDelete);
                    challenge.getRecipeSubmissions().clear();
                    challengeRepository.save(challenge);
                    System.out.println("❌ Deleted submissions for Expired Weekly Challenge ID: " + challenge.getId());
                }

                challenge.setActive(false);
                challengeRepository.save(challenge);
            }
        }

        Challenge featuredChallenge = challenges.stream()
                .filter(Challenge::isFeatured)
                .filter(ch -> ch.getDeadline().isBefore(today) || ch.getDeadline().isEqual(today))
                .findFirst()
                .orElse(null);

        if (featuredChallenge != null) {
            featuredWinnerRepository.deleteAll();

            // Retrieve global leaderboard entries and compute the top 3
            List<GlobalLeaderboardEntry> topThree = leaderboardRepository.findAll().stream()
                    .sorted(Comparator.comparingInt(GlobalLeaderboardEntry::getTotalPoints).reversed())
                    .limit(3)
                    .collect(Collectors.toList());

            // Map them to FeaturedWinner entities
            List<FeaturedWinner> winnersToSave = topThree.stream()
                    .map(entry -> {
                        User user = userRepository.findByEmailIgnoreCase(entry.getUserEmail()).orElse(null);
                        String username = (user != null && user.getUsername() != null) ? user.getUsername() : entry.getUserEmail();
                        return new FeaturedWinner(entry.getUserEmail(), username, entry.getTotalPoints());
                    })
                    .collect(Collectors.toList());

            // Save the top three winners to the FeaturedWinner table
            featuredWinnerRepository.saveAll(winnersToSave);
            System.out.println("Saved Featured Winners: ");
            for (FeaturedWinner winner : winnersToSave) {
                System.out.println("Winner: " + winner.getUserEmail() + " with " + winner.getTotalPoints() + " points");
            }
            List<GlobalLeaderboardEntry> leaderboardEntries = leaderboardRepository.findAll()
                    .stream()
                    .sorted(Comparator.comparingInt(GlobalLeaderboardEntry::getTotalPoints).reversed())
                    .collect(Collectors.toList());

            if (leaderboardEntries.isEmpty()) {
                System.out.println("⚠️ No leaderboard entries found. Skipping badge awards.");
            } else {
                if (leaderboardEntries.size() >= 1) {
                    System.out.println("Awarding Master Chef to: " + leaderboardEntries.get(0).getUserEmail());
                    userService.awardBadgeToUser(leaderboardEntries.get(0).getUserEmail(), "Master Chef");
                }
                if (leaderboardEntries.size() >= 2) {
                    System.out.println("Awarding Elite Cook to: " + leaderboardEntries.get(1).getUserEmail());
                    userService.awardBadgeToUser(leaderboardEntries.get(1).getUserEmail(), "Elite Cook");
                }
                if (leaderboardEntries.size() >= 3) {
                    System.out.println("Awarding Challenger Star to: " + leaderboardEntries.get(2).getUserEmail());
                    userService.awardBadgeToUser(leaderboardEntries.get(2).getUserEmail(), "Challenger Star");
                }
            }


            leaderboardRepository.deleteAll();
            System.out.println("✅ Global leaderboard has been reset.");

            System.out.println("Global leaderboard has been reset.");

            // Delete submissions for the featured challenge.
            List<RecipeSubmission> featuredSubmissions = featuredChallenge.getRecipeSubmissions();
            if (!featuredSubmissions.isEmpty()) {
                recipeSubmissionRepository.deleteAll(featuredSubmissions);
                featuredChallenge.getRecipeSubmissions().clear();
                challengeRepository.save(featuredChallenge);
                System.out.println("Deleted submissions for Featured Challenge ID: " + featuredChallenge.getId());
            }
            leaderboardRepository.deleteAll();
        }

        // Remove "featured" status from expired challenge
        featuredChallenge.setFeatured(false);
        challengeRepository.save(featuredChallenge);

        // Automatically feature the next challenge with the nearest deadline
        Optional<Challenge> nextFeatured = challenges.stream()
                .filter(ch -> ch.getDeadline().isAfter(today) && !ch.isFeatured())
                .min(Comparator.comparing(Challenge::getDeadline));

        if (nextFeatured.isPresent()) {
            Challenge newFeatured = nextFeatured.get();
            newFeatured.setFeatured(true);
            challengeRepository.save(newFeatured);
            System.out.println("🎉 New Featured Challenge: " + newFeatured.getTitle());
        } else {
            System.out.println("⚠️ No upcoming challenges found!");
        }

        recalculateLeaderboard();
        System.out.println("✅ Leaderboard recalculated after reset.");



    }

    @Override
    public List<FeaturedWinner> getFeaturedWinners() {
        return featuredWinnerRepository.findAll()
                .stream()
                .sorted(Comparator.comparingInt(FeaturedWinner::getTotalPoints).reversed())
                .collect(Collectors.toList());
    }

    @Transactional
    public void recalculateLeaderboard() {
        System.out.println("🔄 Recalculating global leaderboard...");

        List<Challenge> challenges = challengeRepository.findAll();
        Map<String, Integer> globalUserPoints = new HashMap<>();
        // Map to record each user's earliest submission id (smaller id means older)
        Map<String, Long> userEarliestSubmissionId = new HashMap<>();

        for (Challenge challenge : challenges) {
            int maxPoints = challenge.getPoints();
            List<RecipeSubmission> submissions = challenge.getSubmissions();
            if (submissions == null || submissions.isEmpty()) continue;

            Map<String, RecipeSubmission> bestSubmissionByUser = new HashMap<>();
            for (RecipeSubmission sub : submissions) {
                String userEmail = sub.getUser().getEmail();
                // Compare likes first; if equal, choose the one with the smaller (older) id.
                bestSubmissionByUser.merge(userEmail, sub, (existing, newSub) ->
                        (newSub.getRecipe().getLikes() > existing.getRecipe().getLikes()) ||
                                (newSub.getRecipe().getLikes() == existing.getRecipe().getLikes() &&
                                        newSub.getId() < existing.getId())
                                ? newSub : existing
                );
            }

            List<RecipeSubmission> bestSubmissions = new ArrayList<>(bestSubmissionByUser.values());
            bestSubmissions.sort(
                    Comparator.comparingInt((RecipeSubmission s) -> s.getRecipe().getLikes()).reversed()
                            .thenComparing(RecipeSubmission::getId) // Tie-breaker: lower id wins
            );

            Map<String, Integer> challengePoints = new HashMap<>();
            for (int i = 0; i < bestSubmissions.size(); i++) {
                RecipeSubmission sub = bestSubmissions.get(i);
                String userEmail = sub.getUser().getEmail();
                if (sub.getRecipe().getLikes() > 0) {  // Only users with likes get points
                    int pointsEarned;
                    switch (i) {
                        case 0:
                            pointsEarned = maxPoints;
                            break;
                        case 1:
                            pointsEarned = (int) (maxPoints * 0.7);
                            break;
                        case 2:
                            pointsEarned = (int) (maxPoints * 0.5);
                            break;
                        default:
                            pointsEarned = (int) (maxPoints * 0.1);
                            break;
                    }
                    challengePoints.put(userEmail, pointsEarned);
                    // Record earliest submission id for tie-breaking.
                    Long subId = sub.getId();
                    if (!userEarliestSubmissionId.containsKey(userEmail) ||
                            subId < userEarliestSubmissionId.get(userEmail)) {
                        userEarliestSubmissionId.put(userEmail, subId);
                    }
                }
            }
            // Sum challenge points into the global points.
            for (var entry : challengePoints.entrySet()) {
                globalUserPoints.merge(entry.getKey(), entry.getValue(), Integer::sum);
            }
        }

        System.out.println("🔥 Updated Leaderboard:");
        globalUserPoints.forEach((user, points) ->
                System.out.println("User: " + user + " - Points: " + points)
        );

        // Build a list of leaderboard entries along with their earliest submission ids.
        List<Map.Entry<GlobalLeaderboardEntry, Long>> leaderboardEntriesWithIds =
                globalUserPoints.entrySet().stream()
                        .map(entry -> {
                            User user = userRepository.findByEmail(entry.getKey()).orElse(null);
                            String username = user != null ? user.getUsername() : entry.getKey();
                            GlobalLeaderboardEntry gle = new GlobalLeaderboardEntry(entry.getKey(), username, entry.getValue());
                            // Look up the earliest submission id for this user (may be null if not recorded)
                            Long earliestId = userEarliestSubmissionId.get(entry.getKey());
                            return new AbstractMap.SimpleEntry<>(gle, earliestId);
                        })
                        .collect(Collectors.toList());

        // Sort by total points descending; if tied, the one with the smaller (older) id wins.
        leaderboardEntriesWithIds.sort((e1, e2) -> {
            int cmp = Integer.compare(e2.getKey().getTotalPoints(), e1.getKey().getTotalPoints());
            if (cmp == 0) {
                Long id1 = e1.getValue();
                Long id2 = e2.getValue();
                if (id1 == null && id2 == null) return 0;
                if (id1 == null) return 1;
                if (id2 == null) return -1;
                return id1.compareTo(id2);
            } else {
                return cmp;
            }
        });

        List<GlobalLeaderboardEntry> newLeaderboard = leaderboardEntriesWithIds.stream()
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        // Replace the leaderboard with the new ordering.
        leaderboardRepository.deleteAll();
        leaderboardRepository.saveAll(newLeaderboard);
    }


    @Override
    public List<Recipe> getSubmittedRecipes(Long challengeId) {
        Challenge challenge = challengeRepository.findById(challengeId)
                .orElseThrow(() -> new RuntimeException("Challenge not found"));

        //  Ensure Lazy-Loaded Collections are initialized before returning data
        List<Recipe> submittedRecipes = challenge.getSubmittedRecipes();
        System.out.println("Fetched " + submittedRecipes.size() + " submitted recipes for Challenge " + challengeId);

        return submittedRecipes;
    }
    @Override
    public ChallengeDTO submitRecipe(Long challengeId, Long recipeId, String userEmail) {
        Challenge challenge = challengeRepository.findById(challengeId)
                .orElseThrow(() -> new RuntimeException("Challenge not found"));
        Recipe recipe = recipeRepository.findById(recipeId)
                .orElseThrow(() -> new RuntimeException("Recipe not found"));
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Check if the user has already submitted this recipe to the challenge
        boolean alreadySubmitted = recipeSubmissionRepository.existsByChallengeAndRecipe(challenge, recipe);
        if (alreadySubmitted) {
            throw new IllegalStateException("This recipe has already been submitted for this challenge!");
        }

        // Log to debug
        System.out.println("User " + userEmail + " is submitting Recipe ID: " + recipeId + " to Challenge ID: " + challengeId);

        long userSubmissions = recipeSubmissionRepository.countByChallengeAndUser(challenge, user);
        if (userSubmissions >= challenge.getMaxSubmissions()) {
            throw new IllegalStateException("Submission limit reached for this challenge!");
        }

        // Create and save the submission
        RecipeSubmission submission = new RecipeSubmission();
        submission.setChallenge(challenge);
        submission.setRecipe(recipe);
        submission.setUser(user);
        submission.setSubmissionDate(LocalDate.now());
        recipeSubmissionRepository.save(submission);

        System.out.println("Recipe submitted successfully for Challenge ID: " + challengeId);

        return mapToDTO(challenge);
    }


    @Override
    public ChallengeDTO getChallengeById(Long id) {
        Challenge challenge = challengeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Challenge not found"));

        boolean isActive = !LocalDate.now().isAfter(challenge.getDeadline());
        challenge.setActive(isActive);

        // Convert Challenge entity to DTO
        ChallengeDTO dto = mapToDTO(challenge);

        // Ensure submitted recipes are included
        List<Recipe> submittedRecipes = challenge.getSubmittedRecipes();
        List<RecipeDTO> recipeDTOs = submittedRecipes.stream()
                .map(recipeService::mapEntityToDTO)
                .collect(Collectors.toList());
        dto.setSubmittedRecipes(recipeDTOs);
        return dto;
    }


    @Override
    public ChallengeDTO createChallenge(ChallengeDTO challengeDTO) {
        Challenge challenge = mapToEntity(challengeDTO);
        challenge.setActive(false);
        // Save the new challenge first
        Challenge savedChallenge = challengeRepository.save(challenge);

        // If it's featured, ensure it's in the FeaturedChallenge table
        if (challengeDTO.isFeatured()) {
            Optional<FeaturedChallenge> existingFeatured = featuredChallengeRepository.findByChallenge(savedChallenge);
            if (existingFeatured.isEmpty()) {
                FeaturedChallenge newFeatured = new FeaturedChallenge(savedChallenge);
                featuredChallengeRepository.save(newFeatured);
                System.out.println("✅ Challenge added to Featured Table: " + savedChallenge.getTitle());
            }
        }

        return mapToDTO(savedChallenge);
    }

    @Override
    public List<GlobalLeaderboardEntry> getGlobalLeaderboard() {
        return leaderboardRepository.findAll()
                .stream()
                .sorted(Comparator.comparingInt(GlobalLeaderboardEntry::getTotalPoints).reversed())
                .collect(Collectors.toList());
    }


    @Override
    public ChallengeDTO updateChallenge(Long id, ChallengeDTO challengeDTO) {
        Challenge challenge = challengeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Challenge not found"));

        challenge.setTitle(challengeDTO.getTitle());
        challenge.setDescription(challengeDTO.getDescription());
        challenge.setImageUrl(challengeDTO.getImageUrl());
        challenge.setDeadline(LocalDate.parse(challengeDTO.getDeadline()));
        challenge.setPoints(challengeDTO.getPoints());
        challenge.setMaxSubmissions(challengeDTO.getMaxSubmissions());
        boolean wasFeatured = challenge.isFeatured();  // Store old featured status
        challenge.setFeatured(challengeDTO.isFeatured());

        // Save changes first
        Challenge updatedChallenge = challengeRepository.save(challenge);

        // Handle featured/unfeatured logic
        if (challengeDTO.isFeatured()) {
            Optional<FeaturedChallenge> existingFeatured = featuredChallengeRepository.findByChallenge(updatedChallenge);
            if (existingFeatured.isEmpty()) {
                FeaturedChallenge newFeatured = new FeaturedChallenge(updatedChallenge);
                featuredChallengeRepository.save(newFeatured);
                System.out.println("✅ Edited challenge added to Featured Table: " + updatedChallenge.getTitle());
            }
        } else if (wasFeatured) {
            // If it was featured but is no longer featured, remove it from the table
            featuredChallengeRepository.findByChallenge(updatedChallenge).ifPresent(featuredChallengeRepository::delete);
            System.out.println("🚫 Challenge removed from Featured Table: " + updatedChallenge.getTitle());
        }

        return mapToDTO(updatedChallenge);
    }


    @Override
    public void voteChallenge(Long id, int voteValue) {
        Challenge challenge = challengeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Challenge not found"));
        challenge.setPoints(challenge.getPoints() + voteValue);
        challengeRepository.save(challenge);
    }

    @Override
    public boolean isRecipeAlreadySubmitted(Long recipeId) {
        return challengeRepository.findAll().stream()
                .flatMap(challenge -> challenge.getSubmittedRecipes().stream())
                .anyMatch(recipe -> recipe.getId().equals(recipeId));
    }
    @Override
    @Transactional
    public void deleteChallenge(Long id) {
        Optional<Challenge> challengeOpt = challengeRepository.findById(id);

        if (challengeOpt.isPresent()) {
            Challenge challenge = challengeOpt.get();

            // Remove the FeaturedChallenge entry
            featuredChallengeRepository.deleteByChallenge(challenge);

            // Remove related recipe submissions
            recipeSubmissionRepository.deleteByChallenge(challenge);

            // Finally, delete the challenge
            challengeRepository.deleteById(id);
        }
    }


    // Helper methods to map between entity and DTO.
    private ChallengeDTO mapToDTO(Challenge challenge) {
        ChallengeDTO dto = new ChallengeDTO();
        dto.setId(challenge.getId());
        dto.setTitle(challenge.getTitle());
        dto.setDescription(challenge.getDescription());
        dto.setImageUrl(challenge.getImageUrl());
        dto.setDeadline(challenge.getDeadline().toString());
        dto.setPoints(challenge.getPoints());
        dto.setActive(challenge.isActive());
        dto.setMaxSubmissions(challenge.getMaxSubmissions());
        dto.setFeatured(challenge.isFeatured());
        List<Recipe> recipes = challenge.getSubmittedRecipes();
        List<RecipeDTO> recipeDTOs = recipes.stream()
                .map(recipeService::mapEntityToDTO) // Or your custom method
                .collect(Collectors.toList());
        dto.setSubmittedRecipes(recipeDTOs);
        return dto;
    }

    private Challenge mapToEntity(ChallengeDTO dto) {
        Challenge challenge = new Challenge();
        challenge.setTitle(dto.getTitle());
        challenge.setDescription(dto.getDescription());
        challenge.setImageUrl(dto.getImageUrl());
        challenge.setDeadline(LocalDate.parse(dto.getDeadline()));
        challenge.setPoints(dto.getPoints());
        // When creating a new entity, default to active; status can be recalculated later.
        challenge.setActive(true);
        challenge.setMaxSubmissions(dto.getMaxSubmissions());
        challenge.setFeatured(dto.isFeatured());
        return challenge;
    }
}
