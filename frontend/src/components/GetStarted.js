function GetStarted() {
    return (<div>
        <h1>Get Started</h1>
    <p><strong>OML projects</strong> usually consist of nested OML files (.oml), a catalog file (.xml), and a build file (.gradle).</p>
    <ol>
        <li>
            <p>Each OML file describes an ontology, and each ontology has a unique IRI (namespace). OML files are written in textual BNF and they have a mapping by some rule in the catalog file.</p>
        </li>
        <li>
            <p>The catalog file maps OML IRIs to their corresponding file path.</p>
        </li>
        <li>
            <p>Build files contain dependencies of your OML project and side effects of analysis performed on your OML project using OML Rosetta.</p>
        </li>
    </ol>
    <p><strong>Steps</strong></p>
    <ol>
        <li>Download OML Rosetta and create a new workspace</li>
        <li>Right-click the created subfolder and select New -&gt; OML Model</li>
        <li>Create various Vocabularies, Vocabulary Bundles, Descriptions, and Description Bundles</li>
        <li>Run Gradle build tasks to check for logical consistency in your model</li>
        <li>Download the directory of your OML project</li>
    </ol>
    <p>Once you have an OML project directory, go to the PlantOML online server and upload your directory with the choose file button. You can edit any files you want straight from the browser. Then, pick options for how you want your graph generated and submit.</p>
    <p>For each file, we will generate an OML graph with the format of &lt;filename&gt;.png. (convert this into a nice HTML structure)</p>
        </div>);
}

export default GetStarted