import React, { useEffect, useState } from "react";
import axios from "axios";
import { useNavigate } from "react-router-dom";
import UserFavoritesModal from "./UserFavoritesModal";
import UserBadgesModal from "./UserBadgesModal";
import AdminMealPlan from "./AdminMealPlan";
import AdminBooks from "./AdminBooks";
import AdminShoppingList from "./AdminShoppingList";
import "./Dashboard.css"; // Import our custom dashboard styles

const Dashboard = () => {
    const [users, setUsers] = useState([]);
    const [selectedUser, setSelectedUser] = useState(null);
    const [showRecipesModal, setShowRecipesModal] = useState(false);
    const [selectedUserRecipes, setSelectedUserRecipes] = useState([]);
    const [selectedUserName, setSelectedUserName] = useState("");
    const [isAddingUser, setIsAddingUser] = useState(false);
    const [showFavoritesModal, setShowFavoritesModal] = useState(false);
    const [selectedFavoriteUser, setSelectedFavoriteUser] = useState(null);
    const [showBadgesModal, setShowBadgesModal] = useState(false);
    const [selectedBadgeUser, setSelectedBadgeUser] = useState(null);
    const [showMealPlanModal, setShowMealPlanModal] = useState(false);
    const [selectedMealPlanUser, setSelectedMealPlanUser] = useState(null);
    const [showBooksModal, setShowBooksModal] = useState(false);
    const [selectedBooksUser, setSelectedBooksUser] = useState(null);
    const [showShoppingListModal, setShowShoppingListModal] = useState(false);
    const [selectedShoppingUser, setSelectedShoppingUser] = useState(null);
// Existing filter states...

// NEW: Add an "activeStatus" filter

    // State for adding a new user; note we added isVerified here
    const [newUser, setNewUser] = useState({
        email: "",
        username: "",
        password: "",
        image_uri: "",
        recipe_generation_count: 0,
        recipe_generation_cycle_start: "",
        reset_token: "",
        reset_token_expiry: "",
        reset_token_verified: false,
        isVerified: false,          // <-- Added
        role: "USER",
        subscription_expiry: "",
        subscription_type: "FREE",
        stripe_customer_id: "",
    });

    // Filter states
    const [searchEmail, setSearchEmail] = useState("");
    const [searchUsername, setSearchUsername] = useState("");
    const [filterSubscription, setFilterSubscription] = useState("");
    const [filterRole, setFilterRole] = useState("");
    const [filterActiveStatus, setFilterActiveStatus] = useState(""); // empty = show all

    const token = localStorage.getItem("token");
    const navigate = useNavigate();

    const fetchUsers = () => {
        axios
            .get("http://localhost:8081/api/admin/users", {
                headers: { Authorization: `Bearer ${token}` },
            })
            .then((response) => setUsers(response.data))
            .catch((error) => console.error("Error fetching users:", error));
    };

    useEffect(() => {
        fetchUsers();
    }, [token]);

    const handleLogout = () => {
        localStorage.removeItem("token");
        localStorage.removeItem("role");
        navigate("/login");
    };

    const handleEditClick = (user) => {
        setSelectedUser({
            ...user,
            password: ""  // Clear the password field for editing
        });
    };

    const handleInputChange = (e) => {
        const { name, value, type, checked } = e.target;
        const updatedValue = type === "checkbox" ? checked : value;
        setSelectedUser((prev) => ({
            ...prev,
            [name]: updatedValue,
        }));
    };

    const handleViewFavorites = (user) => {
        setSelectedFavoriteUser(user);
        setShowFavoritesModal(true);
    };

    const handleCloseFavoritesModal = () => {
        setShowFavoritesModal(false);
        setSelectedFavoriteUser(null);
    };

    const handleUpdateUser = async () => {
        try {
            // Create a shallow copy of the user object
            const userToSend = { ...selectedUser };

            // If password is empty or only whitespace, remove it from the request
            if (!userToSend.password || userToSend.password.trim() === "") {
                delete userToSend.password;
            }

            await axios.put(
                `http://localhost:8081/api/auth/${selectedUser.id}`,
                userToSend,
                { headers: { Authorization: `Bearer ${token}` } }
            );

            setSelectedUser(null);
            fetchUsers();
        } catch (error) {
            console.error("Error updating user:", error);
        }
    };

    const handleViewRecipes = async (user) => {
        try {
            setSelectedUserName(user.username);
            const response = await axios.get(
                `http://localhost:8081/api/admin/users/${user.id}/recipes`,
                { headers: { Authorization: `Bearer ${token}` } }
            );
            setSelectedUserRecipes(response.data);
            setShowRecipesModal(true);
        } catch (error) {
            console.error("Error fetching user recipes:", error);
        }
    };

    const handleCloseRecipesModal = () => {
        setShowRecipesModal(false);
        setSelectedUserRecipes([]);
    };

    const handleCancelEdit = () => {
        setSelectedUser(null);
    };

    const handleDeleteUser = async (userId) => {
        if (!window.confirm("Are you sure you want to delete this user?")) {
            return;
        }
        try {
            await axios.delete(`http://localhost:8081/api/admin/users/${userId}`, {
                headers: { Authorization: `Bearer ${token}` },
            });
            fetchUsers();
        } catch (error) {
            console.error("Error deleting user:", error);
        }
    };

    const handleViewBadges = (user) => {
        setSelectedBadgeUser(user);
        setShowBadgesModal(true);
    };

    const handleOpenAddUser = () => {
        // Reset the newUser state
        setNewUser({
            email: "",
            username: "",
            password: "",
            image_uri: "",
            recipe_generation_count: 0,
            recipe_generation_cycle_start: "",
            reset_token: "",
            reset_token_expiry: "",
            reset_token_verified: false,
            isVerified: false,      // <-- Make sure we reset to false
            role: "USER",
            subscription_expiry: "",
            subscription_type: "FREE",
            stripe_customer_id: "",
        });
        setIsAddingUser(true);
    };

    const handleAddUserChange = (e) => {
        const { name, value, type, checked } = e.target;
        const updatedValue = type === "checkbox" ? checked : value;
        setNewUser((prev) => ({
            ...prev,
            [name]: updatedValue,
        }));
    };

    const handleAddUser = async () => {
        try {
            await axios.post("http://localhost:8081/api/auth", newUser, {
                headers: { Authorization: `Bearer ${token}` },
            });
            setIsAddingUser(false);
            fetchUsers();
        } catch (error) {
            console.error("Error adding user:", error);
        }
    };

    const handleCancelAddUser = () => {
        setIsAddingUser(false);
    };

    // Handlers for viewing meal plans, books, shopping list
    const handleViewMealPlans = (user) => {
        setSelectedMealPlanUser(user);
        setShowMealPlanModal(true);
    };

    const handleViewBooks = (user) => {
        setSelectedBooksUser(user);
        setShowBooksModal(true);
    };

    const handleViewShoppingList = (user) => {
        setSelectedShoppingUser(user);
        setShowShoppingListModal(true);
    };

    const handleCloseMealPlanModal = () => {
        setShowMealPlanModal(false);
        setSelectedMealPlanUser(null);
    };

    const handleCloseShoppingListModal = () => {
        setShowShoppingListModal(false);
        setSelectedShoppingUser(null);
    };

    // Filtering logic
    const filteredUsers = users.filter((u) => {
        const matchesEmail = u.email.toLowerCase().includes(searchEmail.toLowerCase());
        const matchesUsername = u.username.toLowerCase().includes(searchUsername.toLowerCase());
        const matchesSubscription = filterSubscription
            ? u.subscriptionType === filterSubscription
            : true;
        const matchesRole = filterRole ? u.role === filterRole : true;
        let matchesActive = true;
        if (filterActiveStatus === "ACTIVE") {
            matchesActive = (u.verified === true);
        } else if (filterActiveStatus === "INACTIVE") {
            matchesActive = (u.verified === false);
        }

        return (
            matchesEmail &&
            matchesUsername &&
            matchesSubscription &&
            matchesRole &&
            matchesActive
        );
    });

    return (
        <div className="dashboard-container">
            <div className="dashboard-header">
                <h1 className="dashboard-title">Users Dashboard</h1>
                <div className="header-buttons">
                    <button onClick={handleOpenAddUser} className="btn btn-add">
                        Add User
                    </button>
                </div>
            </div>

            {/* Filter Controls */}
            <div className="dashboard-filters">
                <input
                    type="text"
                    placeholder="Search by Email"
                    value={searchEmail}
                    onChange={(e) => setSearchEmail(e.target.value)}
                    className="filter-input"
                />
                <input
                    type="text"
                    placeholder="Search by Username"
                    value={searchUsername}
                    onChange={(e) => setSearchUsername(e.target.value)}
                    className="filter-input"
                />
                <select
                    value={filterSubscription}
                    onChange={(e) => setFilterSubscription(e.target.value)}
                    className="filter-select"
                >
                    <option value="">All Subscriptions</option>
                    <option value="FREE">FREE</option>
                    <option value="PLUS">PLUS</option>
                    <option value="PRO">PRO</option>
                </select>
                <select
                    value={filterRole}
                    onChange={(e) => setFilterRole(e.target.value)}
                    className="filter-select"
                >
                    <option value="">All Roles</option>
                    <option value="user">USER</option>
                    <option value="ADMIN">ADMIN</option>
                </select>
                {/* NEW: Active/Non-active filter */}
                <select
                    value={filterActiveStatus}
                    onChange={(e) => setFilterActiveStatus(e.target.value)}
                    className="filter-select"
                >
                    <option value="">All Statuses</option>
                    <option value="ACTIVE">Active</option>
                    <option value="INACTIVE">Inactive</option>
                </select>
            </div>

            <table className="dashboard-table">
                <thead>
                <tr>
                    <th>ID</th>
                    <th>Email</th>
                    <th>Username</th>
                    <th>Role</th>
                    <th>Shopping</th>
                    <th>Image URI</th>
                    <th>Books</th>
                    <th>Favorites</th>
                    <th>Subscription Type</th>
                    <th>Subscription Expiry</th>
                    <th>Stripe Cust. ID</th>
                    <th>Gen. Count</th>
                    <th>Gen. Cycle Start</th>
                    <th>Reset Token</th>
                    <th>Reset Expiry</th>
                    <th>Reset Verified</th>
                    <th>Is Verified</th> {/* <-- Added */}
                    <th>Badges</th>
                    <th>MealPlans</th>
                    <th>Recipes</th>
                    <th>Actions</th>
                </tr>
                </thead>
                <tbody>
                {filteredUsers.map((u) => (
                    <tr key={u.id}>
                        <td>{u.id}</td>
                        <td>{u.email}</td>
                        <td>{u.username}</td>
                        <td>{u.role}</td>
                        <td>
                            <button
                                onClick={() => handleViewShoppingList(u)}
                                className="btn btn-view"
                            >
                                View Shopping List
                            </button>
                        </td>
                        <td>
                            {u.imageUri ? (
                                <img
                                    src={u.imageUri}
                                    alt={u.title}
                                    className="user-image"
                                />
                            ) : (
                                "No Image"
                            )}
                        </td>
                        <td>
                            <button onClick={() => handleViewBooks(u)} className="btn btn-view">
                                View Books
                            </button>
                        </td>
                        <td>
                            <button
                                onClick={() => handleViewFavorites(u)}
                                className="btn btn-view"
                            >
                                View Favorites
                            </button>
                        </td>
                        <td>{u.subscriptionType}</td>
                        <td>{u.subscriptionExpiry || "N/A"}</td>
                        <td>{u.stripeCustomerId || "N/A"}</td>
                        <td>{u.recipeGenerationCount}</td>
                        <td>{u.recipeGenerationCycleStart || "N/A"}</td>
                        <td>{u.resetToken || "N/A"}</td>
                        <td>{u.resetTokenExpiry || "N/A"}</td>
                        <td>{u.resetTokenVerified ? "Yes" : "No"}</td>
                        <td>{u.verified ? "Yes" : "No"}</td>
                        <td>
                            <button
                                onClick={() => handleViewBadges(u)}
                                className="btn btn-view"
                            >
                                View Badges
                            </button>
                        </td>
                        <td>
                            <button
                                onClick={() => handleViewMealPlans(u)}
                                className="btn btn-view"
                            >
                                View Meal Plans
                            </button>
                        </td>
                        <td>
                            <button
                                onClick={() => handleViewRecipes(u)}
                                className="btn btn-view"
                            >
                                View Recipes
                            </button>
                        </td>
                        <td>
                            <button
                                onClick={() => handleEditClick(u)}
                                className="btn btn-edit"
                            >
                                Edit
                            </button>
                            <button
                                onClick={() => handleDeleteUser(u.id)}
                                className="btn btn-delete"
                            >
                                Delete
                            </button>
                        </td>
                    </tr>
                ))}
                </tbody>
            </table>

            {/* Edit User Modal */}
            {selectedUser && (
                <div className="modal-overlay">
                    <div className="modal-content">
                        <h2 className="modal-header">Edit User (ID {selectedUser.id})</h2>
                        <div className="modal-grid">
                            <label>
                                Email:
                                <input
                                    type="email"
                                    name="email"
                                    value={selectedUser.email || ""}
                                    onChange={handleInputChange}
                                />
                            </label>
                            <label>
                                Username:
                                <input
                                    type="text"
                                    name="username"
                                    value={selectedUser.username || ""}
                                    onChange={handleInputChange}
                                />
                            </label>
                            <label>
                                Password:
                                <input
                                    type="text"
                                    name="password"
                                    placeholder="Leave empty to keep current password"
                                    value={selectedUser.password || ""}
                                    onChange={handleInputChange}
                                />
                            </label>
                            <label>
                                Role:
                                <select
                                    name="role"
                                    value={selectedUser.role || "USER"}
                                    onChange={handleInputChange}
                                >
                                    <option value="USER">USER</option>
                                    <option value="ADMIN">ADMIN</option>
                                </select>
                            </label>
                            <label>
                                Image URI:
                                <input
                                    type="text"
                                    name="imageUri"
                                    value={selectedUser.imageUri || ""}
                                    onChange={handleInputChange}
                                />
                            </label>
                            <label>
                                Subscription Type:
                                <select
                                    name="subscriptionType"
                                    value={selectedUser.subscriptionType || "FREE"}
                                    onChange={handleInputChange}
                                >
                                    <option value="FREE">FREE</option>
                                    <option value="PLUS">PLUS</option>
                                    <option value="PRO">PRO</option>
                                </select>
                            </label>
                            <label>
                                Subscription Expiry:
                                <input
                                    type="text"
                                    name="subscriptionExpiry"
                                    placeholder="YYYY-MM-DDTHH:mm:ss"
                                    value={selectedUser.subscriptionExpiry || ""}
                                    onChange={handleInputChange}
                                />
                            </label>
                            <label>
                                Stripe Customer ID:
                                <input
                                    type="text"
                                    name="stripeCustomerId"
                                    value={selectedUser.stripeCustomerId || ""}
                                    onChange={handleInputChange}
                                />
                            </label>
                            <label>
                                Recipe Gen. Count:
                                <input
                                    type="number"
                                    name="recipeGenerationCount"
                                    value={selectedUser.recipeGenerationCount || 0}
                                    onChange={handleInputChange}
                                />
                            </label>
                            <label>
                                Recipe Gen. Cycle Start:
                                <input
                                    type="text"
                                    name="recipeGenerationCycleStart"
                                    placeholder="YYYY-MM-DDTHH:mm:ss"
                                    value={selectedUser.recipeGenerationCycleStart || ""}
                                    onChange={handleInputChange}
                                />
                            </label>
                            <label>
                                Reset Token:
                                <input
                                    type="text"
                                    name="resetToken"
                                    value={selectedUser.resetToken || ""}
                                    onChange={handleInputChange}
                                />
                            </label>
                            <label>
                                Reset Token Expiry:
                                <input
                                    type="text"
                                    name="resetTokenExpiry"
                                    placeholder="YYYY-MM-DDTHH:mm:ss"
                                    value={selectedUser.resetTokenExpiry || ""}
                                    onChange={handleInputChange}
                                />
                            </label>
                            <label className="checkbox-label">
                                Reset Verified:
                                <input
                                    type="checkbox"
                                    name="resetTokenVerified"
                                    checked={!!selectedUser.resetTokenVerified}
                                    onChange={handleInputChange}
                                />
                            </label>
                            {/* New field: isVerified */}
                            <label className="checkbox-label">
                                Is Verified:
                                <input
                                    type="checkbox"
                                    name="verified"  // use 'verified' to match the JSON key
                                    checked={!!selectedUser.verified} // read selectedUser.verified instead of isVerified
                                    onChange={handleInputChange}
                                />
                            </label>
                        </div>
                        <div className="modal-buttons">
                            <button onClick={handleCancelEdit} className="btn btn-cancel">
                                Cancel
                            </button>
                            <button onClick={handleUpdateUser} className="btn btn-save">
                                Save
                            </button>
                        </div>
                    </div>
                </div>
            )}

            {/* Add User Modal */}
            {isAddingUser && (
                <div className="modal-overlay">
                    <div className="modal-content">
                        <h2 className="modal-header">Add New User</h2>
                        <div className="modal-grid">
                            <label>
                                Email:
                                <input
                                    type="email"
                                    name="email"
                                    value={newUser.email}
                                    onChange={handleAddUserChange}
                                />
                            </label>
                            <label>
                                Username:
                                <input
                                    type="text"
                                    name="username"
                                    value={newUser.username}
                                    onChange={handleAddUserChange}
                                />
                            </label>
                            <label>
                                Password:
                                <input
                                    type="text"
                                    name="password"
                                    value={newUser.password}
                                    onChange={handleAddUserChange}
                                />
                            </label>
                            <label>
                                Role:
                                <select
                                    name="role"
                                    value={newUser.role}
                                    onChange={handleAddUserChange}
                                >
                                    <option value="USER">USER</option>
                                    <option value="ADMIN">ADMIN</option>
                                </select>
                            </label>
                            <label>
                                Image URI:
                                <input
                                    type="text"
                                    name="image_uri"
                                    value={newUser.image_uri}
                                    onChange={handleAddUserChange}
                                />
                            </label>
                            <label>
                                Subscription Type:
                                <select
                                    name="subscription_type"
                                    value={newUser.subscription_type}
                                    onChange={handleAddUserChange}
                                >
                                    <option value="FREE">FREE</option>
                                    <option value="PLUS">PLUS</option>
                                    <option value="PRO">PRO</option>
                                </select>
                            </label>
                            <label>
                                Subscription Expiry:
                                <input
                                    type="text"
                                    name="subscription_expiry"
                                    placeholder="YYYY-MM-DDTHH:mm:ss"
                                    value={newUser.subscription_expiry}
                                    onChange={handleAddUserChange}
                                />
                            </label>
                            <label>
                                Stripe Customer ID:
                                <input
                                    type="text"
                                    name="stripe_customer_id"
                                    value={newUser.stripe_customer_id}
                                    onChange={handleAddUserChange}
                                />
                            </label>
                            <label>
                                Recipe Gen. Count:
                                <input
                                    type="number"
                                    name="recipe_generation_count"
                                    value={newUser.recipe_generation_count}
                                    onChange={handleAddUserChange}
                                />
                            </label>
                            <label>
                                Recipe Gen. Cycle Start:
                                <input
                                    type="text"
                                    name="recipe_generation_cycle_start"
                                    placeholder="YYYY-MM-DDTHH:mm:ss"
                                    value={newUser.recipe_generation_cycle_start}
                                    onChange={handleAddUserChange}
                                />
                            </label>
                            <label>
                                Reset Token:
                                <input
                                    type="text"
                                    name="reset_token"
                                    value={newUser.reset_token}
                                    onChange={handleAddUserChange}
                                />
                            </label>
                            <label>
                                Reset Token Expiry:
                                <input
                                    type="text"
                                    name="reset_token_expiry"
                                    placeholder="YYYY-MM-DDTHH:mm:ss"
                                    value={newUser.reset_token_expiry}
                                    onChange={handleAddUserChange}
                                />
                            </label>
                            <label className="checkbox-label">
                                Reset Verified:
                                <input
                                    type="checkbox"
                                    name="reset_token_verified"
                                    checked={newUser.reset_token_verified}
                                    onChange={handleAddUserChange}
                                />
                            </label>
                            {/* New field: isVerified */}
                            <label className="checkbox-label">
                                Is Verified:
                                <input
                                    type="checkbox"
                                    name="isVerified"
                                    checked={newUser.isVerified}
                                    onChange={handleAddUserChange}
                                />
                            </label>
                        </div>
                        <div className="modal-buttons">
                            <button onClick={handleCancelAddUser} className="btn btn-cancel">
                                Cancel
                            </button>
                            <button onClick={handleAddUser} className="btn btn-save">
                                Add User
                            </button>
                        </div>
                    </div>
                </div>
            )}

            {/* Badges Modal */}
            {showBadgesModal && selectedBadgeUser && (
                <div className="modal-overlay">
                    <div className="modal-content">
                        <UserBadgesModal
                            userId={selectedBadgeUser.id}
                            userName={selectedBadgeUser.username}
                            token={token}
                            onClose={() => {
                                setShowBadgesModal(false);
                                setSelectedBadgeUser(null);
                            }}
                        />
                    </div>
                </div>
            )}

            {/* Favorites Modal */}
            {showFavoritesModal && selectedFavoriteUser && (
                <div className="modal-overlay">
                    <div className="modal-content">
                        <UserFavoritesModal
                            userId={selectedFavoriteUser.id}
                            userName={selectedFavoriteUser.username}
                            token={token}
                            onClose={handleCloseFavoritesModal}
                        />
                    </div>
                </div>
            )}

            {/* Recipes Modal */}
            {showRecipesModal && (
                <div className="modal-overlay">
                    <div className="modal-content">
                        <h2 className="modal-header">{selectedUserName}'s Recipes</h2>
                        {selectedUserRecipes.length === 0 ? (
                            <p>No recipes found for this user.</p>
                        ) : (
                            <table className="dashboard-table">
                                <thead>
                                <tr>
                                    <th>ID</th>
                                    <th>Title</th>
                                    <th>Prep Time</th>
                                    <th>Cook Time</th>
                                    <th>Public</th>
                                </tr>
                                </thead>
                                <tbody>
                                {selectedUserRecipes.map((r) => (
                                    <tr key={r.id}>
                                        <td>{r.id}</td>
                                        <td>{r.title}</td>
                                        <td>{r.prepTime || "0"}</td>
                                        <td>{r.cookTime || "0"}</td>
                                        <td>{r.public ? "Yes" : "No"}</td>
                                    </tr>
                                ))}
                                </tbody>
                            </table>
                        )}
                        <div className="modal-buttons">
                            <button onClick={handleCloseRecipesModal} className="btn btn-cancel">
                                Close
                            </button>
                        </div>
                    </div>
                </div>
            )}

            {/* Shopping List Modal */}
            {showShoppingListModal && selectedShoppingUser && (
                <div className="modal-overlay">
                    <div className="modal-content">
                        <AdminShoppingList
                            userId={selectedShoppingUser.id}
                            userName={selectedShoppingUser.username}
                            token={token}
                            onClose={handleCloseShoppingListModal}
                        />
                    </div>
                </div>
            )}

            {/* Meal Plans Modal */}
            {showMealPlanModal && selectedMealPlanUser && (
                <div className="modal-overlay">
                    <div className="modal-content">
                        <AdminMealPlan
                            userId={selectedMealPlanUser.id}
                            userName={selectedMealPlanUser.username}
                            token={token}
                            onClose={handleCloseMealPlanModal}
                        />
                    </div>
                </div>
            )}

            {/* Books Modal */}
            {showBooksModal && selectedBooksUser && (
                <div className="modal-overlay">
                    <div className="modal-content">
                        <AdminBooks
                            userId={selectedBooksUser.id}
                            token={token}
                            onClose={() => setShowBooksModal(false)}
                        />
                    </div>
                </div>
            )}
        </div>
    );
};

