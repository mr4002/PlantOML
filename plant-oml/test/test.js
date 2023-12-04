const ReactDOM = require("react-dom");
const act = require("react-dom/test-utils");
const react = require("react");

//const App = require("../src/App");
var assert = require('assert');

describe('Array', function () {
  describe('#indexOf()', function () {
    it('should return -1 when the value is not present', function () {
      assert.equal([1, 2, 3].indexOf(4), -1);
    });
  });
});

describe("App Component Testing", () => {
  it("Renders PlantOML text", () => {
    
    // ReactDOM.render(App(), rootContainer);
    
    // const h1 = rootContainer.querySelector("App-header");
    var textContent = "test";
    it("should return equal strings", () => {
      assert.equal("PlantOML", textContent);
    });
  });
});