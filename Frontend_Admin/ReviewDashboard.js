import React, { useEffect, useState } from "react";
import axios from "axios";
import "./ReviewDashboard.css"; // Import the custom CSS

const ReviewDashboard = ({ recipeId }) => {
    const [reviews, setReviews] = useState([]);
    const token = localStorage.getItem("token");

    // Function to fetch reviews for the given recipe
    const fetchReviews = () => {
        axios
            .get(`http://localhost:8081/api/reviews/recipe/${recipeId}`, {
                headers: { Authorization: `Bearer ${token}` },
            })
            .then((response) => setReviews(response.data))
            .catch((error) =>
                console.error("Error fetching reviews:", error)
            );
    };

    useEffect(() => {
        fetchReviews();
    }, [recipeId, token]);

    // Delete review handler
    const handleDeleteReview = async (reviewId) => {
        if (!window.confirm("Are you sure you want to delete this review?"))
            return;
        try {
            await axios.delete(`http://localhost:8081/api/reviews/${reviewId}`, {
                headers: { Authorization: `Bearer ${token}` },
            });
            fetchReviews();
        } catch (error) {
            console.error("Error deleting review:", error);
        }
    };

    return (
        <div className="review-dashboard-container">
            <h2 className="review-dashboard-header">Reviews for Recipe {recipeId}</h2>
            {reviews.length === 0 ? (
                <p>No reviews available.</p>
            ) : (
                <ul className="review-list">
                    {reviews.map((review) => (
                        <li key={review.id} className="review-item">
                            <div>
                                <strong>Reviewer:</strong> {review.username}
                            </div>
                            <div>
                                <strong>Rating:</strong> <span>{review.rating} / 5</span>
                            </div>
                            <div>
                                <strong>Comment:</strong>
                                <p>{review.comment}</p>
                            </div>
                            <div>
                                <strong>Reviewed on:</strong>{" "}
                                {review.createdAt
                                    ? new Date(review.createdAt).toLocaleString()
                                    : ""}
                            </div>
                            <button
                                onClick={() => handleDeleteReview(review.id)}
                                className="review-delete-btn"
                            >
                                Delete
                            </button>
                        </li>
                    ))}
                </ul>
            )}
        </div>
    );
};

export default ReviewDashboard;


/**

 import React, { useEffect, useState } from "react";
 import axios from "axios";
 import "./ReviewDashboard.css"; // Import the custom CSS
 import API_URL from "./config";
 const ReviewDashboard = ({ recipeId }) => {
    const [reviews, setReviews] = useState([]);
    const token = localStorage.getItem("token");

    // Function to fetch reviews for the given recipe
    const fetchReviews = () => {
        axios
            .get(`${API_URL}/api/reviews/recipe/${recipeId}`, {
                headers: { Authorization: `Bearer ${token}` },
            })
            .then((response) => setReviews(response.data))
            .catch((error) =>
                console.error("Error fetching reviews:", error)
            );
    };

    useEffect(() => {
        fetchReviews();
    }, [recipeId, token]);

    // Delete review handler
    const handleDeleteReview = async (reviewId) => {
        if (!window.confirm("Are you sure you want to delete this review?"))
            return;
        try {
            await axios.delete(`${API_URL}/api/reviews/${reviewId}`, {
                headers: { Authorization: `Bearer ${token}` },
            });
            fetchReviews();
        } catch (error) {
            console.error("Error deleting review:", error);
        }
    };

    return (
        <div className="review-dashboard-container">
            <h2 className="review-dashboard-header">Reviews for Recipe {recipeId}</h2>
            {reviews.length === 0 ? (
                <p>No reviews available.</p>
            ) : (
                <ul className="review-list">
                    {reviews.map((review) => (
                        <li key={review.id} className="review-item">
                            <div>
                                <strong>Reviewer:</strong> {review.username}
                            </div>
                            <div>
                                <strong>Rating:</strong> <span>{review.rating} / 5</span>
                            </div>
                            <div>
                                <strong>Comment:</strong>
                                <p>{review.comment}</p>
                            </div>
                            <div>
                                <strong>Reviewed on:</strong>{" "}
                                {review.createdAt
                                    ? new Date(review.createdAt).toLocaleString()
                                    : ""}
                            </div>
                            <button
                                onClick={() => handleDeleteReview(review.id)}
                                className="review-delete-btn"
                            >
                                Delete
                            </button>
                        </li>
                    ))}
                </ul>
            )}
        </div>
    );
};

 export default ReviewDashboard;



 **/