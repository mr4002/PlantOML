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
import guru.nidi.graphviz.attribute.Attributes;
import guru.nidi.graphviz.attribute.ForLink;
import guru.nidi.graphviz.attribute.GraphAttr;
import guru.nidi.graphviz.attribute.Label;
import guru.nidi.graphviz.attribute.Rank;
import guru.nidi.graphviz.attribute.Shape;
import guru.nidi.graphviz.attribute.GraphAttr.SplineMode;
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
        System.out.println(inputResource.getURI().toFileString());
        // initialize the DOT graph (TODO: what is graph structure going to look like???)
        dot = graph("omldiagram")
<<<<<<< HEAD
                .directed()
                // TODO maybe allow these to be customized from the frontend
                .graphAttr().with(Rank.dir(RankDir.BOTTOM_TO_TOP), GraphAttr.splines(SplineMode.SPLINE))
                .nodeAttr().with(Shape.PLAIN_TEXT);

=======
            .directed()
        // TODO maybe allow these to be customized from the frontend
            .graphAttr().with(Rank.dir(RankDir.BOTTOM_TO_TOP), GraphAttr.splines(SplineMode.SPLINE))
            .nodeAttr().with(Shape.PLAIN_TEXT);
            
>>>>>>> 35e8ee2f2949de409090a15661be02984928aa13
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
            Graphviz.fromGraph(dot).render(Format.DOT).toOutputStream(baos);
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
        if (classifier.isRef()) return true;
        Set<Term> supers = OmlSearch.findSpecializationSuperTerms(classifier);
        for (Term t : supers) {
            linkMembers(classifier, t, Arrow.EMPTY);
        }
        return true;
    }

    @Override
    public Boolean caseAspect(Aspect aspect) {
        if (aspect.isRef()) return true;
        Ontology ontology = aspect.getOntology();
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
            tableNode("lightgrey", aspect.getName(), header, members);
        }
        return null; // intentionally let OmlSwitch call caseClassifier
    }

    @Override
    public Boolean caseConcept(Concept concept) {
        if (concept.isRef()) return true;
        Ontology ontology = concept.getOntology();
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
            tableNode("lightgrey", concept.getName(), header, members);
        }
        return null; // intentionally let OmlSwitch call caseClassifier
    }

    public Boolean caseStructure(Structure structure) {
        // TODO
        return null; // intentionally let OmlSwitch call caseClassifier
    }

    @Override
    public Boolean caseRelationEntity(RelationEntity relation) {
        if (relation.isRef()) return true;
        List<Entity> sources = relation.getSources();
        List<Entity> targets = relation.getTargets();
        Ontology ontology = relation.getOntology();

        // find a suitable edge label
        String edgeName = null;
        if (relation.getForwardRelation() != null) {
            edgeName = relation.getForwardRelation().getName();
        }
        if (edgeName == null) edgeName = pascalCaseToSpaced(relation.getName());
        Label l = Label.of(edgeName);

        // link all source-target pairs
        if (sources != null && targets != null) {
            for (Entity s : sources) {
                addOutsideNodeIfNecessary(s, ontology);
                for (Entity t : targets) {
                    linkMembers(s, t, l);
                }
            }
        }

        // below is for if RelationEntities should be their own nodes rather than just arrows

        // List<String> members = new ArrayList<>();
        // // only add the node if it does not yet exist
        // if (!nodes.containsKey(relation.getName())) {
        //     // need all members from every ref in this ontology
        //     if (relation.getOntology() == ontology) {
        //         members.addAll(handleKeyAxioms(relation));
        //         members.addAll(handlePropertyRestrictionAxioms(relation));
        //     }
        //     for (Member m : OmlSearch.findRefs(relation)) {
        //         if (m.getOntology() != ontology) continue;
        //         RelationEntity c = (RelationEntity) m;
        //         members.addAll(handleKeyAxioms(c));
        //         members.addAll(handlePropertyRestrictionAxioms(c));
        //     }
        //     // the header is generic
        //     List<String> header = new ArrayList<>();
        //     header.add("«relation entity»");
        //     header.add(relation.getName());
        //     tableNode("lightgrey", relation.getName(), header, members);
        // }

        return true; // intentionally do not let OmlSwitch call caseClassifier
    }

    @Override
    public Boolean caseConceptInstance(ConceptInstance instance) {
        if (instance.isRef()) return true;
        List<String> header = new ArrayList<>();
        for (Classifier c : OmlSearch.findTypes(instance)) {
            header.add("«" + c.getAbbreviatedIri() + "»");
        }
        header.add(instance.getName());
        List<String> members = handlePropertyValueAssertions(instance);
        tableNode("lightgrey", instance.getName(), header, members);
        return true;
    }

    @Override
    public Boolean caseRelationInstance(RelationInstance instance) {
        if (instance.isRef()) return true;
        // generate node
        List<String> header = new ArrayList<>();
        for (Classifier c : OmlSearch.findTypes(instance)) {
            header.add("«" + c.getAbbreviatedIri() + "»");
        }
        header.add(instance.getName());
        List<String> members = handlePropertyValueAssertions(instance);
        tableNode("lightgrey", instance.getName(), header, members);

        // generate edges
        for (NamedInstance objectSource: instance.getSources()) {
            linkMembers(instance, objectSource, Label.of("«from»"));
        }
        for (NamedInstance objectTarget : instance.getTargets()) {
            linkMembers(instance, objectTarget, Label.of("«to»"));
        }

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
            initializeEdgesIfNecessary(instance.getName());
            for (PropertyValueAssertion pva : pvas) {
                Literal literal = pva.getLiteralValue();
                StructureInstance struct = pva.getContainedValue();
                if (literal != null) { // literal values belong in the table
                    literals.add(pva.getProperty().getAbbreviatedIri() + " : " + literal.getStringValue());
                } else if (struct != null) { // structure values belong outside the table
                    // TODO
                } else { // reference values belong outside the table
                    linkMembers(instance, pva.getReferencedValue(), Label.of("«" + pva.getProperty().getName() + "»"));
                }
            }
            return literals;
        }
        return null;
    }

    //////////////////////
    // Helper Functions //
    //////////////////////

    void initializeEdgesIfNecessary(String node) {
        if (!edges.containsKey(node)) {
            edges.put(node, new ArrayList<>());
        }
    }

    // check if member comes from outside the ontology and
    // if so make sure that it has a node in the graph
    void addOutsideNodeIfNecessary(Member member, Ontology compareTo) {
        if (member.getOntology() != compareTo && !nodes.containsKey(member.getName())) {
            List<String> otherHeader = new ArrayList<>();
            String c = member.getClass().toString();
            c = c.substring(c.lastIndexOf('.')+1, c.length()-4);
            otherHeader.add("«" + pascalCaseToSpaced(c) + "»");
            otherHeader.add(member.getName());
            // just pick a default color
            tableNode("#97E87F", member.getName(), otherHeader, null);
        }
    }

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
        nodes.put(name, instNode);
        initializeEdgesIfNecessary(name);
        return instNode;
    }



    // creates a link between members
    @SafeVarargs
    private void linkMembers(Member src, Member dst, Attributes<? extends ForLink>... attributes) {
        // ideally I wouldn't make a new node but there's no guarantee that the dst node exists yet
        Link edge = to(node(dst.getName()));
        if (attributes != null) {
            edge = edge.with(attributes);
        }
        edges.get(src.getName()).add(edge);
        addOutsideNodeIfNecessary(dst, src.getOntology());
    }

    private String xmlEscape(String original) {
        return original
                .replace("&", "&amp;")
                // .replace("'", "&apos;") // not mentioned on graphviz website
                .replace("\"", "&quot;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    public String pascalCaseToSpaced(String str) {
        // Regular Expression
        String regex = "([a-z])([A-Z]+)";

        // Replacement string
        String replacement = "$1 $2";

        // Replace the given regex
        // with replacement string
        // and convert it to lower case.
        return str.replaceAll(regex, replacement).toLowerCase();
    }

}