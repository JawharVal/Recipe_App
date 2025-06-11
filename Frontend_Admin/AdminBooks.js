import React, { useEffect, useState } from "react";
import axios from "axios";

const AdminBooks = ({ userId, token, onClose }) => {
    const [books, setBooks] = useState([]); // list of BookDTO objects
    const [recipesMap, setRecipesMap] = useState({}); // key: bookId, value: recipes array
    const [editingBook, setEditingBook] = useState(null);
    const [editBookData, setEditBookData] = useState({
        title: "",
        description: "",
        color: "",
        recipeIds: "",
    });

    // Fetch all books for a given user (using the endpoint: GET /api/books/user/{userId})
    const fetchBooks = async () => {
        try {
            const response = await axios.get(`http://localhost:8081/api/books/user/${userId}`, {
                headers: { Authorization: `Bearer ${token}` },
            });
            setBooks(response.data);
        } catch (error) {
            console.error("Error fetching books:", error);
        }
    };

    // For a given book, fetch details for each recipe (GET /api/recipes/{id})
    const fetchRecipesForBook = async (book) => {
        if (!book.recipeIds || book.recipeIds.length === 0) {
            return [];
        }
        try {
            const recipePromises = book.recipeIds.map((recipeId) =>
                axios
                    .get(`http://localhost:8081/api/recipes/${recipeId}`, {
                        headers: { Authorization: `Bearer ${token}` },
                    })
                    .then((res) => res.data)
            );
            const recipes = await Promise.all(recipePromises);
            return recipes;
        } catch (error) {
            console.error(`Error fetching recipes for book ${book.id}:`, error);
            return [];
        }
    };

    // Load recipes for each book after books are fetched
    useEffect(() => {
        const loadRecipesForBooks = async () => {
            const newRecipesMap = {};
            for (const book of books) {
                const recipes = await fetchRecipesForBook(book);
                newRecipesMap[book.id] = recipes;
            }
            setRecipesMap(newRecipesMap);
        };
        if (books.length > 0) {
            loadRecipesForBooks();
        }
    }, [books, token]);

    useEffect(() => {
        if (userId && token) {
            fetchBooks();
        }
    }, [userId, token]);

    // --- Book deletion ---
    const handleDeleteBook = async (bookId) => {
        if (!window.confirm("Are you sure you want to delete this book?")) return;
        try {
            await axios.delete(`http://localhost:8081/api/admin/books/${bookId}`, {
                headers: { Authorization: `Bearer ${token}` },
            });
            fetchBooks();
        } catch (error) {
            console.error("Error deleting book:", error);
        }
    };

    // --- Open edit modal for a book ---
    const handleEditBook = (book) => {
        setEditingBook(book);
        // Pre-fill the edit form with the book's current details.
        setEditBookData({
            title: book.title,
            description: book.description,
            color: book.color,
            recipeIds: book.recipeIds ? book.recipeIds.join(", ") : "",
        });
    };

    const handleEditBookChange = (e) => {
        const { name, value } = e.target;
        setEditBookData((prev) => ({ ...prev, [name]: value }));
    };

    // --- Save updated book ---
    const handleSaveBook = async () => {
        // Convert recipeIds string to an array of numbers.
        const recipeIds = editBookData.recipeIds
            .split(",")
            .map((id) => parseInt(id.trim(), 10))
            .filter((id) => !isNaN(id));
        const updatedBookData = { ...editBookData, recipeIds };
        try {
            await axios.put(`http://localhost:8081/api/admin/books/${editingBook.id}`, updatedBookData, {
                headers: { Authorization: `Bearer ${token}` },
            });
            setEditingBook(null);
            fetchBooks();
        } catch (error) {
            console.error("Error updating book:", error);
        }
    };

    // --- Remove a recipe from a book ---
    const handleRemoveRecipe = async (bookId, recipeId) => {
        try {
            await axios.delete(`http://localhost:8081/api/admin/books/${bookId}/recipes/${recipeId}`, {
                headers: { Authorization: `Bearer ${token}` },
            });
            fetchBooks();
        } catch (error) {
            console.error("Error removing recipe from book:", error);
        }
    };

    return (
        <div className="fixed inset-0 flex items-center justify-center bg-black bg-opacity-50 p-4">
            <div className="bg-white p-6 rounded shadow-lg w-full max-w-5xl overflow-y-auto max-h-full">
                <h2 className="text-xl font-semibold mb-4">Books for User {userId}</h2>
                {books.length === 0 ? (
                    <p>No books found for this user.</p>
                ) : (
                    <div>
                        {books.map((book) => (
                            <div key={book.id} className="border p-4 mb-4">
                                <h3 className="text-lg font-bold">
                                    Title: {book.title} (ID: {book.id})
                                </h3>
                                <p>
                                    <strong>Description:</strong> {book.description}
                                </p>
                                <p>
                                    <strong>Color:</strong> {book.color}
                                </p>
                                <div className="mt-2">
                                    <strong>Recipes:</strong>
                                    {recipesMap[book.id] && recipesMap[book.id].length > 0 ? (
                                        <ul className="list-disc ml-6">
                                            {recipesMap[book.id].map((recipe) => (
                                                <li key={recipe.id} className="flex items-center gap-2">
                          <span>
                            {recipe.title} (ID: {recipe.id})
                          </span>
                                                    <button
                                                        onClick={() => handleRemoveRecipe(book.id, recipe.id)}
                                                        className="bg-red-500 text-white px-1 rounded"
                                                    >
                                                        Remove
                                                    </button>
                                                </li>
                                            ))}
                                        </ul>
                                    ) : (
                                        <p>No recipes in this book.</p>
                                    )}
                                </div>
                                <div className="mt-2 flex gap-2">
                                    <button onClick={() => handleEditBook(book)} className="bg-blue-500 text-white px-2 py-1 rounded">
                                        Edit Book
                                    </button>
                                    <button onClick={() => handleDeleteBook(book.id)} className="bg-red-500 text-white px-2 py-1 rounded">
                                        Delete Book
                                    </button>
                                </div>
                            </div>
                        ))}
                    </div>
                )}
                <div className="flex justify-end mt-4">
                    <button onClick={onClose} className="bg-gray-300 text-black px-4 py-2 rounded">
                        Close
                    </button>
                </div>
            </div>

            {/* Edit Book Modal */}
            {editingBook && (
                <div className="fixed inset-0 flex items-center justify-center bg-black bg-opacity-50 p-4">
                    <div className="bg-white p-6 rounded shadow-lg w-full max-w-md">
                        <h2 className="text-xl font-semibold mb-4">Edit Book (ID: {editingBook.id})</h2>
                        <div className="mb-2">
                            <label className="block">Title:</label>
                            <input
                                type="text"
                                name="title"
                                value={editBookData.title}
                                onChange={handleEditBookChange}
                                className="w-full p-2 border rounded"
                            />
                        </div>
                        <div className="mb-2">
                            <label className="block">Description:</label>
                            <input
                                type="text"
                                name="description"
                                value={editBookData.description}
                                onChange={handleEditBookChange}
                                className="w-full p-2 border rounded"
                            />
                        </div>
                        <div className="mb-2">
                            <label className="block">Color:</label>
                            <input
                                type="text"
                                name="color"
                                value={editBookData.color}
                                onChange={handleEditBookChange}
                                className="w-full p-2 border rounded"
                            />
                        </div>
                        <div className="mb-2">
                            <label className="block">Recipe IDs (comma separated):</label>
                            <input
                                type="text"
                                name="recipeIds"
                                value={editBookData.recipeIds}
                                onChange={handleEditBookChange}
                                className="w-full p-2 border rounded"
                            />
                        </div>
                        <div className="flex justify-end mt-4 gap-2">
                            <button onClick={() => setEditingBook(null)} className="bg-gray-300 text-black px-4 py-2 rounded">
                                Cancel
                            </button>
                            <button onClick={handleSaveBook} className="bg-green-500 text-white px-4 py-2 rounded">
                                Save
                            </button>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
};

export default AdminBooks;


/**

 import React, { useEffect, useState } from "react";
 import axios from "axios";
 import API_URL from "./config";
 const AdminBooks = ({ userId, token, onClose }) => {
    const [books, setBooks] = useState([]); // list of BookDTO objects
    const [recipesMap, setRecipesMap] = useState({}); // key: bookId, value: recipes array
    const [editingBook, setEditingBook] = useState(null);
    const [editBookData, setEditBookData] = useState({
        title: "",
        description: "",
        color: "",
        recipeIds: "",
    });

    // Fetch all books for a given user (using the endpoint: GET /api/books/user/{userId})
    const fetchBooks = async () => {
        try {
            const response = await axios.get(`${API_URL}/api/books/user/${userId}`, {
                headers: { Authorization: `Bearer ${token}` },
            });
            setBooks(response.data);
        } catch (error) {
            console.error("Error fetching books:", error);
        }
    };

    // For a given book, fetch details for each recipe (GET /api/recipes/{id})
    const fetchRecipesForBook = async (book) => {
        if (!book.recipeIds || book.recipeIds.length === 0) {
            return [];
        }
        try {
            const recipePromises = book.recipeIds.map((recipeId) =>
                axios
                    .get(`${API_URL}/api/recipes/${recipeId}`, {
                        headers: { Authorization: `Bearer ${token}` },
                    })
                    .then((res) => res.data)
            );
            const recipes = await Promise.all(recipePromises);
            return recipes;
        } catch (error) {
            console.error(`Error fetching recipes for book ${book.id}:`, error);
            return [];
        }
    };

    // Load recipes for each book after books are fetched
    useEffect(() => {
        const loadRecipesForBooks = async () => {
            const newRecipesMap = {};
            for (const book of books) {
                const recipes = await fetchRecipesForBook(book);
                newRecipesMap[book.id] = recipes;
            }
            setRecipesMap(newRecipesMap);
        };
        if (books.length > 0) {
            loadRecipesForBooks();
        }
    }, [books, token]);

    useEffect(() => {
        if (userId && token) {
            fetchBooks();
        }
    }, [userId, token]);

    // --- Book deletion ---
    const handleDeleteBook = async (bookId) => {
        if (!window.confirm("Are you sure you want to delete this book?")) return;
        try {
            await axios.delete(`${API_URL}/api/admin/books/${bookId}`, {
                headers: { Authorization: `Bearer ${token}` },
            });
            fetchBooks();
        } catch (error) {
            console.error("Error deleting book:", error);
        }
    };

    // --- Open edit modal for a book ---
    const handleEditBook = (book) => {
        setEditingBook(book);
        // Pre-fill the edit form with the book's current details.
        setEditBookData({
            title: book.title,
            description: book.description,
            color: book.color,
            recipeIds: book.recipeIds ? book.recipeIds.join(", ") : "",
        });
    };

    const handleEditBookChange = (e) => {
        const { name, value } = e.target;
        setEditBookData((prev) => ({ ...prev, [name]: value }));
    };

    // --- Save updated book ---
    const handleSaveBook = async () => {
        // Convert recipeIds string to an array of numbers.
        const recipeIds = editBookData.recipeIds
            .split(",")
            .map((id) => parseInt(id.trim(), 10))
            .filter((id) => !isNaN(id));
        const updatedBookData = { ...editBookData, recipeIds };
        try {
            await axios.put(`http://${API_URL}/pi/admin/books/${editingBook.id}`, updatedBookData, {
                headers: { Authorization: `Bearer ${token}` },
            });
            setEditingBook(null);
            fetchBooks();
        } catch (error) {
            console.error("Error updating book:", error);
        }
    };

    // --- Remove a recipe from a book ---
    const handleRemoveRecipe = async (bookId, recipeId) => {
        try {
            await axios.delete(`${API_URL}/api/admin/books/${bookId}/recipes/${recipeId}`, {
                headers: { Authorization: `Bearer ${token}` },
            });
            fetchBooks();
        } catch (error) {
            console.error("Error removing recipe from book:", error);
        }
    };

    return (
        <div className="fixed inset-0 flex items-center justify-center bg-black bg-opacity-50 p-4">
            <div className="bg-white p-6 rounded shadow-lg w-full max-w-5xl overflow-y-auto max-h-full">
                <h2 className="text-xl font-semibold mb-4">Books for User {userId}</h2>
                {books.length === 0 ? (
                    <p>No books found for this user.</p>
                ) : (
                    <div>
                        {books.map((book) => (
                            <div key={book.id} className="border p-4 mb-4">
                                <h3 className="text-lg font-bold">
                                    Title: {book.title} (ID: {book.id})
                                </h3>
                                <p>
                                    <strong>Description:</strong> {book.description}
                                </p>
                                <p>
                                    <strong>Color:</strong> {book.color}
                                </p>
                                <div className="mt-2">
                                    <strong>Recipes:</strong>
                                    {recipesMap[book.id] && recipesMap[book.id].length > 0 ? (
                                        <ul className="list-disc ml-6">
                                            {recipesMap[book.id].map((recipe) => (
                                                <li key={recipe.id} className="flex items-center gap-2">
                          <span>
                            {recipe.title} (ID: {recipe.id})
                          </span>
                                                    <button
                                                        onClick={() => handleRemoveRecipe(book.id, recipe.id)}
                                                        className="bg-red-500 text-white px-1 rounded"
                                                    >
                                                        Remove
                                                    </button>
                                                </li>
                                            ))}
                                        </ul>
                                    ) : (
                                        <p>No recipes in this book.</p>
                                    )}
                                </div>
                                <div className="mt-2 flex gap-2">
                                    <button onClick={() => handleEditBook(book)} className="bg-blue-500 text-white px-2 py-1 rounded">
                                        Edit Book
                                    </button>
                                    <button onClick={() => handleDeleteBook(book.id)} className="bg-red-500 text-white px-2 py-1 rounded">
                                        Delete Book
                                    </button>
                                </div>
                            </div>
                        ))}
                    </div>
                )}
                <div className="flex justify-end mt-4">
                    <button onClick={onClose} className="bg-gray-300 text-black px-4 py-2 rounded">
                        Close
                    </button>
                </div>
            </div>


{editingBook && (
    <div className="fixed inset-0 flex items-center justify-center bg-black bg-opacity-50 p-4">
        <div className="bg-white p-6 rounded shadow-lg w-full max-w-md">
            <h2 className="text-xl font-semibold mb-4">Edit Book (ID: {editingBook.id})</h2>
            <div className="mb-2">
                <label className="block">Title:</label>
                <input
                    type="text"
                    name="title"
                    value={editBookData.title}
                    onChange={handleEditBookChange}
                    className="w-full p-2 border rounded"
                />
            </div>
            <div className="mb-2">
                <label className="block">Description:</label>
                <input
                    type="text"
                    name="description"
                    value={editBookData.description}
                    onChange={handleEditBookChange}
                    className="w-full p-2 border rounded"
                />
            </div>
            <div className="mb-2">
                <label className="block">Color:</label>
                <input
                    type="text"
                    name="color"
                    value={editBookData.color}
                    onChange={handleEditBookChange}
                    className="w-full p-2 border rounded"
                />
            </div>
            <div className="mb-2">
                <label className="block">Recipe IDs (comma separated):</label>
                <input
                    type="text"
                    name="recipeIds"
                    value={editBookData.recipeIds}
                    onChange={handleEditBookChange}
                    className="w-full p-2 border rounded"
                />
            </div>
            <div className="flex justify-end mt-4 gap-2">
                <button onClick={() => setEditingBook(null)} className="bg-gray-300 text-black px-4 py-2 rounded">
                    Cancel
                </button>
                <button onClick={handleSaveBook} className="bg-green-500 text-white px-4 py-2 rounded">
                    Save
                </button>
            </div>
        </div>
    </div>
)}
</div>
);
};

 export default AdminBooks;




 **/
