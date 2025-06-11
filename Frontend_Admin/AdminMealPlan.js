// AdminMealPlan.js
import React, { useEffect, useState } from "react";
import axios from "axios";

// Utility: Generate an array of dates (as ISO strings) from start to end (inclusive)
const generateDateRange = (startDate, daysCount) => {
    const dates = [];
    for (let i = 0; i < daysCount; i++) {
        const d = new Date(startDate);
        d.setDate(d.getDate() + i);
        // Format as YYYY-MM-DD
        dates.push(d.toISOString().slice(0, 10));
    }
    return dates;
};

const AdminMealPlan = ({ userId, userName, token, onClose }) => {
    const [fetchedMealPlans, setFetchedMealPlans] = useState([]); // raw MealPlanDTOs from backend
    const [dateRange, setDateRange] = useState([]); // Array of date strings (YYYY-MM-DD)
    const [selectedDate, setSelectedDate] = useState("");
    const [recipeId, setRecipeId] = useState(""); // for adding a recipe
    const [noteContent, setNoteContent] = useState(""); // for adding a note
    const [error, setError] = useState("");

    // Fetch meal plans for this user (all records)
    const fetchMealPlans = async () => {
        try {
            const response = await axios.get(
                `http://localhost:8081/api/admin/users/${userId}/mealplans`,
                { headers: { Authorization: `Bearer ${token}` } }
            );
            setFetchedMealPlans(response.data);
        } catch (err) {
            console.error("Error fetching meal plans:", err);
            setError(err.message);
        }
    };

    // On component mount, generate a date range (e.g., today + next 14 days)
    useEffect(() => {
        const today = new Date();
        const range = generateDateRange(today, 14); // Adjust the number as needed
        setDateRange(range);
        if (range.length > 0) {
            setSelectedDate(range[0]); // default selected date is today
        }
        // Also fetch the MealPlans
        if (userId && token) {
            fetchMealPlans();
        }
    }, [userId, token]);

    // Find a meal plan for a given date (if it exists)
    const findMealPlanByDate = (dateStr) => {
        // Assume mealPlan.date is in format "YYYY-MM-DD" or ISO string starting with YYYY-MM-DD
        return fetchedMealPlans.find(mp => mp.date.startsWith(dateStr));
    };

    // 2) Add a recipe to a specific date
    const handleAddRecipe = async (e) => {
        e.preventDefault();
        if (!selectedDate || !recipeId) return;
        try {
            await axios.post(
                `http://localhost:8081/api/admin/users/${userId}/mealplans/${selectedDate}/recipes/${recipeId}`,
                null, // no body required
                { headers: { Authorization: `Bearer ${token}` } }
            );
            // After success, re-fetch meal plans
            fetchMealPlans();
            setRecipeId("");
        } catch (err) {
            console.error("Error adding recipe:", err);
            setError(err.message);
        }
    };

    // 3) Remove a recipe from a specific date
    const handleRemoveRecipe = async (date, recipeIdToRemove) => {
        try {
            await axios.delete(
                `http://localhost:8081/api/admin/users/${userId}/mealplans/${date}/recipes/${recipeIdToRemove}`,
                { headers: { Authorization: `Bearer ${token}` } }
            );
            fetchMealPlans();
        } catch (err) {
            console.error("Error removing recipe:", err);
            setError(err.message);
        }
    };

    // 4) Add a note to a specific date
    const handleAddNote = async (e) => {
        e.preventDefault();
        if (!selectedDate || !noteContent) return;
        try {
            await axios.post(
                `http://localhost:8081/api/admin/users/${userId}/mealplans/${selectedDate}/notes`,
                { content: noteContent },
                { headers: { Authorization: `Bearer ${token}` } }
            );
            fetchMealPlans();
            setNoteContent("");
        } catch (err) {
            console.error("Error adding note:", err);
            setError(err.message);
        }
    };

    // 5) Delete a note from a specific date
    const handleDeleteNote = async (date, noteId) => {
        try {
            await axios.delete(
                `http://localhost:8081/api/admin/users/${userId}/mealplans/${date}/notes/${noteId}`,
                { headers: { Authorization: `Bearer ${token}` } }
            );
            fetchMealPlans();
        } catch (err) {
            console.error("Error deleting note:", err);
            setError(err.message);
        }
    };

    return (
        <div className="fixed inset-0 flex items-center justify-center bg-black bg-opacity-50 p-4">
            <div className="bg-white p-6 rounded shadow-lg w-full max-w-5xl overflow-y-auto max-h-full">
                <h2 className="text-xl font-semibold mb-4">{userName}'s Meal Plans</h2>

                {error && <p className="text-red-500 mb-2">Error: {error}</p>}

                {/* Section: Controls to set selectedDate, add Recipe or Note */}
                <div className="flex flex-col gap-4 mb-4">
                    <div className="flex items-center gap-2">
                        <label>Date (YYYY-MM-DD):</label>
                        <select
                            value={selectedDate}
                            onChange={(e) => setSelectedDate(e.target.value)}
                            className="border p-1"
                        >
                            {dateRange.map((d) => (
                                <option key={d} value={d}>
                                    {d}
                                </option>
                            ))}
                        </select>
                    </div>
                    <div className="flex gap-2">
                        <form onSubmit={handleAddRecipe} className="flex gap-2">
                            <input
                                type="text"
                                placeholder="Recipe ID"
                                value={recipeId}
                                onChange={(e) => setRecipeId(e.target.value)}
                                className="border p-1"
                            />
                            <button type="submit" className="bg-blue-500 text-white px-2 rounded">
                                Add Recipe
                            </button>
                        </form>
                        <form onSubmit={handleAddNote} className="flex gap-2">
                            <input
                                type="text"
                                placeholder="Note Content"
                                value={noteContent}
                                onChange={(e) => setNoteContent(e.target.value)}
                                className="border p-1"
                            />
                            <button type="submit" className="bg-green-500 text-white px-2 rounded">
                                Add Note
                            </button>
                        </form>
                    </div>
                </div>

                {/* Table: One row per date in the dateRange */}
                <table className="w-full border-collapse border">
                    <thead>
                    <tr className="bg-gray-200">
                        <th className="border p-2">Date</th>
                        <th className="border p-2">Recipes</th>
                        <th className="border p-2">Notes</th>
                    </tr>
                    </thead>
                    <tbody>
                    {dateRange.map((dateStr) => {
                        // For each date, see if a MealPlan exists
                        const mealPlan = findMealPlanByDate(dateStr);
                        return (
                            <tr key={dateStr} className="border">
                                <td className="p-2">{dateStr}</td>
                                <td className="p-2">
                                    {mealPlan && mealPlan.recipes && mealPlan.recipes.length > 0 ? (
                                        mealPlan.recipes.map((r) => (
                                            <div key={r.id} className="flex items-center gap-2">
                          <span>
                            {r.title} (ID: {r.id})
                          </span>
                                                <button
                                                    className="bg-red-500 text-white px-1 rounded"
                                                    onClick={() => handleRemoveRecipe(dateStr, r.id)}
                                                >
                                                    X
                                                </button>
                                            </div>
                                        ))
                                    ) : (
                                        <em>No recipes</em>
                                    )}
                                </td>
                                <td className="p-2">
                                    {mealPlan && mealPlan.notes && mealPlan.notes.length > 0 ? (
                                        mealPlan.notes.map((note) => (
                                            <div key={note.id} className="flex items-center gap-2">
                                                <span>{note.content}</span>
                                                <button
                                                    className="bg-red-500 text-white px-1 rounded"
                                                    onClick={() => handleDeleteNote(dateStr, note.id)}
                                                >
                                                    X
                                                </button>
                                            </div>
                                        ))
                                    ) : (
                                        <em>No notes</em>
                                    )}
                                </td>
                            </tr>
                        );
                    })}
                    </tbody>
                </table>

                <div className="flex justify-end mt-4">
                    <button onClick={onClose} className="bg-gray-300 text-black px-4 py-2 rounded">
                        Close
                    </button>
                </div>
            </div>
        </div>
    );
};

