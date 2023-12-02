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
    private StringBuilder edgeBuilder;
    private StringBuilder conceptInstanceBuilder;

    private OmlSearch omlSearch;

    public Oml2Dot() {
        dotBuilder = new StringBuilder();
        edgeBuilder = new StringBuilder();
        conceptInstanceBuilder = new StringBuilder();
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

        //add concepts to dot graphh
        dotBuilder.append(conceptInstanceBuilder);
        //add edges to dot graph
        dotBuilder.append(edgeBuilder);

        // finish DOT graph
        dotBuilder.append("}\n");
        return dotBuilder.toString();
    }


    @Override
    //description <http://example.com/tutorial2/description/missions#> as missions {
    public Void caseDescription(final Description description) {
//        dotBuilder.append("digraph ").append(description.getIri() ).append(" {\n");                   //<http://example.com/tutorial2/description/missions#>
//        dotBuilder.append(description.getPrefix()).append("\n");                                        //missions

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

        String instanceName = instance.getName().replace("&", "&amp;");                                                   //orbiter
        dotBuilder.append("\"" + instanceName).append("\" [label=<\n");

        dotBuilder.append("<TABLE BORDER=\"0\" CELLBORDER=\"1\" CELLSPACING=\"0\">\n" +
                "<TR><TD BGCOLOR=\"lightgrey\" ALIGN=\"CENTER\">");
        for (Classifier c : OmlSearch.findTypes(instance)) {
            dotBuilder.append("«").append(c.getAbbreviatedIri()).append("»");                   //<<mission:Mission>>
        }

        dotBuilder.append("<BR\n ALIGN=\"CENTER\"/>").append(instanceName);

        dotBuilder.append("</TD></TR>\n");
        Set<PropertyValueAssertion> propertyValueAssertions =  omlSearch.findPropertyValueAssertionsWithSubject(instance);
        for (PropertyValueAssertion propertyValueAssertion : propertyValueAssertions) {
            Literal tmp = propertyValueAssertion.getLiteralValue();
            if (tmp != null) {
                dotBuilder.append("<TR><TD ALIGN=\"LEFT\">").append((propertyValueAssertion.getProperty().getAbbreviatedIri() + " : " + tmp.getStringValue()).replace("&", "&amp;")).append("</TD></TR>\n");
//                dotBuilder.append(propertyValueAssertion.getProperty().getAbbreviatedIri()).append(" : ");
//                dotBuilder.append(tmp.getStringValue()).append("\n");                                   //base:hasIdentifier "M.01" + base:hasCanonicalName "Orbiter Mission"

            } else {
                String referencedValName = propertyValueAssertion.getReferencedValue().getName().replace("&", "&amp;");
                edgeBuilder.append("\"" + instanceName + "\"")
                        .append(" -> \"")
                        .append(referencedValName.replace("-", "_"))
                        .append("\" [label=\"")
                        .append("«")
                        .append(propertyValueAssertion.getProperty().getName())
                        .append("»\"];\n");

                conceptInstanceBuilder
                        .append("\"" + referencedValName.replace("-", "_") + "\"").append(" [label=<<TABLE BORDER=\"0\" CELLBORDER=\"1\" CELLSPACING=\"0\" BGCOLOR=\"green\">\n")
                        .append("<TR><TD ALIGN=\"CENTER\">«concept instance»<BR ALIGN=\"CENTER\"/>").append(referencedValName).append("</TD></TR>\n")
                        .append("<TR><TD ALIGN=\"LEFT\"></TD></TR>\n")
                        .append("</TABLE>>];\n");
//                dotBuilder.append(propertyValueAssertion.getReferencedValue().getName()).append("\n");                          //objectives:characterize-environment
//                dotBuilder.append("<<").append(propertyValueAssertion.getProperty().getName()).append(">>\n");                  //pursues
            }

        }

        dotBuilder.append("</TABLE>>];\n\n");

        return null;
    }


    @Override
