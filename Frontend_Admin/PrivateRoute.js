import { Navigate, Outlet } from "react-router-dom";
import { jwtDecode } from "jwt-decode";
import NavBar from "./NavBar"; // Import the persistent navbar

const PrivateRoute = () => {
    const token = localStorage.getItem("token");

    if (token) {
        try {
            const decoded = jwtDecode(token);
            const currentTime = Date.now() / 1000;

            if (decoded.exp < currentTime) {
                // Token expired
                localStorage.removeItem("token");
                localStorage.removeItem("role");
                return <Navigate to="/login" />;
            }

            // Token is valid—allow access and show NavBar
            return (
                <>
                    <NavBar /> {/* Persistent top navigation bar */}
                    <Outlet />  {/* Renders the requested private route */}
                </>
            );
        } catch (error) {
            // If decoding fails, clear storage and redirect
            localStorage.removeItem("token");
            localStorage.removeItem("role");
            return <Navigate to="/login" />;
        }
    }

    // If no token, redirect to login
    return <Navigate to="/login" />;
};

export default PrivateRoute;
