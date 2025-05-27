import './projectpage.css';
import { useState } from 'react';
import { Task } from '../../entities/project/task/Task.tsx';
import { useEffect } from 'react';
import search from './../../assets/search.svg';

function ProjectPage() {
    const mockTasks = [
        {
            id: 1,
            name: 'Задача 1',
            variants: ['Вариант A', 'Вариант B', 'Вариант C'],
        },
        {
            id: 2,
            name: 'Задача 2',
            variants: ['Вариант X', 'Вариант Y'],
        },
        {
            id: 3,
            name: 'Задача 3',
            variants: ['Вариант 1', 'Вариант 2', 'Вариант 3', 'Вариант 4'],
        },
    ];

    const [filtered, setFiltered] = useState(mockTasks);
    const [input, setInput] = useState<string>('');

    useEffect(() => {
        const result = mockTasks.filter(project =>
            project.name.toLowerCase().includes(input.toLowerCase())
        );
        setFiltered(result);
    }, [input]);

    return (
        <>
            <div className="header">
                <div className="header__info">
                    <h2>Project Alpha</h2>
                    <div className="search">
                        <input
                            type="text"
                            placeholder="поиск"
                            onChange={e => {
                                setInput(e.target.value);
                            }}
                        />
                        <img className="search__img" src={search} alt="" />
                    </div>
                </div>
                {/* <div className="header__profile">
                                <img src={profilelogo} alt="" />
                                <button>Выйти</button>
                            </div> */}
            </div>
            <div className="tasks__container">
                {filtered.map(task => {
                    return <Task name={task.name} variants={task.variants} />;
                })}
            </div>
        </>
    );
}

export default ProjectPage;
