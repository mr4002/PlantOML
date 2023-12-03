package com.plantoml.plantomlserver.translator;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.StreamSupport;

import io.opencaesar.oml.*;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;

import guru.nidi.graphviz.attribute.Label;
import guru.nidi.graphviz.attribute.Rank;
import guru.nidi.graphviz.attribute.Shape;
import guru.nidi.graphviz.attribute.Rank.RankDir;
import guru.nidi.graphviz.engine.Format;
import guru.nidi.graphviz.engine.Graphviz;
import guru.nidi.graphviz.model.Graph;
import guru.nidi.graphviz.model.Link;
import guru.nidi.graphviz.model.Node;
import io.opencaesar.oml.util.OmlSearch;
import io.opencaesar.oml.util.OmlSwitch;

import static guru.nidi.graphviz.model.Factory.*;

public class Oml2Dot extends OmlSwitch<Boolean> {

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
    public Boolean caseAspect(final Aspect aspect) {
        List<String> header = new ArrayList<>();
        for (Term c : OmlSearch.findSuperTerms(aspect)) {
            header.add("«" + c.getAbbreviatedIri() + "»");
        }
        header.add(aspect.getName());

        List<String> members = handleKeyAxioms(aspect);
        members.addAll(handlePropertyRestrictionAxioms(aspect));

        Node instNode = tableNode("lightgrey", aspect.getName(), header, members);
        nodes.put(aspect.getName(), instNode);
        return true;
    }

    @Override
    public Boolean caseConcept(final Concept concept) {
        List<String> header = new ArrayList<>();
        for (Term c : OmlSearch.findSuperTerms(concept)) {
            header.add("«" + c.getAbbreviatedIri() + "»");
        }
        header.add(concept.getName());

        handleInstanceEnumerationAxiom(concept);
        List<String> members = handleKeyAxioms(concept);
        members.addAll(handlePropertyRestrictionAxioms(concept));

        Node instNode = tableNode("lightgrey", concept.getName(), header, members);
        nodes.put(concept.getName(), instNode);
        return true;
    }

    @Override
    public Boolean caseRelationEntity(RelationEntity relation) {
        List<String> header = new ArrayList<>();
        for (Term c : OmlSearch.findSuperTerms(relation)) {
            header.add("«" + c.getAbbreviatedIri() + "»");
        }
        header.add(relation.getName());

        List<String> members = handleKeyAxioms(relation);
        members.addAll(handlePropertyRestrictionAxioms(relation));

        // TODO to and from 

        Node instNode = tableNode("lightgrey", relation.getName(), header, members);
        nodes.put(relation.getName(), instNode);

        return true;
    }

    @Override
//    instance orbiter : mission:Mission [
//          base:hasIdentifier "M.01"
//          base:hasCanonicalName "Orbiter Mission"
//          mission:pursues objectives:characterize-atmosphere
//          mission:pursues objectives:characterize-environment
//          mission:pursues objectives:characterize-gravitational-field
//      ]
    public Boolean caseConceptInstance(final ConceptInstance instance) {
        List<String> header = new ArrayList<>();
        for (Classifier c : OmlSearch.findTypes(instance)) {
            header.add("«" + c.getAbbreviatedIri() + "»");
        }
        header.add(instance.getName());

        List<String> members = handlePropertyValueAssertions(instance);

        Node instNode = tableNode("lightgrey", instance.getName(), header, members);
        nodes.put(instance.getName(), instNode);
        return true;
    }

    @Override
    // relation instance orbiter-ground-data-system.orbiter-spacecraft.command.uplink : mission:Junction [
    //     from interfaces:orbiter-ground-data-system.commandOut
    //     to interfaces:orbiter-spacecraft.commandIn
    //     base:hasIdentifier "J.01"
    //     base:hasCanonicalName "Orbiter Command Uplink"
    // ]
    public Boolean caseRelationInstance(RelationInstance instance) {
        List<String> header = new ArrayList<>();
        for (Classifier c : OmlSearch.findTypes(instance)) {
            header.add("«" + c.getAbbreviatedIri() + "»");
        }
        header.add(instance.getName());

        List<String> members = handlePropertyValueAssertions(instance);

        for (NamedInstance objectSource: instance.getSources()) {
            linkToNamedInstance(instance, objectSource, Label.of("«from»"));
        }

        for (NamedInstance objectTarget : instance.getTargets()) {              
            linkToNamedInstance(instance, objectTarget, Label.of("«to»"));
        }

        Node instNode = tableNode("lightgrey", instance.getName(), header, members);
        nodes.put(instance.getName(), instNode);
        return true;
    }

