import React from 'react'; 
import "./register.css"
import { Link } from 'react-router-dom'; 

function Register() {
    return(
        <div className='register'>
            <div className='container'>
                <h1>Регистрация</h1>
                <input type="text" placeholder='введите email или username'/>
                <input type="text" placeholder='введите пароль'/>
                <button className='form__button'>Зарегистрироваться</button>
                <span><span>Есть аккаунт? </span> 
                <Link to="/login" className='link'>Войти</Link></span>
            </div>
        </div>
    )
}

export default Register;

