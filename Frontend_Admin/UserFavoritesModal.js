import React, { useEffect, useState } from "react";
import axios from "axios";

const UserFavoritesModal = ({ userId, userName, token, onClose }) => {
    const [favorites, setFavorites] = useState([]);

    // Fetch the favorites for the specified user (admin view)
    useEffect(() => {
        axios
            .get(`http://localhost:8081/api/admin/users/${userId}/favorites`, {
                headers: { Authorization: `Bearer ${token}` },
            })
            .then((response) => setFavorites(response.data))
            .catch((error) =>
                console.error("Error fetching favorites for user:", error)
            );
    }, [userId, token]);

    // Remove a recipe from favorites for that user
    const handleRemoveFavorite = async (recipeId) => {
        if (!window.confirm("Are you sure you want to remove this recipe from favorites?")) return;
        try {
            await axios.delete(`http://localhost:8081/api/admin/users/${userId}/favorites/${recipeId}`, {
                headers: { Authorization: `Bearer ${token}` },
            });
            // Update local state after deletion
            setFavorites((prev) => prev.filter((r) => r.id !== recipeId));
        } catch (error) {
            console.error("Error removing favorite recipe:", error);
        }
    };

    return (
        <div className="fixed inset-0 flex items-center justify-center bg-black bg-opacity-50">
            <div className="bg-white p-6 rounded shadow-lg w-11/12 max-w-3xl overflow-y-auto max-h-full">
                <h2 className="text-xl font-semibold mb-4">{userName}'s Favorite Recipes</h2>
                {favorites.length === 0 ? (
                    <p>No favorite recipes found for this user.</p>
                ) : (
                    <table className="w-full border-collapse border">
                        <thead>
                        <tr className="bg-gray-200">
                            <th className="border p-2">ID</th>
                            <th className="border p-2">Title</th>
                            <th className="border p-2">Image</th>
                            <th className="border p-2">Actions</th>
                        </tr>
                        </thead>
                        <tbody>
                        {favorites.map((recipe) => (
                            <tr key={recipe.id} className="border">
                                <td className="p-2">{recipe.id}</td>
                                <td className="p-2">{recipe.title}</td>
                                <td className="p-2">
                                    {recipe.imageUri ? (
                                        <img
                                            src={recipe.imageUri}
                                            alt={recipe.title}
                                            style={{ width: "80px", height: "auto" }}
                                        />
                                    ) : (
                                        "No Image"
                                    )}
                                </td>
                                <td className="p-2">
                                    <button
                                        onClick={() => handleRemoveFavorite(recipe.id)}
                                        className="bg-red-500 text-white px-2 py-1 rounded"
                                    >
                                        Remove
                                    </button>
                                </td>
                            </tr>
                        ))}
                        </tbody>
                    </table>
                )}
                <div className="flex justify-end mt-4">
                    <button
                        onClick={onClose}
                        className="bg-gray-300 text-black px-4 py-2 rounded"
                    >
                        Close
                    </button>
                </div>
            </div>
        </div>
    );
};

export default UserFavoritesModal;


/**

 import React, { useEffect, useState } from "react";
 import axios from "axios";
 import API_URL from "./config";
 const UserFavoritesModal = ({ userId, userName, token, onClose }) => {
    const [favorites, setFavorites] = useState([]);

    // Fetch the favorites for the specified user (admin view)
    useEffect(() => {
        axios
            .get(`${API_URL}/api/admin/users/${userId}/favorites`, {
                headers: { Authorization: `Bearer ${token}` },
            })
            .then((response) => setFavorites(response.data))
            .catch((error) =>
                console.error("Error fetching favorites for user:", error)
            );
    }, [userId, token]);

    // Remove a recipe from favorites for that user
    const handleRemoveFavorite = async (recipeId) => {
        if (!window.confirm("Are you sure you want to remove this recipe from favorites?")) return;
        try {
            await axios.delete(`${API_URL}/api/admin/users/${userId}/favorites/${recipeId}`, {
                headers: { Authorization: `Bearer ${token}` },
            });
            // Update local state after deletion
            setFavorites((prev) => prev.filter((r) => r.id !== recipeId));
        } catch (error) {
            console.error("Error removing favorite recipe:", error);
        }
    };

    return (
        <div className="fixed inset-0 flex items-center justify-center bg-black bg-opacity-50">
            <div className="bg-white p-6 rounded shadow-lg w-11/12 max-w-3xl overflow-y-auto max-h-full">
                <h2 className="text-xl font-semibold mb-4">{userName}'s Favorite Recipes</h2>
                {favorites.length === 0 ? (
                    <p>No favorite recipes found for this user.</p>
                ) : (
                    <table className="w-full border-collapse border">
                        <thead>
                        <tr className="bg-gray-200">
                            <th className="border p-2">ID</th>
                            <th className="border p-2">Title</th>
                            <th className="border p-2">Image</th>
                            <th className="border p-2">Actions</th>
                        </tr>
                        </thead>
                        <tbody>
                        {favorites.map((recipe) => (
                            <tr key={recipe.id} className="border">
                                <td className="p-2">{recipe.id}</td>
                                <td className="p-2">{recipe.title}</td>
                                <td className="p-2">
                                    {recipe.imageUri ? (
                                        <img
                                            src={recipe.imageUri}
                                            alt={recipe.title}
                                            style={{ width: "80px", height: "auto" }}
                                        />
                                    ) : (
                                        "No Image"
                                    )}
                                </td>
                                <td className="p-2">
                                    <button
                                        onClick={() => handleRemoveFavorite(recipe.id)}
                                        className="bg-red-500 text-white px-2 py-1 rounded"
                                    >
                                        Remove
                                    </button>
                                </td>
                            </tr>
                        ))}
                        </tbody>
                    </table>
                )}
                <div className="flex justify-end mt-4">
                    <button
                        onClick={onClose}
                        className="bg-gray-300 text-black px-4 py-2 rounded"
                    >
                        Close
                    </button>
                </div>
            </div>
        </div>
    );
};

 export default UserFavoritesModal;



 **/