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

public class Oml2DotApp {
    private static final Logger LOGGER = LoggerFactory.getLogger(Oml2DotApp.class);

    public Oml2DotApp(){
    }


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
//                LOGGER.error("Validation errors: " + validationResults);
                throw new IllegalStateException("Validation errors: " + validationResults);
            }

            //convert to DOT format
            String dotRepresentation = convertToDot(resource);
//            LOGGER.info("Converted OML to DOT format: " + dotRepresentation);
            System.out.println("===================");
            System.out.println(resource.getURI());
            System.out.println(dotRepresentation);
            return dotRepresentation;

        } catch (IOException e) {
//            LOGGER.error("Error parsing OML file: " + e.getMessage(), e);
            return null;
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