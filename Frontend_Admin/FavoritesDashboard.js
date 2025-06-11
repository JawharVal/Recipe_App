import React, { useEffect, useState } from "react";
import axios from "axios";
import { useNavigate } from "react-router-dom";

const FavoritesDashboard = () => {
    const [favorites, setFavorites] = useState([]);
    const token = localStorage.getItem("token");
    const navigate = useNavigate();

    // Fetch favorite recipes from the backend
    const fetchFavorites = () => {
        axios
            .get("http://localhost:8081/api/auth/favorites", {
                headers: { Authorization: `Bearer ${token}` },
            })
            .then((response) => setFavorites(response.data))
            .catch((error) =>
                console.error("Error fetching favorite recipes:", error)
            );
    };

    useEffect(() => {
        fetchFavorites();
    }, [token]);

    // Handler to remove a recipe from favorites
    const handleRemoveFavorite = async (recipeId) => {
        if (!window.confirm("Are you sure you want to remove this recipe from favorites?")) return;
        try {
            await axios.delete(`http://localhost:8081/api/auth/favorites/${recipeId}`, {
                headers: { Authorization: `Bearer ${token}` },
            });
            fetchFavorites();
        } catch (error) {
            console.error("Error removing favorite recipe:", error);
        }
    };

    // Optional: Navigation back to Dashboard
    const handleBack = () => {
        navigate("/dashboard");
    };

    return (
        <div className="p-6">
            <div className="flex justify-between items-center mb-4">
                <h1 className="text-2xl font-bold">Favorite Recipes</h1>
                <button
                    onClick={handleBack}
                    className="bg-blue-500 text-white px-4 py-2 rounded"
                >
                    Back to Dashboard
                </button>
            </div>

            {favorites.length === 0 ? (
                <p>No favorite recipes found.</p>
            ) : (
                <table className="w-full border-collapse border">
                    <thead>
                    <tr className="bg-gray-200">
                        <th className="border p-2">ID</th>
                        <th className="border p-2">Title</th>
                        <th className="border p-2">Author</th>
                        <th className="border p-2">Image</th>
                        <th className="border p-2">Actions</th>
                    </tr>
                    </thead>
                    <tbody>
                    {favorites.map((fav) => (
                        <tr key={fav.id} className="border">
                            <td className="p-2">{fav.id}</td>
                            <td className="p-2">{fav.title}</td>
                            <td className="p-2">{fav.authorId}</td>
                            <td className="p-2">
                                {fav.imageUri ? (
                                    <img
                                        src={fav.imageUri}
                                        alt={fav.title}
                                        style={{ width: "80px", height: "auto" }}
                                    />
                                ) : (
                                    "No Image"
                                )}
                            </td>
                            <td className="p-2">
                                <button
                                    onClick={() => handleRemoveFavorite(fav.id)}
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
        </div>
    );
};

export default FavoritesDashboard;


/**

 import React, { useEffect, useState } from "react";
 import axios from "axios";
 import { useNavigate } from "react-router-dom";
 import API_URL from "./config";
 const FavoritesDashboard = () => {
    const [favorites, setFavorites] = useState([]);
    const token = localStorage.getItem("token");
    const navigate = useNavigate();

    // Fetch favorite recipes from the backend
    const fetchFavorites = () => {
        axios
            .get(`${API_URL}/api/auth/favorites`, {
                headers: { Authorization: `Bearer ${token}` },
            })
            .then((response) => setFavorites(response.data))
            .catch((error) =>
                console.error("Error fetching favorite recipes:", error)
            );
    };

    useEffect(() => {
        fetchFavorites();
    }, [token]);

    // Handler to remove a recipe from favorites
    const handleRemoveFavorite = async (recipeId) => {
        if (!window.confirm("Are you sure you want to remove this recipe from favorites?")) return;
        try {
            await axios.delete(`${API_URL}/api/auth/favorites/${recipeId}`, {
                headers: { Authorization: `Bearer ${token}` },
            });
            fetchFavorites();
        } catch (error) {
            console.error("Error removing favorite recipe:", error);
        }
    };

    // Optional: Navigation back to Dashboard
    const handleBack = () => {
        navigate("/dashboard");
    };

    return (
        <div className="p-6">
            <div className="flex justify-between items-center mb-4">
                <h1 className="text-2xl font-bold">Favorite Recipes</h1>
                <button
                    onClick={handleBack}
                    className="bg-blue-500 text-white px-4 py-2 rounded"
                >
                    Back to Dashboard
                </button>
            </div>

            {favorites.length === 0 ? (
                <p>No favorite recipes found.</p>
            ) : (
                <table className="w-full border-collapse border">
                    <thead>
                    <tr className="bg-gray-200">
                        <th className="border p-2">ID</th>
                        <th className="border p-2">Title</th>
                        <th className="border p-2">Author</th>
                        <th className="border p-2">Image</th>
                        <th className="border p-2">Actions</th>
                    </tr>
                    </thead>
                    <tbody>
                    {favorites.map((fav) => (
                        <tr key={fav.id} className="border">
                            <td className="p-2">{fav.id}</td>
                            <td className="p-2">{fav.title}</td>
                            <td className="p-2">{fav.authorId}</td>
                            <td className="p-2">
                                {fav.imageUri ? (
                                    <img
                                        src={fav.imageUri}
                                        alt={fav.title}
                                        style={{ width: "80px", height: "auto" }}
                                    />
                                ) : (
                                    "No Image"
                                )}
                            </td>
                            <td className="p-2">
                                <button
                                    onClick={() => handleRemoveFavorite(fav.id)}
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
        </div>
    );
};

 export default FavoritesDashboard;


 **/