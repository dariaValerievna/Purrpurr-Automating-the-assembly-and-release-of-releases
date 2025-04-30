import { Link } from "react-router-dom";
import React from 'react';  
import "./login.css"

function Login() {
    console.log("login")
    return(
        <div className="login">
            <div className="container">
                <h1>Авторизация</h1>
                <input type="text" placeholder='введите email или username'/>
                <input type="text" placeholder='введите пароль'/>
                <button className="form__button">Войти</button>
                <span><span>Нет аккаунта? </span> 
                <Link to="/register" className="link">Зарегистрироваться</Link></span>
            </div>
        </div>
    )
}

export default Login;