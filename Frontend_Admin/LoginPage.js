import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";

import { jwtDecode } from "jwt-decode";
import "./LoginPage.css"; // Import the custom CSS file
import api from "./axiosInstance";

const LoginPage = () => {
    const [email, setEmail] = useState("");
    const [password, setPassword] = useState("");
    const [error, setError] = useState("");
    const navigate = useNavigate();

    // Check for a valid token on component mount
    useEffect(() => {
        const token = localStorage.getItem("token");
        if (token) {
            try {
                const decoded = jwtDecode(token);
                const currentTime = Date.now() / 1000;
                if (decoded.exp > currentTime && decoded.role === "ADMIN") {
                    navigate("/admin-home");
                }
            } catch (err) {
                localStorage.removeItem("token");
                localStorage.removeItem("role");
            }
        }
    }, [navigate]);

    const handleLogin = async (e) => {
        e.preventDefault();
        try {
            const response = await api.post("/auth/login", { email, password });

            const accessToken = response.data.accessToken;
            const decoded = jwtDecode(accessToken);

            if (decoded.role !== "ADMIN") {
                setError("Access Denied: You are not an admin.");
                return;
            }

            localStorage.setItem("token", accessToken);
            localStorage.setItem("role", decoded.role);
            navigate("/admin-home");
        } catch (error) {
            setError("Invalid email or password.");
        }
    };

    return (
        <div className="login-container">
            <div className="background-overlay"></div>
            <form onSubmit={handleLogin} className="login-form animate-fadeIn">
                {/* Updated title to mention LeGourmand */}
                <h1 className="form-titles">
                    LeGourmand<br />
                </h1>
                <h2 className="form-title"> Admin Panel</h2>
                {error && <p className="error-message">{error}</p>}

                <label htmlFor="email" className="form-label">Email</label>
                <input
                    type="email"
                    id="email"
                    placeholder="Enter your email"
                    value={email}
                    onChange={(e) => setEmail(e.target.value)}
                    className="form-input"
                    required
                />

                <label htmlFor="password" className="form-label">Password</label>
                <input
                    type="password"
                    id="password"
                    placeholder="Enter your password"
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                    className="form-input"
                    required
                />

                <button type="submit" className="submit-button">Login</button>
            </form>
        </div>
    );
};

export default LoginPage;


//FOR DEPLOY
/**
 import { useEffect, useState } from "react";
 import { useNavigate } from "react-router-dom";
 import axios from "axios";
 import { jwtDecode } from "jwt-decode";
 import "./LoginPage.css";
 import API_URL from "./config"; // Import the custom CSS file

 const LoginPage = () => {
    const [email, setEmail] = useState("");
    const [password, setPassword] = useState("");
    const [error, setError] = useState("");
    const navigate = useNavigate();

    // Check for a valid token on component mount
    useEffect(() => {
        const token = localStorage.getItem("token");
        if (token) {
            try {
                const decoded = jwtDecode(token);
                const currentTime = Date.now() / 1000;
                if (decoded.exp > currentTime && decoded.role === "ADMIN") {
                    navigate("/admin-home");
                }
            } catch (err) {
                localStorage.removeItem("token");
                localStorage.removeItem("role");
            }
        }
    }, [navigate]);

    const handleLogin = async (e) => {
        e.preventDefault();
        try {
            const response = await axios.post(`${API_URL}/api/auth/login`, {
                email,
                password,
            });
            const accessToken = response.data.accessToken;
            const decoded = jwtDecode(accessToken);

            if (decoded.role !== "ADMIN") {
                setError("Access Denied: You are not an admin.");
                return;
            }

            localStorage.setItem("token", accessToken);
            localStorage.setItem("role", decoded.role);
            navigate("/admin-home");
        } catch (error) {
            setError("Invalid email or password.");
        }
    };

    return (
        <div className="login-container">
            <div className="background-overlay"></div>
            <form onSubmit={handleLogin} className="login-form animate-fadeIn">

<h1 className="form-titles">
    LeGourmand<br />
</h1>
<h2 className="form-title"> Admin Panel</h2>
{error && <p className="error-message">{error}</p>}

<label htmlFor="email" className="form-label">Email</label>
<input
    type="email"
    id="email"
    placeholder="Enter your email"
    value={email}
    onChange={(e) => setEmail(e.target.value)}
    className="form-input"
    required
/>

<label htmlFor="password" className="form-label">Password</label>
<input
    type="password"
    id="password"
    placeholder="Enter your password"
    value={password}
    onChange={(e) => setPassword(e.target.value)}
    className="form-input"
    required
/>

<button type="submit" className="submit-button">Login</button>
</form>
</div>
);
};

export default LoginPage;


//FOR DEPLOY
/**
 import { useEffect, useState } from "react";
 import { useNavigate } from "react-router-dom";
 import axios from "axios";
 import { jwtDecode } from "jwt-decode";
 import "./LoginPage.css";
 import API_URL from "./config"; // Import the custom CSS file

 const LoginPage = () => {
    const [email, setEmail] = useState("");
    const [password, setPassword] = useState("");
    const [error, setError] = useState("");
    const navigate = useNavigate();

    // Check for a valid token on component mount
    useEffect(() => {
        const token = localStorage.getItem("token");
        if (token) {
            try {
                const decoded = jwtDecode(token);
                const currentTime = Date.now() / 1000;
                if (decoded.exp > currentTime && decoded.role === "ADMIN") {
                    navigate("/admin-home");
                }
            } catch (err) {
                localStorage.removeItem("token");
                localStorage.removeItem("role");
            }
        }
    }, [navigate]);

    const handleLogin = async (e) => {
        e.preventDefault();
        try {
            const response = await axios.post(`${API_URL}/api/auth/login`, {
                email,
                password,
            });
            const accessToken = response.data.accessToken;
            const decoded = jwtDecode(accessToken);

            if (decoded.role !== "ADMIN") {
                setError("Access Denied: You are not an admin.");
                return;
            }

            localStorage.setItem("token", accessToken);
            localStorage.setItem("role", decoded.role);
            navigate("/admin-home");
        } catch (error) {
            setError("Invalid email or password.");
        }
    };

    return (
        <div className="login-container">
            <div className="background-overlay"></div>
            <form onSubmit={handleLogin} className="login-form animate-fadeIn">

<h1 className="form-titles">
    LeGourmand<br />
</h1>
<h2 className="form-title"> Admin Panel</h2>
{error && <p className="error-message">{error}</p>}

<label htmlFor="email" className="form-label">Email</label>
<input
    type="email"
    id="email"
    placeholder="Enter your email"
    value={email}
    onChange={(e) => setEmail(e.target.value)}
    className="form-input"
    required
/>

<label htmlFor="password" className="form-label">Password</label>
<input
    type="password"
    id="password"
    placeholder="Enter your password"
    value={password}
    onChange={(e) => setPassword(e.target.value)}
    className="form-input"
    required
/>

<button type="submit" className="submit-button">Login</button>
</form>
</div>
);
};


 export default LoginPage;**/

