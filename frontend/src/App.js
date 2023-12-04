import React from 'react';
import { BrowserRouter as Router, Route, Routes } from 'react-router-dom';
import Home from './components/Home';
import Server from './components/Server';
import GetStarted from './components/GetStarted';
import Sidebar from './components/SideBar';
import './App.css'

const App = () => {
    return (
        <Router>
            <div className="app-container">
                <Sidebar />
                <div className="content">
                    <Routes>
                        <Route exact path="/" element={<Home />} />
                        <Route exact path="/server" element={<Server />} />
                        <Route exact path="/start" element={<GetStarted />} />
                    </Routes>
                </div>
            </div>
        </Router>
    );
};

export default App;
