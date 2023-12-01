package com.plantoml.plantomlserver.translator;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import io.opencaesar.oml.dsl.OmlStandaloneSetup;
import io.opencaesar.oml.resource.OmlJsonResourceFactory;
import io.opencaesar.oml.resource.OmlXMIResourceFactory;
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
            tempFile = File.createTempFile("oml_", ".oml");
            try (FileWriter writer = new FileWriter(tempFile)) {
                writer.write(omlText);
            }

            // Now load the resource from the file
            Resource resource = resourceSet.createResource(URI.createFileURI(tempFile.getAbsolutePath()));
            resource.load(null);

            // Convert to DOT format and clean up the temporary file
            String dotRepresentation = convertToDot(resource);
            LOGGER.info("Converted OML to DOT format: ");
            LOGGER.info(dotRepresentation);

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
//        // convert the string into an InputStream for EMF to process
//        ByteArrayInputStream inStream = new ByteArrayInputStream(omlText.getBytes(StandardCharsets.UTF_8));
//        // create a resource and load the OML text from the InputStream
//        Resource resource = resourceSet.createResource(URI.createURI("dummy:/example.oml"));
//        try {
//            resource.load(inStream, Collections.emptyMap());
//            LOGGER.info("Resource loaded: " + resource.toString());
//
//            //TODO: figure out why validation always fails
//            // perform validation if necessary
////            String validationResults = OmlValidator.validate(resource);
////            if (!validationResults.isEmpty()) {
////                LOGGER.error("Validation errors in resource: " + validationResults);
////                throw new IllegalStateException("Validation errors: " + validationResults);
////            }
//
//            // traverse the resource and convert to DOT format
//            String dotRepresentation = convertToDot(resource);
//            LOGGER.info("Converted OML to DOT format: ");
//            LOGGER.info( dotRepresentation);
//
//            LOGGER.info("=================================================================");
//            LOGGER.info("                          E N D ");
//            LOGGER.info("                        Oml to Dot");
//            LOGGER.info("=================================================================");
//
//            return dotRepresentation; //on success
//
//        } catch (Exception e) {
//            LOGGER.error("Error parsing OML text: " + e.getMessage(), e);
//        }
//
//        return null; //on error/failure
//    }

    private String convertToDot(Resource resource) {
        Oml2Dot oml2Dot = new Oml2Dot();
        return oml2Dot.convert(resource);
    }

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