import './projects.css';
import { Project } from '../../entities/project/project/Project.tsx';
import { useState, useEffect } from 'react';
import search from './../../assets/search.svg';
import './../../widgets/header/header.css'
import { getProjects } from '../../api/api.js';

type Project={name:string; 
    defaultBranch: string
}

function Projects() {
    const [projects, setProjects] = useState<Project[]>([])

      const [filtered, setFiltered] = useState(projects);
      const [input, setInput] = useState<string>('');

      useEffect(() => {
        const result = projects.filter(project =>
          project.name.toLowerCase().includes(input.toLowerCase())
        );
        setFiltered(result);
      }, [input]);

      useEffect(() => {


    const fetchData = async () => {
      try {
        const data = await getProjects();
        setProjects(data)
      } catch (err) {
        console.error('Ошибка при получении:', err);
            }
    };

    fetchData();
  }, []);

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
                            branch={proj.defaultBranch}
                        />
                    );
                })}
            </div>
        </div>
    );
}

export default Projects;
