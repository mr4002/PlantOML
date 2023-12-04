import React from 'react';
import './Options.css'

const OptionsComponent = () => {
  return (
    <div className='flexbox'>
        <label className="label">
            Node Shape:
        </label>
        <select name="nodeShape">
            <option value=""></option>
            <option value="box">Box &#x25A0;</option>
            <option value="ellipse">Ellipse &#9898;</option>
            <option value="diamond">Diamond &#9830;</option>
        </select>
        <br />

        <label className="label">
            Node Color:  
        </label> 
        <select name="nodeColor">
            <option value=""></option>
            <option value="red"> Red 🔴</option>
            <option value="blue">Blue 🔵</option>
            <option value="green">Black ⚫</option>
        </select>
        <br />

        <label className="label">
            Edge Color: 
        </label>  
        <select name="edgeColor">
            <option value=""></option>
            <option value="black">Black ⚫</option>
            <option value="blue">Blue 🔵</option>
        </select>
        <br />

        <label className="label">
            Edge Style:   
        </label>
        <select name="edgeStyle">
            <option value=""></option>
            <option value="solid">Solid &#x2500;</option>
            <option value="dashed">Dashed ----</option>
            <option value="dotted">Dotted '''''</option>
        </select>
        <br />

        <label className="label">
            Graph Layout:   
        </label>
        <select name="graphLayout">
            <option value=""></option>
            <option value="dot">Dot</option>
            <option value="neato">Neato</option>
            <option value="circo">Circo</option>
            <option value="fdp">FDP</option>
        </select>
        <br />

        <label className="label">
        Graph Background Color:  
        </label> 
        <select name="graphBackgroundColor">
            <option value=""></option>
            <option value="white">White ⚪</option>
            <option value="lightblue">Blue 🔵</option>
        </select>
        

        <label className="label">
            DPI:
         </label>
        <input type="range" name="dpi" min="72" max="300" step="1" />
        <br />
    </div>
  );
};

export default OptionsComponent;