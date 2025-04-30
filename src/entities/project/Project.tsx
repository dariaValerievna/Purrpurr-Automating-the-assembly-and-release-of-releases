import React from "react";
import "./project.css"

export function Project() {
    return(
        <div className="project__container">
            <div className="project__name">
                <p>Название проекта</p>
            </div>
            <div className="project__container__info">
                Ветки: <br />
                Merge Request: <br />
                Сущности (файлы): <br />
                Дата последнего изменения:
            </div>
        </div>
    )
}