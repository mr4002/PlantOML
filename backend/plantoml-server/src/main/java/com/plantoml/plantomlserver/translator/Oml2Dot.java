package com.plantoml.plantomlserver.translator;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.StreamSupport;

import io.opencaesar.oml.*;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;

import guru.nidi.graphviz.attribute.Arrow;
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

    /////////////////////////
    // OmlSwitch Overrides //
    /////////////////////////

    @Override
    public Boolean caseClassifier(Classifier classifier) {
        Set<Term> supers = OmlSearch.findSpecializationSuperTerms(classifier);
        classifier = classifier.isRef() ? (Classifier) classifier.getRef() : classifier;
        if (!edges.containsKey(classifier.getName()) && !supers.isEmpty()) {
            edges.put(classifier.getName(), new ArrayList<>());
        }
        for (Term t : supers) {
            Classifier c = (Classifier) t;
            // ideally I wouldn't make a new node but there's no guarantee that the c node exists yet
            Link edge = to(node(c.getName()));
            edge = edge.with(Arrow.EMPTY);
            edges.get(classifier.getName()).add(edge);

            // check if c comes from outside and needs a node
            if (c.getOntology() != classifier.getOntology() && !nodes.containsKey(c.getName())) {
                List<String> otherHeader = new ArrayList<>();
                if (c instanceof Structure) {
                    otherHeader.add("«structure»");
                } else if (c instanceof Aspect) {
                    otherHeader.add("«aspect»");
                } else if (c instanceof Concept) {
                    otherHeader.add("«concept»");
                } else { // must be a relation entity
                    otherHeader.add("«relation entity»");
                }
                otherHeader.add(c.getName());
                // just pick a default color
                nodes.put(c.getName(), tableNode("#97E87F", c.getName(), otherHeader, null));
            }
        }
        return true;
    }

    @Override
    public Boolean caseAspect(Aspect aspect) {
        Ontology ontology = aspect.getOntology();
        // find the original
        aspect = aspect.isRef() ? aspect.getRef() : aspect;
        // only add the node if it does not yet exist
        if (!nodes.containsKey(aspect.getName())) {
            // need all members from every ref in this ontology
            List<String> members = new ArrayList<>();
            if (aspect.getOntology() == ontology) {
                members.addAll(handleKeyAxioms(aspect));
                members.addAll(handlePropertyRestrictionAxioms(aspect));
            }
            for (Member m : OmlSearch.findRefs(aspect)) {
                if (m.getOntology() != ontology) continue;
                Aspect c = (Aspect) m;
                members.addAll(handleKeyAxioms(c));
                members.addAll(handlePropertyRestrictionAxioms(c));
            }
            // the header is generic
            List<String> header = new ArrayList<>();
            header.add("«aspect»");
            header.add(aspect.getName());
            Node instNode = tableNode("lightgrey", aspect.getName(), header, members);
            nodes.put(aspect.getName(), instNode);
        }
        return null; // intentionally let OmlSwitch call caseClassifier
    }

    @Override
    public Boolean caseConcept(Concept concept) {
        Ontology ontology = concept.getOntology();
        // find the original
        concept = concept.isRef() ? concept.getRef() : concept;
        // only add the node if it does not yet exist
        if (!nodes.containsKey(concept.getName())) {
            // need all members from every ref in this ontology
            List<String> members = new ArrayList<>();
            if (concept.getOntology() == ontology) {
                handleInstanceEnumerationAxiom(concept);
                members.addAll(handleKeyAxioms(concept));
                members.addAll(handlePropertyRestrictionAxioms(concept));
            }
            for (Member m : OmlSearch.findRefs(concept)) {
                if (m.getOntology() != ontology) continue;
                Concept c = (Concept) m;
                handleInstanceEnumerationAxiom(c);
                members.addAll(handleKeyAxioms(c));
                members.addAll(handlePropertyRestrictionAxioms(c));
            }
            // the header is generic
            List<String> header = new ArrayList<>();
            header.add("«concept»");
            header.add(concept.getName());
            Node instNode = tableNode("lightgrey", concept.getName(), header, members);
            nodes.put(concept.getName(), instNode);
        }
        return null; // intentionally let OmlSwitch call caseClassifier
    }

    public Boolean caseStructure(Structure structure) {
        // TODO
        return null; // intentionally let OmlSwitch call caseClassifier
    }

    @Override
    public Boolean caseRelationEntity(RelationEntity relation) {

        // TODO edges for to and from relationships

        Ontology ontology = relation.getOntology();
        // find the original
        relation = relation.isRef() ? relation.getRef() : relation;
        // only add the node if it does not yet exist
        if (!nodes.containsKey(relation.getName())) {
            // need all members from every ref in this ontology
            List<String> members = new ArrayList<>();
            if (relation.getOntology() == ontology) {
                members.addAll(handleKeyAxioms(relation));
                members.addAll(handlePropertyRestrictionAxioms(relation));
            }
            for (Member m : OmlSearch.findRefs(relation)) {
                if (m.getOntology() != ontology) continue;
                RelationEntity c = (RelationEntity) m;
                members.addAll(handleKeyAxioms(c));
                members.addAll(handlePropertyRestrictionAxioms(c));
            }
            // the header is generic
            List<String> header = new ArrayList<>();
            header.add("«relation entity»");
            header.add(relation.getName());
            Node instNode = tableNode("lightgrey", relation.getName(), header, members);
            nodes.put(relation.getName(), instNode);
        }
        return null; // intentionally let OmlSwitch call caseClassifier
    }

    @Override
