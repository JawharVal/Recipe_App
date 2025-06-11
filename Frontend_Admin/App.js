import { BrowserRouter as Router, Routes, Route } from "react-router-dom";
import LoginPage from "./LoginPage";
import Dashboard from "./Dashboard";
import PrivateRoute from "./PrivateRoute";
import RecipeDashboard from "./RecipeDashboard"; // For recipes
import ReviewDashboard from "./ReviewDashboard";
import { useParams } from "react-router-dom";
import ReportDashboard from "./ReportDashboard";
import FavoritesDashboard from "./FavoritesDashboard";
import ShoppingList from "./ShoppingList";
import AdminShoppingList from "./ShoppingList";
import AdminFeedback from "./AdminFeedback";
import AdminNewsletter from "./AdminNewsletter";
import ChallengesDashboard from "./ChallengesDashboard";
import AdminHome from "./AdminHome";
import NavBar from "./NavBar"; // Import the navbar

const ReviewDashboardWrapper = () => {
    const { recipeId } = useParams();
    return <ReviewDashboard recipeId={recipeId} />;
};

function App() {
    return (
        <Router>
            <Routes>
                {/* Public Routes */}
                <Route path="/" element={<LoginPage />} />
                <Route path="/login" element={<LoginPage />} />

                {/* Private Routes (Require Authentication) */}
                <Route element={<PrivateRoute />}>
                    <Route path="/admin-home" element={<AdminHome />} />
                    <Route path="/dashboard" element={<Dashboard />} />
                    <Route path="/recipes" element={<RecipeDashboard />} />
                    <Route path="/reviews/:recipeId" element={<ReviewDashboardWrapper />} />
                    <Route path="/reports" element={<ReportDashboard />} />
                    <Route path="/favorites" element={<FavoritesDashboard />} />
                    <Route path="/newsletter" element={<AdminNewsletter token={localStorage.getItem("token")} onClose={() => {}} />} />
                    <Route path="/challenges" element={<ChallengesDashboard />} />
                    <Route path="/shopping-list" element={<AdminShoppingList />} />
                    <Route path="/feedback" element={<AdminFeedback token={localStorage.getItem("token")} />} />
                </Route>
                {/* Catch-all: Redirect unknown routes to login */}
                <Route path="*" element={<LoginPage />} />
            </Routes>
        </Router>
    );
}

export default App;
