import './projects.css';
import { Project } from '../../entities/project/project/Project.tsx';
import { useState, useEffect } from 'react';
import search from './../../assets/search.svg';
import './../../widgets/header/header.css'

export const mockProjects = [
    {
        id: 1,
        name: 'Project Alpha',
        created_at: '2024-12-01',
    },
    {
        id: 2,
        name: 'Beta Build',
        created_at: '2025-01-15',
    },
    {
        id: 3,
        name: 'Gamma Test Suite',
        created_at: '2025-02-10',
    },
    {
        id: 4,
        name: 'Delta Analytics',
        created_at: '2025-03-05',
    },
];

function Projects() {

      const [filtered, setFiltered] = useState(mockProjects);
      const [input, setInput] = useState<string>('');

      useEffect(() => {
        const result = mockProjects.filter(project =>
          project.name.toLowerCase().includes(input.toLowerCase())
        );
        setFiltered(result);
      }, [input]);

    return (
        <div>
            <div className="header">
                <div className="header__info">
                    <h2>Проекты</h2>
                    <div className="search">
                        <input type="text" placeholder="поиск" onChange={(e)=>{setInput(e.target.value)}}/>
                        <img className="search__img" src={search} alt="" />
                    </div>
                </div>
                {/* <div className="header__profile">
                    <img src={profilelogo} alt="" />
                    <button>Выйти</button>
                </div> */}
            </div>
            <div className="projects__container">
                {filtered.map((proj, index) => {
                    return (
                        <Project
                            key={index}
                            name={proj.name}
                            date={proj.created_at}
                        />
                    );
                })}
            </div>
        </div>
    );
}

export default Projects;
