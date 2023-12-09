package com.plantoml.plantomlserver.translator;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
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

/**
 * This class is responsible for parsing OML files and converting them to DOT format.
 * It sets up the necessary OML infrastructure, validates the OML file, and then uses
 * the Oml2Dot class to perform the conversion.
 */
public class Oml2DotApp {
    private static final Logger LOGGER = LoggerFactory.getLogger(Oml2DotApp.class);

    /**
     * Constructor for Oml2DotApp.
     */
    public Oml2DotApp(){
    }

    /**
     * Parses the OML file at the given path and converts it to DOT format.
     *
     * @param omlFilePath The path to the OML file to be parsed.
     * @return A String representation of the DOT format of the given OML file.
     */
    public String parse(Path omlFilePath) {


        // set up the OML infrastructure
        OmlStandaloneSetup.doSetup();
        OmlXMIResourceFactory.register();
        OmlJsonResourceFactory.register();
        final ResourceSet resourceSet = new ResourceSetImpl();
        resourceSet.eAdapters().add(new ECrossReferenceAdapterEx());

        try {
            //load the OML file into the resource set
            URI fileUri = URI.createFileURI(omlFilePath.toString());
            Resource resource = resourceSet.createResource(fileUri);
            resource.load(null);

            //validation
            String validationResults = OmlValidator.validate(resource);
            if (!validationResults.isEmpty()) {
                System.out.println("ERROR VALIDATING: " + resource.getURI());
                LOGGER.error("Validation errors: " + validationResults);
                throw new IllegalStateException("Validation errors: " + validationResults);
            }

            //convert to DOT format
            String dotRepresentation = convertToDot(resource);
            LOGGER.info("Converted OML to DOT format: " + dotRepresentation);
            System.out.println("===================");
            System.out.println(resource.getURI());
            System.out.println(dotRepresentation);
            return dotRepresentation;

        } catch (IOException e) {
            LOGGER.error("Error parsing OML file: " + e.getMessage(), e);
            return null;
        }
    }

    /**
     * Converts the given resource to DOT format.
     *
     * @param resource The OML resource to be converted.
     * @return A String representation of the DOT format of the resource.
     */
    private String convertToDot(Resource resource) {
        Oml2Dot oml2Dot = new Oml2Dot();
        return oml2Dot.convert(resource);
    }

    /**
     * A custom ECrossReferenceAdapter extension that filters cross-references
     * to a specified set of resources.
     */
    private class ECrossReferenceAdapterEx extends ECrossReferenceAdapter {

        private Set<Resource> allResources = Collections.emptySet();

        /**
         * Sets the resources to be considered for cross-referencing.
         *
         * @param allResources A set of resources to be used for cross-referencing.
         */
        public void setAllResources(Set<Resource> allResources) {
            this.allResources = allResources;
        }

        /**
         * Returns the inverse references of an EObject, filtered by the set of resources.
         *
         * @param eObject The EObject whose inverse references are to be returned.
         * @return A collection of settings referencing the EObject.
         */
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