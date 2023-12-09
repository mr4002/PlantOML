const ReactDOM = require("react-dom");
const act = require("react-dom/test-utils");
const react = require("react");

const App = require("../src/App");
var assert = require('assert');

let rootContainer;

beforeEach(() => {
  rootContainer = document.createElement("div");
  document.body.appendChild(rootContainer);
});

afterEach(() => {
  document.body.removeChild(rootContainer);
  rootContainer = null;
});

describe("Frontend PlantOML text", () => {
  it("Displays frontend OML input", () => {
    
    //ReactDOM.render(App(), rootContainer);
    const h1 = rootContainer.querySelector("a");
    act(() => {
      ReactDOM.render(<App />, rootContainer);
    });
    const [files, setFiles] = useState([]);
    assert.equal(h1, setFiles); // check uploaded file tree
  });
});
