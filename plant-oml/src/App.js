import './App.css';
import { Buffer } from 'buffer';
import React  from 'react';

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
      </header>
    </div>
  );
}

const printOMLNS = (f) =>{
  f.preventDefault();
  var omlcode = f.target.omlcode.value;
  f.target.omlcode.value = "";
  const hex = Buffer.from(omlcode, 'utf8').toString('hex');
  console.log(hex)
}

const printOMLHEX = (f) =>{
  f.preventDefault();
  var omlcode = f.target.omlcode.value;
  f.target.omlcode.value = "";
  console.log(omlcode)
}

const printOMLDS = (f) =>{
  f.preventDefault();
  var omlcode = f.target.omlcode.value;
  f.target.omlcode.value = "";
  const hex = Buffer.from(omlcode, 'utf8').toString('hex');
  console.log(hex)
}


export default App;
