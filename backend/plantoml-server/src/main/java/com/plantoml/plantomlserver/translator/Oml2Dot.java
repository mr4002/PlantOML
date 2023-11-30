package com.plantoml.plantomlserver.translator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;

import io.opencaesar.oml.Annotation;
import io.opencaesar.oml.AnnotationProperty;
import io.opencaesar.oml.Argument;
import io.opencaesar.oml.Aspect;
import io.opencaesar.oml.BooleanLiteral;
import io.opencaesar.oml.BuiltInPredicate;
import io.opencaesar.oml.CardinalityRestrictionKind;
import io.opencaesar.oml.ClassifierEquivalenceAxiom;
import io.opencaesar.oml.Concept;
import io.opencaesar.oml.ConceptInstance;
import io.opencaesar.oml.DecimalLiteral;
import io.opencaesar.oml.Description;
import io.opencaesar.oml.DescriptionBundle;
import io.opencaesar.oml.DifferentFromPredicate;
import io.opencaesar.oml.DoubleLiteral;
import io.opencaesar.oml.Element;
import io.opencaesar.oml.Import;
import io.opencaesar.oml.InstanceEnumerationAxiom;
import io.opencaesar.oml.IntegerLiteral;
import io.opencaesar.oml.KeyAxiom;
import io.opencaesar.oml.Literal;
import io.opencaesar.oml.LiteralEnumerationAxiom;
import io.opencaesar.oml.Member;
import io.opencaesar.oml.NamedInstance;
import io.opencaesar.oml.Ontology;
import io.opencaesar.oml.Predicate;
import io.opencaesar.oml.PropertyCardinalityRestrictionAxiom;
import io.opencaesar.oml.PropertyEquivalenceAxiom;
import io.opencaesar.oml.PropertyPredicate;
import io.opencaesar.oml.PropertyRangeRestrictionAxiom;
import io.opencaesar.oml.PropertyRestrictionAxiom;
import io.opencaesar.oml.PropertySelfRestrictionAxiom;
import io.opencaesar.oml.PropertyValueAssertion;
import io.opencaesar.oml.PropertyValueRestrictionAxiom;
import io.opencaesar.oml.QuotedLiteral;
import io.opencaesar.oml.RangeRestrictionKind;
import io.opencaesar.oml.Relation;
import io.opencaesar.oml.RelationBase;
import io.opencaesar.oml.RelationEntity;
import io.opencaesar.oml.RelationEntityPredicate;
import io.opencaesar.oml.RelationInstance;
import io.opencaesar.oml.ReverseRelation;
import io.opencaesar.oml.Rule;
import io.opencaesar.oml.SameAsPredicate;
import io.opencaesar.oml.Scalar;
import io.opencaesar.oml.ScalarEquivalenceAxiom;
import io.opencaesar.oml.ScalarProperty;
import io.opencaesar.oml.SpecializationAxiom;
import io.opencaesar.oml.Structure;
import io.opencaesar.oml.StructureInstance;
import io.opencaesar.oml.StructuredProperty;
import io.opencaesar.oml.Term;
import io.opencaesar.oml.TypeAssertion;
import io.opencaesar.oml.TypePredicate;
import io.opencaesar.oml.UnreifiedRelation;
import io.opencaesar.oml.Vocabulary;
import io.opencaesar.oml.VocabularyBundle;
import io.opencaesar.oml.util.OmlSwitch;

import io.opencaesar.oml.util.OmlRead;


public class Oml2Dot extends OmlSwitch<Void> {

    private StringBuilder dotBuilder;

    public Oml2Dot() {
        this.dotBuilder = new StringBuilder();
    }

    public String convert(Resource inputResource) {
        // initialize the DOT graph (TODO: what is graph structure going to look like???)
        dotBuilder.append("digraph G {\n");

        // traverse all elements in the resource
        Iterable<EObject> iterable = () -> inputResource.getAllContents();
        StreamSupport.stream(iterable.spliterator(), false)
                .forEach(eObject -> doSwitch(eObject));

        // finish DOT graph
        dotBuilder.append("}\n");
        return dotBuilder.toString();
    }


    @Override
    public Void caseAnnotation(final Annotation annotation) {
        //addAnnotation(annotation.getAnnotatedElement(), annotation);
        Element element = annotation.getAnnotatedElement();
        this.dotBuilder.append(annotation.getLiteralValue().getValue());
//        element.
//        if (element instanceof Ontology) {
//            owl.addOntologyAnnotation(ontology, createAnnotation(annotation));
//        } else if (element instanceof Member) {
//            owl.addAnnotationAssertion(ontology, ((Member)element).getIri(), createAnnotation(annotation));
//        }
        return null;
    }

//    @Override
//    public Void caseVocabulary(final Vocabulary vocabulary) {
//        ontology = owl.createOntology(vocabulary.getPrefix(), vocabulary.getNamespace());
//        owl.addOntologyAnnotation(ontology, owl.getAnnotation(OmlConstants.type, owl.createIri(OmlConstants.Vocabulary)));
//        return null;
//    }
//
//    @Override
//    public Void caseVocabularyBundle(final VocabularyBundle bundle) {
//        ontology = owl.createOntology(bundle.getPrefix(), bundle.getNamespace());
//        owl.addOntologyAnnotation(ontology, owl.getAnnotation(OmlConstants.type, owl.createIri(OmlConstants.VocabularyBundle)));
//        return null;
//    }
//
//    @Override
//    public Void caseDescription(final Description description) {
//        ontology = owl.createOntology(description.getPrefix(), description.getNamespace());
//        owl.addOntologyAnnotation(ontology, owl.getAnnotation(OmlConstants.type, owl.createIri(OmlConstants.Description)));
//        return null;
//    }

}
