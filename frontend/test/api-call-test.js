const ReactDOM = require("react-dom");
const act = require("react-dom/test-utils");
const react = require("react");

//const App = require("../src/App");
var assert = require('assert');

describe("Frontend API Call Testing", () => {
  it("Gets a response from the API server", () => {
    
    // ReactDOM.render(App(), rootContainer);
    
    // const h1 = rootContainer.querySelector("App-header");
    it("should call the API from the frontend", () => {
      const response = fetch("localhost:8080/"); // calls local API backend server
      const diagrams = response.json();
      console.log(diagrams);
      assert.equal("Greetings from Spring Boot!", diagrams);
    });

  });
});
