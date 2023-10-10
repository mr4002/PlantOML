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
        <form onSubmit={printOML}>
          <label>
            <textarea placeholder = "OML code"  name="omlcode"/>
          </label>
          <input type="submit" value="Submit" />
        </form>
      </header>
    </div>
  );
}

const printOML = (f) =>{
  f.preventDefault();
  var omlcode = f.target.omlcode.value;
  f.target.omlcode.value = "";
  const hex = Buffer.from(omlcode, 'utf8').toString('hex');
  console.log(hex)
}

export default App;
