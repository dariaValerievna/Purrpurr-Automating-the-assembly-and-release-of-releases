import "./statuswarning.css"
import React from "react"

function StatusWarning() {
    return(
        <div className="warning__container">
            <p className="warning__title">Задачи не в статусе For Release или In Release!</p>
            <p className="warning__description">№ задачи, заголовок, статус, теги, автор, разработчик</p>
        </div>
    )
}

export default StatusWarning;