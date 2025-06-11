// AdminShoppingList.js
import React, { useEffect, useState } from "react";
import axios from "axios";

const AdminShoppingList = ({ userId, userName, token, onClose }) => {
    const [items, setItems] = useState([]);
    const [newItem, setNewItem] = useState({
        name: "",
        category: "",
        count: 1,
    });
    const [editingItem, setEditingItem] = useState(null);

    // Fetch all shopping list items for the specified user (admin view)
    const fetchItems = async () => {
        try {
            const response = await axios.get(
                `http://localhost:8081/api/admin/users/${userId}/shopping-list`,
                { headers: { Authorization: `Bearer ${token}` } }
            );
            setItems(response.data);
        } catch (error) {
            console.error("Error fetching shopping list items:", error);
        }
    };

    useEffect(() => {
        if (userId && token) {
            fetchItems();
        }
    }, [userId, token]);

    // Handle input changes for add or edit form
    const handleInputChange = (e) => {
        const { name, value, type } = e.target;
        const val = type === "number" ? parseInt(value, 10) : value;
        if (editingItem) {
            setEditingItem({ ...editingItem, [name]: val });
        } else {
            setNewItem({ ...newItem, [name]: val });
        }
    };

    // Add a new item for this user
    const handleAddItem = async (e) => {
        e.preventDefault();
        try {
            await axios.post(
                `http://localhost:8081/api/admin/users/${userId}/shopping-list`,
                newItem,
                { headers: { Authorization: `Bearer ${token}` } }
            );
            setNewItem({ name: "", category: "", count: 1 });
            fetchItems();
        } catch (error) {
            console.error("Error adding shopping list item:", error);
        }
    };

    // Update an existing item for this user
    const handleUpdateItem = async (e) => {
        e.preventDefault();
        try {
            await axios.put(
                `http://localhost:8081/api/admin/users/${userId}/shopping-list/${editingItem.id}`,
                editingItem,
                { headers: { Authorization: `Bearer ${token}` } }
            );
            setEditingItem(null);
            fetchItems();
        } catch (error) {
            console.error("Error updating shopping list item:", error);
        }
    };

    // Delete a single item
    const handleDeleteItem = async (id) => {
        if (!window.confirm("Are you sure you want to delete this item?")) return;
        try {
            await axios.delete(
                `http://localhost:8081/api/admin/users/${userId}/shopping-list/${id}`,
                { headers: { Authorization: `Bearer ${token}` } }
            );
            fetchItems();
        } catch (error) {
            console.error("Error deleting shopping list item:", error);
        }
    };

    // Bulk delete (if supported by your backend)
    const handleBulkDelete = async () => {
        if (!window.confirm("Are you sure you want to delete all items?")) return;
        try {
            const ids = items.map((item) => item.id);
            await axios.delete(
                `http://localhost:8081/api/admin/users/${userId}/shopping-list/bulk`,
                {
                    data: ids,
                    headers: { Authorization: `Bearer ${token}` },
                }
            );
            fetchItems();
        } catch (error) {
            console.error("Error bulk deleting shopping list items:", error);
        }
    };

    return (
        <div className="fixed inset-0 flex items-center justify-center bg-black bg-opacity-50 p-4">
            <div className="bg-white p-6 rounded shadow-lg w-full max-w-4xl overflow-y-auto max-h-full">
                <h2 className="text-xl font-semibold mb-4">
                    {userName}'s Shopping List
                </h2>

                {/* Form for adding a new item or editing an existing item */}
                <form
                    onSubmit={editingItem ? handleUpdateItem : handleAddItem}
                    className="mb-4"
                >
                    <div className="flex flex-wrap gap-2">
                        <input
                            type="text"
                            name="name"
                            placeholder="Item Name"
                            value={editingItem ? editingItem.name : newItem.name}
                            onChange={handleInputChange}
                            className="p-2 border rounded"
                            required
                        />
                        <input
                            type="text"
                            name="category"
                            placeholder="Category"
                            value={editingItem ? editingItem.category : newItem.category}
                            onChange={handleInputChange}
                            className="p-2 border rounded"
                        />
                        <input
                            type="number"
                            name="count"
                            placeholder="Count"
                            value={editingItem ? editingItem.count : newItem.count}
                            onChange={handleInputChange}
                            className="p-2 border rounded w-24"
                            min="1"
                        />
                        <button
                            type="submit"
                            className="bg-blue-500 text-white px-4 py-2 rounded"
                        >
                            {editingItem ? "Update" : "Add"}
                        </button>
                        {editingItem && (
                            <button
                                type="button"
                                onClick={() => setEditingItem(null)}
                                className="bg-gray-300 text-black px-4 py-2 rounded"
                            >
                                Cancel
                            </button>
                        )}
                    </div>
                </form>

                {/* Bulk delete button */}
                <button
                    onClick={handleBulkDelete}
                    className="bg-red-500 text-white px-4 py-2 rounded mb-4"
                >
                    Delete All Items
                </button>

                {/* Shopping list items table */}
                <table className="w-full border-collapse border">
                    <thead>
                    <tr className="bg-gray-200">
                        <th className="border p-2">ID</th>
                        <th className="border p-2">Name</th>
                        <th className="border p-2">Category</th>
                        <th className="border p-2">Count</th>
                        <th className="border p-2">Actions</th>
                    </tr>
                    </thead>
                    <tbody>
                    {items.map((item) => (
                        <tr key={item.id} className="border">
                            <td className="p-2">{item.id}</td>
                            <td className="p-2">{item.name}</td>
                            <td className="p-2">{item.category}</td>
                            <td className="p-2">{item.count}</td>
                            <td className="p-2">
                                <button
                                    onClick={() => setEditingItem(item)}
                                    className="bg-blue-500 text-white px-2 py-1 rounded mr-2"
                                >
                                    Edit
                                </button>
                                <button
                                    onClick={() => handleDeleteItem(item.id)}
                                    className="bg-red-500 text-white px-2 py-1 rounded"
                                >
                                    Delete
                                </button>
                            </td>
                        </tr>
                    ))}
                    {items.length === 0 && (
                        <tr>
                            <td className="p-2" colSpan="5">
                                No items found.
                            </td>
                        </tr>
                    )}
                    </tbody>
                </table>

                {/* Close button */}
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

export default AdminShoppingList;


/**

 // AdminShoppingList.js
 import React, { useEffect, useState } from "react";
 import axios from "axios";
 import API_URL from "./config";
 const AdminShoppingList = ({ userId, userName, token, onClose }) => {
    const [items, setItems] = useState([]);
    const [newItem, setNewItem] = useState({
        name: "",
        category: "",
        count: 1,
    });
    const [editingItem, setEditingItem] = useState(null);

    // Fetch all shopping list items for the specified user (admin view)
    const fetchItems = async () => {
        try {
            const response = await axios.get(
                `${API_URL}/api/admin/users/${userId}/shopping-list`,
                { headers: { Authorization: `Bearer ${token}` } }
            );
            setItems(response.data);
        } catch (error) {
            console.error("Error fetching shopping list items:", error);
        }
    };

    useEffect(() => {
        if (userId && token) {
            fetchItems();
        }
    }, [userId, token]);

    // Handle input changes for add or edit form
    const handleInputChange = (e) => {
        const { name, value, type } = e.target;
        const val = type === "number" ? parseInt(value, 10) : value;
        if (editingItem) {
            setEditingItem({ ...editingItem, [name]: val });
        } else {
            setNewItem({ ...newItem, [name]: val });
        }
    };

    // Add a new item for this user
    const handleAddItem = async (e) => {
        e.preventDefault();
        try {
            await axios.post(
                `${API_URL}/api/admin/users/${userId}/shopping-list`,
                newItem,
                { headers: { Authorization: `Bearer ${token}` } }
            );
            setNewItem({ name: "", category: "", count: 1 });
            fetchItems();
        } catch (error) {
            console.error("Error adding shopping list item:", error);
        }
    };

    // Update an existing item for this user
    const handleUpdateItem = async (e) => {
        e.preventDefault();
        try {
            await axios.put(
                `${API_URL}/api/admin/users/${userId}/shopping-list/${editingItem.id}`,
                editingItem,
                { headers: { Authorization: `Bearer ${token}` } }
            );
            setEditingItem(null);
            fetchItems();
        } catch (error) {
            console.error("Error updating shopping list item:", error);
        }
    };

    // Delete a single item
    const handleDeleteItem = async (id) => {
        if (!window.confirm("Are you sure you want to delete this item?")) return;
        try {
            await axios.delete(
                `${API_URL}/api/admin/users/${userId}/shopping-list/${id}`,
                { headers: { Authorization: `Bearer ${token}` } }
            );
            fetchItems();
        } catch (error) {
            console.error("Error deleting shopping list item:", error);
        }
    };

    // Bulk delete (if supported by your backend)
    const handleBulkDelete = async () => {
        if (!window.confirm("Are you sure you want to delete all items?")) return;
        try {
            const ids = items.map((item) => item.id);
            await axios.delete(
                `${API_URL}/api/admin/users/${userId}/shopping-list/bulk`,
                {
                    data: ids,
                    headers: { Authorization: `Bearer ${token}` },
                }
            );
            fetchItems();
        } catch (error) {
            console.error("Error bulk deleting shopping list items:", error);
        }
    };

    return (
        <div className="fixed inset-0 flex items-center justify-center bg-black bg-opacity-50 p-4">
            <div className="bg-white p-6 rounded shadow-lg w-full max-w-4xl overflow-y-auto max-h-full">
                <h2 className="text-xl font-semibold mb-4">
                    {userName}'s Shopping List
                </h2>


<form
    onSubmit={editingItem ? handleUpdateItem : handleAddItem}
    className="mb-4"
>
    <div className="flex flex-wrap gap-2">
        <input
            type="text"
            name="name"
            placeholder="Item Name"
            value={editingItem ? editingItem.name : newItem.name}
            onChange={handleInputChange}
            className="p-2 border rounded"
            required
        />
        <input
            type="text"
            name="category"
            placeholder="Category"
            value={editingItem ? editingItem.category : newItem.category}
            onChange={handleInputChange}
            className="p-2 border rounded"
        />
        <input
            type="number"
            name="count"
            placeholder="Count"
            value={editingItem ? editingItem.count : newItem.count}
            onChange={handleInputChange}
            className="p-2 border rounded w-24"
            min="1"
        />
        <button
            type="submit"
            className="bg-blue-500 text-white px-4 py-2 rounded"
        >
            {editingItem ? "Update" : "Add"}
        </button>
        {editingItem && (
            <button
                type="button"
                onClick={() => setEditingItem(null)}
                className="bg-gray-300 text-black px-4 py-2 rounded"
            >
                Cancel
            </button>
        )}
    </div>
</form>


<button
    onClick={handleBulkDelete}
    className="bg-red-500 text-white px-4 py-2 rounded mb-4"
>
    Delete All Items
</button>


<table className="w-full border-collapse border">
    <thead>
    <tr className="bg-gray-200">
        <th className="border p-2">ID</th>
        <th className="border p-2">Name</th>
        <th className="border p-2">Category</th>
        <th className="border p-2">Count</th>
        <th className="border p-2">Actions</th>
    </tr>
    </thead>
    <tbody>
    {items.map((item) => (
        <tr key={item.id} className="border">
            <td className="p-2">{item.id}</td>
            <td className="p-2">{item.name}</td>
            <td className="p-2">{item.category}</td>
            <td className="p-2">{item.count}</td>
            <td className="p-2">
                <button
                    onClick={() => setEditingItem(item)}
                    className="bg-blue-500 text-white px-2 py-1 rounded mr-2"
                >
                    Edit
                </button>
                <button
                    onClick={() => handleDeleteItem(item.id)}
                    className="bg-red-500 text-white px-2 py-1 rounded"
                >
                    Delete
                </button>
            </td>
        </tr>
    ))}
    {items.length === 0 && (
        <tr>
            <td className="p-2" colSpan="5">
                No items found.
            </td>
        </tr>
    )}
    </tbody>
</table>


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

 export default AdminShoppingList;



 **/