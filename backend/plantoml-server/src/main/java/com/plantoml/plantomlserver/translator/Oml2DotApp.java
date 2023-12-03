package com.plantoml.plantomlserver.translator;

import java.io.*;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import io.opencaesar.oml.Ontology;
import io.opencaesar.oml.dsl.OmlStandaloneSetup;
import io.opencaesar.oml.resource.OmlJsonResourceFactory;
import io.opencaesar.oml.resource.OmlXMIResourceFactory;
import io.opencaesar.oml.util.OmlCatalog;
import io.opencaesar.oml.util.OmlRead;
import io.opencaesar.oml.validate.OmlValidator;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.util.ECrossReferenceAdapter;

public class Oml2DotApp {
    private static final Logger LOGGER = LoggerFactory.getLogger(Oml2DotApp.class);

    public Oml2DotApp(){
    }

    public String parse(String omlText) {

        LOGGER.info("=================================================================");
        LOGGER.info("                        S T A R T");
        LOGGER.info("                        Oml to Dot");
        LOGGER.info("=================================================================");

        // set up the OML infrastructure
        OmlStandaloneSetup.doSetup();
        OmlXMIResourceFactory.register();
        OmlJsonResourceFactory.register();
        final ResourceSet resourceSet = new ResourceSetImpl();
        resourceSet.eAdapters().add(new ECrossReferenceAdapterEx());

        // Write the OML text to a temporary file
        File tempFile = null;
        try {
            // tempFile = new File(System.getProperty("user.dir") + "/src/main/java/com/plantoml/plantomlserver/useromlprojectTutorial1/src/oml/example.com/tutorial1/description/restaurant.oml");
            tempFile = new File(System.getProperty("user.dir") + "/src/main/java/com/plantoml/plantomlserver/useromlprojectTutorial2/src/oml/example.com/tutorial2/description/components.oml");
            try (FileWriter writer = new FileWriter(tempFile)) {
                writer.write(omlText);
            }

            try {
                // final File inputCatalogFile = new File(System.getProperty("user.dir") + "/src/main/java/com/plantoml/plantomlserver/useromlprojectTutorial1/catalog.xml");
                final File inputCatalogFile = new File(System.getProperty("user.dir") + "/src/main/java/com/plantoml/plantomlserver/useromlprojectTutorial2/catalog.xml");
                final OmlCatalog inputCatalog = OmlCatalog.create(URI.createFileURI(inputCatalogFile.toString()));
                LOGGER.info("Catalog file loaded successfully. Resolved URIs:");
                for (URI uri : inputCatalog.getResolvedUris()) {
                    LOGGER.info(uri.toString());
                }
            } catch (IOException e) {
                LOGGER.error("Error parsing catalog file: " + e.getMessage(), e);
            }

            // Now load the resource from the file
            Resource resource = resourceSet.createResource(URI.createFileURI(tempFile.getAbsolutePath()));
            resource.load(null);
            LOGGER.info("Loaded ontologies:");
            for (Ontology o : OmlRead.getImportedOntologyClosure(OmlRead.getOntology(resource), false)) {
                LOGGER.info(o.getNamespace() + " as " + o.getPrefix());
            }
            
            // perform validation
            String validationResults = OmlValidator.validate(resource);
            System.out.println(resource.getURI());
            if (!validationResults.isEmpty()) {
                LOGGER.error("Validation errors in resource: " + validationResults);
                throw new IllegalStateException("Validation errors: " + validationResults);
            }

            // Convert to DOT format and clean up the temporary file
            String dotRepresentation = convertToDot(resource);
            LOGGER.info("Converted OML to DOT format: ");
            LOGGER.info(dotRepresentation);

            LOGGER.info("=================================================================");
            LOGGER.info("                          E N D ");
            LOGGER.info("                        Oml to Dot");
            LOGGER.info("=================================================================");

            return dotRepresentation;
        } catch (IOException e) {
            LOGGER.error("Error parsing OML text: ", e);
            return null; // Or handle the error as appropriate
        } finally {
            // Clean up the temporary file if it was created
            if (tempFile != null && tempFile.exists()) {
                tempFile.delete();
            }
        }
    }

    private String convertToDot(Resource resource) {
        Oml2Dot oml2Dot = new Oml2Dot();
        return oml2Dot.convert(resource);
    }

    // private byte[] convertToImage(Resource resource) {
    //     Oml2Dot oml2Dot = new Oml2Dot();
    //     return oml2Dot.convert(resource);
    // }

    private class ECrossReferenceAdapterEx extends ECrossReferenceAdapter {

        private Set<Resource> allResources = Collections.emptySet();

        public void setAllResources(Set<Resource> allResources) {
            this.allResources = allResources;
        }

        @Override
        public Collection<EStructuralFeature.Setting> getInverseReferences(EObject eObject) {
            var references = super.getInverseReferences(eObject);
            if (!allResources.isEmpty()) {
                for (var i = references.iterator(); i.hasNext();) {
                    var setting = i.next();
                    if (!allResources.contains(setting.getEObject().eResource())) {
                        i.remove();
                    }
                }
            }
            return references;
        }
    }
}
