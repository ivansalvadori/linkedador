package br.ufsc.inf.lapesd.linkedator.test;

import java.io.IOException;
import java.io.InputStream;

import br.ufsc.inf.lapesd.linkedator.Linkedator;
import br.ufsc.inf.lapesd.linkedator.ModelBasedLinkedator;
import br.ufsc.inf.lapesd.linkedator.links.NullLinkVerifier;
import org.apache.commons.io.IOUtils;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.google.gson.Gson;

import br.ufsc.inf.lapesd.linkedator.SemanticMicroserviceDescription;

public class EquivalentPropertiesAndClassesTest {
    Linkedator linkedator;

    @Before
    public void configure() throws IOException {
        linkedator = TestUtils.createLinkedator(getClass().getResourceAsStream(
                "/equivalentPropertiesAndClasses/domainOntology.owl"), Lang.RDFXML);

        String microserviceOfPeopleDescription = IOUtils.toString(this.getClass().getResourceAsStream("/equivalentPropertiesAndClasses/microserviceOfPeopleDescription.jsonld"), "UTF-8");
        SemanticMicroserviceDescription microservicesDescription = new Gson().fromJson(microserviceOfPeopleDescription, SemanticMicroserviceDescription.class);
        microservicesDescription.setIpAddress("192.168.10.1");
        microservicesDescription.setServerPort("8080");
        microservicesDescription.setUriBase("/service/");
        linkedator.register(microservicesDescription);

        String policeReportDescriptionContent = IOUtils.toString(this.getClass().getResourceAsStream("/equivalentPropertiesAndClasses/microserviceOfPoliceReportDescription.jsonld"), "UTF-8");
        SemanticMicroserviceDescription policeReportDescription = new Gson().fromJson(policeReportDescriptionContent, SemanticMicroserviceDescription.class);
        policeReportDescription.setIpAddress("192.168.10.2");
        policeReportDescription.setServerPort("8080");
        policeReportDescription.setUriBase("/service/");
        linkedator.register(policeReportDescription);

    }

    @Test
    public void mustCreateExplicitLinkInPoliceRepor() throws IOException {
        Model model = ModelFactory.createDefaultModel();
        try (InputStream in = this.getClass().getResourceAsStream("/equivalentPropertiesAndClasses/policeReport.jsonld")) {
            RDFDataMgr.read(model, in, Lang.JSONLD);
        }
        linkedator.createLinks(model, new NullLinkVerifier());

        Assert.assertTrue(QueryExecutionFactory.create(TestUtils.SPARQL_PROLOGUE +
                "ASK WHERE {\n" +
                "  <http://10.1.1.2/service/report/123> ssp:victim ?v.\n" +
                "  ?v a sch:Person.\n" +
                "  ?v owl:sameAs <http://192.168.10.1:8080/service/vitima?x=123456&y=88888>.\n" +
                "}", model).execAsk());
    }

    @Test
    public void mustCreateInferredLinkInPerson() throws IOException {
        Model model = ModelFactory.createDefaultModel();
        try (InputStream in = this.getClass().getResourceAsStream("/equivalentPropertiesAndClasses/person.jsonld")) {
            RDFDataMgr.read(model, in, Lang.JSONLD);
        }
        linkedator.createLinks(model, new NullLinkVerifier());

        Assert.assertTrue(QueryExecutionFactory.create(TestUtils.SPARQL_PROLOGUE +
                "ASK WHERE {\n" +
                "  <http://10.1.1.1/people-microservice/13579> ssp:envolvedIn ?r.\n" +
                "  ?r a ssp:PoliceReport.\n" +
                "  ?r owl:sameAs <http://192.168.10.2:8080/service/reports/13579>.\n" +
                "}", model).execAsk());
    }
}
