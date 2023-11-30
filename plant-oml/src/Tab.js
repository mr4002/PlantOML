//Tab format from https://www.geeksforgeeks.org/how-to-create-tabs-in-reactjs/

import React, { useState } from "react";
 
const Tab = ({ label, onClick, isActive }) => (
    <div
        className={`tab ${isActive ? "active" : ""}`}
        onClick={onClick}
    >
        {label}
    </div>
);
 
export default Tab;