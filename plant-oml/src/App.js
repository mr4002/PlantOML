import './App.css';
import { Buffer } from 'buffer';
import React  from 'react';
import logo from './logo.svg'
import imgresult from './img.jpeg'

const imageRef = React.createRef();

function App() {
  console.log("start");
  return (
    <div className="App">
      <header className="App-header">
        <p>
          PlantOML
        </p>
        <form onSubmit={printOMLNS}>
          <label>
            <textarea placeholder = "OML normal string"  name="omlcode"/>
          </label>
          <input type="submit" value="Submit" />
        </form>
        <form onSubmit={printOMLHEX}>
          <label>
            <textarea placeholder = "OML HEX"  name="omlcode"/>
          </label>
          <input type="submit" value="Submit" />
        </form>
        <form onSubmit={printOMLDS}>
          <label>
            <textarea placeholder = "OML deflated string"  name="omlcode"/>
          </label>
          <input type="submit" value="Submit" />
        </form>
        <img src={logo} alt="logo" ref={imageRef} name="imgdisplay" />
      </header>
    </div>
  );
}

const printOMLNS = (f) =>{
  f.preventDefault();
  var omlcode = f.target.omlcode.value;
  f.target.omlcode.value = "";
  const hex = Buffer.from(omlcode, 'utf8').toString('hex');
  renderOMLDiagram(hex);
}

const printOMLHEX = (f) =>{
  f.preventDefault();
  var omlcode = f.target.omlcode.value;
  f.target.omlcode.value = "";
  renderOMLDiagram(omlcode);
}

const printOMLDS = (f) =>{
  f.preventDefault();
  var deflate = require('deflate-js')
  var omlcode = f.target.omlcode.value;
  f.target.omlcode.value = "";
  var decompressed = deflate.inflate(omlcode);
  const hex = Buffer.from(decompressed, 'utf8').toString('hex');
  renderOMLDiagram(hex);
}


const renderOMLDiagram = (hex) => {
  console.log(hex);
  imageRef.current.src = imgresult
}

export default App;