//    relation instance orbiter-ground-data-system.orbiter-spacecraft.command.uplink : mission:Junction [
//      from interfaces:orbiter-ground-data-system.commandOut
//      to interfaces:orbiter-spacecraft.commandIn
//      base:hasIdentifier "J.01"
//      base:hasCanonicalName "Orbiter Command Uplink"
//    ]
    public Void caseRelationInstance(RelationInstance object) {

        String instanceName = object.getName().replace("&", "&amp;");
        dotBuilder.append("\"" + instanceName).append("\" [label=<\n");                                                 //orbiter-ground-data-system.orbiter-spacecraft.command.uplink


        dotBuilder.append("<TABLE BORDER=\"0\" CELLBORDER=\"1\" CELLSPACING=\"0\">\n<TR><TD BGCOLOR=\"lightgrey\" ALIGN=\"CENTER\">");
        for (Classifier c : OmlSearch.findTypes(object)) {
            dotBuilder.append("«").append(c.getAbbreviatedIri()).append("»");                   ////mission:Junction              NOTE: think kepler 16b is typo in reference graph
        }

        dotBuilder.append("<BR\n ALIGN=\"CENTER\"/>").append(instanceName);


        dotBuilder.append("</TD></TR>\n");
        Set<PropertyValueAssertion> propertyValueAssertions =  omlSearch.findPropertyValueAssertionsWithSubject(object);
        for (PropertyValueAssertion propertyValueAssertion : propertyValueAssertions) {
            Literal tmp = propertyValueAssertion.getLiteralValue();
            if (tmp != null) {                                                                                           //base:hasIdentifier "J.01"
                dotBuilder.append("<TR><TD ALIGN=\"LEFT\">").append((propertyValueAssertion.getProperty().getAbbreviatedIri() + " : " + tmp.getStringValue()).replace("&", "&amp;")).append("</TD></TR>\n");
            } else { //this does nothing for this example but likely catches a corner case
                String referencedValName = propertyValueAssertion.getReferencedValue().getName().replace("&", "&amp;");
                edgeBuilder.append("\"" + instanceName + "\"")
                        .append(" -> \"")
                        .append(referencedValName.replace("-", "_"))
                        .append("\" [label=\"")
                        .append("«")
                        .append(propertyValueAssertion.getProperty().getName())
                        .append("»\"];\n");

                conceptInstanceBuilder
                        .append("\"" + referencedValName.replace("-", "_") + "\"").append(" [label=<<TABLE BORDER=\"0\" CELLBORDER=\"1\" CELLSPACING=\"0\" BGCOLOR=\"green\">\n")
                        .append("<TR><TD ALIGN=\"CENTER\">«concept instance»<BR ALIGN=\"CENTER\"/>").append(referencedValName).append("</TD></TR>\n")
                        .append("<TR><TD ALIGN=\"LEFT\"></TD></TR>\n")
                        .append("</TABLE>>];\n");
            }
        }

        EList<NamedInstance> objectSources =  object.getSources();
        for (NamedInstance objectSource: objectSources) {                                                               //orbiter-ground-data-system.commandOut
            String referencedValName = objectSource.getName();
            conceptInstanceBuilder
                    .append("\"" + referencedValName.replace("-", "_") + "\"").append(" [label=<<TABLE BORDER=\"0\" CELLBORDER=\"1\" CELLSPACING=\"0\" BGCOLOR=\"green\">\n")
                    .append("<TR><TD ALIGN=\"CENTER\">«concept instance»<BR ALIGN=\"CENTER\"/>").append(referencedValName).append("</TD></TR>\n")
                    .append("<TR><TD ALIGN=\"LEFT\"></TD></TR>\n")
                    .append("</TABLE>>];\n");
            edgeBuilder.append("\"" + instanceName + "\"")
                    .append(" -> \"")
                    .append(referencedValName.replace("-", "_"))
                    .append("\" [label=\"")
                    .append("«from»\"];\n");
        }

        EList<NamedInstance> objectTargets =  object.getTargets();
        for (NamedInstance objectTarget: objectTargets) {                                                               //orbiter-spacecraft.commandIn
            String referencedValName = objectTarget.getName();
            conceptInstanceBuilder
                    .append("\"" + referencedValName.replace("-", "_") + "\"").append(" [label=<<TABLE BORDER=\"0\" CELLBORDER=\"1\" CELLSPACING=\"0\" BGCOLOR=\"green\">\n")
                    .append("<TR><TD ALIGN=\"CENTER\">«concept instance»<BR ALIGN=\"CENTER\"/>").append(referencedValName).append("</TD></TR>\n")
                    .append("<TR><TD ALIGN=\"LEFT\"></TD></TR>\n")
                    .append("</TABLE>>];\n");
            edgeBuilder.append("\"" + instanceName + "\"")
                    .append(" -> \"")
                    .append(referencedValName.replace("-", "_"))
                    .append("\" [label=\"")
                    .append("«from»\"];\n");
        }

        dotBuilder.append("</TABLE>>];\n\n");

        return null;
    }




}
