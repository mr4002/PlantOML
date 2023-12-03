import React from 'react';
import { Link } from 'react-router-dom';
import './SideBar.css'

const Sidebar = () => {
  return (
    <div className="sidebar">
          <div className="item">
          <Link to="/">Home</Link>
          </div>
          <div className="item">
          <Link to="/start">Get Started</Link>
          </div>
          <div className="item"> 
          <Link to="/server">Online Server</Link>
          </div>
          
    </div>
  );
};
export default Sidebar;