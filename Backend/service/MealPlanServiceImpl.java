package com.example.demo.service;

import com.example.demo.dto.MealPlanDTO;
import com.example.demo.dto.NoteDTO;
import com.example.demo.dto.RecipeDTO;
import com.example.demo.model.MealPlan;
import com.example.demo.model.Note;
import com.example.demo.model.Recipe;
import com.example.demo.model.User;
import com.example.demo.repositories.MealPlanRepository;
import com.example.demo.repositories.NoteRepository;
import com.example.demo.repositories.RecipeRepository;
import com.example.demo.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class MealPlanServiceImpl implements MealPlanService {

    @Autowired
    private MealPlanRepository mealPlanRepository;

    @Autowired
    private NoteRepository noteRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RecipeRepository recipeRepository;

    @Override
    public MealPlanDTO getMealPlanForDate(String userEmail, LocalDate date) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));
        MealPlan mealPlan = mealPlanRepository.findByUserAndDate(user, date)
                .orElseGet(() -> {
                    MealPlan newMealPlan = new MealPlan(user, date);
                    return mealPlanRepository.save(newMealPlan);
                });
        return mapMealPlanToDTO(mealPlan);
    }

    @Override
    public List<MealPlanDTO> getAllMealPlans(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));
        List<MealPlan> mealPlans = mealPlanRepository.findByUser(user);
        return mealPlans.stream().map(this::mapMealPlanToDTO).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void addRecipeToMealPlan(String userEmail, LocalDate date, Long recipeId) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Recipe recipe = recipeRepository.findById(recipeId)
                .orElseThrow(() -> new RuntimeException("Recipe not found"));

        // Ensure the recipe belongs to the user
        if (!recipe.getAuthor().getEmail().equals(userEmail)) {
            throw new RuntimeException("Unauthorized to add this recipe to meal plan");
        }

        MealPlan mealPlan = mealPlanRepository.findByUserAndDate(user, date)
                .orElseGet(() -> {
                    MealPlan newMealPlan = new MealPlan(user, date);
                    return mealPlanRepository.save(newMealPlan);
                });

        if (!mealPlan.getRecipes().contains(recipe)) {
            mealPlan.getRecipes().add(recipe);
            mealPlanRepository.save(mealPlan);
        }
    }

    @Override
    @Transactional
    public void removeRecipeFromMealPlan(String userEmail, LocalDate date, Long recipeId) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Recipe recipe = recipeRepository.findById(recipeId)
                .orElseThrow(() -> new RuntimeException("Recipe not found"));

        MealPlan mealPlan = mealPlanRepository.findByUserAndDate(user, date)
                .orElseThrow(() -> new RuntimeException("Meal plan not found for the given date"));

        if (mealPlan.getRecipes().contains(recipe)) {
            mealPlan.getRecipes().remove(recipe);
            mealPlanRepository.save(mealPlan);
        }
    }

    @Override
    @Transactional
    public NoteDTO addNoteToMealPlan(String userEmail, LocalDate date, NoteDTO noteDTO) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        MealPlan mealPlan = mealPlanRepository.findByUserAndDate(user, date)
                .orElseGet(() -> {
                    MealPlan newMealPlan = new MealPlan(user, date);
                    return mealPlanRepository.save(newMealPlan);
                });

        Note note = new Note();
        note.setContent(noteDTO.getContent());
        note.setMealPlan(mealPlan);

        note = noteRepository.save(note);
        mealPlan.getNotes().add(note);
        mealPlanRepository.save(mealPlan);

        return mapNoteToDTO(note);
    }

    @Override
    @Transactional
    public void deleteNoteFromMealPlan(String userEmail, LocalDate date, Long noteId) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        MealPlan mealPlan = mealPlanRepository.findByUserAndDate(user, date)
                .orElseThrow(() -> new RuntimeException("Meal plan not found for the given date"));

        Note note = noteRepository.findById(noteId)
                .orElseThrow(() -> new RuntimeException("Note not found"));

        if (!note.getMealPlan().equals(mealPlan)) {
            throw new RuntimeException("Note does not belong to the specified meal plan");
        }

        mealPlan.getNotes().remove(note);
        noteRepository.delete(note);
    }

    // Utility methods to map entities to DTOs

    private MealPlanDTO mapMealPlanToDTO(MealPlan mealPlan) {
        MealPlanDTO dto = new MealPlanDTO();
        dto.setId(mealPlan.getId());
        dto.setDate(mealPlan.getDate());

        List<RecipeDTO> recipeDTOs = mealPlan.getRecipes().stream()
                .map(recipe -> {
                    RecipeDTO recipeDTO = new RecipeDTO();
                    recipeDTO.setId(recipe.getId());
                    recipeDTO.setTitle(recipe.getTitle());
                    recipeDTO.setAuthorUsername(recipe.getAuthor().getUsername());
                    recipeDTO.setAuthorId(recipe.getAuthor().getId());
                    // Map other fields as needed
                    return recipeDTO;
                })
                .collect(Collectors.toList());
        dto.setRecipes(recipeDTOs);

        List<NoteDTO> noteDTOs = mealPlan.getNotes().stream()
                .map(note -> {
                    NoteDTO noteDTO = new NoteDTO();
                    noteDTO.setId(note.getId());
                    noteDTO.setContent(note.getContent());
                    return noteDTO;
                })
                .collect(Collectors.toList());
        dto.setNotes(noteDTOs);

        return dto;
    }

    private NoteDTO mapNoteToDTO(Note note) {
        NoteDTO dto = new NoteDTO();
        dto.setId(note.getId());
        dto.setContent(note.getContent());
        return dto;
    }
}
