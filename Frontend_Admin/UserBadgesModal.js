import React, { useEffect, useState } from "react";
import axios from "axios";

const UserBadgesModal = ({ userId, userName, token, onClose }) => {
    const [badges, setBadges] = useState({});
    const [badgeName, setBadgeName] = useState("");
    const [badgeCount, setBadgeCount] = useState(0);
    const [editingBadge, setEditingBadge] = useState(null);

    // Fetch the badges for the specified user
    const fetchBadges = () => {
        axios
            .get(`http://localhost:8081/api/admin/users/${userId}/badges`, {
                headers: { Authorization: `Bearer ${token}` },
            })
            .then((response) => setBadges(response.data))
            .catch((error) =>
                console.error("Error fetching badges for user:", error)
            );
    };

    useEffect(() => {
        fetchBadges();
    }, [userId, token]);

    const handleAddOrUpdateBadge = () => {
        axios
            .post(`http://localhost:8081/api/admin/users/${userId}/badges`, { badge: badgeName, count: badgeCount }, {
                headers: { Authorization: `Bearer ${token}` },
            })
            .then(() => {
                fetchBadges();
                setBadgeName("");
                setBadgeCount(0);
                setEditingBadge(null);
            })
            .catch((error) =>
                console.error("Error adding/updating badge:", error)
            );
    };

    const handleDeleteBadge = (badgeKey) => {
        if (!window.confirm("Are you sure you want to delete this badge?")) return;
        axios
            .delete(`http://localhost:8081/api/admin/users/${userId}/badges/${badgeKey}`, {
                headers: { Authorization: `Bearer ${token}` },
            })
            .then(() => fetchBadges())
            .catch((error) =>
                console.error("Error deleting badge:", error)
            );
    };

    return (
        <div className="fixed inset-0 flex items-center justify-center bg-black bg-opacity-50">
            <div className="bg-white p-6 rounded shadow-lg w-11/12 max-w-3xl overflow-y-auto max-h-full">
                <h2 className="text-xl font-semibold mb-4">{userName}'s Badges</h2>
                {Object.keys(badges).length === 0 ? (
                    <p>No badges found for this user.</p>
                ) : (
                    <table className="w-full border-collapse border">
                        <thead>
                        <tr className="bg-gray-200">
                            <th className="border p-2">Badge</th>
                            <th className="border p-2">Count</th>
                            <th className="border p-2">Actions</th>
                        </tr>
                        </thead>
                        <tbody>
                        {Object.entries(badges).map(([key, count]) => (
                            <tr key={key} className="border">
                                <td className="p-2">{key}</td>
                                <td className="p-2">{count}</td>
                                <td className="p-2">
                                    <button
                                        onClick={() => {
                                            setEditingBadge(key);
                                            setBadgeName(key);
                                            setBadgeCount(count);
                                        }}
                                        className="bg-blue-500 text-white px-2 py-1 rounded mr-2"
                                    >
                                        Edit
                                    </button>
                                    <button
                                        onClick={() => handleDeleteBadge(key)}
                                        className="bg-red-500 text-white px-2 py-1 rounded"
                                    >
                                        Delete
                                    </button>
                                </td>
                            </tr>
                        ))}
                        </tbody>
                    </table>
                )}
                <div className="mt-4">
                    <h3 className="text-lg font-semibold mb-2">
                        {editingBadge ? "Edit Badge" : "Add New Badge"}
                    </h3>
                    <div className="flex space-x-2">
                        <input
                            type="text"
                            placeholder="Badge Name"
                            value={badgeName}
                            onChange={(e) => setBadgeName(e.target.value)}
                            className="p-2 border rounded"
                        />
                        <input
                            type="number"
                            placeholder="Count"
                            value={badgeCount}
                            onChange={(e) => setBadgeCount(Number(e.target.value))}
                            className="p-2 border rounded"
                        />
                        <button
                            onClick={handleAddOrUpdateBadge}
                            className="bg-green-500 text-white px-4 py-2 rounded"
                        >
                            {editingBadge ? "Update" : "Add"}
                        </button>
                    </div>
                </div>
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

export default UserBadgesModal;


/**

 import React, { useEffect, useState } from "react";
 import axios from "axios";
 import API_URL from "./config";
 const UserBadgesModal = ({ userId, userName, token, onClose }) => {
    const [badges, setBadges] = useState({});
    const [badgeName, setBadgeName] = useState("");
    const [badgeCount, setBadgeCount] = useState(0);
    const [editingBadge, setEditingBadge] = useState(null);

    // Fetch the badges for the specified user
    const fetchBadges = () => {
        axios
            .get(`${API_URL}/api/admin/users/${userId}/badges`, {
                headers: { Authorization: `Bearer ${token}` },
            })
            .then((response) => setBadges(response.data))
            .catch((error) =>
                console.error("Error fetching badges for user:", error)
            );
    };

    useEffect(() => {
        fetchBadges();
    }, [userId, token]);

    const handleAddOrUpdateBadge = () => {
        axios
            .post(`${API_URL}/api/admin/users/${userId}/badges`, { badge: badgeName, count: badgeCount }, {
                headers: { Authorization: `Bearer ${token}` },
            })
            .then(() => {
                fetchBadges();
                setBadgeName("");
                setBadgeCount(0);
                setEditingBadge(null);
            })
            .catch((error) =>
                console.error("Error adding/updating badge:", error)
            );
    };

    const handleDeleteBadge = (badgeKey) => {
        if (!window.confirm("Are you sure you want to delete this badge?")) return;
        axios
            .delete(`${API_URL}/api/admin/users/${userId}/badges/${badgeKey}`, {
                headers: { Authorization: `Bearer ${token}` },
            })
            .then(() => fetchBadges())
            .catch((error) =>
                console.error("Error deleting badge:", error)
            );
    };

    return (
        <div className="fixed inset-0 flex items-center justify-center bg-black bg-opacity-50">
            <div className="bg-white p-6 rounded shadow-lg w-11/12 max-w-3xl overflow-y-auto max-h-full">
                <h2 className="text-xl font-semibold mb-4">{userName}'s Badges</h2>
                {Object.keys(badges).length === 0 ? (
                    <p>No badges found for this user.</p>
                ) : (
                    <table className="w-full border-collapse border">
                        <thead>
                        <tr className="bg-gray-200">
                            <th className="border p-2">Badge</th>
                            <th className="border p-2">Count</th>
                            <th className="border p-2">Actions</th>
                        </tr>
                        </thead>
                        <tbody>
                        {Object.entries(badges).map(([key, count]) => (
                            <tr key={key} className="border">
                                <td className="p-2">{key}</td>
                                <td className="p-2">{count}</td>
                                <td className="p-2">
                                    <button
                                        onClick={() => {
                                            setEditingBadge(key);
                                            setBadgeName(key);
                                            setBadgeCount(count);
                                        }}
                                        className="bg-blue-500 text-white px-2 py-1 rounded mr-2"
                                    >
                                        Edit
                                    </button>
                                    <button
                                        onClick={() => handleDeleteBadge(key)}
                                        className="bg-red-500 text-white px-2 py-1 rounded"
                                    >
                                        Delete
                                    </button>
                                </td>
                            </tr>
                        ))}
                        </tbody>
                    </table>
                )}
                <div className="mt-4">
                    <h3 className="text-lg font-semibold mb-2">
                        {editingBadge ? "Edit Badge" : "Add New Badge"}
                    </h3>
                    <div className="flex space-x-2">
                        <input
                            type="text"
                            placeholder="Badge Name"
                            value={badgeName}
                            onChange={(e) => setBadgeName(e.target.value)}
                            className="p-2 border rounded"
                        />
                        <input
                            type="number"
                            placeholder="Count"
                            value={badgeCount}
                            onChange={(e) => setBadgeCount(Number(e.target.value))}
                            className="p-2 border rounded"
                        />
                        <button
                            onClick={handleAddOrUpdateBadge}
                            className="bg-green-500 text-white px-4 py-2 rounded"
                        >
                            {editingBadge ? "Update" : "Add"}
                        </button>
                    </div>
                </div>
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

 export default UserBadgesModal;




 **/
