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

/**
 * Converts Ontological Modeling Language (OML) elements to DOT format for graph visualization.
 * <p>
 * This class traverses OML elements and constructs a graph representation using the DOT language,
 * suitable for rendering with Graphviz.
 */
public class Oml2Dot extends OmlSwitch<Boolean> {

    private Graph dot;
    private Map<String, Node> nodes; // indexed by node name
    private Map<String, Collection<Link>> edges; // edges outgoing from each node

    /**
     * Constructor for Oml2Dot.
     * Initializes internal structures for nodes and edges.
     */
    public Oml2Dot() {
        nodes = new HashMap<>();
        edges = new HashMap<>();
    }

    /**
     * Converts the contents of an EMF Resource containing OML data to a DOT format graph.
     * <p>
     * This method iterates through all elements within the provided EMF Resource and builds
     * a graph representation in DOT format.
     *
     * @param inputResource The EMF Resource containing OML data.
     * @return A String representation of the graph in DOT format.
     */
    public String convert(Resource inputResource) {
        System.out.println(inputResource.getURI().toFileString());
        // initialize the DOT graph (TODO: what is graph structure going to look like???)
        dot = graph("omldiagram")
            .directed()
        // TODO maybe allow these to be customized from the frontend
            .graphAttr().with(Rank.dir(RankDir.BOTTOM_TO_TOP), GraphAttr.splines(SplineMode.SPLINE))
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

    /**
     * Processes Classifier elements in OML.
     * <p>
     * This method handles Classifier elements and establishes links with their super terms,
     * contributing to the construction of the graph.
     *
     * @param classifier The Classifier element to process.
     * @return true if the element was processed successfully, false otherwise.
     */
    @Override
    public Boolean caseClassifier(Classifier classifier) {
        if (classifier.isRef()) return true;
        Set<Term> supers = OmlSearch.findSpecializationSuperTerms(classifier);
        for (Term t : supers) {
            linkMembers(classifier, t, Arrow.EMPTY);
        }
        return true;
    }

    /**
     * Processes Aspect elements in OML.
     * <p>
     * This method handles Aspect elements, creating graph nodes for them and linking them
     * with other related elements. It calls other methods to handle specific aspects of the Aspect,
     * such as key axioms and property restrictions.
     *
     * @param aspect The Aspect element to process.
     * @return null, indicating that the parent Classifier processing should be invoked.
     */
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

    /**
     * Processes Concept elements in OML.
     * <p>
     * This method handles Concept elements, creating graph nodes and linking them with their super terms
     * and related aspects, as well as handling key axioms and property restrictions.
     *
     * @param concept The Concept element to process.
     * @return null, indicating that the parent Classifier processing should be invoked.
     */
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

    /**
     * Processes Structure elements in OML.
     * <p>
     * TODO: Provide implementation details once available.
     *
     * @param structure The Structure element to process.
     * @return null, indicating that the parent Classifier processing should be invoked.
     */
    public Boolean caseStructure(Structure structure) {
        // TODO
        return null; // intentionally let OmlSwitch call caseClassifier
    }

    /**
     * Processes RelationEntity elements in OML.
     * <p>
     * This method deals with RelationEntity elements, linking source and target entities and setting
     * appropriate labels for edges in the graph.
     *
     * @param relation The RelationEntity element to process.
     * @return true if the element was processed successfully, false otherwise.
     */
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

    /**
     * Processes ConceptInstance elements in OML.
     * <p>
     * Creates graph nodes for each ConceptInstance and handles property value assertions.
     *
     * @param instance The ConceptInstance element to process.
     * @return true if the element was processed successfully, false otherwise.
     */
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

    /**
     * Processes RelationInstance elements in OML.
     * <p>
     * This method generates nodes and edges for RelationInstance elements, linking them with their
     * source and target NamedInstances.
     *
     * @param instance The RelationInstance element to process.
     * @return true if the element was processed successfully, false otherwise.
     */
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

    /**
     * Processes StructureInstance elements in OML.
     * <p>
     * TODO: Provide implementation details once available.
     *
     * @param instance The StructureInstance element to process.
     * @return null, indicating that the parent Classifier processing should be invoked.
     */
    public Boolean caseStructureInstance(StructureInstance instance) {
        // TODO
        return null;
    }

    ///////////////////////////////////////////////////////////////////////
    // Handlers For Grammar Nodes That Do Not Really Work In An Override //
    ///////////////////////////////////////////////////////////////////////

    /**
     * Handles the Instance Enumeration Axiom for a given Concept in OML.
     * <p>
     * TODO: Provide implementation details once available.
     *
     * @param concept The Concept element to process.
     */
    private void handleInstanceEnumerationAxiom(Concept concept) {
        // TODO
    }

    /**
     * Generates a list of key axioms associated with a given Entity.
     * <p>
     * TODO: Provide implementation details once available.
     *
     * @param entity The Entity element to process.
     * @return A list of strings representing key axioms.
     */
    private List<String> handleKeyAxioms(Entity entity) {
        // TODO
        return new ArrayList<>();
    }

    /**
     * Generates a list of property restriction axioms for a given Classifier.
     * <p>
     * TODO: Provide implementation details once available.
     *
     * @param classifier The Classifier element to process.
     * @return A list of strings representing property restriction axioms.
     */
    private List<String> handlePropertyRestrictionAxioms(Classifier classifier) {
        // TODO
        return new ArrayList<>();
    }

    /**
     * Handles all property value assertions for an Instance.
     * <p>
     * Processes NamedInstance elements, creating list of literals or linking members based on the
     * PropertyValueAssertions.
     *
     * @param in The Instance element to process.
     * @return A list of strings representing property values, or null if not applicable.
     */
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

    /**
     * Initializes the edges map for a given node if it hasn't been initialized yet.
     *
     * @param node The name of the node for which to initialize edges.
     */
    void initializeEdgesIfNecessary(String node) {
        if (!edges.containsKey(node)) {
            edges.put(node, new ArrayList<>());
        }
    }

    /**
     * Adds a node to the graph for a Member element if it's outside the current Ontology.
     *
     * @param member The Member element to check and add.
     * @param compareTo The Ontology to compare with.
     */
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

    /**
     * Creates a table node for the DOT graph.
     * <p>
     * This method builds a table node with a header and optional member rows. The header background
     * is colored, and members have a white background.
     *
     * @param color The background color for the header.
     * @param name The name of the node.
     * @param headers The list of header strings.
     * @param members The list of member strings.
     * @return A Node object representing the table node.
     */
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



    /**
     * Links two members in the graph with specified attributes for the edge.
     * <p>
     * This method creates a link (edge) between two nodes in the DOT graph, representing the
     * relationship between the source and destination members. Additional attributes for styling
     * the link can be provided.
     *
     * @param src The source member.
     * @param dst The destination member.
     * @param attributes Optional attributes for the link.
     */
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

    /**
     * Escapes XML special characters in a string.
     * <p>
     * This method is used to ensure that any XML special characters in strings are properly escaped
     * to avoid errors when rendering as XML or HTML. Specifically, it escapes ampersands, quotes,
     * and angle brackets.
     *
     * @param original The original string to be escaped.
     * @return The escaped string.
     */
    private String xmlEscape(String original) {
        return original
                .replace("&", "&amp;")
                // .replace("'", "&apos;") // not mentioned on graphviz website
                .replace("\"", "&quot;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    /**
     * Converts a PascalCase string to a spaced lowercase string.
     * <p>
     * This method is useful for converting class or type names following the PascalCase convention
     * into a more readable spaced lowercase format. It inserts a space before each uppercase
     * letter and converts the entire string to lowercase.
     *
     * @param str The PascalCase string to be converted.
     * @return The spaced lowercase string.
     */
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