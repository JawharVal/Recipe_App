import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import api from "./config";
import "./AdminHome.css";

import {
    BarChart,
    Bar,
    XAxis,
    YAxis,
    CartesianGrid,
    Tooltip,
    Legend,
    ResponsiveContainer
} from 'recharts';

const StatCard = ({ label, value }) => (
    <div className="stat-card">
        <h3>{value ?? "…"}</h3>
        <span>{label}</span>
    </div>
);

const AdminHome = () => {
    const navigate = useNavigate();
    const [stats, setStats] = useState(null);
    const [activeUsers, setActiveUsers] = useState([]);
    const [rankingType, setRankingType] = useState("publicRecipeCount");
    const sortedUsers = [...activeUsers].sort((a, b) => b[rankingType] - a[rankingType]);

    useEffect(() => {
        api.get("/api/admin/active-users")
            .then(res => setActiveUsers(res.data))
            .catch(err => console.error("❌ Failed to load active users", err));

        api.get("/api/admin/stats")
            .then(res => setStats(res.data))
            .catch(err => console.error("Failed to load stats", err));
    }, []);

    return (
        <div className="admin-home">
            <h1 className="title">Admin Panel</h1>
            {/* NAVIGATION */}
            <div className="section centered-section">
                <h2 className="section-title">Admin Actions</h2>
                <div className="button-container">
                    <button onClick={() => navigate("/dashboard")} className="nav-button">Manage Users</button>
                    <button onClick={() => navigate("/recipes")} className="nav-button">Manage Recipes</button>
                    <button onClick={() => navigate("/reports")} className="nav-button">View Reports</button>
                    <button onClick={() => navigate("/newsletter")} className="nav-button">Newsletter</button>
                    <button onClick={() => navigate("/challenges")} className="nav-button">Challenges</button>
                    <button onClick={() => navigate("/feedback")} className="nav-button">User Feedback</button>
                </div>
            </div>


            {/* STATS SECTION */}
            <div className="section">
                <h2 className="section-title">App Statistics</h2>
                <div className="stats-grid">
                    <StatCard label="Users" value={stats?.totalUsers} />
                    <StatCard label="Recipes" value={stats?.totalRecipes} />
                    <StatCard label="Total Likes" value={stats?.totalLikes} />
                    <StatCard label="Reviews" value={stats?.totalComments} />
                    <StatCard label="Recipe Reports" value={stats?.totalRecipeReports} />
                    <StatCard label="Review Reports" value={stats?.totalReviewReports} />
                </div>
            </div>


            {/* CHART SECTION */}
            <div className="section chart-section">
                <h2 className="section-title">Most Active Users</h2>
                <div className="chart-card">
                    <ResponsiveContainer width="100%" height={Math.max(400, sortedUsers.length * 45)}>
                        <BarChart
                            data={sortedUsers}
                            layout="vertical"
                            margin={{ top: 20, right: 30, left: 100, bottom: 20 }}
                        >
                            <CartesianGrid strokeDasharray="3 3" />
                            <XAxis
                                type="number"
                                allowDecimals={false}
                                tickCount={10}
                                interval={0}
                                domain={[0, 'dataMax + 1']}
                                tickFormatter={(tick) => Math.round(tick)}
                            />

                            <YAxis dataKey="username" type="category" width={120} />
                            <Tooltip />
                            <Legend />
                            <Bar
                                dataKey={rankingType}
                                fill={rankingType === "followerCount" ? "#348f8f" : "#D4AF37"}
                                name={rankingType === "followerCount" ? "Followers" : "Public Recipes"}
                            />
                        </BarChart>
                    </ResponsiveContainer>


                </div>
                <div style={{ textAlign: "center", marginTop: "1rem" }}>
                    <label style={{ color: "#e0c97f", marginRight: "10px", fontWeight: "bold" }}>Rank by:</label>
                    <select
                        value={rankingType}
                        onChange={(e) => setRankingType(e.target.value)}
                        style={{
                            padding: "6px 10px",
                            borderRadius: "5px",
                            border: "1px solid #D4AF37",
                            background: "#1c1c1c",
                            color: "#fff"
                        }}
                    >
                        <option value="publicRecipeCount">Public Recipes</option>
                        <option value="followerCount">Followers</option>
                    </select>
                </div>
            </div>
        </div>

    );
};

export default AdminHome;
