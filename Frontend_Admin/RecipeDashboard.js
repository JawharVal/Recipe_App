import React, { useEffect, useState } from "react";
import axios from "axios";
import { useNavigate } from "react-router-dom";
import "./RecipeDashboard.css";

const RecipeDashboard = () => {
    const [searchTitle, setSearchTitle] = useState("");
    const [searchAuthor, setSearchAuthor] = useState("");
    const [recipes, setRecipes] = useState([]);
    const [selectedRecipe, setSelectedRecipe] = useState(null);
    const [users, setUsers] = useState([]);
    const token = localStorage.getItem("token");
    const navigate = useNavigate();
    const [isAddingRecipe, setIsAddingRecipe] = useState(false);
    const [newRecipe, setNewRecipe] = useState({
        title: "",
        prepTime: "",
        cookTime: "",
        ingredients: "",
        instructions: "",
        notes: "",
        imageUri: "",
        url: "",
        servings: "",
        difficulty: "",
        cuisine: "",
        source: "",
        video: "",
        calories: "",
        carbohydrates: "",
        protein: "",
        fat: "",
        sugar: "",
        tags: "",
        isPublic: false,
        authorId: null,
    });

    // NEW: Delete modal state
    const [showDeleteModal, setShowDeleteModal] = useState(false);
    const [recipeToDelete, setRecipeToDelete] = useState(null);

    // Fetch all recipes from the backend
    const fetchRecipes = () => {
        axios
            .get("http://localhost:8081/api/recipes", {
                headers: { Authorization: `Bearer ${token}` },
            })
            .then((response) => setRecipes(response.data))
            .catch((error) => console.error("Error fetching recipes:", error));
    };

    // Fetch all users (admin only)
    const fetchUsers = () => {
        axios
            .get("http://localhost:8081/api/admin/users", {
                headers: { Authorization: `Bearer ${token}` },
            })
            .then((response) => setUsers(response.data))
            .catch((error) => console.error("Error fetching users:", error));
    };

    useEffect(() => {
        fetchRecipes();
        fetchUsers();
        // eslint-disable-next-line
    }, [token]);

    // Logout handler
    const handleLogout = () => {
        localStorage.removeItem("token");
        localStorage.removeItem("role");
        navigate("/login");
    };

    // Open the edit modal for a recipe
    const handleEditClick = (recipe) => {
        if (recipe.tags && Array.isArray(recipe.tags)) {
            recipe.tags = recipe.tags.join(", ");
        }
        setSelectedRecipe(recipe);
    };

    // Update local state on input change in the edit modal
    const handleInputChange = (e) => {
        const { name, value, type, checked } = e.target;
        let updatedValue = value;
        if (type === "checkbox") {
            updatedValue = checked;
        }
        setSelectedRecipe({
            ...selectedRecipe,
            [name]: updatedValue,
        });
    };

    // Send updated recipe data to the backend
    const handleUpdateRecipe = async () => {
        try {
            const updatedRecipe = { ...selectedRecipe };
            if (typeof updatedRecipe.tags === "string") {
                updatedRecipe.tags = updatedRecipe.tags
                    .split(",")
                    .map((tag) => tag.trim())
                    .filter((tag) => tag.length > 0);
            }
            await axios.put(
                `http://localhost:8081/api/recipes/${updatedRecipe.id}`,
                updatedRecipe,
                { headers: { Authorization: `Bearer ${token}` } }
            );
            setSelectedRecipe(null);
            fetchRecipes();
        } catch (error) {
            console.error("Error updating recipe:", error);
        }
    };

    // 2) Add new recipe
    const handleOpenAddModal = () => {
        setNewRecipe({
            title: "",
            prepTime: "",
            cookTime: "",
            ingredients: "",
            instructions: "",
            notes: "",
            imageUri: "",
            url: "",
            servings: "",
            difficulty: "",
            cuisine: "",
            source: "",
            video: "",
            calories: "",
            carbohydrates: "",
            protein: "",
            fat: "",
            sugar: "",
            tags: "",
            isPublic: false,
            authorId: null,
        });
        setIsAddingRecipe(true);
    };

    const handleAddRecipeChange = (e) => {
        const { name, value, type, checked } = e.target;
        let updatedValue = value;
        if (type === "checkbox") {
            updatedValue = checked;
        }
        setNewRecipe({
            ...newRecipe,
            [name]: updatedValue,
        });
    };

    const handleAddRecipe = async () => {
        try {
            const recipeToCreate = { ...newRecipe };
            if (typeof recipeToCreate.tags === "string" && recipeToCreate.tags.length > 0) {
                recipeToCreate.tags = recipeToCreate.tags
                    .split(",")
                    .map((tag) => tag.trim())
                    .filter((tag) => tag.length > 0);
            } else {
                recipeToCreate.tags = [];
            }
            await axios.post("http://localhost:8081/api/recipes", recipeToCreate, {
                headers: { Authorization: `Bearer ${token}` },
            });
            setIsAddingRecipe(false);
            fetchRecipes();
        } catch (error) {
            console.error("Error creating recipe:", error);
        }
    };

    const handleCancelAdd = () => {
        setIsAddingRecipe(false);
    };

    // Navigate to the reviews page for a specific recipe
    const handleViewReviews = (recipeId) => {
        navigate(`/reviews/${recipeId}`);
    };

    // Instead of using window.confirm, open a delete modal
    const handleDeleteClick = (id) => {
        setRecipeToDelete(id);
        setShowDeleteModal(true);
    };

    const confirmDeleteRecipe = async () => {
        try {
            await axios.delete(`http://localhost:8081/api/recipes/${recipeToDelete}`, {
                headers: { Authorization: `Bearer ${token}` },
            });
            setShowDeleteModal(false);
            setRecipeToDelete(null);
            fetchRecipes();
        } catch (error) {
            console.error("Error deleting recipe:", error);
        }
    };

    const cancelDelete = () => {
        setShowDeleteModal(false);
        setRecipeToDelete(null);
    };

    const handleCancelEdit = () => {
        setSelectedRecipe(null);
    };

    // Filter recipes by search inputs
    const filteredRecipes = recipes.filter((recipe) => {
        const matchesTitle = recipe.title.toLowerCase().includes(searchTitle.toLowerCase());
        const matchesAuthor = recipe.authorUsername.toLowerCase().includes(searchAuthor.toLowerCase());
        return matchesTitle && matchesAuthor;
    });

    return (
        <div className="recipe-dashboard-container">
            {/* Header */}
            <div className="recipe-header">
                <h1 className="recipe-title">Recipes Dashboard</h1>
                <div className="header-buttons">
                    <button onClick={handleOpenAddModal} className="btn btn-add">
                        Add Recipe
                    </button>
                </div>
            </div>

            {/* Search Fields */}
            <div className="recipe-search">
                <input
                    type="text"
                    placeholder="Search by Title"
                    value={searchTitle}
                    onChange={(e) => setSearchTitle(e.target.value)}
                />
                <input
                    type="text"
                    placeholder="Search by Author"
                    value={searchAuthor}
                    onChange={(e) => setSearchAuthor(e.target.value)}
                />
            </div>

            {/* Recipes Table */}
            <table className="recipe-table">
                <thead>
                <tr>
                    <th>ID</th>
                    <th>Title</th>
                    <th>Author</th>
                    <th>Image</th>
                    <th>Prep Time</th>
                    <th>Cook Time</th>
                    <th>Ingredients</th>
                    <th>Instructions</th>
                    <th>Notes</th>
                    <th>Servings</th>
                    <th>Difficulty</th>
                    <th>Public</th>
                    <th>Tags</th>
                    <th>Reviews</th>
                    <th>Actions</th>
                </tr>
                </thead>
                <tbody>
                {filteredRecipes.map((recipe) => (
                    <tr key={recipe.id}>
                        <td>{recipe.id}</td>
                        <td>{recipe.title}</td>
                        <td>{recipe.authorUsername}</td>
                        <td className="p-2">
                            {recipe.imageUri ? (
                                <img src={recipe.imageUri} alt={recipe.title} className="user-image" />
                            ) : (
                                "No Image"
                            )}
                        </td>
                        <td>{recipe.prepTime}</td>
                        <td>{recipe.cookTime}</td>
                        <td>{recipe.ingredients}</td>
                        <td>{recipe.instructions}</td>
                        <td>{recipe.notes}</td>
                        <td>{recipe.servings}</td>
                        <td>{recipe.difficulty}</td>
                        <td>{recipe.public ? "Yes" : "No"}</td>
                        <td>
                            {recipe.tags
                                ? (Array.isArray(recipe.tags)
                                    ? recipe.tags.join(", ")
                                    : recipe.tags)
                                : "No Tags"}
                        </td>
                        <td>
                            <button onClick={() => handleViewReviews(recipe.id)} className="btn btn-view">
                                View Reviews
                            </button>
                        </td>
                        <td>
                            <button onClick={() => handleEditClick(recipe)} className="btn btn-edit">
                                Edit
                            </button>
                            <button onClick={() => handleDeleteClick(recipe.id)} className="btn btn-delete">
                                Delete
                            </button>
                        </td>
                    </tr>
                ))}
                </tbody>
            </table>

            {/* Edit Recipe Modal */}
            {selectedRecipe && (
                <div className="modal-overlay">
                    <div className="modal-content">
                        <h2 className="modal-header">Edit Recipe: {selectedRecipe.title}</h2>
                        <div className="modal-grid">
                            <label>
                                Title:
                                <input
                                    type="text"
                                    name="title"
                                    value={selectedRecipe.title}
                                    onChange={handleInputChange}
                                />
                            </label>
                            <label>
                                Prep Time:
                                <input
                                    type="text"
                                    name="prepTime"
                                    value={selectedRecipe.prepTime || ""}
                                    onChange={handleInputChange}
                                />
                            </label>
                            <label>
                                Cook Time:
                                <input
                                    type="text"
                                    name="cookTime"
                                    value={selectedRecipe.cookTime || ""}
                                    onChange={handleInputChange}
                                />
                            </label>
                            <label>
                                Ingredients:
                                <textarea
                                    name="ingredients"
                                    value={selectedRecipe.ingredients || ""}
                                    onChange={handleInputChange}
                                />
                            </label>
                            <label>
                                Instructions:
                                <textarea
                                    name="instructions"
                                    value={selectedRecipe.instructions || ""}
                                    onChange={handleInputChange}
                                />
                            </label>
                            <label>
                                Notes:
                                <textarea
                                    name="notes"
                                    value={selectedRecipe.notes || ""}
                                    onChange={handleInputChange}
                                />
                            </label>
                            <label>
                                Image URI:
                                <input
                                    type="text"
                                    name="imageUri"
                                    value={selectedRecipe.imageUri || ""}
                                    onChange={handleInputChange}
                                />
                            </label>
                            <label>
                                URL:
                                <input
                                    type="text"
                                    name="url"
                                    value={selectedRecipe.url || ""}
                                    onChange={handleInputChange}
                                />
                            </label>
                            <label>
                                Servings:
                                <input
                                    type="text"
                                    name="servings"
                                    value={selectedRecipe.servings || ""}
                                    onChange={handleInputChange}
                                />
                            </label>
                            <label>
                                Difficulty:
                                <input
                                    type="text"
                                    name="difficulty"
                                    value={selectedRecipe.difficulty || ""}
                                    onChange={handleInputChange}
                                />
                            </label>
                            <label>
                                Cuisine:
                                <input
                                    type="text"
                                    name="cuisine"
                                    value={selectedRecipe.cuisine || ""}
                                    onChange={handleInputChange}
                                />
                            </label>
                            <label>
                                Source:
                                <input
                                    type="text"
                                    name="source"
                                    value={selectedRecipe.source || ""}
                                    onChange={handleInputChange}
                                />
                            </label>
                            <label>
                                Video:
                                <input
                                    type="text"
                                    name="video"
                                    value={selectedRecipe.video || ""}
                                    onChange={handleInputChange}
                                />
                            </label>
                            <label>
                                Calories:
                                <input
                                    type="text"
                                    name="calories"
                                    value={selectedRecipe.calories || ""}
                                    onChange={handleInputChange}
                                />
                            </label>
                            <label>
                                Carbohydrates:
                                <input
                                    type="text"
                                    name="carbohydrates"
                                    value={selectedRecipe.carbohydrates || ""}
                                    onChange={handleInputChange}
                                />
                            </label>
                            <label>
                                Protein:
                                <input
                                    type="text"
                                    name="protein"
                                    value={selectedRecipe.protein || ""}
                                    onChange={handleInputChange}
                                />
                            </label>
                            <label>
                                Fat:
                                <input
                                    type="text"
                                    name="fat"
                                    value={selectedRecipe.fat || ""}
                                    onChange={handleInputChange}
                                />
                            </label>
                            <label>
                                Sugar:
                                <input
                                    type="text"
                                    name="sugar"
                                    value={selectedRecipe.sugar || ""}
                                    onChange={handleInputChange}
                                />
                            </label>
                            <label>
                                Tags (comma separated):
                                <input
                                    type="text"
                                    name="tags"
                                    value={selectedRecipe.tags || ""}
                                    onChange={handleInputChange}
                                />
                            </label>
                            <label>
                                Public:
                                <input
                                    type="checkbox"
                                    name="public"
                                    checked={selectedRecipe.public || false}
                                    onChange={handleInputChange}
                                    className="ml-2"
                                />
                            </label>
                        </div>
                        <div className="modal-buttons">
                            <button onClick={handleCancelEdit} className="btn btn-cancel">
                                Cancel
                            </button>
                            <button onClick={handleUpdateRecipe} className="btn btn-save">
                                Save
                            </button>
                        </div>
                    </div>
                </div>
            )}

            {/* Add Recipe Modal */}
            {isAddingRecipe && (
                <div className="modal-overlay">
                    <div className="modal-content">
                        <h2 className="modal-header">Add New Recipe</h2>
                        <div className="modal-grid">
                            <label>
                                Title:
                                <input
                                    type="text"
                                    name="title"
                                    value={newRecipe.title}
                                    onChange={handleAddRecipeChange}
                                />
                            </label>
                            <label>
                                Prep Time:
                                <input
                                    type="text"
                                    name="prepTime"
                                    value={newRecipe.prepTime}
                                    onChange={handleAddRecipeChange}
                                />
                            </label>
                            <label>
                                Cook Time:
                                <input
                                    type="text"
                                    name="cookTime"
                                    value={newRecipe.cookTime}
                                    onChange={handleAddRecipeChange}
                                />
                            </label>
                            <label>
                                Ingredients:
                                <textarea
                                    name="ingredients"
                                    value={newRecipe.ingredients}
                                    onChange={handleAddRecipeChange}
                                />
                            </label>
                            <label>
                                Instructions:
                                <textarea
                                    name="instructions"
                                    value={newRecipe.instructions}
                                    onChange={handleAddRecipeChange}
                                />
                            </label>
                            <label>
                                Notes:
                                <textarea
                                    name="notes"
                                    value={newRecipe.notes}
                                    onChange={handleAddRecipeChange}
                                />
                            </label>
                            <label>
                                Image URI:
                                <input
                                    type="text"
                                    name="imageUri"
                                    value={newRecipe.imageUri}
                                    onChange={handleAddRecipeChange}
                                />
                            </label>
                            <label>
                                URL:
                                <input
                                    type="text"
                                    name="url"
                                    value={newRecipe.url}
                                    onChange={handleAddRecipeChange}
                                />
                            </label>
                            <label>
                                Servings:
                                <input
                                    type="text"
                                    name="servings"
                                    value={newRecipe.servings}
                                    onChange={handleAddRecipeChange}
                                />
                            </label>
                            <label>
                                Difficulty:
                                <input
                                    type="text"
                                    name="difficulty"
                                    value={newRecipe.difficulty}
                                    onChange={handleAddRecipeChange}
                                />
                            </label>
                            <label>
                                Cuisine:
                                <input
                                    type="text"
                                    name="cuisine"
                                    value={newRecipe.cuisine}
                                    onChange={handleAddRecipeChange}
                                />
                            </label>
                            <label>
                                Source:
                                <input
                                    type="text"
                                    name="source"
                                    value={newRecipe.source}
                                    onChange={handleAddRecipeChange}
                                />
                            </label>
                            <label>
                                Video:
                                <input
                                    type="text"
                                    name="video"
                                    value={newRecipe.video}
                                    onChange={handleAddRecipeChange}
                                />
                            </label>
                            <label>
                                Calories:
                                <input
                                    type="text"
                                    name="calories"
                                    value={newRecipe.calories}
                                    onChange={handleAddRecipeChange}
                                />
                            </label>
                            <label>
                                Carbohydrates:
                                <input
                                    type="text"
                                    name="carbohydrates"
                                    value={newRecipe.carbohydrates}
                                    onChange={handleAddRecipeChange}
                                />
                            </label>
                            <label>
                                Protein:
                                <input
                                    type="text"
                                    name="protein"
                                    value={newRecipe.protein}
                                    onChange={handleAddRecipeChange}
                                />
                            </label>
                            <label>
                                Fat:
                                <input
                                    type="text"
                                    name="fat"
                                    value={newRecipe.fat}
                                    onChange={handleAddRecipeChange}
                                />
                            </label>
                            <label>
                                Sugar:
                                <input
                                    type="text"
                                    name="sugar"
                                    value={newRecipe.sugar}
                                    onChange={handleAddRecipeChange}
                                />
                            </label>
                            <label>
                                Tags (comma separated):
                                <input
                                    type="text"
                                    name="tags"
                                    value={newRecipe.tags}
                                    onChange={handleAddRecipeChange}
                                />
                            </label>
                            <label>
                                Public:
                                <input
                                    type="checkbox"
                                    name="isPublic"
                                    checked={newRecipe.isPublic}
                                    onChange={handleAddRecipeChange}
                                    className="ml-2"
                                />
                            </label>
                            <label>
                                Author:
                                <select
                                    name="authorId"
                                    value={newRecipe.authorId || ""}
                                    onChange={handleAddRecipeChange}
                                >
                                    <option value="">-- Select Author --</option>
                                    {users.map((u) => (
                                        <option key={u.id} value={u.id}>
                                            {u.username} (ID: {u.id})
                                        </option>
                                    ))}
                                </select>
                            </label>
                        </div>
                        <div className="modal-buttons">
                            <button onClick={handleCancelAdd} className="btn btn-cancel">
                                Cancel
                            </button>
                            <button onClick={handleAddRecipe} className="btn btn-save">
                                Add
                            </button>
                        </div>
                    </div>
                </div>
            )}

            {/* Delete Confirmation Modal */}
            {showDeleteModal && (
                <div className="modal-overlay">
                    <div className="modal-content">
                        <h2 className="modal-header">Confirm Deletion</h2>
                        <p>Are you sure you want to delete this recipe?</p>
                        <div className="modal-buttons">
                            <button onClick={cancelDelete} className="btn btn-cancel">
                                Cancel
                            </button>
                            <button onClick={confirmDeleteRecipe} className="btn btn-delete">
                                Delete
                            </button>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
};

export default RecipeDashboard;


/**

 import React, { useEffect, useState } from "react";
 import axios from "axios";
 import { useNavigate } from "react-router-dom";
 import "./RecipeDashboard.css";
 import API_URL from "./config";
 const RecipeDashboard = () => {
    const [searchTitle, setSearchTitle] = useState("");
    const [searchAuthor, setSearchAuthor] = useState("");
    const [recipes, setRecipes] = useState([]);
    const [selectedRecipe, setSelectedRecipe] = useState(null);
    const [users, setUsers] = useState([]);
    const token = localStorage.getItem("token");
    const navigate = useNavigate();
    const [isAddingRecipe, setIsAddingRecipe] = useState(false);
    const [newRecipe, setNewRecipe] = useState({
        title: "",
        prepTime: "",
        cookTime: "",
        ingredients: "",
        instructions: "",
        notes: "",
        imageUri: "",
        url: "",
        servings: "",
        difficulty: "",
        cuisine: "",
        source: "",
        video: "",
        calories: "",
        carbohydrates: "",
        protein: "",
        fat: "",
        sugar: "",
        tags: "",
        isPublic: false,
        authorId: null,
    });

    // NEW: Delete modal state
    const [showDeleteModal, setShowDeleteModal] = useState(false);
    const [recipeToDelete, setRecipeToDelete] = useState(null);

    // Fetch all recipes from the backend
    const fetchRecipes = () => {
        axios
            .get(`${API_URL}/api/recipes`, {
                headers: { Authorization: `Bearer ${token}` },
            })
            .then((response) => setRecipes(response.data))
            .catch((error) => console.error("Error fetching recipes:", error));
    };

    // Fetch all users (admin only)
    const fetchUsers = () => {
        axios
            .get(`l${API_URL}/api/admin/users`, {
                headers: { Authorization: `Bearer ${token}` },
            })
            .then((response) => setUsers(response.data))
            .catch((error) => console.error("Error fetching users:", error));
    };

    useEffect(() => {
        fetchRecipes();
        fetchUsers();
        // eslint-disable-next-line
    }, [token]);

    // Logout handler
    const handleLogout = () => {
        localStorage.removeItem("token");
        localStorage.removeItem("role");
        navigate("/login");
    };

    // Open the edit modal for a recipe
    const handleEditClick = (recipe) => {
        if (recipe.tags && Array.isArray(recipe.tags)) {
            recipe.tags = recipe.tags.join(", ");
        }
        setSelectedRecipe(recipe);
    };

    // Update local state on input change in the edit modal
    const handleInputChange = (e) => {
        const { name, value, type, checked } = e.target;
        let updatedValue = value;
        if (type === "checkbox") {
            updatedValue = checked;
        }
        setSelectedRecipe({
            ...selectedRecipe,
            [name]: updatedValue,
        });
    };

    // Send updated recipe data to the backend
    const handleUpdateRecipe = async () => {
        try {
            const updatedRecipe = { ...selectedRecipe };
            if (typeof updatedRecipe.tags === "string") {
                updatedRecipe.tags = updatedRecipe.tags
                    .split(",")
                    .map((tag) => tag.trim())
                    .filter((tag) => tag.length > 0);
            }
            await axios.put(
                `${API_URL}/api/recipes/${updatedRecipe.id}`,
                updatedRecipe,
                { headers: { Authorization: `Bearer ${token}` } }
            );
            setSelectedRecipe(null);
            fetchRecipes();
        } catch (error) {
            console.error("Error updating recipe:", error);
        }
    };

    // 2) Add new recipe
    const handleOpenAddModal = () => {
        setNewRecipe({
            title: "",
            prepTime: "",
            cookTime: "",
            ingredients: "",
            instructions: "",
            notes: "",
            imageUri: "",
            url: "",
            servings: "",
            difficulty: "",
            cuisine: "",
            source: "",
            video: "",
            calories: "",
            carbohydrates: "",
            protein: "",
            fat: "",
            sugar: "",
            tags: "",
            isPublic: false,
            authorId: null,
        });
        setIsAddingRecipe(true);
    };

    const handleAddRecipeChange = (e) => {
        const { name, value, type, checked } = e.target;
        let updatedValue = value;
        if (type === "checkbox") {
            updatedValue = checked;
        }
        setNewRecipe({
            ...newRecipe,
            [name]: updatedValue,
        });
    };

    const handleAddRecipe = async () => {
        try {
            const recipeToCreate = { ...newRecipe };
            if (typeof recipeToCreate.tags === "string" && recipeToCreate.tags.length > 0) {
                recipeToCreate.tags = recipeToCreate.tags
                    .split(",")
                    .map((tag) => tag.trim())
                    .filter((tag) => tag.length > 0);
            } else {
                recipeToCreate.tags = [];
            }
            await axios.post(`${API_URL}/api/recipes`, recipeToCreate, {
                headers: { Authorization: `Bearer ${token}` },
            });
            setIsAddingRecipe(false);
            fetchRecipes();
        } catch (error) {
            console.error("Error creating recipe:", error);
        }
    };

    const handleCancelAdd = () => {
        setIsAddingRecipe(false);
    };

    // Navigate to the reviews page for a specific recipe
    const handleViewReviews = (recipeId) => {
        navigate(`/reviews/${recipeId}`);
    };

    // Instead of using window.confirm, open a delete modal
    const handleDeleteClick = (id) => {
        setRecipeToDelete(id);
        setShowDeleteModal(true);
    };

    const confirmDeleteRecipe = async () => {
        try {
            await axios.delete(`${API_URL}/api/recipes/${recipeToDelete}`, {
                headers: { Authorization: `Bearer ${token}` },
            });
            setShowDeleteModal(false);
            setRecipeToDelete(null);
            fetchRecipes();
        } catch (error) {
            console.error("Error deleting recipe:", error);
        }
    };

    const cancelDelete = () => {
        setShowDeleteModal(false);
        setRecipeToDelete(null);
    };

    const handleCancelEdit = () => {
        setSelectedRecipe(null);
    };

    // Filter recipes by search inputs
    const filteredRecipes = recipes.filter((recipe) => {
        const matchesTitle = recipe.title.toLowerCase().includes(searchTitle.toLowerCase());
        const matchesAuthor = recipe.authorUsername.toLowerCase().includes(searchAuthor.toLowerCase());
        return matchesTitle && matchesAuthor;
    });

    return (
        <div className="recipe-dashboard-container">

<div className="recipe-header">
    <h1 className="recipe-title">Recipes Dashboard</h1>
    <div className="header-buttons">
        <button onClick={handleOpenAddModal} className="btn btn-add">
            Add Recipe
        </button>
    </div>
</div>


<div className="recipe-search">
    <input
        type="text"
        placeholder="Search by Title"
        value={searchTitle}
        onChange={(e) => setSearchTitle(e.target.value)}
    />
    <input
        type="text"
        placeholder="Search by Author"
        value={searchAuthor}
        onChange={(e) => setSearchAuthor(e.target.value)}
    />
</div>


<table className="recipe-table">
    <thead>
    <tr>
        <th>ID</th>
        <th>Title</th>
        <th>Author</th>
        <th>Image</th>
        <th>Prep Time</th>
        <th>Cook Time</th>
        <th>Ingredients</th>
        <th>Instructions</th>
        <th>Notes</th>
        <th>Servings</th>
        <th>Difficulty</th>
        <th>Public</th>
        <th>Tags</th>
        <th>Reviews</th>
        <th>Actions</th>
    </tr>
    </thead>
    <tbody>
    {filteredRecipes.map((recipe) => (
        <tr key={recipe.id}>
            <td>{recipe.id}</td>
            <td>{recipe.title}</td>
            <td>{recipe.authorUsername}</td>
            <td className="p-2">
                {recipe.imageUri ? (
                    <img src={recipe.imageUri} alt={recipe.title} className="user-image" />
                ) : (
                    "No Image"
                )}
            </td>
            <td>{recipe.prepTime}</td>
            <td>{recipe.cookTime}</td>
            <td>{recipe.ingredients}</td>
            <td>{recipe.instructions}</td>
            <td>{recipe.notes}</td>
            <td>{recipe.servings}</td>
            <td>{recipe.difficulty}</td>
            <td>{recipe.public ? "Yes" : "No"}</td>
            <td>
                {recipe.tags
                    ? (Array.isArray(recipe.tags)
                        ? recipe.tags.join(", ")
                        : recipe.tags)
                    : "No Tags"}
            </td>
            <td>
                <button onClick={() => handleViewReviews(recipe.id)} className="btn btn-view">
                    View Reviews
                </button>
            </td>
            <td>
                <button onClick={() => handleEditClick(recipe)} className="btn btn-edit">
                    Edit
                </button>
                <button onClick={() => handleDeleteClick(recipe.id)} className="btn btn-delete">
                    Delete
                </button>
            </td>
        </tr>
    ))}
    </tbody>
</table>

{selectedRecipe && (
    <div className="modal-overlay">
        <div className="modal-content">
            <h2 className="modal-header">Edit Recipe: {selectedRecipe.title}</h2>
            <div className="modal-grid">
                <label>
                    Title:
                    <input
                        type="text"
                        name="title"
                        value={selectedRecipe.title}
                        onChange={handleInputChange}
                    />
                </label>
                <label>
                    Prep Time:
                    <input
                        type="text"
                        name="prepTime"
                        value={selectedRecipe.prepTime || ""}
                        onChange={handleInputChange}
                    />
                </label>
                <label>
                    Cook Time:
                    <input
                        type="text"
                        name="cookTime"
                        value={selectedRecipe.cookTime || ""}
                        onChange={handleInputChange}
                    />
                </label>
                <label>
                    Ingredients:
                    <textarea
                        name="ingredients"
                        value={selectedRecipe.ingredients || ""}
                        onChange={handleInputChange}
                    />
                </label>
                <label>
                    Instructions:
                    <textarea
                        name="instructions"
                        value={selectedRecipe.instructions || ""}
                        onChange={handleInputChange}
                    />
                </label>
                <label>
                    Notes:
                    <textarea
                        name="notes"
                        value={selectedRecipe.notes || ""}
                        onChange={handleInputChange}
                    />
                </label>
                <label>
                    Image URI:
                    <input
                        type="text"
                        name="imageUri"
                        value={selectedRecipe.imageUri || ""}
                        onChange={handleInputChange}
                    />
                </label>
                <label>
                    URL:
                    <input
                        type="text"
                        name="url"
                        value={selectedRecipe.url || ""}
                        onChange={handleInputChange}
                    />
                </label>
                <label>
                    Servings:
                    <input
                        type="text"
                        name="servings"
                        value={selectedRecipe.servings || ""}
                        onChange={handleInputChange}
                    />
                </label>
                <label>
                    Difficulty:
                    <input
                        type="text"
                        name="difficulty"
                        value={selectedRecipe.difficulty || ""}
                        onChange={handleInputChange}
                    />
                </label>
                <label>
                    Cuisine:
                    <input
                        type="text"
                        name="cuisine"
                        value={selectedRecipe.cuisine || ""}
                        onChange={handleInputChange}
                    />
                </label>
                <label>
                    Source:
                    <input
                        type="text"
                        name="source"
                        value={selectedRecipe.source || ""}
                        onChange={handleInputChange}
                    />
                </label>
                <label>
                    Video:
                    <input
                        type="text"
                        name="video"
                        value={selectedRecipe.video || ""}
                        onChange={handleInputChange}
                    />
                </label>
                <label>
                    Calories:
                    <input
                        type="text"
                        name="calories"
                        value={selectedRecipe.calories || ""}
                        onChange={handleInputChange}
                    />
                </label>
                <label>
                    Carbohydrates:
                    <input
                        type="text"
                        name="carbohydrates"
                        value={selectedRecipe.carbohydrates || ""}
                        onChange={handleInputChange}
                    />
                </label>
                <label>
                    Protein:
                    <input
                        type="text"
                        name="protein"
                        value={selectedRecipe.protein || ""}
                        onChange={handleInputChange}
                    />
                </label>
                <label>
                    Fat:
                    <input
                        type="text"
                        name="fat"
                        value={selectedRecipe.fat || ""}
                        onChange={handleInputChange}
                    />
                </label>
                <label>
                    Sugar:
                    <input
                        type="text"
                        name="sugar"
                        value={selectedRecipe.sugar || ""}
                        onChange={handleInputChange}
                    />
                </label>
                <label>
                    Tags (comma separated):
                    <input
                        type="text"
                        name="tags"
                        value={selectedRecipe.tags || ""}
                        onChange={handleInputChange}
                    />
                </label>
                <label>
                    Public:
                    <input
                        type="checkbox"
                        name="public"
                        checked={selectedRecipe.public || false}
                        onChange={handleInputChange}
                        className="ml-2"
                    />
                </label>
            </div>
            <div className="modal-buttons">
                <button onClick={handleCancelEdit} className="btn btn-cancel">
                    Cancel
                </button>
                <button onClick={handleUpdateRecipe} className="btn btn-save">
                    Save
                </button>
            </div>
        </div>
    </div>
)}


{isAddingRecipe && (
    <div className="modal-overlay">
        <div className="modal-content">
            <h2 className="modal-header">Add New Recipe</h2>
            <div className="modal-grid">
                <label>
                    Title:
                    <input
                        type="text"
                        name="title"
                        value={newRecipe.title}
                        onChange={handleAddRecipeChange}
                    />
                </label>
                <label>
                    Prep Time:
                    <input
                        type="text"
                        name="prepTime"
                        value={newRecipe.prepTime}
                        onChange={handleAddRecipeChange}
                    />
                </label>
                <label>
                    Cook Time:
                    <input
                        type="text"
                        name="cookTime"
                        value={newRecipe.cookTime}
                        onChange={handleAddRecipeChange}
                    />
                </label>
                <label>
                    Ingredients:
                    <textarea
                        name="ingredients"
                        value={newRecipe.ingredients}
                        onChange={handleAddRecipeChange}
                    />
                </label>
                <label>
                    Instructions:
                    <textarea
                        name="instructions"
                        value={newRecipe.instructions}
                        onChange={handleAddRecipeChange}
                    />
                </label>
                <label>
                    Notes:
                    <textarea
                        name="notes"
                        value={newRecipe.notes}
                        onChange={handleAddRecipeChange}
                    />
                </label>
                <label>
                    Image URI:
                    <input
                        type="text"
                        name="imageUri"
                        value={newRecipe.imageUri}
                        onChange={handleAddRecipeChange}
                    />
                </label>
                <label>
                    URL:
                    <input
                        type="text"
                        name="url"
                        value={newRecipe.url}
                        onChange={handleAddRecipeChange}
                    />
                </label>
                <label>
                    Servings:
                    <input
                        type="text"
                        name="servings"
                        value={newRecipe.servings}
                        onChange={handleAddRecipeChange}
                    />
                </label>
                <label>
                    Difficulty:
                    <input
                        type="text"
                        name="difficulty"
                        value={newRecipe.difficulty}
                        onChange={handleAddRecipeChange}
                    />
                </label>
                <label>
                    Cuisine:
                    <input
                        type="text"
                        name="cuisine"
                        value={newRecipe.cuisine}
                        onChange={handleAddRecipeChange}
                    />
                </label>
                <label>
                    Source:
                    <input
                        type="text"
                        name="source"
                        value={newRecipe.source}
                        onChange={handleAddRecipeChange}
                    />
                </label>
                <label>
                    Video:
                    <input
                        type="text"
                        name="video"
                        value={newRecipe.video}
                        onChange={handleAddRecipeChange}
                    />
                </label>
                <label>
                    Calories:
                    <input
                        type="text"
                        name="calories"
                        value={newRecipe.calories}
                        onChange={handleAddRecipeChange}
                    />
                </label>
                <label>
                    Carbohydrates:
                    <input
                        type="text"
                        name="carbohydrates"
                        value={newRecipe.carbohydrates}
                        onChange={handleAddRecipeChange}
                    />
                </label>
                <label>
                    Protein:
                    <input
                        type="text"
                        name="protein"
                        value={newRecipe.protein}
                        onChange={handleAddRecipeChange}
                    />
                </label>
                <label>
                    Fat:
                    <input
                        type="text"
                        name="fat"
                        value={newRecipe.fat}
                        onChange={handleAddRecipeChange}
                    />
                </label>
                <label>
                    Sugar:
                    <input
                        type="text"
                        name="sugar"
                        value={newRecipe.sugar}
                        onChange={handleAddRecipeChange}
                    />
                </label>
                <label>
                    Tags (comma separated):
                    <input
                        type="text"
                        name="tags"
                        value={newRecipe.tags}
                        onChange={handleAddRecipeChange}
                    />
                </label>
                <label>
                    Public:
                    <input
                        type="checkbox"
                        name="isPublic"
                        checked={newRecipe.isPublic}
                        onChange={handleAddRecipeChange}
                        className="ml-2"
                    />
                </label>
                <label>
                    Author:
                    <select
                        name="authorId"
                        value={newRecipe.authorId || ""}
                        onChange={handleAddRecipeChange}
                    >
                        <option value="">-- Select Author --</option>
                        {users.map((u) => (
                            <option key={u.id} value={u.id}>
                                {u.username} (ID: {u.id})
                            </option>
                        ))}
                    </select>
                </label>
            </div>
            <div className="modal-buttons">
                <button onClick={handleCancelAdd} className="btn btn-cancel">
                    Cancel
                </button>
                <button onClick={handleAddRecipe} className="btn btn-save">
                    Add
                </button>
            </div>
        </div>
    </div>
)}

{showDeleteModal && (
    <div className="modal-overlay">
        <div className="modal-content">
            <h2 className="modal-header">Confirm Deletion</h2>
            <p>Are you sure you want to delete this recipe?</p>
            <div className="modal-buttons">
                <button onClick={cancelDelete} className="btn btn-cancel">
                    Cancel
                </button>
                <button onClick={confirmDeleteRecipe} className="btn btn-delete">
                    Delete
                </button>
            </div>
        </div>
    </div>
)}
</div>
);
};

 export default RecipeDashboard;



 **/