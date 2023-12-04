import React from 'react';
import { Link } from 'react-router-dom';
import HomeIcon from '@mui/icons-material/Home';
import GetStartedIcon from '@mui/icons-material/PlayCircleOutline';
import ServerIcon from '@mui/icons-material/Cloud';
import './SideBar.css';

const Sidebar = () => {
    return (
        <div className="sidebar">
            <div className="item">
                <Link to="/"><HomeIcon htmlColor='black'/></Link>
            </div>
            <div className="item">
                <Link to="/start"><GetStartedIcon htmlColor='black'/></Link>
            </div>
            <div className="item">
                <Link to="/server"><ServerIcon htmlColor='black'/></Link>
            </div>
        </div>
    );
};

export default Sidebar;
