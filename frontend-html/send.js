async function sendRequest() {
  const response = await fetch("http://localhost:4041");
  const message = await response.json();
  console.log(message);
}

sendRequest();
console.log("test");