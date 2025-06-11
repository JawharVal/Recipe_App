import { useNavigate } from "react-router-dom";
import "./NavBar.css"; // Import styles
import logo from "./logo.png"; // Adjust path based on your project

const NavBar = () => {
    const navigate = useNavigate();

    const handleLogout = () => {
        localStorage.removeItem("token"); // Clear the token
        localStorage.removeItem("role");
        navigate("/login"); // Redirect to login page
    };

    const handleGoHome = () => {
        navigate("/admin-home"); // Redirect to AdminHome page
    };

    return (
        <nav className="navbar">
            {/* Left Side: LeGourmand and Logo (clickable) */}
            <div className="logo-container" onClick={handleGoHome} style={{ cursor: "pointer" }}>
                <img src={logo} alt="Logo" className="navbar-logo" />
                <h1 className="logo">LeGourmand</h1>
            </div>

            {/* Right Side: Logout Button */}
            <button className="logout-button" onClick={handleLogout}>Logout</button>
        </nav>
    );
};

export default NavBar;
