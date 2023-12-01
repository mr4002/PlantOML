package com.plantoml.plantomlserver.translator;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import io.opencaesar.oml.*;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;

import io.opencaesar.oml.util.OmlSwitch;
import io.opencaesar.oml.util.OmlSearch;

import io.opencaesar.oml.util.OmlRead;



public class Oml2Dot extends OmlSwitch<Void> {

    private StringBuilder dotBuilder;

    private OmlSearch omlSearch;

    public Oml2Dot() {
        this.dotBuilder = new StringBuilder();
        omlSearch = new OmlSearch();
    }

    public String convert(Resource inputResource) {
        // initialize the DOT graph (TODO: what is graph structure going to look like???)
        dotBuilder.append("digraph G {\n");
        dotBuilder.append("node [shape=plaintext];\nrankdir=BT;\n");

        // traverse all elements in the resource
        Iterable<EObject> iterable = () -> inputResource.getAllContents();
        StreamSupport.stream(iterable.spliterator(), false)
                .forEach(eObject -> doSwitch(eObject));

        // finish DOT graph
        dotBuilder.append("}\n");
        return dotBuilder.toString();
    }


    @Override
    //description <http://example.com/tutorial2/description/missions#> as missions {
    public Void caseDescription(final Description description) {
//        dotBuilder.append("digraph ").append(description.getIri() ).append(" {\n");                   //<http://example.com/tutorial2/description/missions#>
        dotBuilder.append(description.getPrefix()).append("\n");                                        //missions

        return null;
    }

    @Override
//    instance orbiter : mission:Mission [
//    		base:hasIdentifier "M.01"
//    		base:hasCanonicalName "Orbiter Mission"
//    		mission:pursues objectives:characterize-atmosphere
//    		mission:pursues objectives:characterize-environment
//    		mission:pursues objectives:characterize-gravitational-field
//    	]
    public Void caseConceptInstance(final ConceptInstance instance) {

        for (Classifier c : OmlSearch.findTypes(instance)) {
            dotBuilder.append("<<").append(c.getAbbreviatedIri()).append(">>\n");
        }
        dotBuilder.append(instance.getName()).append("\n");

        Set<PropertyValueAssertion> propertyValueAssertions =  omlSearch.findPropertyValueAssertionsWithSubject(instance);
        for (PropertyValueAssertion propertyValueAssertion : propertyValueAssertions) {
            Literal tmp = propertyValueAssertion.getLiteralValue(); //TODO: use omlSearch as this fails occasionally when tmp is null
            if (tmp != null) {
                dotBuilder.append(propertyValueAssertion.getProperty().getAbbreviatedIri()).append(" : ");
                dotBuilder.append(tmp.getStringValue()).append("\n");                                   //base:hasIdentifier "M.01" + base:hasCanonicalName "Orbiter Mission"
            } else {
                dotBuilder.append(propertyValueAssertion.getReferencedValue().getName()).append("\n"); //objectives:characterize-environment
                dotBuilder.append("<<").append(propertyValueAssertion.getProperty().getName()).append(">>\n");                  //pursues
            }

        }

        dotBuilder.append("==============\n");

        return null;
    }

    @Override
    public Void caseConcept(final Concept concept) {
        dotBuilder.append(concept.getAbbreviatedIri()).append("\n");
        return null;
    }

}
