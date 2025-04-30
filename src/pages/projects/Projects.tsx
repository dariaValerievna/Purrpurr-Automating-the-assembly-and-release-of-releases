import React from "react";
import "./projects.css"
import { Project } from "../../entities/project/Project.tsx"
import Header from "../../widgets/header/Header.tsx"

function Projects() {
    return(
        <div>
            <Header name={"Проекты"}/>
            <div className="projects__container">
                <Project />
                <Project />
                <Project />
                <Project />
                <Project />
            </div>
        </div>
    )
}

export default Projects;