    // helper function to make a green box node for nodes that come from other files
    private Node tableNode(String color, String name, List<String> headers, List<String> members) {
        // I wanted to use a DOT Record to build the table
        // but I couldn't find out how to get the formatting right
        StringBuilder table = new StringBuilder();
        table.append("<");
        table.append("<TABLE BORDER=\"0\" CELLBORDER=\"1\" CELLSPACING=\"0\">");
        if (headers != null) {
            table.append("<TR><TD BGCOLOR=\"" + xmlEscape(color) + "\" ALIGN=\"CENTER\">");
            for (int i = 0; i < headers.size(); i++) {
                if (i != 0) table.append("<BR ALIGN=\"CENTER\"/>");
                table.append(xmlEscape(headers.get(i)));
            }
            table.append("</TD></TR>");
        }
        if (members != null) {
            for (String member : members) {
                table.append("<TR><TD ALIGN=\"LEFT\">");
                table.append(xmlEscape(member));
                table.append("</TD></TR>");
            }
        } else {
            // // write a blank line to show no members
            // table.append("<TR><TD ALIGN=\"LEFT\"></TD></TR>\n");
        }
        table.append("</TABLE>");
        table.append(">");

        Node instNode = node(name);
        instNode = instNode.with(Label.raw(table.toString()));
        return instNode;
    }

    private void handleInstanceEnumerationAxiom(Concept concept) {
        // TODO
    }

    private List<String> handleKeyAxioms(Entity entity) {
        // TODO
        return null;
    }

    private List<String> handlePropertyRestrictionAxioms(Classifier classifier) {
        // TODO
        return null;
    }

    // handles all properties of an instance
    private List<String> handlePropertyValueAssertions(Instance in) {
        if (in instanceof NamedInstance) {
            NamedInstance instance = (NamedInstance) in;
            List<String> literals = new ArrayList<>();
            Set<PropertyValueAssertion> pvas = OmlSearch.findPropertyValueAssertionsWithSubject(instance);
            if (!pvas.isEmpty()) {
                edges.put(instance.getName(), new ArrayList<>());
            }
            for (PropertyValueAssertion pva : pvas) {
                Literal literal = pva.getLiteralValue();
                StructureInstance struct = pva.getContainedValue();
                if (literal != null) { // literal values belong in the table
                    literals.add(pva.getProperty().getAbbreviatedIri() + " : " + literal.getStringValue());
                } else if (struct != null) { // structure values belong outside the table
                    // TODO
                } else { // reference values belong outside the table
                    linkToNamedInstance(instance, pva.getReferencedValue(), Label.of("«" + pva.getProperty().getName() + "»"));
                }
            }
            return literals;
        }
        return null;
    }

    // creates a link to from a source
    private void linkToNamedInstance(NamedInstance src, NamedInstance dst, Label label) {
        // ideally I wouldn't make a new node but there's no guarantee that the dst node exists yet
        Link edge = to(node(dst.getName()));

        if (label != null) {
            edge = edge.with(label);
        }
        edges.get(src.getName()).add(edge);

        // check if dst comes from outside and needs a node
        if (src.getOntology() != dst.getOntology() && !nodes.containsKey(dst.getName())) {
            List<String> otherHeader = new ArrayList<>();
            if (dst instanceof ConceptInstance) {
                otherHeader.add("«concept instance»");
            } else { // must be relation instance
                otherHeader.add("«relation instance»");
            }
            otherHeader.add(dst.getName());
            // just pick a default color
            nodes.put(dst.getName(), tableNode("#97E87F", dst.getName(), otherHeader, null));
        }
    }

    private String xmlEscape(String original) {
        return original
            .replace("&", "&amp;")
            // .replace("'", "&apos") // not mentioned on graphviz website
            .replace("\"", "&quot;")
            .replace("<", "&lt;")
            .replace(">", "&gt;");
    }

}