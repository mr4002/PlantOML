package com.plantoml.plantomlserver.translator;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.StreamSupport;

import io.opencaesar.oml.*;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;

import guru.nidi.graphviz.attribute.Label;
import guru.nidi.graphviz.attribute.Rank;
import guru.nidi.graphviz.attribute.Records;
import guru.nidi.graphviz.attribute.Shape;
import guru.nidi.graphviz.attribute.Style;
import guru.nidi.graphviz.attribute.Rank.RankDir;
import guru.nidi.graphviz.engine.Format;
import guru.nidi.graphviz.engine.Graphviz;
import guru.nidi.graphviz.model.Graph;
import guru.nidi.graphviz.model.Link;
import guru.nidi.graphviz.model.Node;
import io.opencaesar.oml.util.OmlSwitch;

import static io.opencaesar.oml.util.OmlSearch.*;
import static guru.nidi.graphviz.model.Factory.*;

public class Oml2Dot extends OmlSwitch<Void> {

    private Graph dot;
    private Map<String, Node> nodes; // indexed by node name
    private Map<String, Collection<Link>> edges; // edges outgoing from each node

    public Oml2Dot() {
        nodes = new HashMap<>();
        edges = new HashMap<>();
    }

    public String convert(Resource inputResource) {
        // initialize the DOT graph (TODO: what is graph structure going to look like???)
        dot = graph("omldiagram")
            .directed()
        // TODO maybe allow these to be customized from the frontend
            .graphAttr().with(Rank.dir(RankDir.BOTTOM_TO_TOP))
            .nodeAttr().with(Shape.PLAIN_TEXT);


        // traverse all elements in the resource
        Iterable<EObject> iterable = () -> inputResource.getAllContents();
        StreamSupport.stream(iterable.spliterator(), false)
                .forEach(eObject -> doSwitch(eObject));

        // add nodes to dot graph
        nodes.forEach((name, node) -> dot = dot.with(node));
        // add edges to dot graph
        for (Entry<String, Collection<Link>> e : edges.entrySet()) {
            Node node = nodes.get(e.getKey());
            for (Link edge : e.getValue()) {
                dot = dot.with(node.link(edge));
            }
        }

        // finish DOT graph
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Graphviz.fromGraph(dot).width(200).render(Format.DOT).toOutputStream(baos);
            return baos.toString();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
//    instance orbiter : mission:Mission [
//          base:hasIdentifier "M.01"
//          base:hasCanonicalName "Orbiter Mission"
//          mission:pursues objectives:characterize-atmosphere
//          mission:pursues objectives:characterize-environment
//          mission:pursues objectives:characterize-gravitational-field
//      ]
    public Void caseConceptInstance(final ConceptInstance instance) {

        // I wanted to use a DOT Record to build the table
        // but I couldn't find out how to get the formatting right
        StringBuilder table = new StringBuilder();
        table.append("<");
        table.append("<TABLE BORDER=\"0\" CELLBORDER=\"1\" CELLSPACING=\"0\">");
        table.append("<TR><TD BGCOLOR=\"lightgrey\" ALIGN=\"CENTER\">");
        for (Classifier c : findTypes(instance)) {
            table.append("«" + c.getAbbreviatedIri() + "»");
            table.append("<BR ALIGN=\"CENTER\"/>");
        }
        table.append(instance.getName());
        table.append("</TD></TR>");

        // members of this instance
        Set<PropertyValueAssertion> pvas = findPropertyValueAssertionsWithSubject(instance);
        if (!pvas.isEmpty()) {
            edges.put(instance.getName(), new ArrayList<>());
        }
        for (PropertyValueAssertion pva : pvas) {
            Literal literal = pva.getLiteralValue();
            StructureInstance struct = pva.getContainedValue();
            if (literal != null) { // literal values belong in the table
                table.append("<TR><TD ALIGN=\"LEFT\">");
                table.append(pva.getProperty().getAbbreviatedIri() + " : " + literal.getStringValue());
                table.append("</TD></TR>\n");
            } else if (struct != null) { // structure values belong outside the table
                // TODO
            } else { // reference values belong outside the table
                NamedInstance other = pva.getReferencedValue();
                Link edge = to(node(other.getName())); // ideally I wouldn't make a new node but there's no guarantee that the other node exists yet
                edge = edge.with(Label.of("«" + pva.getProperty().getName() + "»"));
                edges.get(instance.getName()).add(edge);
                // check if other comes from outside and needs a node
                if (other.getOntology() != instance.getOntology() && !nodes.containsKey(other.getName())) {
                    nodes.put(other.getName(), outsideNamedInstance(other));
                }
            }
        }

        table.append("</TABLE>");
        table.append(">");

        Node instNode = node(instance.getName());
        instNode = instNode.with(Shape.RECORD);
        instNode = instNode.with(Label.raw(table.toString()));
        nodes.put(instance.getName(), instNode);
        return null;
    }


    // @Override
//    relation instance orbiter-ground-data-system.orbiter-spacecraft.command.uplink : mission:Junction [
//      from interfaces:orbiter-ground-data-system.commandOut
//      to interfaces:orbiter-spacecraft.commandIn
//      base:hasIdentifier "J.01"
//      base:hasCanonicalName "Orbiter Command Uplink"
//    ]
//     public Void caseRelationInstance(RelationInstance object) {

//         String instanceName = object.getName().replace("&", "&amp;");
//         dotBuilder.append("\"" + instanceName).append("\" [label=<\n");                                                 //orbiter-ground-data-system.orbiter-spacecraft.command.uplink


//         dotBuilder.append("<TABLE BORDER=\"0\" CELLBORDER=\"1\" CELLSPACING=\"0\">\n<TR><TD BGCOLOR=\"lightgrey\" ALIGN=\"CENTER\">");
//         for (Classifier c : findTypes(object)) {
//             dotBuilder.append("«").append(c.getAbbreviatedIri()).append("»");                   ////mission:Junction              NOTE: think kepler 16b is typo in reference graph
//         }

//         dotBuilder.append("<BR\n ALIGN=\"CENTER\"/>").append(instanceName);


//         dotBuilder.append("</TD></TR>\n");
//         Set<PropertyValueAssertion> pvas = findPropertyValueAssertionsWithSubject(object);
//         for (PropertyValueAssertion pva : pvas) {
//             Literal tmp = pva.getLiteralValue();
//             if (tmp != null) {                                                                                           //base:hasIdentifier "J.01"
//                 dotBuilder.append("<TR><TD ALIGN=\"LEFT\">").append((pva.getProperty().getAbbreviatedIri() + " : " + tmp.getStringValue()).replace("&", "&amp;")).append("</TD></TR>\n");
//             } else { //this does nothing for this example but likely catches a corner case
//                 String referencedValName = pva.getReferencedValue().getName().replace("&", "&amp;");
//                 edgeBuilder.append("\"" + instanceName + "\"")
//                         .append(" -> \"")
//                         .append(referencedValName.replace("-", "_"))
//                         .append("\" [label=\"")
//                         .append("«")
//                         .append(pva.getProperty().getName())
//                         .append("»\"];\n");

//                 conceptInstanceBuilder
//                         .append("\"" + referencedValName.replace("-", "_") + "\"").append(" [label=<<TABLE BORDER=\"0\" CELLBORDER=\"1\" CELLSPACING=\"0\" BGCOLOR=\"green\">\n")
//                         .append("<TR><TD ALIGN=\"CENTER\">«concept instance»<BR ALIGN=\"CENTER\"/>").append(referencedValName).append("</TD></TR>\n")
//                         .append("<TR><TD ALIGN=\"LEFT\"></TD></TR>\n")
//                         .append("</TABLE>>];\n");
//             }
//         }

//         EList<NamedInstance> objectSources =  object.getSources();
//         for (NamedInstance objectSource: objectSources) {                                                               //orbiter-ground-data-system.commandOut
//             String referencedValName = objectSource.getName();
//             conceptInstanceBuilder
//                     .append("\"" + referencedValName.replace("-", "_") + "\"").append(" [label=<<TABLE BORDER=\"0\" CELLBORDER=\"1\" CELLSPACING=\"0\" BGCOLOR=\"green\">\n")
//                     .append("<TR><TD ALIGN=\"CENTER\">«concept instance»<BR ALIGN=\"CENTER\"/>").append(referencedValName).append("</TD></TR>\n")
//                     .append("<TR><TD ALIGN=\"LEFT\"></TD></TR>\n")
//                     .append("</TABLE>>];\n");
//             edgeBuilder.append("\"" + instanceName + "\"")
//                     .append(" -> \"")
//                     .append(referencedValName.replace("-", "_"))
//                     .append("\" [label=\"")
//                     .append("«from»\"];\n");
//         }

//         EList<NamedInstance> objectTargets =  object.getTargets();
//         for (NamedInstance objectTarget: objectTargets) {                                                               //orbiter-spacecraft.commandIn
//             String referencedValName = objectTarget.getName();
//             conceptInstanceBuilder
//                     .append("\"" + referencedValName.replace("-", "_") + "\"").append(" [label=<<TABLE BORDER=\"0\" CELLBORDER=\"1\" CELLSPACING=\"0\" BGCOLOR=\"green\">\n")
//                     .append("<TR><TD ALIGN=\"CENTER\">«concept instance»<BR ALIGN=\"CENTER\"/>").append(referencedValName).append("</TD></TR>\n")
//                     .append("<TR><TD ALIGN=\"LEFT\"></TD></TR>\n")
//                     .append("</TABLE>>];\n");
//             edgeBuilder.append("\"" + instanceName + "\"")
//                     .append(" -> \"")
//                     .append(referencedValName.replace("-", "_"))
//                     .append("\" [label=\"")
//                     .append("«from»\"];\n");
//         }

//         dotBuilder.append("</TABLE>>];\n\n");

//         return null;
//     }


// }

    // helper function to make a green box node for nodes that come from other files
    private Node outsideNamedInstance(NamedInstance instance) {
        StringBuilder table = new StringBuilder();

        table.append("<");
        table.append("<TABLE BORDER=\"0\" CELLBORDER=\"1\" CELLSPACING=\"0\">");
        table.append("<TR><TD BGCOLOR=\"green\" ALIGN=\"CENTER\">");
        if (instance instanceof ConceptInstance) {
            table.append("«concept instance»");
        } else { // must be relation instance
            table.append("«relation instance»");
        }
        table.append("<BR ALIGN=\"CENTER\"/>");
        table.append(instance.getName());
        table.append("</TD></TR>");
        table.append("<TR><TD ALIGN=\"LEFT\"></TD></TR>\n");
        table.append("</TABLE>");
        table.append(">");

        Node instNode = node(instance.getName());
        instNode = instNode.with(Shape.RECORD);
        instNode = instNode.with(Label.raw(table.toString()));
        return instNode;
    }

}