import React, { useEffect, useState } from "react";
import axios from "axios";
import "./ReportDashboard.css";

const ReportDashboard = () => {
    const [recipeReports, setRecipeReports] = useState([]);
    const [reviewReports, setReviewReports] = useState([]);
    const [recipeSortOrder, setRecipeSortOrder] = useState("newest");
    const [reviewSortOrder, setReviewSortOrder] = useState("newest");
    const token = localStorage.getItem("token");

    // Fetch recipe reports
    const fetchRecipeReports = () => {
        axios
            .get("http://localhost:8081/api/recipeReports/", {
                headers: { Authorization: `Bearer ${token}` },
            })
            .then((response) => setRecipeReports(response.data))
            .catch((error) =>
                console.error("Error fetching recipe reports:", error)
            );
    };

    // Fetch review reports
    const fetchReviewReports = () => {
        axios
            .get("http://localhost:8081/api/reviewReports/", {
                headers: { Authorization: `Bearer ${token}` },
            })
            .then((response) => setReviewReports(response.data))
            .catch((error) =>
                console.error("Error fetching review reports:", error)
            );
    };

    // Mark all reports as read when the ReportDashboard is mounted
    useEffect(() => {
        axios
            .patch("http://localhost:8081/api/admin/reports/mark-read", null, {
                headers: { Authorization: `Bearer ${token}` },
            })
            .then(() => {
                // Optionally, you could refetch reports if needed
                fetchRecipeReports();
                fetchReviewReports();
            })
            .catch((error) =>
                console.error("Error marking reports as read:", error)
            );
    }, [token]);

    useEffect(() => {
        fetchRecipeReports();
        fetchReviewReports();
    }, [token]);

    // Sorting helpers
    const sortedRecipeReports = [...recipeReports].sort((a, b) => {
        if (!a.reportedAt || !b.reportedAt) return 0;
        const dateA = new Date(a.reportedAt);
        const dateB = new Date(b.reportedAt);
        return recipeSortOrder === "newest" ? dateB - dateA : dateA - dateB;
    });

    const sortedReviewReports = [...reviewReports].sort((a, b) => {
        if (!a.reportedAt || !b.reportedAt) return 0;
        const dateA = new Date(a.reportedAt);
        const dateB = new Date(b.reportedAt);
        return reviewSortOrder === "newest" ? dateB - dateA : dateA - dateB;
    });

    // Delete handlers
    const handleDeleteRecipeReport = async (reportId) => {
        if (!window.confirm("Are you sure you want to delete this recipe report?"))
            return;
        try {
            await axios.delete(
                `http://localhost:8081/api/recipeReports/${reportId}`,
                { headers: { Authorization: `Bearer ${token}` } }
            );
            fetchRecipeReports();
        } catch (error) {
            console.error("Error deleting recipe report:", error);
        }
    };

    const handleDeleteReviewReport = async (reportId) => {
        if (!window.confirm("Are you sure you want to delete this review report?"))
            return;
        try {
            await axios.delete(
                `http://localhost:8081/api/reviewReports/${reportId}`,
                { headers: { Authorization: `Bearer ${token}` } }
            );
            fetchReviewReports();
        } catch (error) {
            console.error("Error deleting review report:", error);
        }
    };

    // Scroll function to review reports section
    const scrollToReviewReports = () => {
        const reviewSection = document.getElementById("review-reports");
        if (reviewSection) {
            reviewSection.scrollIntoView({ behavior: "smooth" });
        }
    };

    return (
        <div className="report-dashboard-container">
            <h1 className="dashboard-title">Reports Dashboard</h1>

            {/* Button to scroll to Review Reports */}
            <div className="scroll-button-container">
                <button onClick={scrollToReviewReports} className="btn btn-scroll">
                    Go to Review Reports
                </button>
            </div>

            {/* Recipe Reports Section */}
            <div className="section-header">
                <h2 className="section-title">Recipe Reports</h2>
                <select
                    value={recipeSortOrder}
                    onChange={(e) => setRecipeSortOrder(e.target.value)}
                    className="filter-select"
                >
                    <option value="newest">Newest First</option>
                    <option value="oldest">Oldest First</option>
                </select>
            </div>
            {recipeReports.length === 0 ? (
                <p className="no-data">No recipe reports found.</p>
            ) : (
                <table className="report-table">
                    <thead>
                    <tr>
                        <th>Report ID</th>
                        <th>Recipe Title</th>
                        <th>Reporter</th>
                        <th>Reason</th>
                        <th>Reported At</th>
                        <th>Actions</th>
                    </tr>
                    </thead>
                    <tbody>
                    {sortedRecipeReports.map((report) => (
                        <tr key={report.id}>
                            <td>{report.id}</td>
                            <td>{report.recipeTitle || "N/A"}</td>
                            <td>{report.reporterUsername || "N/A"}</td>
                            <td>{report.reason}</td>
                            <td>
                                {report.reportedAt
                                    ? new Date(report.reportedAt).toLocaleString()
                                    : ""}
                            </td>
                            <td>
                                <button
                                    onClick={() => handleDeleteRecipeReport(report.id)}
                                    className="btn btn-delete"
                                >
                                    Delete
                                </button>
                            </td>
                        </tr>
                    ))}
                    </tbody>
                </table>
            )}

            {/* Review Reports Section */}
            <div id="review-reports" className="section-header">
                <h2 className="section-title">Review Reports</h2>
                <select
                    value={reviewSortOrder}
                    onChange={(e) => setReviewSortOrder(e.target.value)}
                    className="filter-select"
                >
                    <option value="newest">Newest First</option>
                    <option value="oldest">Oldest First</option>
                </select>
            </div>
            {reviewReports.length === 0 ? (
                <p className="no-data">No review reports found.</p>
            ) : (
                <table className="report-table">
                    <thead>
                    <tr>
                        <th>Report ID</th>
                        <th>Review Comment</th>
                        <th>Reporter</th>
                        <th>Reason</th>
                        <th>Reported At</th>
                        <th>Actions</th>
                    </tr>
                    </thead>
                    <tbody>
                    {sortedReviewReports.map((report) => (
                        <tr key={report.id}>
                            <td>{report.id}</td>
                            <td>{report.reviewComment || "N/A"}</td>
                            <td>{report.reporterUsername || "N/A"}</td>
                            <td>{report.reason}</td>
                            <td>
                                {report.reportedAt
                                    ? new Date(report.reportedAt).toLocaleString()
                                    : ""}
                            </td>
                            <td>
                                <button
                                    onClick={() => handleDeleteReviewReport(report.id)}
                                    className="btn btn-delete"
                                >
                                    Delete
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

export default ReportDashboard;


/**

 import React, { useEffect, useState } from "react";
 import axios from "axios";
 import "./ReportDashboard.css";
 import API_URL from "./config";
 const ReportDashboard = () => {
    const [recipeReports, setRecipeReports] = useState([]);
    const [reviewReports, setReviewReports] = useState([]);
    const [recipeSortOrder, setRecipeSortOrder] = useState("newest");
    const [reviewSortOrder, setReviewSortOrder] = useState("newest");
    const token = localStorage.getItem("token");

    // Fetch recipe reports
    const fetchRecipeReports = () => {
        axios
            .get(`${API_URL}/api/recipeReports/`, {
                headers: { Authorization: `Bearer ${token}` },
            })
            .then((response) => setRecipeReports(response.data))
            .catch((error) =>
                console.error("Error fetching recipe reports:", error)
            );
    };

    // Fetch review reports
    const fetchReviewReports = () => {
        axios
            .get(`${API_URL}/api/reviewReports/`, {
                headers: { Authorization: `Bearer ${token}` },
            })
            .then((response) => setReviewReports(response.data))
            .catch((error) =>
                console.error("Error fetching review reports:", error)
            );
    };

    // Mark all reports as read when the ReportDashboard is mounted
    useEffect(() => {
        axios
            .patch(`${API_URL}/api/admin/reports/mark-read`, null, {
                headers: { Authorization: `Bearer ${token}` },
            })
            .then(() => {
                // Optionally, you could refetch reports if needed
                fetchRecipeReports();
                fetchReviewReports();
            })
            .catch((error) =>
                console.error("Error marking reports as read:", error)
            );
    }, [token]);

    useEffect(() => {
        fetchRecipeReports();
        fetchReviewReports();
    }, [token]);

    // Sorting helpers
    const sortedRecipeReports = [...recipeReports].sort((a, b) => {
        if (!a.reportedAt || !b.reportedAt) return 0;
        const dateA = new Date(a.reportedAt);
        const dateB = new Date(b.reportedAt);
        return recipeSortOrder === "newest" ? dateB - dateA : dateA - dateB;
    });

    const sortedReviewReports = [...reviewReports].sort((a, b) => {
        if (!a.reportedAt || !b.reportedAt) return 0;
        const dateA = new Date(a.reportedAt);
        const dateB = new Date(b.reportedAt);
        return reviewSortOrder === "newest" ? dateB - dateA : dateA - dateB;
    });

    // Delete handlers
    const handleDeleteRecipeReport = async (reportId) => {
        if (!window.confirm("Are you sure you want to delete this recipe report?"))
            return;
        try {
            await axios.delete(
                `${API_URL}/api/recipeReports/${reportId}`,
                { headers: { Authorization: `Bearer ${token}` } }
            );
            fetchRecipeReports();
        } catch (error) {
            console.error("Error deleting recipe report:", error);
        }
    };

    const handleDeleteReviewReport = async (reportId) => {
        if (!window.confirm("Are you sure you want to delete this review report?"))
            return;
        try {
            await axios.delete(
                `${API_URL}/api/reviewReports/${reportId}`,
                { headers: { Authorization: `Bearer ${token}` } }
            );
            fetchReviewReports();
        } catch (error) {
            console.error("Error deleting review report:", error);
        }
    };

    // Scroll function to review reports section
    const scrollToReviewReports = () => {
        const reviewSection = document.getElementById("review-reports");
        if (reviewSection) {
            reviewSection.scrollIntoView({ behavior: "smooth" });
        }
    };

    return (
        <div className="report-dashboard-container">
            <h1 className="dashboard-title">Reports Dashboard</h1>


<div className="scroll-button-container">
    <button onClick={scrollToReviewReports} className="btn btn-scroll">
        Go to Review Reports
    </button>
</div>


<div className="section-header">
    <h2 className="section-title">Recipe Reports</h2>
    <select
        value={recipeSortOrder}
        onChange={(e) => setRecipeSortOrder(e.target.value)}
        className="filter-select"
    >
        <option value="newest">Newest First</option>
        <option value="oldest">Oldest First</option>
    </select>
</div>
{recipeReports.length === 0 ? (
    <p className="no-data">No recipe reports found.</p>
) : (
    <table className="report-table">
        <thead>
        <tr>
            <th>Report ID</th>
            <th>Recipe Title</th>
            <th>Reporter</th>
            <th>Reason</th>
            <th>Reported At</th>
            <th>Actions</th>
        </tr>
        </thead>
        <tbody>
        {sortedRecipeReports.map((report) => (
            <tr key={report.id}>
                <td>{report.id}</td>
                <td>{report.recipeTitle || "N/A"}</td>
                <td>{report.reporterUsername || "N/A"}</td>
                <td>{report.reason}</td>
                <td>
                    {report.reportedAt
                        ? new Date(report.reportedAt).toLocaleString()
                        : ""}
                </td>
                <td>
                    <button
                        onClick={() => handleDeleteRecipeReport(report.id)}
                        className="btn btn-delete"
                    >
                        Delete
                    </button>
                </td>
            </tr>
        ))}
        </tbody>
    </table>
)}


<div id="review-reports" className="section-header">
    <h2 className="section-title">Review Reports</h2>
    <select
        value={reviewSortOrder}
        onChange={(e) => setReviewSortOrder(e.target.value)}
        className="filter-select"
    >
        <option value="newest">Newest First</option>
        <option value="oldest">Oldest First</option>
    </select>
</div>
{reviewReports.length === 0 ? (
    <p className="no-data">No review reports found.</p>
) : (
    <table className="report-table">
        <thead>
        <tr>
            <th>Report ID</th>
            <th>Review Comment</th>
            <th>Reporter</th>
            <th>Reason</th>
            <th>Reported At</th>
            <th>Actions</th>
        </tr>
        </thead>
        <tbody>
        {sortedReviewReports.map((report) => (
            <tr key={report.id}>
                <td>{report.id}</td>
                <td>{report.reviewComment || "N/A"}</td>
                <td>{report.reporterUsername || "N/A"}</td>
                <td>{report.reason}</td>
                <td>
                    {report.reportedAt
                        ? new Date(report.reportedAt).toLocaleString()
                        : ""}
                </td>
                <td>
                    <button
                        onClick={() => handleDeleteReviewReport(report.id)}
                        className="btn btn-delete"
                    >
                        Delete
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

 export default ReportDashboard;



 **/