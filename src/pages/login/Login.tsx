import './login.css';
import { useNavigate } from 'react-router-dom';

function Login() {
    const navigate = useNavigate();

    const CLIENT_ID = 'CLIENT_ID'; //добавим позже
    const REDIRECT_URI = 'http://localhost:3000/projects';

    const gitlabAuthUrl = `https://gitlab.com/oauth/authorize?client_id=${CLIENT_ID}&redirect_uri=${REDIRECT_URI}&response_type=code&scope=read_user+read_api`;

    function handleLogin() {
        // window.location.href = gitlabAuthUrl; будет позже
        navigate('/projects');
    }

    return (
        <div className="login__page">
            <button className="gitlab-login-btn" onClick={handleLogin}>
                Войти через GitLab
            </button>
        </div>
    );
}

export default Login;
