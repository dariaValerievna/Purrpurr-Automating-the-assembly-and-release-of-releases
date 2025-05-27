import './App.css';
import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import Login from './pages/login/Login.tsx';
import Projects from './pages/projects/Projects.tsx';
import ProjectPage from './pages/projectpage/ProjectPage.tsx';

function App() {
    return (
        <Router>
            <Routes>
                <Route path="/" element={<Login />} />
                <Route path="/projects" element={<Projects />} />
                <Route path="/testproject" element={<ProjectPage />} />
            </Routes>
        </Router>
    );
}

export default App;
