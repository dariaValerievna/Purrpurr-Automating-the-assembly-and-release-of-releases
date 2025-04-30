import './App.css'
import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import Register from "./pages/register/Register.tsx"
import Login from "./pages/login/Login.tsx"
import Projects from "./pages/projects/Projects.tsx"
import ProjectPage from "./pages/projectpage/ProjectPage.tsx"

function App() {

  return (
    <Router>
      <Routes>
        <Route path="/login" element={<Login />} />
        <Route path="/register" element={<Register />} />
        <Route path="/projects" element={<Projects />} />
        <Route path="/testproject" element={<ProjectPage />} />
      </Routes>
    </ Router>
  )
}

export default App
