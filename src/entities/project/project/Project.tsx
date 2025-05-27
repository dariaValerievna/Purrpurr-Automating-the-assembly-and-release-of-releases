import { Link } from 'react-router-dom';
import './project.css';

type ProjectProps = {
    name: string;
    date: string;
};

export function Project(props: ProjectProps) {
    // логика для авторизации, будет позже
    // const location = useLocation();

    // useEffect(() => {
    //     const params = new URLSearchParams(location.search);
    //     const code = params.get('code');
    //     if (code) {
    //         console.log('OAuth code:', code);
    //     }
    // }, [location]);

    return (
        <div className="project__container">
            <div className="project__name">
                <Link style={{all: "unset"}} to="/testproject">
                    <p>{props.name}</p>
                </Link>
            </div>

            <div className="project__container__info">
                Ветки: <br />
                Merge Request: <br />
                Сущности (файлы): <br />
                Дата последнего изменения: {props.date}
            </div>
        </div>
    );
}
