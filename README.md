# PlantOML

## Introduction
PlantOML is a graph visualization tool which allows the user to input an OML file and retrieve a visualization of that file as a graph image. The visualization part is an automatic process handled by the application's backend, which uses a custom-built OML to DOT compiler to then input the translated DOT grammar into GraphViz, a pre-existing graph visualization tool. The returned images are given to the user in multiple optional formats, with the default being a png file.

## Background
The PlantOML application is inspired from the PlantUML web application, which provides similar functionality for UML syntax as an input. The PlantOML application runs as a full-stack layered application, with Java springboot as the backend API server, a python Flask server to generate the images, and a React.js web application as the frontend. The custom-made OML to DOT compiler is handled in the Java backend, with manual implementation of the rules of the grammar of OML parsed into inputs of the DOT language. Our application offers output in .png (lossless), so the user can optionally convert to .jpeg and .svg themselves after receiving them.

## What is OML?
Ontological Modeling Language (OML) is a modeling language specified at https://www.opencaesar.io/oml/. It is used to create system engineering vocabularies that can describe systems. OML supports several concrete members such as Aspect, Concept, Relation Entity, Structure, Scalar, Annotation Property, Concept instance, and Relation Instance. You can define Vocabularies, Vocabulary Bundles, Descriptions, and Description Bundles. Vocabularies are a set of ways to describe a system or object, and vocabulary bundles allow you to do closed-system analysis. A description is an instance of the vocabulary and a description bundle similarly lets you do closed-system analysis. OML has the following beneficial characteristics: expressivity, modularity, extensibility, and description logic. It’s concise and easy to understand syntax makes it a great choice to model complex systems. By generating graphs of these complex systems, PlantOML can give you a way to better visualize the relationships in your system.

## Get Started
### OML projects usually consist of nested OML files (.oml), a catalog file (.xml), and a build file (.gradle).

1. Each OML file describes an ontology, and each ontology has a unique IRI (namespace). OML files are written in textual BNF and they have a mapping by some rule in the catalog file.

2. The catalog file maps OML IRIs to their corresponding file path.

3. Build files contain dependencies of your OML project and side effects of analysis performed on your OML project using OML Rosetta.

### Steps

1. Download OML Rosetta and create a new workspace
2. Right-click the created subfolder and select New -> OML Model
3. Create various Vocabularies, Vocabulary Bundles, Descriptions, and Description Bundles
4. Run Gradle build tasks to check for logical consistency in your model
5. Download the directory of your OML project

Once you have an OML project directory, go to the PlantOML online server and upload your directory with the choose file button. You can edit any files you want straight from the browser. Then, pick options for how you want your graph generated and submit.

For each file, we will generate an OML graph with the format of <filename>.png. (convert this into a nice HTML structure)
