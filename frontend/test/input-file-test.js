const ReactDOM = require("react-dom");
const act = require("react-dom/test-utils");
const react = require("react");

//const App = require("../src/App");
var assert = require('assert');
const App = require("../src/App");

describe("Input File testing", () => {
  it("Gets local files", () => {
    
    // ReactDOM.render(App(), rootContainer);
    const [values, setValues] = react.useState([]);
      async function onUpload(event) {
        if (event?.target.files?.length) {
          const data = event.target.files[0].text();
          const json = JSON.parse(data);
          setValues(json);
        } else {
          throw new Error('couldnt get files');
        }
        return data;
    }
      var returned_data = onUpload(App().handleFolderUpload);
      assert.equal(data.length !== 0, true);
    });
});
