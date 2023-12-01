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
    //instance orbiter : mission:Mission [
    //		base:hasIdentifier "M.01"
    //		base:hasCanonicalName "Orbiter Mission"
    //		mission:pursues objectives:characterize-atmosphere
    //		mission:pursues objectives:characterize-environment
    //		mission:pursues objectives:characterize-gravitational-field
    //	]
    public Void caseConceptInstance(final ConceptInstance instance) {

        dotBuilder.append(instance.getName()).append("\n");                                                          //orbiter
//            dotBuilder.append(instance.getAbbreviatedIri()).append("\n");
//            dotBuilder.append(instance.getIri()).append("\n");
//            dotBuilder.append(instance.getName()).append("\n");
//            dotBuilder.append(instance.getRef()).append("\n");
//            dotBuilder.append(instance.resolve()).append("\n");
//        dotBuilder.append("====++++=====\n");
//        List<PropertyValueAssertion> propertyValueAssertions = instance.getOwnedPropertyValues();
//        for (PropertyValueAssertion propertyValueAssertion : propertyValueAssertions) {
//            dotBuilder.append(propertyValueAssertion.getReferencedValue()).append("\n");
//        }


//        Set<Classifier> allTypes = this.omlSearch.findTypes(instance);
//
//        for (Classifier classifier : allTypes) {
//            // Append the classifier's information to the dotBuilder
////            dotBuilder.append(classifier.getAbbreviatedIri()).append("\n");
////            dotBuilder.append(classifier.getIri()).append("\n");
////            dotBuilder.append(classifier.getName()).append("\n");
////            dotBuilder.append(classifier.getRef()).append("\n");
////            dotBuilder.append(classifier.resolve()).append("\n");
////
////            dotBuilder.append(classifier.getOwnedEquivalences()).append("\n");
//            EList<PropertyRestrictionAxiom> propertyRestrictions = classifier.getOwnedPropertyRestrictions();
//            for (PropertyRestrictionAxiom axiom : propertyRestrictions) {
//                // Access the name or other properties of the axiom
//                SemanticProperty property = axiom.getProperty();
//                String propertyName = property.getName();
////                dotBuilder.append(propertyName).append("\n");
//            }
//        }
//        dotBuilder.append("====++++=====\n");

        EList<PropertyValueAssertion> propertyValueAssertions2 = instance.getOwnedPropertyValues();
        for (PropertyValueAssertion propertyValueAssertion : propertyValueAssertions2) {
            Literal tmp = propertyValueAssertion.getLiteralValue();
            if (tmp != null) {
                dotBuilder.append(tmp.getStringValue()).append("\n");                                   //M.01 + Orbiter Mission
            }

//            dotBuilder.append(propertyValueAssertion.getProperty().getName()).append("\n");
        }

//        dotBuilder.append("--\\/-----\n").append(this.omlSearch.
//        findAllTypes(instance)).append("\n--/\\--\n");

//        Set<Classifier> allTypes = this.omlSearch.findAllTypes(instance);

        // Start the section in the dotBuilder

        // Loop over each classifier in the set
//        for (Classifier classifier : allTypes) {
//            // Append the classifier's string representation to the dotBuilder
//            dotBuilder.append(classifier.getName()).append("\n");
//        }

        // End the section in the dotBuilder






//        dotBuilder.append(instance.getOwnedPropertyValues()).append("\n");

//        EList<TypeAssertion> ownedTypes = instance.getOwnedTypes();
//        for (TypeAssertion typeAssertion : ownedTypes) { //not it
//            dotBuilder.append(typeAssertion.getType().getName()).append("\n");
//        }
//
//        dotBuilder.append("++++++++++++++\n");
//
//        EList<Entity> entities = instance.getEntityTypes();
//        for (Entity entity : entities) { //not it
//            dotBuilder.append(entity.getName()).append("\n");
//        }
//
//        dotBuilder.append("++++++++++++++\n");
//
//        EList<Classifier> classifiers = instance.getTypes();
//        for (Classifier type : classifiers) { //not it
//            dotBuilder.append(type.getName()).append("\n");
//        }

        dotBuilder.append("==============\n");

        return null;
    }

    @Override
    public Void caseConcept(final Concept concept) {
        dotBuilder.append(concept.getAbbreviatedIri()).append("\n");
        return null;
    }




}
