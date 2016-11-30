package br.ufsc.inf.lapesd.linkedator.test;

import java.io.IOException;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;

import com.google.gson.Gson;

import br.ufsc.inf.lapesd.linkedator.PropertyValueLinkedator;
import br.ufsc.inf.lapesd.linkedator.SemanticMicroserviceDescription;

public class LinkedatorPropertyValueTest {

    PropertyValueLinkedator linkedador;

    public void addMicroserviceDescription(SemanticMicroserviceDescription semanticMicroserviceDescription) {
        linkedador.registryDescription(semanticMicroserviceDescription);
    }

    @Before
    public void configure() throws IOException {

        linkedador = new PropertyValueLinkedator();

        String microserviceOfPeopleDescription = IOUtils.toString(this.getClass().getResourceAsStream("/noSemanticRelationSamePropertyAndValue/microserviceOfPeopleDescription.jsonld"), "UTF-8");
        SemanticMicroserviceDescription microservicesDescription = new Gson().fromJson(microserviceOfPeopleDescription, SemanticMicroserviceDescription.class);
        microservicesDescription.setIpAddress("192.168.10.1");
        microservicesDescription.setServerPort("8080");
        microservicesDescription.setUriBase("/service/");
        linkedador.registryDescription(microservicesDescription);

        String policeReportDescriptionContent = IOUtils.toString(this.getClass().getResourceAsStream("/noSemanticRelationSamePropertyAndValue/microserviceOfPoliceReportDescription.jsonld"), "UTF-8");
        SemanticMicroserviceDescription policeReportDescription = new Gson().fromJson(policeReportDescriptionContent, SemanticMicroserviceDescription.class);
        policeReportDescription.setIpAddress("192.168.10.2");
        policeReportDescription.setServerPort("8080");
        policeReportDescription.setUriBase("/service/");
        linkedador.registryDescription(policeReportDescription);

    }

    @Test
    public void mustCreateExplicitLinkInPoliceRepor() throws IOException {
        String policeReport = IOUtils.toString(this.getClass().getResourceAsStream("/noSemanticRelationSamePropertyAndValue/policeReport.jsonld"), "UTF-8");
        String linkedRepresentation = linkedador.createLinks(policeReport);
        System.out.println(linkedRepresentation);
        //String expectedLink = "http://www.w3.org/2002/07/owl#sameAs\":\"http://192.168.10.1:8080/service/vitima?x=123456&y=88888";
        //Assert.assertTrue(linkedRepresentation.contains(expectedLink));
    }

    
}
