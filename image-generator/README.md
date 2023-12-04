Install graphviz using https://graphviz.org/download/, install flask and other python dependencies using pip.
Start the server using python -m flask
You cant test this endpoint by using "curl -X POST -F "dot_file=@example.dot" http://localhost:5000/generate_image -o output.png"

