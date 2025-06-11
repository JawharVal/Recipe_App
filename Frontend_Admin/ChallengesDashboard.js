import React, { useEffect, useState } from "react";
import axios from "axios";
import "./ChallengesDashboard.css";

const ChallengesDashboard = () => {
    const [recipeSubmissions, setRecipeSubmissions] = useState([]);
    const [challenges, setChallenges] = useState([]);
    const [editingChallenge, setEditingChallenge] = useState(null);
    const [featuredWinners, setFeaturedWinners] = useState([]);
    const [editingWinner, setEditingWinner] = useState(null);
    const [leaderboard, setLeaderboard] = useState([]);
    const [editingLeaderboard, setEditingLeaderboard] = useState(null);
    const [editingSubmission, setEditingSubmission] = useState(null);
    const token = localStorage.getItem("token");
// NEW: State for adding a new challenge
    const [isAddingChallenge, setIsAddingChallenge] = useState(false);
    // -------------------- Challenges --------------------
    const fetchChallenges = async () => {
        try {
            const response = await axios.get("http://localhost:8081/api/challenges", {
                headers: { Authorization: `Bearer ${token}` },
            });
            setChallenges(response.data);
        } catch (error) {
            console.error("Error fetching challenges:", error);
        }
    };
    // NEW: Add a challenge from a front-end form
    const addChallenge = async (challengeData) => {
        try {
            const challengePayload = {
                ...challengeData,
                featured: challengeData.featured,  // No need for conversion, already a boolean
                active: false, // Explicitly set active to false
            };

            console.log("Sending challenge:", challengePayload); // Debugging log

            await axios.post("http://localhost:8081/api/challenges", challengePayload, {
                headers: { Authorization: `Bearer ${token}` },
            });

            setIsAddingChallenge(false);
            fetchChallenges();
        } catch (error) {
            console.error("Error creating challenge:", error);
        }
    };


    /**
     * Modal component for adding a new Challenge
     */
    const AddChallengeModal = ({ onClose, onAddChallenge }) => {
        const [formData, setFormData] = useState({
            title: "",
            description: "",
            imageUrl: "",
            deadline: "",
            points: 0,
            featured: false, // false => casual, true => monthly
            maxSubmissions: 1,
            active: false,
        });

        const handleChange = (e) => {
            const { name, value, type } = e.target;
            // If user chooses "challengeOfTheMonth", featured = true; else false
            if (name === "featured") {
                const isFeatured = value === "challengeOfTheMonth";
                setFormData({ ...formData, featured: isFeatured });
            } else if (type === "number") {
                setFormData({ ...formData, [name]: parseInt(value, 10) });
            } else {
                setFormData({ ...formData, [name]: value });
            }
        };

        const handleSubmit = async (e) => {
            e.preventDefault();
            // Validate form data if needed
            onAddChallenge(formData);
        };

        return (
            <div className="modal-overlay">
                <div className="modal-content">
                    <h2 className="modal-header">Add New Challenge</h2>
                    <form onSubmit={handleSubmit} className="modal-grid">
                        <input
                            type="text"
                            name="title"
                            value={formData.title}
                            onChange={handleChange}
                            placeholder="Title"
                            className="modal-input"
                            required
                        />
                        <textarea
                            name="description"
                            value={formData.description}
                            onChange={handleChange}
                            placeholder="Description"
                            className="modal-input"
                            required
                        />
                        <label className="modal-label">Deadline:</label>
                        <input
                            type="date"
                            name="deadline"
                            value={formData.deadline}
                            onChange={handleChange}
                            className="modal-input"
                            required
                        />
                        <label className="modal-label">Points:</label>
                        <input
                            type="number"
                            name="points"
                            value={formData.points}
                            onChange={handleChange}
                            placeholder="Points"
                            className="modal-input"
                        />
                        <label className="modal-label">Challenge Type:</label>
                        <select
                            name="featured"
                            value={formData.featured ? "challengeOfTheMonth" : "casual"}
                            onChange={handleChange}
                            className="modal-select"
                        >
                            <option value="casual">Casual Challenge</option>
                            <option value="challengeOfTheMonth">Challenge of the Month</option>
                        </select>
                        <label className="modal-label">Max Submissions:</label>
                        <input
                            type="number"
                            name="maxSubmissions"
                            value={formData.maxSubmissions}
                            onChange={handleChange}
                            placeholder="Max Submissions"
                            className="modal-input"
                        />
                        <label className="modal-label">Image URL:</label>
                        <input
                            type="text"
                            name="imageUrl"
                            value={formData.imageUrl}
                            onChange={handleChange}
                            placeholder="Image URL"
                            className="modal-input"
                        />
                        {formData.imageUrl && (
                            <img src={formData.imageUrl} alt="Preview" className="modal-image-preview" />
                        )}

                        <div className="modal-buttons">
                            <button type="button" onClick={onClose} className="btn btn-cancel">
                                Cancel
                            </button>
                            <button type="submit" className="btn btn-save">
                                Create
                            </button>
                        </div>
                    </form>
                </div>
            </div>
        );
    };

    const handleEditChallenge = (challenge) => {
        setEditingChallenge({ ...challenge });
    };

    const handleChallengeInputChange = (e) => {
        const { name, value, type, checked } = e.target;
        let newValue = type === "checkbox" ? checked : value;
        if (name === "featured") {
            newValue = value === "challengeOfTheMonth";
        }
        setEditingChallenge({ ...editingChallenge, [name]: newValue });
    };

    const updateChallenge = async () => {
        try {
            await axios.put(
                `http://localhost:8081/api/challenges/${editingChallenge.id}`,
                editingChallenge,
                { headers: { Authorization: `Bearer ${token}` } }
            );
            setEditingChallenge(null);
            fetchChallenges();
        } catch (error) {
            console.error("Error updating challenge:", error);
        }
    };

    const deleteChallenge = async (id) => {
        if (!window.confirm("Are you sure you want to delete this challenge?")) return;
        try {
            await axios.delete(`http://localhost:8081/api/challenges/${id}`, {
                headers: { Authorization: `Bearer ${token}` },
            });
            fetchChallenges();
        } catch (error) {
            console.error("Error deleting challenge:", error);
        }
    };

    // -------------------- Featured Winners --------------------
    const fetchFeaturedWinners = async () => {
        try {
            const response = await axios.get("http://localhost:8081/api/challenges/featured-winners", {
                headers: { Authorization: `Bearer ${token}` },
            });
            setFeaturedWinners(response.data);
        } catch (error) {
            console.error("Error fetching featured winners:", error);
        }
    };

    const handleEditWinner = (winner) => {
        setEditingWinner({ ...winner });
    };

    const handleWinnerInputChange = (e) => {
        const { name, value, type } = e.target;
        const newValue = type === "number" ? parseInt(value, 10) : value;
        setEditingWinner({ ...editingWinner, [name]: newValue });
    };

    const updateWinner = async () => {
        try {
            await axios.put(
                `http://localhost:8081/api/challenges/featured-winners/${editingWinner.id}`,
                editingWinner,
                { headers: { Authorization: `Bearer ${token}` } }
            );
            setEditingWinner(null);
            fetchFeaturedWinners();
        } catch (error) {
            console.error("Error updating featured winner:", error);
        }
    };

    const deleteWinner = async (id) => {
        if (!window.confirm("Are you sure you want to delete this winner?")) return;
        try {
            await axios.delete(`http://localhost:8081/api/challenges/featured-winners/${id}`, {
                headers: { Authorization: `Bearer ${token}` },
            });
            fetchFeaturedWinners();
        } catch (error) {
            console.error("Error deleting featured winner:", error);
        }
    };

    const addWinner = async (newWinner) => {
        try {
            await axios.post("http://localhost:8081/api/challenges/featured-winners", newWinner, {
                headers: { Authorization: `Bearer ${token}` },
            });
            fetchFeaturedWinners();
        } catch (error) {
            console.error("Error adding featured winner:", error);
        }
    };

    // -------------------- Global Leaderboard --------------------
    const fetchLeaderboard = async () => {
        try {
            const response = await axios.get("http://localhost:8081/api/challenges/global-leaderboard", {
                headers: { Authorization: `Bearer ${token}` },
            });
            setLeaderboard(response.data);
        } catch (error) {
            console.error("Error fetching global leaderboard:", error);
        }
    };

    const handleEditLeaderboard = (entry) => {
        setEditingLeaderboard({ ...entry });
    };

    const handleLeaderboardInputChange = (e) => {
        const { name, value, type } = e.target;
        const newValue = type === "number" ? parseInt(value, 10) : value;
        setEditingLeaderboard({ ...editingLeaderboard, [name]: newValue });
    };

    const updateLeaderboardEntry = async () => {
        try {
            await axios.put(
                `http://localhost:8081/api/challenges/global-leaderboard/${editingLeaderboard.id}`,
                editingLeaderboard,
                { headers: { Authorization: `Bearer ${token}` } }
            );
            setEditingLeaderboard(null);
            fetchLeaderboard();
        } catch (error) {
            console.error("Error updating leaderboard entry:", error);
        }
    };

    const deleteLeaderboardEntry = async (id) => {
        if (!window.confirm("Are you sure you want to delete this leaderboard entry?")) return;
        try {
            await axios.delete(`http://localhost:8081/api/challenges/global-leaderboard/${id}`, {
                headers: { Authorization: `Bearer ${token}` },
            });
            fetchLeaderboard();
        } catch (error) {
            console.error("Error deleting leaderboard entry:", error);
        }
    };

    const addLeaderboardEntry = async (newEntry) => {
        try {
            await axios.post("http://localhost:8081/api/challenges/global-leaderboard", newEntry, {
                headers: { Authorization: `Bearer ${token}` },
            });
            fetchLeaderboard();
        } catch (error) {
            console.error("Error adding leaderboard entry:", error);
        }
    };

    // -------------------- Recipe Submissions --------------------
    const fetchRecipeSubmissions = async () => {
        try {
            const response = await axios.get("http://localhost:8081/api/challenges/recipe-submissions", {
                headers: { Authorization: `Bearer ${token}` },
            });
            setRecipeSubmissions(response.data);
        } catch (error) {
            console.error("Error fetching recipe submissions:", error);
        }
    };

    const handleEditSubmission = (submission) => {
        setEditingSubmission({ ...submission });
    };

    const handleSubmissionChange = (e) => {
        const { name, value } = e.target;
        setEditingSubmission({ ...editingSubmission, [name]: value });
    };

    const updateSubmission = async () => {
        try {
            await axios.put(
                `http://localhost:8081/api/challenges/recipe-submissions/${editingSubmission.id}`,
                editingSubmission,
                { headers: { Authorization: `Bearer ${token}` } }
            );
            setEditingSubmission(null);
            fetchRecipeSubmissions();
        } catch (error) {
            console.error("Error updating recipe submission:", error);
        }
    };

    const deleteSubmission = async (id) => {
        if (!window.confirm("Are you sure you want to delete this submission?")) return;
        try {
            await axios.delete(`http://localhost:8081/api/challenges/recipe-submissions/${id}`, {
                headers: { Authorization: `Bearer ${token}` },
            });
            fetchRecipeSubmissions();
        } catch (error) {
            console.error("Error deleting recipe submission:", error);
        }
    };

    // Initial fetch
    useEffect(() => {
        if (token) {
            fetchChallenges();
            fetchFeaturedWinners();
            fetchLeaderboard();
            fetchRecipeSubmissions();
        }
    }, [token]);

    return (
        <div className="challenges-dashboard-container">
            <h1 className="dashboard-title">Challenges Dashboard</h1>
            {/* Add Challenge Button */}
            <div className="add-challenge-container">
                <button className="btn btn-add-challenge" onClick={() => setIsAddingChallenge(true)}>
                    Add New Challenge
                </button>
            </div>
            {/* Challenges Table */}
            <table className="dashboard-table">
                <thead>
                <tr>
                    <th>ID</th>
                    <th>Title</th>
                    <th>Description</th>
                    <th>Image</th>
                    <th>Deadline</th>
                    <th>Points</th>
                    <th>Type</th>
                    <th>Max Submissions</th>
                    <th>Actions</th>
                </tr>
                </thead>
                <tbody>
                {challenges.map((challenge) => (
                    <tr key={challenge.id}>
                        <td>{challenge.id}</td>
                        <td>{challenge.title}</td>
                        <td>{challenge.description}</td>
                        <td>
                            {challenge.imageUrl ? (
                                <img src={challenge.imageUrl} alt={challenge.title} className="challenge-image" />
                            ) : (
                                "No Image"
                            )}
                        </td>
                        <td>{challenge.deadline}</td>
                        <td>{challenge.points}</td>
                        <td>{challenge.featured ? "Challenge of the Month" : "Casual Challenge"}</td>
                        <td>{challenge.maxSubmissions}</td>
                        <td>
                            <button onClick={() => handleEditChallenge(challenge)} className="btn btn-edit">
                                Edit
                            </button>
                            <button onClick={() => deleteChallenge(challenge.id)} className="btn btn-delete">
                                Delete
                            </button>
                        </td>
                    </tr>
                ))}
                {challenges.length === 0 && (
                    <tr>
                        <td className="text-center" colSpan="10">No challenges found.</td>
                    </tr>
                )}
                </tbody>
            </table>

            {/* Edit Challenge Modal */}
            {editingChallenge && (
                <div className="modal-overlay">
                    <div className="modal-content">
                        <h2 className="modal-header">Edit Challenge</h2>
                        <div className="modal-grid">
                            <input
                                type="text"
                                name="title"
                                value={editingChallenge.title}
                                onChange={handleChallengeInputChange}
                                placeholder="Title"
                                className="modal-input"
                            />
                            <textarea
                                name="description"
                                value={editingChallenge.description}
                                onChange={handleChallengeInputChange}
                                placeholder="Description"
                                className="modal-input"
                            />
                            <input
                                type="date"
                                name="deadline"
                                value={editingChallenge.deadline}
                                onChange={handleChallengeInputChange}
                                className="modal-input"
                            />
                            <input
                                type="number"
                                name="points"
                                value={editingChallenge.points}
                                onChange={handleChallengeInputChange}
                                placeholder="Points"
                                className="modal-input"
                            />
                            <div className="modal-select-group">
                                <label>Challenge Type:</label>
                                <select
                                    name="featured"
                                    value={editingChallenge.featured ? "challengeOfTheMonth" : "casual"}
                                    onChange={handleChallengeInputChange}
                                    className="modal-select"
                                >
                                    <option value="casual">Casual Challenge</option>
                                    <option value="challengeOfTheMonth">Challenge of the Month</option>
                                </select>
                            </div>
                            <input
                                type="number"
                                name="maxSubmissions"
                                value={editingChallenge.maxSubmissions}
                                onChange={handleChallengeInputChange}
                                placeholder="Max Submissions"
                                className="modal-input"
                            />
                            <div className="modal-image-group">
                                <input
                                    type="text"
                                    name="imageUrl"
                                    value={editingChallenge.imageUrl || ""}
                                    onChange={handleChallengeInputChange}
                                    placeholder="Image URL"
                                    className="modal-input"
                                />
                                {editingChallenge.imageUrl && (
                                    <img src={editingChallenge.imageUrl} alt="Preview" className="modal-image-preview" />
                                )}
                            </div>
                        </div>
                        <div className="modal-buttons">
                            <button onClick={() => setEditingChallenge(null)} className="btn btn-cancel">
                                Cancel
                            </button>
                            <button onClick={updateChallenge} className="btn btn-save">
                                Save
                            </button>
                        </div>
                    </div>
                </div>
            )}
            {/* Add Challenge Modal */}
            {isAddingChallenge && (
                <AddChallengeModal
                    onClose={() => setIsAddingChallenge(false)}
                    onAddChallenge={addChallenge}
                />
            )}
            {/* Featured Winners Section */}
            <h2 className="section-title">Featured Winners</h2>
            <table className="dashboard-table">
                <thead>
                <tr className="table-header">
                    <th>ID</th>
                    <th>User Email</th>
                    <th>Username</th>
                    <th>Total Points</th>
                    <th>Actions</th>
                </tr>
                </thead>
                <tbody>
                {featuredWinners.map((winner) => (
                    <tr key={winner.id}>
                        <td>{winner.id}</td>
                        <td>{winner.userEmail}</td>
                        <td>{winner.username}</td>
                        <td>{winner.totalPoints}</td>
                        <td>
                            <button onClick={() => handleEditWinner(winner)} className="btn btn-edit">
                                Edit
                            </button>
                            <button onClick={() => deleteWinner(winner.id)} className="btn btn-delete">
                                Delete
                            </button>
                        </td>
                    </tr>
                ))}
                {featuredWinners.length === 0 && (
                    <tr>
                        <td className="text-center" colSpan="5">
                            No featured winners found.
                        </td>
                    </tr>
                )}
                </tbody>
            </table>

            {/* Global Leaderboard Section */}
            <h2 className="section-title">Global Leaderboard</h2>
            <table className="dashboard-table">
                <thead>
                <tr className="table-header">
                    <th>ID</th>
                    <th>User Email</th>
                    <th>Username</th>
                    <th>Total Points</th>
                    <th>Actions</th>
                </tr>
                </thead>
                <tbody>
                {leaderboard.map((entry) => (
                    <tr key={entry.id}>
                        <td>{entry.id}</td>
                        <td>{entry.userEmail}</td>
                        <td>{entry.username}</td>
                        <td>{entry.totalPoints}</td>
                        <td>
                            <button onClick={() => handleEditLeaderboard(entry)} className="btn btn-edit">
                                Edit
                            </button>
                            <button onClick={() => deleteLeaderboardEntry(entry.id)} className="btn btn-delete">
                                Delete
                            </button>
                        </td>
                    </tr>
                ))}
                {leaderboard.length === 0 && (
                    <tr>
                        <td className="text-center" colSpan="5">
                            No leaderboard entries found.
                        </td>
                    </tr>
                )}
                </tbody>
            </table>

            {/* Recipe Submissions Section */}
            <h2 className="section-title">Recipe Submissions</h2>
            <table className="dashboard-table">
                <thead>
                <tr className="table-header">
                    <th>ID</th>
                    <th>Challenge</th>
                    <th>Recipe</th>
                    <th>User</th>
                    <th>Submission Date</th>
                    <th>Actions</th>
                </tr>
                </thead>
                <tbody>
                {recipeSubmissions.map((submission) => (
                    <tr key={submission.id}>
                        <td>{submission.id}</td>
                        <td>{submission.challenge?.title || "N/A"}</td>
                        <td>{submission.recipe?.name || "N/A"}</td>
                        <td>{submission.user?.username || "N/A"}</td>
                        <td>{submission.submissionDate}</td>
                        <td>
                            <button onClick={() => handleEditSubmission(submission)} className="btn btn-edit">
                                Edit
                            </button>
                            <button onClick={() => deleteSubmission(submission.id)} className="btn btn-delete">
                                Delete
                            </button>
                        </td>
                    </tr>
                ))}
                </tbody>
            </table>

            {/* Edit Recipe Submission Modal */}
            {editingSubmission && (
                <div className="modal-overlay">
                    <div className="modal-content">
                        <h2 className="modal-header">Edit Recipe Submission</h2>
                        <div className="modal-grid">
                            <input
                                type="date"
                                name="submissionDate"
                                value={editingSubmission.submissionDate}
                                onChange={handleSubmissionChange}
                                className="modal-input"
                            />
                        </div>
                        <button onClick={updateSubmission} className="btn btn-save modal-button">
                            Save
                        </button>
                    </div>
                </div>
            )}

            {/* Edit Featured Winner Modal */}
            {editingWinner && (
                <div className="modal-overlay">
                    <div className="modal-content">
                        <h2 className="modal-header">Edit Featured Winner</h2>
                        <div className="modal-grid">
                            <input
                                type="text"
                                name="userEmail"
                                value={editingWinner.userEmail}
                                onChange={handleWinnerInputChange}
                                placeholder="User Email"
                                className="modal-input"
                            />
                            <input
                                type="text"
                                name="username"
                                value={editingWinner.username}
                                onChange={handleWinnerInputChange}
                                placeholder="Username"
                                className="modal-input"
                            />
                            <input
                                type="number"
                                name="totalPoints"
                                value={editingWinner.totalPoints}
                                onChange={handleWinnerInputChange}
                                placeholder="Total Points"
                                className="modal-input"
                            />
                        </div>
                        <div className="modal-buttons">
                            <button onClick={() => setEditingWinner(null)} className="btn btn-cancel">
                                Cancel
                            </button>
                            <button onClick={updateWinner} className="btn btn-save">
                                Save
                            </button>
                        </div>
                    </div>
                </div>
            )}

            {/* Edit Leaderboard Entry Modal */}
            {editingLeaderboard && (
                <div className="modal-overlay">
                    <div className="modal-content">
                        <h2 className="modal-header">Edit Leaderboard Entry</h2>
                        <div className="modal-grid">
                            <input
                                type="text"
                                name="userEmail"
                                value={editingLeaderboard.userEmail}
                                onChange={handleLeaderboardInputChange}
                                placeholder="User Email"
                                className="modal-input"
                            />
                            <input
                                type="text"
                                name="username"
                                value={editingLeaderboard.username}
                                onChange={handleLeaderboardInputChange}
                                placeholder="Username"
                                className="modal-input"
                            />
                            <input
                                type="number"
                                name="totalPoints"
                                value={editingLeaderboard.totalPoints}
                                onChange={handleLeaderboardInputChange}
                                placeholder="Total Points"
                                className="modal-input"
                            />
                        </div>
                        <div className="modal-buttons">
                            <button onClick={() => setEditingLeaderboard(null)} className="btn btn-cancel">
                                Cancel
                            </button>
                            <button onClick={updateLeaderboardEntry} className="btn btn-save">
                                Save
                            </button>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
};

export default ChallengesDashboard;


/**

 import React, { useEffect, useState } from "react";
 import axios from "axios";
 import "./ChallengesDashboard.css";

 const ChallengesDashboard = () => {
    const [recipeSubmissions, setRecipeSubmissions] = useState([]);
    const [challenges, setChallenges] = useState([]);
    const [editingChallenge, setEditingChallenge] = useState(null);
    const [featuredWinners, setFeaturedWinners] = useState([]);
    const [editingWinner, setEditingWinner] = useState(null);
    const [leaderboard, setLeaderboard] = useState([]);
    const [editingLeaderboard, setEditingLeaderboard] = useState(null);
    const [editingSubmission, setEditingSubmission] = useState(null);
    const token = localStorage.getItem("token");
// NEW: State for adding a new challenge
    const [isAddingChallenge, setIsAddingChallenge] = useState(false);
    // -------------------- Challenges --------------------
    const fetchChallenges = async () => {
        try {
            const response = await axios.get(`${API_URL}/api/challenges`, {
                headers: { Authorization: `Bearer ${token}` },
            });
            setChallenges(response.data);
        } catch (error) {
            console.error("Error fetching challenges:", error);
        }
    };
    // NEW: Add a challenge from a front-end form
    const addChallenge = async (challengeData) => {
        try {
            const challengePayload = {
                ...challengeData,
                featured: challengeData.featured,  // No need for conversion, already a boolean
                active: false, // Explicitly set active to false
            };

            console.log("Sending challenge:", challengePayload); // Debugging log

            await axios.post(`${API_URL}/api/challenges`, challengePayload, {
                headers: { Authorization: `Bearer ${token}` },
            });

            setIsAddingChallenge(false);
            fetchChallenges();
        } catch (error) {
            console.error("Error creating challenge:", error);
        }
    };



const AddChallengeModal = ({ onClose, onAddChallenge }) => {
    const [formData, setFormData] = useState({
        title: "",
        description: "",
        imageUrl: "",
        deadline: "",
        points: 0,
        featured: false, // false => casual, true => monthly
        maxSubmissions: 1,
        active: false,
    });

    const handleChange = (e) => {
        const { name, value, type } = e.target;
        // If user chooses "challengeOfTheMonth", featured = true; else false
        if (name === "featured") {
            const isFeatured = value === "challengeOfTheMonth";
            setFormData({ ...formData, featured: isFeatured });
        } else if (type === "number") {
            setFormData({ ...formData, [name]: parseInt(value, 10) });
        } else {
            setFormData({ ...formData, [name]: value });
        }
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        // Validate form data if needed
        onAddChallenge(formData);
    };

    return (
        <div className="modal-overlay">
            <div className="modal-content">
                <h2 className="modal-header">Add New Challenge</h2>
                <form onSubmit={handleSubmit} className="modal-grid">
                    <input
                        type="text"
                        name="title"
                        value={formData.title}
                        onChange={handleChange}
                        placeholder="Title"
                        className="modal-input"
                        required
                    />
                    <textarea
                        name="description"
                        value={formData.description}
                        onChange={handleChange}
                        placeholder="Description"
                        className="modal-input"
                        required
                    />
                    <label className="modal-label">Deadline:</label>
                    <input
                        type="date"
                        name="deadline"
                        value={formData.deadline}
                        onChange={handleChange}
                        className="modal-input"
                        required
                    />
                    <label className="modal-label">Points:</label>
                    <input
                        type="number"
                        name="points"
                        value={formData.points}
                        onChange={handleChange}
                        placeholder="Points"
                        className="modal-input"
                    />
                    <label className="modal-label">Challenge Type:</label>
                    <select
                        name="featured"
                        value={formData.featured ? "challengeOfTheMonth" : "casual"}
                        onChange={handleChange}
                        className="modal-select"
                    >
                        <option value="casual">Casual Challenge</option>
                        <option value="challengeOfTheMonth">Challenge of the Month</option>
                    </select>
                    <label className="modal-label">Max Submissions:</label>
                    <input
                        type="number"
                        name="maxSubmissions"
                        value={formData.maxSubmissions}
                        onChange={handleChange}
                        placeholder="Max Submissions"
                        className="modal-input"
                    />
                    <label className="modal-label">Image URL:</label>
                    <input
                        type="text"
                        name="imageUrl"
                        value={formData.imageUrl}
                        onChange={handleChange}
                        placeholder="Image URL"
                        className="modal-input"
                    />
                    {formData.imageUrl && (
                        <img src={formData.imageUrl} alt="Preview" className="modal-image-preview" />
                    )}

                    <div className="modal-buttons">
                        <button type="button" onClick={onClose} className="btn btn-cancel">
                            Cancel
                        </button>
                        <button type="submit" className="btn btn-save">
                            Create
                        </button>
                    </div>
                </form>
            </div>
        </div>
    );
};

const handleEditChallenge = (challenge) => {
    setEditingChallenge({ ...challenge });
};

const handleChallengeInputChange = (e) => {
    const { name, value, type, checked } = e.target;
    let newValue = type === "checkbox" ? checked : value;
    if (name === "featured") {
        newValue = value === "challengeOfTheMonth";
    }
    setEditingChallenge({ ...editingChallenge, [name]: newValue });
};

const updateChallenge = async () => {
    try {
        await axios.put(
            `${API_URL}/api/challenges/${editingChallenge.id}`,
            editingChallenge,
            { headers: { Authorization: `Bearer ${token}` } }
        );
        setEditingChallenge(null);
        fetchChallenges();
    } catch (error) {
        console.error("Error updating challenge:", error);
    }
};

const deleteChallenge = async (id) => {
    if (!window.confirm("Are you sure you want to delete this challenge?")) return;
    try {
        await axios.delete(`${API_URL}/api/challenges/${id}`, {
            headers: { Authorization: `Bearer ${token}` },
        });
        fetchChallenges();
    } catch (error) {
        console.error("Error deleting challenge:", error);
    }
};

// -------------------- Featured Winners --------------------
const fetchFeaturedWinners = async () => {
    try {
        const response = await axios.get(`${API_URL}/api/challenges/featured-winners`, {
            headers: { Authorization: `Bearer ${token}` },
        });
        setFeaturedWinners(response.data);
    } catch (error) {
        console.error("Error fetching featured winners:", error);
    }
};

const handleEditWinner = (winner) => {
    setEditingWinner({ ...winner });
};

const handleWinnerInputChange = (e) => {
    const { name, value, type } = e.target;
    const newValue = type === "number" ? parseInt(value, 10) : value;
    setEditingWinner({ ...editingWinner, [name]: newValue });
};

const updateWinner = async () => {
    try {
        await axios.put(
            `${API_URL}/api/challenges/featured-winners/${editingWinner.id}`,
            editingWinner,
            { headers: { Authorization: `Bearer ${token}` } }
        );
        setEditingWinner(null);
        fetchFeaturedWinners();
    } catch (error) {
        console.error("Error updating featured winner:", error);
    }
};

const deleteWinner = async (id) => {
    if (!window.confirm("Are you sure you want to delete this winner?")) return;
    try {
        await axios.delete(`${API_URL}/api/challenges/featured-winners/${id}`, {
            headers: { Authorization: `Bearer ${token}` },
        });
        fetchFeaturedWinners();
    } catch (error) {
        console.error("Error deleting featured winner:", error);
    }
};

const addWinner = async (newWinner) => {
    try {
        await axios.post(`${API_URL}/api/challenges/featured-winners`, newWinner, {
            headers: { Authorization: `Bearer ${token}` },
        });
        fetchFeaturedWinners();
    } catch (error) {
        console.error("Error adding featured winner:", error);
    }
};

// -------------------- Global Leaderboard --------------------
const fetchLeaderboard = async () => {
    try {
        const response = await axios.get(`${API_URL}/api/challenges/global-leaderboard`, {
            headers: { Authorization: `Bearer ${token}` },
        });
        setLeaderboard(response.data);
    } catch (error) {
        console.error("Error fetching global leaderboard:", error);
    }
};

const handleEditLeaderboard = (entry) => {
    setEditingLeaderboard({ ...entry });
};

const handleLeaderboardInputChange = (e) => {
    const { name, value, type } = e.target;
    const newValue = type === "number" ? parseInt(value, 10) : value;
    setEditingLeaderboard({ ...editingLeaderboard, [name]: newValue });
};

const updateLeaderboardEntry = async () => {
    try {
        await axios.put(
            `${API_URL}/api/challenges/global-leaderboard/${editingLeaderboard.id}`,
            editingLeaderboard,
            { headers: { Authorization: `Bearer ${token}` } }
        );
        setEditingLeaderboard(null);
        fetchLeaderboard();
    } catch (error) {
        console.error("Error updating leaderboard entry:", error);
    }
};

const deleteLeaderboardEntry = async (id) => {
    if (!window.confirm("Are you sure you want to delete this leaderboard entry?")) return;
    try {
        await axios.delete(`${API_URL}/api/challenges/global-leaderboard/${id}`, {
            headers: { Authorization: `Bearer ${token}` },
        });
        fetchLeaderboard();
    } catch (error) {
        console.error("Error deleting leaderboard entry:", error);
    }
};

const addLeaderboardEntry = async (newEntry) => {
    try {
        await axios.post(`${API_URL}/api/challenges/global-leaderboard`, newEntry, {
            headers: { Authorization: `Bearer ${token}` },
        });
        fetchLeaderboard();
    } catch (error) {
        console.error("Error adding leaderboard entry:", error);
    }
};

// -------------------- Recipe Submissions --------------------
const fetchRecipeSubmissions = async () => {
    try {
        const response = await axios.get(`${API_URL}/api/challenges/recipe-submissions`, {
            headers: { Authorization: `Bearer ${token}` },
        });
        setRecipeSubmissions(response.data);
    } catch (error) {
        console.error("Error fetching recipe submissions:", error);
    }
};

const handleEditSubmission = (submission) => {
    setEditingSubmission({ ...submission });
};

const handleSubmissionChange = (e) => {
    const { name, value } = e.target;
    setEditingSubmission({ ...editingSubmission, [name]: value });
};

const updateSubmission = async () => {
    try {
        await axios.put(
            `${API_URL}/api/challenges/recipe-submissions/${editingSubmission.id}`,
            editingSubmission,
            { headers: { Authorization: `Bearer ${token}` } }
        );
        setEditingSubmission(null);
        fetchRecipeSubmissions();
    } catch (error) {
        console.error("Error updating recipe submission:", error);
    }
};

const deleteSubmission = async (id) => {
    if (!window.confirm("Are you sure you want to delete this submission?")) return;
    try {
        await axios.delete(`${API_URL}/api/challenges/recipe-submissions/${id}`, {
            headers: { Authorization: `Bearer ${token}` },
        });
        fetchRecipeSubmissions();
    } catch (error) {
        console.error("Error deleting recipe submission:", error);
    }
};

// Initial fetch
useEffect(() => {
    if (token) {
        fetchChallenges();
        fetchFeaturedWinners();
        fetchLeaderboard();
        fetchRecipeSubmissions();
    }
}, [token]);

return (
    <div className="challenges-dashboard-container">
        <h1 className="dashboard-title">Challenges Dashboard</h1>

        <div className="add-challenge-container">
            <button className="btn btn-add-challenge" onClick={() => setIsAddingChallenge(true)}>
                Add New Challenge
            </button>
        </div>

        <table className="dashboard-table">
            <thead>
            <tr>
                <th>ID</th>
                <th>Title</th>
                <th>Description</th>
                <th>Image</th>
                <th>Deadline</th>
                <th>Points</th>
                <th>Type</th>
                <th>Max Submissions</th>
                <th>Actions</th>
            </tr>
            </thead>
            <tbody>
            {challenges.map((challenge) => (
                <tr key={challenge.id}>
                    <td>{challenge.id}</td>
                    <td>{challenge.title}</td>
                    <td>{challenge.description}</td>
                    <td>
                        {challenge.imageUrl ? (
                            <img src={challenge.imageUrl} alt={challenge.title} className="challenge-image" />
                        ) : (
                            "No Image"
                        )}
                    </td>
                    <td>{challenge.deadline}</td>
                    <td>{challenge.points}</td>
                    <td>{challenge.featured ? "Challenge of the Month" : "Casual Challenge"}</td>
                    <td>{challenge.maxSubmissions}</td>
                    <td>
                        <button onClick={() => handleEditChallenge(challenge)} className="btn btn-edit">
                            Edit
                        </button>
                        <button onClick={() => deleteChallenge(challenge.id)} className="btn btn-delete">
                            Delete
                        </button>
                    </td>
                </tr>
            ))}
            {challenges.length === 0 && (
                <tr>
                    <td className="text-center" colSpan="10">No challenges found.</td>
                </tr>
            )}
            </tbody>
        </table>


        {editingChallenge && (
            <div className="modal-overlay">
                <div className="modal-content">
                    <h2 className="modal-header">Edit Challenge</h2>
                    <div className="modal-grid">
                        <input
                            type="text"
                            name="title"
                            value={editingChallenge.title}
                            onChange={handleChallengeInputChange}
                            placeholder="Title"
                            className="modal-input"
                        />
                        <textarea
                            name="description"
                            value={editingChallenge.description}
                            onChange={handleChallengeInputChange}
                            placeholder="Description"
                            className="modal-input"
                        />
                        <input
                            type="date"
                            name="deadline"
                            value={editingChallenge.deadline}
                            onChange={handleChallengeInputChange}
                            className="modal-input"
                        />
                        <input
                            type="number"
                            name="points"
                            value={editingChallenge.points}
                            onChange={handleChallengeInputChange}
                            placeholder="Points"
                            className="modal-input"
                        />
                        <div className="modal-select-group">
                            <label>Challenge Type:</label>
                            <select
                                name="featured"
                                value={editingChallenge.featured ? "challengeOfTheMonth" : "casual"}
                                onChange={handleChallengeInputChange}
                                className="modal-select"
                            >
                                <option value="casual">Casual Challenge</option>
                                <option value="challengeOfTheMonth">Challenge of the Month</option>
                            </select>
                        </div>
                        <input
                            type="number"
                            name="maxSubmissions"
                            value={editingChallenge.maxSubmissions}
                            onChange={handleChallengeInputChange}
                            placeholder="Max Submissions"
                            className="modal-input"
                        />
                        <div className="modal-image-group">
                            <input
                                type="text"
                                name="imageUrl"
                                value={editingChallenge.imageUrl || ""}
                                onChange={handleChallengeInputChange}
                                placeholder="Image URL"
                                className="modal-input"
                            />
                            {editingChallenge.imageUrl && (
                                <img src={editingChallenge.imageUrl} alt="Preview" className="modal-image-preview" />
                            )}
                        </div>
                    </div>
                    <div className="modal-buttons">
                        <button onClick={() => setEditingChallenge(null)} className="btn btn-cancel">
                            Cancel
                        </button>
                        <button onClick={updateChallenge} className="btn btn-save">
                            Save
                        </button>
                    </div>
                </div>
            </div>
        )}

        {isAddingChallenge && (
            <AddChallengeModal
                onClose={() => setIsAddingChallenge(false)}
                onAddChallenge={addChallenge}
            />
        )}

        <h2 className="section-title">Featured Winners</h2>
        <table className="dashboard-table">
            <thead>
            <tr className="table-header">
                <th>ID</th>
                <th>User Email</th>
                <th>Username</th>
                <th>Total Points</th>
                <th>Actions</th>
            </tr>
            </thead>
            <tbody>
            {featuredWinners.map((winner) => (
                <tr key={winner.id}>
                    <td>{winner.id}</td>
                    <td>{winner.userEmail}</td>
                    <td>{winner.username}</td>
                    <td>{winner.totalPoints}</td>
                    <td>
                        <button onClick={() => handleEditWinner(winner)} className="btn btn-edit">
                            Edit
                        </button>
                        <button onClick={() => deleteWinner(winner.id)} className="btn btn-delete">
                            Delete
                        </button>
                    </td>
                </tr>
            ))}
            {featuredWinners.length === 0 && (
                <tr>
                    <td className="text-center" colSpan="5">
                        No featured winners found.
                    </td>
                </tr>
            )}
            </tbody>
        </table>


        <h2 className="section-title">Global Leaderboard</h2>
        <table className="dashboard-table">
            <thead>
            <tr className="table-header">
                <th>ID</th>
                <th>User Email</th>
                <th>Username</th>
                <th>Total Points</th>
                <th>Actions</th>
            </tr>
            </thead>
            <tbody>
            {leaderboard.map((entry) => (
                <tr key={entry.id}>
                    <td>{entry.id}</td>
                    <td>{entry.userEmail}</td>
                    <td>{entry.username}</td>
                    <td>{entry.totalPoints}</td>
                    <td>
                        <button onClick={() => handleEditLeaderboard(entry)} className="btn btn-edit">
                            Edit
                        </button>
                        <button onClick={() => deleteLeaderboardEntry(entry.id)} className="btn btn-delete">
                            Delete
                        </button>
                    </td>
                </tr>
            ))}
            {leaderboard.length === 0 && (
                <tr>
                    <td className="text-center" colSpan="5">
                        No leaderboard entries found.
                    </td>
                </tr>
            )}
            </tbody>
        </table>


        <h2 className="section-title">Recipe Submissions</h2>
        <table className="dashboard-table">
            <thead>
            <tr className="table-header">
                <th>ID</th>
                <th>Challenge</th>
                <th>Recipe</th>
                <th>User</th>
                <th>Submission Date</th>
                <th>Actions</th>
            </tr>
            </thead>
            <tbody>
            {recipeSubmissions.map((submission) => (
                <tr key={submission.id}>
                    <td>{submission.id}</td>
                    <td>{submission.challenge?.title || "N/A"}</td>
                    <td>{submission.recipe?.name || "N/A"}</td>
                    <td>{submission.user?.username || "N/A"}</td>
                    <td>{submission.submissionDate}</td>
                    <td>
                        <button onClick={() => handleEditSubmission(submission)} className="btn btn-edit">
                            Edit
                        </button>
                        <button onClick={() => deleteSubmission(submission.id)} className="btn btn-delete">
                            Delete
                        </button>
                    </td>
                </tr>
            ))}
            </tbody>
        </table>


        {editingSubmission && (
            <div className="modal-overlay">
                <div className="modal-content">
                    <h2 className="modal-header">Edit Recipe Submission</h2>
                    <div className="modal-grid">
                        <input
                            type="date"
                            name="submissionDate"
                            value={editingSubmission.submissionDate}
                            onChange={handleSubmissionChange}
                            className="modal-input"
                        />
                    </div>
                    <button onClick={updateSubmission} className="btn btn-save modal-button">
                        Save
                    </button>
                </div>
            </div>
        )}


        {editingWinner && (
            <div className="modal-overlay">
                <div className="modal-content">
                    <h2 className="modal-header">Edit Featured Winner</h2>
                    <div className="modal-grid">
                        <input
                            type="text"
                            name="userEmail"
                            value={editingWinner.userEmail}
                            onChange={handleWinnerInputChange}
                            placeholder="User Email"
                            className="modal-input"
                        />
                        <input
                            type="text"
                            name="username"
                            value={editingWinner.username}
                            onChange={handleWinnerInputChange}
                            placeholder="Username"
                            className="modal-input"
                        />
                        <input
                            type="number"
                            name="totalPoints"
                            value={editingWinner.totalPoints}
                            onChange={handleWinnerInputChange}
                            placeholder="Total Points"
                            className="modal-input"
                        />
                    </div>
                    <div className="modal-buttons">
                        <button onClick={() => setEditingWinner(null)} className="btn btn-cancel">
                            Cancel
                        </button>
                        <button onClick={updateWinner} className="btn btn-save">
                            Save
                        </button>
                    </div>
                </div>
            </div>
        )}


        {editingLeaderboard && (
            <div className="modal-overlay">
                <div className="modal-content">
                    <h2 className="modal-header">Edit Leaderboard Entry</h2>
                    <div className="modal-grid">
                        <input
                            type="text"
                            name="userEmail"
                            value={editingLeaderboard.userEmail}
                            onChange={handleLeaderboardInputChange}
                            placeholder="User Email"
                            className="modal-input"
                        />
                        <input
                            type="text"
                            name="username"
                            value={editingLeaderboard.username}
                            onChange={handleLeaderboardInputChange}
                            placeholder="Username"
                            className="modal-input"
                        />
                        <input
                            type="number"
                            name="totalPoints"
                            value={editingLeaderboard.totalPoints}
                            onChange={handleLeaderboardInputChange}
                            placeholder="Total Points"
                            className="modal-input"
                        />
                    </div>
                    <div className="modal-buttons">
                        <button onClick={() => setEditingLeaderboard(null)} className="btn btn-cancel">
                            Cancel
                        </button>
                        <button onClick={updateLeaderboardEntry} className="btn btn-save">
                            Save
                        </button>
                    </div>
                </div>
            </div>
        )}
    </div>
);
};

export default ChallengesDashboard;



 **/