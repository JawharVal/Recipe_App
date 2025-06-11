import React, { useEffect, useState } from "react";
import axios from "axios";
import NavBar from "./NavBar"; // Importing the navbar
import "./AdminFeedback.css"; // Importing styles

const AdminFeedback = () => {
    const token = localStorage.getItem("token");
    const [feedbacks, setFeedbacks] = useState([]);
    const [selectedFeedback, setSelectedFeedback] = useState(null);
    const [conversation, setConversation] = useState([]);
    const [newMessage, setNewMessage] = useState("");

    // Fetch all feedbacks
    const fetchFeedbacks = async () => {
        try {
            const response = await axios.get("http://localhost:8081/api/feedback", {
                headers: { Authorization: `Bearer ${token}` },
            });
            setFeedbacks(response.data);
        } catch (err) {
            console.error("Error fetching feedback:", err);
        }
    };

    useEffect(() => {
        if (token) {
            fetchFeedbacks();
        }
    }, [token]);

    // Fetch conversation for a specific feedback
    const handleViewConversation = async (fb) => {
        setSelectedFeedback(fb);
        try {
            const res = await axios.get(
                `http://localhost:8081/api/feedback/${fb.id}/conversation`,
                { headers: { Authorization: `Bearer ${token}` } }
            );
            setConversation(res.data);
        } catch (err) {
            console.error("Error fetching conversation:", err);
        }
    };

    // Send a reply to the feedback and deliver it to the user via email
    const handleSendMessage = async () => {
        if (!selectedFeedback || newMessage.trim() === "") return;
        try {
            // Post the conversation message
            await axios.post(
                `http://localhost:8081/api/feedback/${selectedFeedback.id}/conversation`,
                { sender: "ADMIN", content: newMessage },
                { headers: { Authorization: `Bearer ${token}` } }
            );

            // Send email reply to the user (this endpoint uses JavaMailSender)
            await axios.put(
                `http://localhost:8081/api/feedback/${selectedFeedback.id}/response`,
                { reply: newMessage },
                { headers: { Authorization: `Bearer ${token}` } }
            );

            // Refresh conversation and clear message input
            await handleViewConversation(selectedFeedback);
            setNewMessage("");
        } catch (err) {
            console.error("Error sending message:", err);
        }
    };

    return (
        <div className="feedback-container">
            <NavBar /> {/* Navbar for consistency */}

            <div className="feedback-content">
                <h2 className="feedback-title">User Feedback</h2>

                {feedbacks.length === 0 ? (
                    <p className="no-feedback">No feedback found.</p>
                ) : (
                    <table className="feedback-table">
                        <thead>
                        <tr>
                            <th>ID</th>
                            <th>User ID</th>
                            <th>Comment</th>
                            <th>Created At</th>
                            <th>Actions</th>
                        </tr>
                        </thead>
                        <tbody>
                        {feedbacks.map((fb) => (
                            <tr key={fb.id}>
                                <td>{fb.id}</td>
                                <td>{fb.userId}</td>
                                <td>{fb.comment}</td>
                                <td>{new Date(fb.createdAt).toLocaleString()}</td>
                                <td>
                                    <button
                                        onClick={() => handleViewConversation(fb)}
                                        className="btn btn-view"
                                    >
                                        View replies
                                    </button>
                                </td>
                            </tr>
                        ))}
                        </tbody>
                    </table>
                )}

                {/* Conversation Section */}
                {selectedFeedback && (
                    <div className="conversation-container">
                        <h3 className="conversation-title">
                            Replies for Feedback #{selectedFeedback.id}
                        </h3>

                        {conversation.length === 0 ? (
                            <p className="no-messages">No messages yet.</p>
                        ) : (
                            <div className="message-box">
                                {conversation.map((msg) => (
                                    <div
                                        key={msg.id}
                                        className={`message ${msg.sender === "ADMIN" ? "admin-message" : "user-message"}`}
                                    >
                                        <strong>{msg.sender}:</strong> {msg.content}
                                        <span className="message-time">
                                            {new Date(msg.createdAt).toLocaleString()}
                                        </span>
                                    </div>
                                ))}
                            </div>
                        )}

                        {/* Message Input */}
                        <div className="message-input">
                            <input
                                type="text"
                                className="input-field"
                                placeholder="Type a message..."
                                value={newMessage}
                                onChange={(e) => setNewMessage(e.target.value)}
                            />
                            <button className="btn btn-send" onClick={handleSendMessage}>
                                Send
                            </button>
                        </div>
                    </div>
                )}
            </div>
        </div>
    );
};

