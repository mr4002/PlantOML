import './App.css';
import { Buffer } from 'buffer';
import React  from 'react';
import Tabs from "./Tabs";

const imageRef = React.createRef();

function App() {
  console.log("start");
  const omlForms = [
    { label: "Normal String" },
    { label: "HEX String" },
    { label: "Deflated String" },
];
  return (
    <div className="App">
      <header className="App-header">
        <p>
          PlantOML
        </p>
        <img src='' ref={imageRef} name="imgdisplay" />
        <Tabs tabs={omlForms} />
      </header>
    </div>
  );
}

export function updateImage(string){
  console.log("updateIMAGE");
  imageRef.current.src = string;
}

export default App;