export default Dashboard;



/**

 import React, { useEffect, useState } from "react";
 import axios from "axios";
 import { useNavigate } from "react-router-dom";
 import UserFavoritesModal from "./UserFavoritesModal";
 import UserBadgesModal from "./UserBadgesModal";
 import AdminMealPlan from "./AdminMealPlan";
 import AdminBooks from "./AdminBooks";
 import AdminShoppingList from "./AdminShoppingList";
 import "./Dashboard.css"; // Import our custom dashboard styles
 import API_URL from "./config";
 const Dashboard = () => {
    const [users, setUsers] = useState([]);
    const [selectedUser, setSelectedUser] = useState(null);
    const [showRecipesModal, setShowRecipesModal] = useState(false);
    const [selectedUserRecipes, setSelectedUserRecipes] = useState([]);
    const [selectedUserName, setSelectedUserName] = useState("");
    const [isAddingUser, setIsAddingUser] = useState(false);
    const [showFavoritesModal, setShowFavoritesModal] = useState(false);
    const [selectedFavoriteUser, setSelectedFavoriteUser] = useState(null);
    const [showBadgesModal, setShowBadgesModal] = useState(false);
    const [selectedBadgeUser, setSelectedBadgeUser] = useState(null);
    const [showMealPlanModal, setShowMealPlanModal] = useState(false);
    const [selectedMealPlanUser, setSelectedMealPlanUser] = useState(null);
    const [showBooksModal, setShowBooksModal] = useState(false);
    const [selectedBooksUser, setSelectedBooksUser] = useState(null);
    const [showShoppingListModal, setShowShoppingListModal] = useState(false);
    const [selectedShoppingUser, setSelectedShoppingUser] = useState(null);
// Existing filter states...

// NEW: Add an "activeStatus" filter

    // State for adding a new user; note we added isVerified here
    const [newUser, setNewUser] = useState({
        email: "",
        username: "",
        password: "",
        image_uri: "",
        recipe_generation_count: 0,
        recipe_generation_cycle_start: "",
        reset_token: "",
        reset_token_expiry: "",
        reset_token_verified: false,
        isVerified: false,          // <-- Added
        role: "USER",
        subscription_expiry: "",
        subscription_type: "FREE",
        stripe_customer_id: "",
    });

    // Filter states
    const [searchEmail, setSearchEmail] = useState("");
    const [searchUsername, setSearchUsername] = useState("");
    const [filterSubscription, setFilterSubscription] = useState("");
    const [filterRole, setFilterRole] = useState("");
    const [filterActiveStatus, setFilterActiveStatus] = useState(""); // empty = show all

    const token = localStorage.getItem("token");
    const navigate = useNavigate();

    const fetchUsers = () => {
        axios
            .get(`${API_URL}/api/admin/users`, {
                headers: { Authorization: `Bearer ${token}` },
            })
            .then((response) => setUsers(response.data))
            .catch((error) => console.error("Error fetching users:", error));
    };

    useEffect(() => {
        fetchUsers();
    }, [token]);

    const handleLogout = () => {
        localStorage.removeItem("token");
        localStorage.removeItem("role");
        navigate("/login");
    };

    const handleEditClick = (user) => {
        setSelectedUser({ ...user });
    };

    const handleInputChange = (e) => {
        const { name, value, type, checked } = e.target;
        const updatedValue = type === "checkbox" ? checked : value;
        setSelectedUser((prev) => ({
            ...prev,
            [name]: updatedValue,
        }));
    };

    const handleViewFavorites = (user) => {
        setSelectedFavoriteUser(user);
        setShowFavoritesModal(true);
    };

    const handleCloseFavoritesModal = () => {
        setShowFavoritesModal(false);
        setSelectedFavoriteUser(null);
    };

    const handleUpdateUser = async () => {
        try {
            await axios.put(
                `${API_URL}/api/auth/${selectedUser.id}`,
                selectedUser,
                { headers: { Authorization: `Bearer ${token}` } }
            );
            setSelectedUser(null);
            fetchUsers();
        } catch (error) {
            console.error("Error updating user:", error);
        }
    };

    const handleViewRecipes = async (user) => {
        try {
            setSelectedUserName(user.username);
            const response = await axios.get(
                `${API_URL}/api/admin/users/${user.id}/recipes`,
                { headers: { Authorization: `Bearer ${token}` } }
            );
            setSelectedUserRecipes(response.data);
            setShowRecipesModal(true);
        } catch (error) {
            console.error("Error fetching user recipes:", error);
        }
    };

    const handleCloseRecipesModal = () => {
        setShowRecipesModal(false);
        setSelectedUserRecipes([]);
    };

    const handleCancelEdit = () => {
        setSelectedUser(null);
    };

    const handleDeleteUser = async (userId) => {
        if (!window.confirm("Are you sure you want to delete this user?")) {
            return;
        }
        try {
            await axios.delete(`${API_URL}/api/admin/users/${userId}`, {
                headers: { Authorization: `Bearer ${token}` },
            });
            fetchUsers();
        } catch (error) {
            console.error("Error deleting user:", error);
        }
    };

    const handleViewBadges = (user) => {
        setSelectedBadgeUser(user);
        setShowBadgesModal(true);
    };

    const handleOpenAddUser = () => {
        // Reset the newUser state
        setNewUser({
            email: "",
            username: "",
            password: "",
            image_uri: "",
            recipe_generation_count: 0,
            recipe_generation_cycle_start: "",
            reset_token: "",
            reset_token_expiry: "",
            reset_token_verified: false,
            isVerified: false,      // <-- Make sure we reset to false
            role: "USER",
            subscription_expiry: "",
            subscription_type: "FREE",
            stripe_customer_id: "",
        });
        setIsAddingUser(true);
    };

    const handleAddUserChange = (e) => {
        const { name, value, type, checked } = e.target;
        const updatedValue = type === "checkbox" ? checked : value;
        setNewUser((prev) => ({
            ...prev,
            [name]: updatedValue,
        }));
    };

    const handleAddUser = async () => {
        try {
            await axios.post(`${API_URL}/api/auth`, newUser, {
                headers: { Authorization: `Bearer ${token}` },
            });
            setIsAddingUser(false);
            fetchUsers();
        } catch (error) {
            console.error("Error adding user:", error);
        }
    };

    const handleCancelAddUser = () => {
        setIsAddingUser(false);
    };

    // Handlers for viewing meal plans, books, shopping list
    const handleViewMealPlans = (user) => {
        setSelectedMealPlanUser(user);
        setShowMealPlanModal(true);
    };

    const handleViewBooks = (user) => {
        setSelectedBooksUser(user);
        setShowBooksModal(true);
    };

    const handleViewShoppingList = (user) => {
        setSelectedShoppingUser(user);
        setShowShoppingListModal(true);
    };

    const handleCloseMealPlanModal = () => {
        setShowMealPlanModal(false);
        setSelectedMealPlanUser(null);
    };

    const handleCloseShoppingListModal = () => {
        setShowShoppingListModal(false);
        setSelectedShoppingUser(null);
    };

    // Filtering logic
    const filteredUsers = users.filter((u) => {
        const matchesEmail = u.email.toLowerCase().includes(searchEmail.toLowerCase());
        const matchesUsername = u.username.toLowerCase().includes(searchUsername.toLowerCase());
        const matchesSubscription = filterSubscription
            ? u.subscriptionType === filterSubscription
            : true;
        const matchesRole = filterRole ? u.role === filterRole : true;
        let matchesActive = true;
        if (filterActiveStatus === "ACTIVE") {
            matchesActive = (u.verified === true);
        } else if (filterActiveStatus === "INACTIVE") {
            matchesActive = (u.verified === false);
        }

        return (
            matchesEmail &&
            matchesUsername &&
            matchesSubscription &&
            matchesRole &&
            matchesActive
        );
    });

    return (
        <div className="dashboard-container">
            <div className="dashboard-header">
                <h1 className="dashboard-title">Users Dashboard</h1>
                <div className="header-buttons">
                    <button onClick={handleOpenAddUser} className="btn btn-add">
                        Add User
                    </button>
                </div>
            </div>


<div className="dashboard-filters">
    <input
        type="text"
        placeholder="Search by Email"
        value={searchEmail}
        onChange={(e) => setSearchEmail(e.target.value)}
        className="filter-input"
    />
    <input
        type="text"
        placeholder="Search by Username"
        value={searchUsername}
        onChange={(e) => setSearchUsername(e.target.value)}
        className="filter-input"
    />
    <select
        value={filterSubscription}
        onChange={(e) => setFilterSubscription(e.target.value)}
        className="filter-select"
    >
        <option value="">All Subscriptions</option>
        <option value="FREE">FREE</option>
        <option value="PLUS">PLUS</option>
        <option value="PRO">PRO</option>
    </select>
    <select
        value={filterRole}
        onChange={(e) => setFilterRole(e.target.value)}
        className="filter-select"
    >
        <option value="">All Roles</option>
        <option value="user">USER</option>
        <option value="ADMIN">ADMIN</option>
    </select>

    <select
        value={filterActiveStatus}
        onChange={(e) => setFilterActiveStatus(e.target.value)}
        className="filter-select"
    >
        <option value="">All Statuses</option>
        <option value="ACTIVE">Active</option>
        <option value="INACTIVE">Inactive</option>
    </select>
</div>

<table className="dashboard-table">
    <thead>
    <tr>
        <th>ID</th>
        <th>Email</th>
        <th>Username</th>
        <th>Role</th>
        <th>Shopping</th>
        <th>Image URI</th>
        <th>Books</th>
        <th>Favorites</th>
        <th>Subscription Type</th>
        <th>Subscription Expiry</th>
        <th>Stripe Cust. ID</th>
        <th>Gen. Count</th>
        <th>Gen. Cycle Start</th>
        <th>Reset Token</th>
        <th>Reset Expiry</th>
        <th>Reset Verified</th>
        <th>Is Verified</th>
        <th>Badges</th>
        <th>MealPlans</th>
        <th>Recipes</th>
        <th>Actions</th>
    </tr>
    </thead>
    <tbody>
    {filteredUsers.map((u) => (
        <tr key={u.id}>
            <td>{u.id}</td>
            <td>{u.email}</td>
            <td>{u.username}</td>
            <td>{u.role}</td>
            <td>
                <button
                    onClick={() => handleViewShoppingList(u)}
                    className="btn btn-view"
                >
                    View Shopping List
                </button>
            </td>
            <td>
                {u.imageUri ? (
                    <img
                        src={u.imageUri}
                        alt={u.title}
                        className="user-image"
                    />
                ) : (
                    "No Image"
                )}
            </td>
            <td>
                <button onClick={() => handleViewBooks(u)} className="btn btn-view">
                    View Books
                </button>
            </td>
            <td>
                <button
                    onClick={() => handleViewFavorites(u)}
                    className="btn btn-view"
                >
                    View Favorites
                </button>
            </td>
            <td>{u.subscriptionType}</td>
            <td>{u.subscriptionExpiry || "N/A"}</td>
            <td>{u.stripeCustomerId || "N/A"}</td>
            <td>{u.recipeGenerationCount}</td>
            <td>{u.recipeGenerationCycleStart || "N/A"}</td>
            <td>{u.resetToken || "N/A"}</td>
            <td>{u.resetTokenExpiry || "N/A"}</td>
            <td>{u.resetTokenVerified ? "Yes" : "No"}</td>
            <td>{u.verified ? "Yes" : "No"}</td>
            <td>
                <button
                    onClick={() => handleViewBadges(u)}
                    className="btn btn-view"
                >
                    View Badges
                </button>
            </td>
            <td>
                <button
                    onClick={() => handleViewMealPlans(u)}
                    className="btn btn-view"
                >
                    View Meal Plans
                </button>
            </td>
            <td>
                <button
                    onClick={() => handleViewRecipes(u)}
                    className="btn btn-view"
                >
                    View Recipes
                </button>
            </td>
            <td>
                <button
                    onClick={() => handleEditClick(u)}
                    className="btn btn-edit"
                >
                    Edit
                </button>
                <button
                    onClick={() => handleDeleteUser(u.id)}
                    className="btn btn-delete"
                >
                    Delete
                </button>
            </td>
        </tr>
    ))}
    </tbody>
</table>


{selectedUser && (
    <div className="modal-overlay">
        <div className="modal-content">
            <h2 className="modal-header">Edit User (ID {selectedUser.id})</h2>
            <div className="modal-grid">
                <label>
                    Email:
                    <input
                        type="email"
                        name="email"
                        value={selectedUser.email || ""}
                        onChange={handleInputChange}
                    />
                </label>
                <label>
                    Username:
                    <input
                        type="text"
                        name="username"
                        value={selectedUser.username || ""}
                        onChange={handleInputChange}
                    />
                </label>
                <label>
                    Password:
                    <input
                        type="text"
                        name="password"
                        value={selectedUser.password || ""}
                        onChange={handleInputChange}
                    />
                </label>
                <label>
                    Role:
                    <select
                        name="role"
                        value={selectedUser.role || "USER"}
                        onChange={handleInputChange}
                    >
                        <option value="USER">USER</option>
                        <option value="ADMIN">ADMIN</option>
                    </select>
                </label>
                <label>
                    Image URI:
                    <input
                        type="text"
                        name="imageUri"
                        value={selectedUser.imageUri || ""}
                        onChange={handleInputChange}
                    />
                </label>
                <label>
                    Subscription Type:
                    <select
                        name="subscriptionType"
                        value={selectedUser.subscriptionType || "FREE"}
                        onChange={handleInputChange}
                    >
                        <option value="FREE">FREE</option>
                        <option value="PLUS">PLUS</option>
                        <option value="PRO">PRO</option>
                    </select>
                </label>
                <label>
                    Subscription Expiry:
                    <input
                        type="text"
                        name="subscriptionExpiry"
                        placeholder="YYYY-MM-DDTHH:mm:ss"
                        value={selectedUser.subscriptionExpiry || ""}
                        onChange={handleInputChange}
                    />
                </label>
                <label>
                    Stripe Customer ID:
                    <input
                        type="text"
                        name="stripeCustomerId"
                        value={selectedUser.stripeCustomerId || ""}
                        onChange={handleInputChange}
                    />
                </label>
                <label>
                    Recipe Gen. Count:
                    <input
                        type="number"
                        name="recipeGenerationCount"
                        value={selectedUser.recipeGenerationCount || 0}
                        onChange={handleInputChange}
                    />
                </label>
                <label>
                    Recipe Gen. Cycle Start:
                    <input
                        type="text"
                        name="recipeGenerationCycleStart"
                        placeholder="YYYY-MM-DDTHH:mm:ss"
                        value={selectedUser.recipeGenerationCycleStart || ""}
                        onChange={handleInputChange}
                    />
                </label>
                <label>
                    Reset Token:
                    <input
                        type="text"
                        name="resetToken"
                        value={selectedUser.resetToken || ""}
                        onChange={handleInputChange}
                    />
                </label>
                <label>
                    Reset Token Expiry:
                    <input
                        type="text"
                        name="resetTokenExpiry"
                        placeholder="YYYY-MM-DDTHH:mm:ss"
                        value={selectedUser.resetTokenExpiry || ""}
                        onChange={handleInputChange}
                    />
                </label>
                <label className="checkbox-label">
                    Reset Verified:
                    <input
                        type="checkbox"
                        name="resetTokenVerified"
                        checked={!!selectedUser.resetTokenVerified}
                        onChange={handleInputChange}
                    />
                </label>

                <label className="checkbox-label">
                    Is Verified:
                    <input
                        type="checkbox"
                        name="verified"  // use 'verified' to match the JSON key
                        checked={!!selectedUser.verified} // read selectedUser.verified instead of isVerified
                        onChange={handleInputChange}
                    />
                </label>
            </div>
            <div className="modal-buttons">
                <button onClick={handleCancelEdit} className="btn btn-cancel">
                    Cancel
                </button>
                <button onClick={handleUpdateUser} className="btn btn-save">
                    Save
                </button>
            </div>
        </div>
    </div>
)}


{isAddingUser && (
    <div className="modal-overlay">
        <div className="modal-content">
            <h2 className="modal-header">Add New User</h2>
            <div className="modal-grid">
                <label>
                    Email:
                    <input
                        type="email"
                        name="email"
                        value={newUser.email}
                        onChange={handleAddUserChange}
                    />
                </label>
                <label>
                    Username:
                    <input
                        type="text"
                        name="username"
                        value={newUser.username}
                        onChange={handleAddUserChange}
                    />
                </label>
                <label>
                    Password:
                    <input
                        type="text"
                        name="password"
                        value={newUser.password}
                        onChange={handleAddUserChange}
                    />
                </label>
                <label>
                    Role:
                    <select
                        name="role"
                        value={newUser.role}
                        onChange={handleAddUserChange}
                    >
                        <option value="USER">USER</option>
                        <option value="ADMIN">ADMIN</option>
                    </select>
                </label>
                <label>
                    Image URI:
                    <input
                        type="text"
                        name="image_uri"
                        value={newUser.image_uri}
                        onChange={handleAddUserChange}
                    />
                </label>
                <label>
                    Subscription Type:
                    <select
                        name="subscription_type"
                        value={newUser.subscription_type}
                        onChange={handleAddUserChange}
                    >
                        <option value="FREE">FREE</option>
                        <option value="PLUS">PLUS</option>
                        <option value="PRO">PRO</option>
                    </select>
                </label>
                <label>
                    Subscription Expiry:
                    <input
                        type="text"
                        name="subscription_expiry"
                        placeholder="YYYY-MM-DDTHH:mm:ss"
                        value={newUser.subscription_expiry}
                        onChange={handleAddUserChange}
                    />
                </label>
                <label>
                    Stripe Customer ID:
                    <input
                        type="text"
                        name="stripe_customer_id"
                        value={newUser.stripe_customer_id}
                        onChange={handleAddUserChange}
                    />
                </label>
                <label>
                    Recipe Gen. Count:
                    <input
                        type="number"
                        name="recipe_generation_count"
                        value={newUser.recipe_generation_count}
                        onChange={handleAddUserChange}
                    />
                </label>
                <label>
                    Recipe Gen. Cycle Start:
                    <input
                        type="text"
                        name="recipe_generation_cycle_start"
                        placeholder="YYYY-MM-DDTHH:mm:ss"
                        value={newUser.recipe_generation_cycle_start}
                        onChange={handleAddUserChange}
                    />
                </label>
                <label>
                    Reset Token:
                    <input
                        type="text"
                        name="reset_token"
                        value={newUser.reset_token}
                        onChange={handleAddUserChange}
                    />
                </label>
                <label>
                    Reset Token Expiry:
                    <input
                        type="text"
                        name="reset_token_expiry"
                        placeholder="YYYY-MM-DDTHH:mm:ss"
                        value={newUser.reset_token_expiry}
                        onChange={handleAddUserChange}
                    />
                </label>
                <label className="checkbox-label">
                    Reset Verified:
                    <input
                        type="checkbox"
                        name="reset_token_verified"
                        checked={newUser.reset_token_verified}
                        onChange={handleAddUserChange}
                    />
                </label>

                <label className="checkbox-label">
                    Is Verified:
                    <input
                        type="checkbox"
                        name="isVerified"
                        checked={newUser.isVerified}
                        onChange={handleAddUserChange}
                    />
                </label>
            </div>
            <div className="modal-buttons">
                <button onClick={handleCancelAddUser} className="btn btn-cancel">
                    Cancel
                </button>
                <button onClick={handleAddUser} className="btn btn-save">
                    Add User
                </button>
            </div>
        </div>
    </div>
)}


{showBadgesModal && selectedBadgeUser && (
    <div className="modal-overlay">
        <div className="modal-content">
            <UserBadgesModal
                userId={selectedBadgeUser.id}
                userName={selectedBadgeUser.username}
                token={token}
                onClose={() => {
                    setShowBadgesModal(false);
                    setSelectedBadgeUser(null);
                }}
            />
        </div>
    </div>
)}


{showFavoritesModal && selectedFavoriteUser && (
    <div className="modal-overlay">
        <div className="modal-content">
            <UserFavoritesModal
                userId={selectedFavoriteUser.id}
                userName={selectedFavoriteUser.username}
                token={token}
                onClose={handleCloseFavoritesModal}
            />
        </div>
    </div>
)}


{showRecipesModal && (
    <div className="modal-overlay">
        <div className="modal-content">
            <h2 className="modal-header">{selectedUserName}'s Recipes</h2>
            {selectedUserRecipes.length === 0 ? (
                <p>No recipes found for this user.</p>
            ) : (
                <table className="dashboard-table">
                    <thead>
                    <tr>
                        <th>ID</th>
                        <th>Title</th>
                        <th>Prep Time</th>
                        <th>Cook Time</th>
                        <th>Public</th>
                    </tr>
                    </thead>
                    <tbody>
                    {selectedUserRecipes.map((r) => (
                        <tr key={r.id}>
                            <td>{r.id}</td>
                            <td>{r.title}</td>
                            <td>{r.prepTime || "0"}</td>
                            <td>{r.cookTime || "0"}</td>
                            <td>{r.public ? "Yes" : "No"}</td>
                        </tr>
                    ))}
                    </tbody>
                </table>
            )}
            <div className="modal-buttons">
                <button onClick={handleCloseRecipesModal} className="btn btn-cancel">
                    Close
                </button>
            </div>
        </div>
    </div>
)}


{showShoppingListModal && selectedShoppingUser && (
    <div className="modal-overlay">
        <div className="modal-content">
            <AdminShoppingList
                userId={selectedShoppingUser.id}
                userName={selectedShoppingUser.username}
                token={token}
                onClose={handleCloseShoppingListModal}
            />
        </div>
    </div>
)}


{showMealPlanModal && selectedMealPlanUser && (
    <div className="modal-overlay">
        <div className="modal-content">
            <AdminMealPlan
                userId={selectedMealPlanUser.id}
                userName={selectedMealPlanUser.username}
                token={token}
                onClose={handleCloseMealPlanModal}
            />
        </div>
    </div>
)}


{showBooksModal && selectedBooksUser && (
    <div className="modal-overlay">
        <div className="modal-content">
            <AdminBooks
                userId={selectedBooksUser.id}
                token={token}
                onClose={() => setShowBooksModal(false)}
            />
        </div>
    </div>
)}
</div>
);
};

 export default Dashboard;




 **/
