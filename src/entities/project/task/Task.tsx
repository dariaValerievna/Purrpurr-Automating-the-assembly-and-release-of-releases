import './task.css';
import { useState } from 'react';

export function Task(props) {
    const [isClicked, setIsClicked] = useState(false);

    return (
        <div>
            <div
                onClick={() => setIsClicked(prev => !prev)}
                className={`task ${isClicked ? 'task1' : ''}`}
            >
                {props.name}
            </div>
            {isClicked ? (
                <div className="task__branches">
                    <div className="branch">{props.variants[0]}</div>
                    <div className="branch">{props.variants[1]}</div>
                    <div className="branch">{props.variants[2]}</div>
                </div>
            ) : (
                ''
            )}
        </div>
    );
}