export default AdminMealPlan;

/**

 // AdminMealPlan.js
 import React, { useEffect, useState } from "react";
 import axios from "axios";
 import API_URL from "./config";
 // Utility: Generate an array of dates (as ISO strings) from start to end (inclusive)
 const generateDateRange = (startDate, daysCount) => {
    const dates = [];
    for (let i = 0; i < daysCount; i++) {
        const d = new Date(startDate);
        d.setDate(d.getDate() + i);
        // Format as YYYY-MM-DD
        dates.push(d.toISOString().slice(0, 10));
    }
    return dates;
};

 const AdminMealPlan = ({ userId, userName, token, onClose }) => {
    const [fetchedMealPlans, setFetchedMealPlans] = useState([]); // raw MealPlanDTOs from backend
    const [dateRange, setDateRange] = useState([]); // Array of date strings (YYYY-MM-DD)
    const [selectedDate, setSelectedDate] = useState("");
    const [recipeId, setRecipeId] = useState(""); // for adding a recipe
    const [noteContent, setNoteContent] = useState(""); // for adding a note
    const [error, setError] = useState("");

    // Fetch meal plans for this user (all records)
    const fetchMealPlans = async () => {
        try {
            const response = await axios.get(
                `{API_URL}/api/admin/users/${userId}/mealplans`,
                { headers: { Authorization: `Bearer ${token}` } }
            );
            setFetchedMealPlans(response.data);
        } catch (err) {
            console.error("Error fetching meal plans:", err);
            setError(err.message);
        }
    };

    // On component mount, generate a date range (e.g., today + next 14 days)
    useEffect(() => {
        const today = new Date();
        const range = generateDateRange(today, 14); // Adjust the number as needed
        setDateRange(range);
        if (range.length > 0) {
            setSelectedDate(range[0]); // default selected date is today
        }
        // Also fetch the MealPlans
        if (userId && token) {
            fetchMealPlans();
        }
    }, [userId, token]);

    // Find a meal plan for a given date (if it exists)
    const findMealPlanByDate = (dateStr) => {
        // Assume mealPlan.date is in format "YYYY-MM-DD" or ISO string starting with YYYY-MM-DD
        return fetchedMealPlans.find(mp => mp.date.startsWith(dateStr));
    };

    // 2) Add a recipe to a specific date
    const handleAddRecipe = async (e) => {
        e.preventDefault();
        if (!selectedDate || !recipeId) return;
        try {
            await axios.post(
                `${API_URL}/api/admin/users/${userId}/mealplans/${selectedDate}/recipes/${recipeId}`,
                null, // no body required
                { headers: { Authorization: `Bearer ${token}` } }
            );
            // After success, re-fetch meal plans
            fetchMealPlans();
            setRecipeId("");
        } catch (err) {
            console.error("Error adding recipe:", err);
            setError(err.message);
        }
    };

    // 3) Remove a recipe from a specific date
    const handleRemoveRecipe = async (date, recipeIdToRemove) => {
        try {
            await axios.delete(
                `${API_URL}/api/admin/users/${userId}/mealplans/${date}/recipes/${recipeIdToRemove}`,
                { headers: { Authorization: `Bearer ${token}` } }
            );
            fetchMealPlans();
        } catch (err) {
            console.error("Error removing recipe:", err);
            setError(err.message);
        }
    };

    // 4) Add a note to a specific date
    const handleAddNote = async (e) => {
        e.preventDefault();
        if (!selectedDate || !noteContent) return;
        try {
            await axios.post(
                `${API_URL}/pi/admin/users/${userId}/mealplans/${selectedDate}/notes`,
                { content: noteContent },
                { headers: { Authorization: `Bearer ${token}` } }
            );
            fetchMealPlans();
            setNoteContent("");
        } catch (err) {
            console.error("Error adding note:", err);
            setError(err.message);
        }
    };

    // 5) Delete a note from a specific date
    const handleDeleteNote = async (date, noteId) => {
        try {
            await axios.delete(
                `${API_URL}/api/admin/users/${userId}/mealplans/${date}/notes/${noteId}`,
                { headers: { Authorization: `Bearer ${token}` } }
            );
            fetchMealPlans();
        } catch (err) {
            console.error("Error deleting note:", err);
            setError(err.message);
        }
    };

    return (
        <div className="fixed inset-0 flex items-center justify-center bg-black bg-opacity-50 p-4">
            <div className="bg-white p-6 rounded shadow-lg w-full max-w-5xl overflow-y-auto max-h-full">
                <h2 className="text-xl font-semibold mb-4">{userName}'s Meal Plans</h2>

                {error && <p className="text-red-500 mb-2">Error: {error}</p>}


<div className="flex flex-col gap-4 mb-4">
    <div className="flex items-center gap-2">
        <label>Date (YYYY-MM-DD):</label>
        <select
            value={selectedDate}
            onChange={(e) => setSelectedDate(e.target.value)}
            className="border p-1"
        >
            {dateRange.map((d) => (
                <option key={d} value={d}>
                    {d}
                </option>
            ))}
        </select>
    </div>
    <div className="flex gap-2">
        <form onSubmit={handleAddRecipe} className="flex gap-2">
            <input
                type="text"
                placeholder="Recipe ID"
                value={recipeId}
                onChange={(e) => setRecipeId(e.target.value)}
                className="border p-1"
            />
            <button type="submit" className="bg-blue-500 text-white px-2 rounded">
                Add Recipe
            </button>
        </form>
        <form onSubmit={handleAddNote} className="flex gap-2">
            <input
                type="text"
                placeholder="Note Content"
                value={noteContent}
                onChange={(e) => setNoteContent(e.target.value)}
                className="border p-1"
            />
            <button type="submit" className="bg-green-500 text-white px-2 rounded">
                Add Note
            </button>
        </form>
    </div>
</div>


<table className="w-full border-collapse border">
    <thead>
    <tr className="bg-gray-200">
        <th className="border p-2">Date</th>
        <th className="border p-2">Recipes</th>
        <th className="border p-2">Notes</th>
    </tr>
    </thead>
    <tbody>
    {dateRange.map((dateStr) => {
        // For each date, see if a MealPlan exists
        const mealPlan = findMealPlanByDate(dateStr);
        return (
            <tr key={dateStr} className="border">
                <td className="p-2">{dateStr}</td>
                <td className="p-2">
                    {mealPlan && mealPlan.recipes && mealPlan.recipes.length > 0 ? (
                        mealPlan.recipes.map((r) => (
                            <div key={r.id} className="flex items-center gap-2">
                          <span>
                            {r.title} (ID: {r.id})
                          </span>
                                <button
                                    className="bg-red-500 text-white px-1 rounded"
                                    onClick={() => handleRemoveRecipe(dateStr, r.id)}
                                >
                                    X
                                </button>
                            </div>
                        ))
                    ) : (
                        <em>No recipes</em>
                    )}
                </td>
                <td className="p-2">
                    {mealPlan && mealPlan.notes && mealPlan.notes.length > 0 ? (
                        mealPlan.notes.map((note) => (
                            <div key={note.id} className="flex items-center gap-2">
                                <span>{note.content}</span>
                                <button
                                    className="bg-red-500 text-white px-1 rounded"
                                    onClick={() => handleDeleteNote(dateStr, note.id)}
                                >
                                    X
                                </button>
                            </div>
                        ))
                    ) : (
                        <em>No notes</em>
                    )}
                </td>
            </tr>
        );
    })}
    </tbody>
</table>

<div className="flex justify-end mt-4">
    <button onClick={onClose} className="bg-gray-300 text-black px-4 py-2 rounded">
        Close
    </button>
</div>
</div>
</div>
);
};

 export default AdminMealPlan;




 **/