//    instance orbiter : mission:Mission [
//          base:hasIdentifier "M.01"
//          base:hasCanonicalName "Orbiter Mission"
//          mission:pursues objectives:characterize-atmosphere
//          mission:pursues objectives:characterize-environment
//          mission:pursues objectives:characterize-gravitational-field
//      ]
    public Boolean caseConceptInstance(ConceptInstance instance) {
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
            linkNamedInstances(instance, objectSource, Label.of("«from»"));
        }

        for (NamedInstance objectTarget : instance.getTargets()) {              
            linkNamedInstances(instance, objectTarget, Label.of("«to»"));
        }

        Node instNode = tableNode("lightgrey", instance.getName(), header, members);
        nodes.put(instance.getName(), instNode);
        return true;
    }

    public Boolean caseStructureInstance(StructureInstance instance) {
        // TODO
        return null;
    }

    ///////////////////////////////////////////////////////////////////////
    // Handlers For Grammar Nodes That Do Not Really Work In An Override //
    ///////////////////////////////////////////////////////////////////////

    private void handleInstanceEnumerationAxiom(Concept concept) {
        // TODO
    }

    private List<String> handleKeyAxioms(Entity entity) {
        // TODO
        return new ArrayList<>();
    }

    private List<String> handlePropertyRestrictionAxioms(Classifier classifier) {
        // TODO
        return new ArrayList<>();
    }

    // handles all properties of an instance
    private List<String> handlePropertyValueAssertions(Instance in) {
        if (in instanceof NamedInstance) {
            NamedInstance instance = (NamedInstance) in;
            List<String> literals = new ArrayList<>();
            Set<PropertyValueAssertion> pvas = OmlSearch.findPropertyValueAssertionsWithSubject(instance);
            if (!edges.containsKey(instance.getName()) && !pvas.isEmpty()) {
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
                    linkNamedInstances(instance, pva.getReferencedValue(), Label.of("«" + pva.getProperty().getName() + "»"));
                }
            }
            return literals;
        }
        return null;
    }

    //////////////////////
    // Helper Functions //
    //////////////////////

    // helper function to make node that is a box
    // color applies to the background of the header
    // header(s) are not separated by solid lines
    // members(s) have white background and are separated by solid lines
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

    // creates a link between named instances
    private void linkNamedInstances(NamedInstance src, NamedInstance dst, Label label) {
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
            // .replace("'", "&apos;") // not mentioned on graphviz website
            .replace("\"", "&quot;")
            .replace("<", "&lt;")
            .replace(">", "&gt;");
    }

}