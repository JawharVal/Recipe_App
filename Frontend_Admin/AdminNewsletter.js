import React, { useEffect, useState } from "react";
import axios from "axios";
import NavBar from "./NavBar"; // Import the NavBar component
import "./AdminNewsletter.css";

const AdminNewsletter = () => {
    const token = localStorage.getItem("token");

    const [subscribers, setSubscribers] = useState([]);
    const [bulkMessage, setBulkMessage] = useState("");
    const [error, setError] = useState("");
    const [isSending, setIsSending] = useState(false);
    const [successMessage, setSuccessMessage] = useState(""); // Store the message from backend

    // Fetch subscribers
    const fetchSubscribers = async () => {
        try {
            const res = await axios.get("http://localhost:8081/api/newsletter/admin/subscribers", {
                headers: { Authorization: `Bearer ${token}` },
            });
            setSubscribers(res.data);
        } catch (err) {
            console.error("Error fetching subscribers:", err);
            setError("Failed to load subscribers.");
        }
    };

    useEffect(() => {
        if (token) {
            fetchSubscribers();
        }
    }, [token]);

    // Send bulk message and fetch response
    const handleSendBulk = async () => {
        setIsSending(true);
        setError("");
        setSuccessMessage(""); // Clear previous messages
        try {
            const response = await axios.post(
                "http://localhost:8081/api/newsletter/admin/bulkSend",
                { message: bulkMessage },
                { headers: { Authorization: `Bearer ${token}` } }
            );
            setSuccessMessage(response.data); // Set success message from backend
            setBulkMessage(""); // Clear the message input
        } catch (err) {
            console.error("Error sending bulk message:", err);
            setError("Failed to send bulk message.");
        } finally {
            setIsSending(false);
        }
    };

    return (
        <div className="newsletter-container">
            {/* Navbar */}
            <NavBar />

            <div className="newsletter-content">
                <h2 className="newsletter-title">Newsletter Subscriptions</h2>

                {/* Error Message */}
                {error && <p className="error-message">{error}</p>}

                {/* Success Message */}
                {successMessage && <p className="success-message">{successMessage}</p>}

                <p className="subscriber-count">Total subscribers: {subscribers.length}</p>

                {/* Subscriber List */}
                <div className="subscriber-list-container">
                    <ul className="subscriber-list">
                        {subscribers.length > 0 ? (
                            subscribers.map((sub) => (
                                <li key={sub.id} className="subscriber-item">
                                    <span className="subscriber-email">{sub.email}</span>
                                    <span className="subscriber-date">
                                        {new Date(sub.subscribedAt).toLocaleString()}
                                    </span>
                                </li>
                            ))
                        ) : (
                            <p className="no-subscribers">No subscribers yet.</p>
                        )}
                    </ul>
                </div>

                {/* Bulk Message Section */}
                <div className="bulk-message-container">
                    <label className="bulk-message-label">Send Bulk Message:</label>
                    <textarea
                        className="bulk-message-textarea"
                        rows={4}
                        value={bulkMessage}
                        onChange={(e) => setBulkMessage(e.target.value)}
                        placeholder="Write your newsletter message here..."
                    />
                </div>

                {/* Buttons */}
                <div className="button-group">
                    <button
                        disabled={isSending}
                        className={`btn-send ${isSending ? "btn-disabled" : ""}`}
                        onClick={handleSendBulk}
                    >
                        {isSending ? "Sending..." : "Send Bulk Message"}
                    </button>
                </div>
            </div>
        </div>
    );
};

export default AdminNewsletter;


/**

 import React, { useEffect, useState } from "react";
 import axios from "axios";
 import NavBar from "./NavBar"; // Import the NavBar component
 import "./AdminNewsletter.css";
 import API_URL from "./config";
 const AdminNewsletter = () => {
    const token = localStorage.getItem("token");

    const [subscribers, setSubscribers] = useState([]);
    const [bulkMessage, setBulkMessage] = useState("");
    const [error, setError] = useState("");
    const [isSending, setIsSending] = useState(false);
    const [successMessage, setSuccessMessage] = useState(""); // Store the message from backend

    // Fetch subscribers
    const fetchSubscribers = async () => {
        try {
            const res = await axios.get(`${API_URL}/api/newsletter/admin/subscribers`, {
                headers: { Authorization: `Bearer ${token}` },
            });
            setSubscribers(res.data);
        } catch (err) {
            console.error("Error fetching subscribers:", err);
            setError("Failed to load subscribers.");
        }
    };

    useEffect(() => {
        if (token) {
            fetchSubscribers();
        }
    }, [token]);

    // Send bulk message and fetch response
    const handleSendBulk = async () => {
        setIsSending(true);
        setError("");
        setSuccessMessage(""); // Clear previous messages
        try {
            const response = await axios.post(
                `${API_URL}/api/newsletter/admin/bulkSend`,
                { message: bulkMessage },
                { headers: { Authorization: `Bearer ${token}` } }
            );
            setSuccessMessage(response.data); // Set success message from backend
            setBulkMessage(""); // Clear the message input
        } catch (err) {
            console.error("Error sending bulk message:", err);
            setError("Failed to send bulk message.");
        } finally {
            setIsSending(false);
        }
    };

    return (
        <div className="newsletter-container">

<NavBar />

<div className="newsletter-content">
    <h2 className="newsletter-title">Newsletter Subscriptions</h2>


    {error && <p className="error-message">{error}</p>}


    {successMessage && <p className="success-message">{successMessage}</p>}

    <p className="subscriber-count">Total subscribers: {subscribers.length}</p>


    <div className="subscriber-list-container">
        <ul className="subscriber-list">
            {subscribers.length > 0 ? (
                subscribers.map((sub) => (
                    <li key={sub.id} className="subscriber-item">
                        <span className="subscriber-email">{sub.email}</span>
                        <span className="subscriber-date">
                                        {new Date(sub.subscribedAt).toLocaleString()}
                                    </span>
                    </li>
                ))
            ) : (
                <p className="no-subscribers">No subscribers yet.</p>
            )}
        </ul>
    </div>


    <div className="bulk-message-container">
        <label className="bulk-message-label">Send Bulk Message:</label>
        <textarea
            className="bulk-message-textarea"
            rows={4}
            value={bulkMessage}
            onChange={(e) => setBulkMessage(e.target.value)}
            placeholder="Write your newsletter message here..."
        />
    </div>


    <div className="button-group">
        <button
            disabled={isSending}
            className={`btn-send ${isSending ? "btn-disabled" : ""}`}
            onClick={handleSendBulk}
        >
            {isSending ? "Sending..." : "Send Bulk Message"}
        </button>
    </div>
</div>
</div>
);
};

 export default AdminNewsletter;



 **/