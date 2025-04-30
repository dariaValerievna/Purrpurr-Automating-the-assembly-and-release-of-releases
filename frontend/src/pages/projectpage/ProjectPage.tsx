import React from "react";
import "./projectpage.css"
import Header from "../../widgets/header/Header.tsx"
import { useState } from "react";

function ProjectPage() {
    const [isClicked, setIsClicked] = useState(false);
    return(
        <div>  
            <Header name={"Тестовый проект"}/>
            <div className="tasks__container">
                <div>
                <div onClick={() => setIsClicked(prev => !prev)} 
                className={`task ${isClicked ? 'task1' : ''}`}>
                    Релизная задача №1
                </div>
                {isClicked ? <div className="task__branches">
                    <div className="branch">Ветка 1</div>
                    <div className="branch">Ветка 2</div>
                    <div className="branch">Ветка 3</div>
                    <div className="branch">Ветка 4</div>
                </div> : ""}
                <div className="task">
                    Релизная задача №2
                </div>
                <div className="task">
                    Релизная задача №3
                </div>
                </div>
            </div>     
        </div>
    )
}

export default ProjectPage;