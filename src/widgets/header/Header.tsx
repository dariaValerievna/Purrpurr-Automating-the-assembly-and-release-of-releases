import search from "./../../assets/search.svg"
import profilelogo from "./../../assets/image1.svg"
import React from "react"
import "./header.css"

function Header(props) {
    return(
        <div className="header">
            <div className="header__info">
                <h2>{props.name}</h2>
                <div className="search">
                    <input type="text" placeholder="поиск"/>
                    <img className="search__img" src={search} alt="" />
                </div>
            </div>
            <div className="header__profile">
                <img src={profilelogo} alt="" />
                <button>Выйти</button>
            </div>
        </div> 
    )
}

export default Header;