export default AdminFeedback;


/**

 import React, { useEffect, useState } from "react";
 import axios from "axios";
 import NavBar from "./NavBar"; // Importing the navbar
 import "./AdminFeedback.css"; // Importing styles
 import API_URL from "./config";
 const AdminFeedback = () => {
    const token = localStorage.getItem("token");
    const [feedbacks, setFeedbacks] = useState([]);
    const [selectedFeedback, setSelectedFeedback] = useState(null);
    const [conversation, setConversation] = useState([]);
    const [newMessage, setNewMessage] = useState("");

    // Fetch all feedbacks
    const fetchFeedbacks = async () => {
        try {
            const response = await axios.get(`${API_URL}/api/feedback`, {
                headers: { Authorization: `Bearer ${token}` },
            });
            setFeedbacks(response.data);
        } catch (err) {
            console.error("Error fetching feedback:", err);
        }
    };

    useEffect(() => {
        if (token) {
            fetchFeedbacks();
        }
    }, [token]);

    // Fetch conversation for a specific feedback
    const handleViewConversation = async (fb) => {
        setSelectedFeedback(fb);
        try {
            const res = await axios.get(
                `${API_URL}/api/feedback/${fb.id}/conversation`,
                { headers: { Authorization: `Bearer ${token}` } }
            );
            setConversation(res.data);
        } catch (err) {
            console.error("Error fetching conversation:", err);
        }
    };

    // Send a reply to the feedback and deliver it to the user via email
    const handleSendMessage = async () => {
        if (!selectedFeedback || newMessage.trim() === "") return;
        try {
            // Post the conversation message
            await axios.post(
                `${API_URL}/api/feedback/${selectedFeedback.id}/conversation`,
                { sender: "ADMIN", content: newMessage },
                { headers: { Authorization: `Bearer ${token}` } }
            );

            // Send email reply to the user (this endpoint uses JavaMailSender)
            await axios.put(
                `${API_URL}/api/feedback/${selectedFeedback.id}/response`,
                { reply: newMessage },
                { headers: { Authorization: `Bearer ${token}` } }
            );

            // Refresh conversation and clear message input
            await handleViewConversation(selectedFeedback);
            setNewMessage("");
        } catch (err) {
            console.error("Error sending message:", err);
        }
    };

    return (
        <div className="feedback-container">
            <NavBar />

<div className="feedback-content">
    <h2 className="feedback-title">User Feedback</h2>

    {feedbacks.length === 0 ? (
        <p className="no-feedback">No feedback found.</p>
    ) : (
        <table className="feedback-table">
            <thead>
            <tr>
                <th>ID</th>
                <th>User ID</th>
                <th>Comment</th>
                <th>Created At</th>
                <th>Actions</th>
            </tr>
            </thead>
            <tbody>
            {feedbacks.map((fb) => (
                <tr key={fb.id}>
                    <td>{fb.id}</td>
                    <td>{fb.userId}</td>
                    <td>{fb.comment}</td>
                    <td>{new Date(fb.createdAt).toLocaleString()}</td>
                    <td>
                        <button
                            onClick={() => handleViewConversation(fb)}
                            className="btn btn-view"
                        >
                            View replies
                        </button>
                    </td>
                </tr>
            ))}
            </tbody>
        </table>
    )}


    {selectedFeedback && (
        <div className="conversation-container">
            <h3 className="conversation-title">
                Replies for Feedback #{selectedFeedback.id}
            </h3>

            {conversation.length === 0 ? (
                <p className="no-messages">No messages yet.</p>
            ) : (
                <div className="message-box">
                    {conversation.map((msg) => (
                        <div
                            key={msg.id}
                            className={`message ${msg.sender === "ADMIN" ? "admin-message" : "user-message"}`}
                        >
                            <strong>{msg.sender}:</strong> {msg.content}
                            <span className="message-time">
                                            {new Date(msg.createdAt).toLocaleString()}
                                        </span>
                        </div>
                    ))}
                </div>
            )}


            <div className="message-input">
                <input
                    type="text"
                    className="input-field"
                    placeholder="Type a message..."
                    value={newMessage}
                    onChange={(e) => setNewMessage(e.target.value)}
                />
                <button className="btn btn-send" onClick={handleSendMessage}>
                    Send
                </button>
            </div>
        </div>
    )}
</div>
</div>
);
};

 export default AdminFeedback;


 **/