function Home() {
    return (<div>
    <h1>Home</h1>
    <p>
    PlantOML is a graph visualization tool which allows the user to input an OML file and retrieve a visualization of that file as a graph image. The visualization part is an automatic process handled by the application's backend, which uses a custom-built OML to DOT compiler to then input the translated DOT grammar into GraphViz, a pre-existing graph visualization tool. The returned images are given to the user in multiple optional formats, with the default being a png file.
    </p>
    <h2>Background</h2>
    <p>
    The PlantOML application is inspired from the PlantUML web application, which provides similar functionality for UML syntax as an input. The PlantOML application runs as a full-stack layered application, with Java springboot as the backend API server, a python Flask server to generate the images, and a React.js web application as the frontend. The custom-made OML to DOT compiler is handled in the Java backend, with manual implementation of the rules of the grammar of OML parsed into inputs of the DOT language. Our application offers output in .png (lossless), so the user can optionally convert to .jpeg and .svg themselves after receiving them.
    </p>
    <h2>What is OML?</h2>
    <p>Ontological Modeling Language (OML) is a modeling language specified at https://www.opencaesar.io/oml/. It is used to create system engineering vocabularies that can describe systems. OML supports several concrete members such as Aspect, Concept, Relation Entity, Structure, Scalar, Annotation Property, Concept instance, and Relation Instance. You can define Vocabularies, Vocabulary Bundles, Descriptions, and Description Bundles. Vocabularies are a set of ways to describe a system or object, and vocabulary bundles allow you to do closed-system analysis. A description is an instance of the vocabulary and a description bundle similarly lets you do closed-system analysis. OML has the following beneficial characteristics: expressivity, modularity, extensibility, and description logic. It’s concise and easy to understand syntax makes it a great choice to model complex systems. By generating graphs of these complex systems, PlantOML can give you a way to better visualize the relationships in your system.
    </p>
    </div>);
}

export default Home