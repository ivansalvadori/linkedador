package br.com.srs.linkedator;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.jena.ontology.ObjectProperty;
import org.apache.jena.ontology.OntResource;

import com.google.gson.JsonObject;
import com.jayway.jsonpath.JsonPath;

public class Linkedador {

    private List<SemanticMicroserviceDescription> semanticMicroserviceDescriptions = new ArrayList<>();
    private OntologyReader ontologyReader;

    public Linkedador(String ontology) {
        this.ontologyReader = new OntologyReader(ontology);

    }

    public void registryDescription(SemanticMicroserviceDescription semanticMicroservceDescription) {
        this.semanticMicroserviceDescriptions.add(semanticMicroservceDescription);
    }

    public String createLinks(String resourceRepresentation) {
        String linkedResourceRepresentation = resourceRepresentation;
        linkedResourceRepresentation = createExplicitLinks(resourceRepresentation);
        linkedResourceRepresentation = createInferredLinks(linkedResourceRepresentation);

        return linkedResourceRepresentation;
    }

    private String createInferredLinks(String resourceRepresentation) {
        String linkedResourceRepresentation = resourceRepresentation;

        List<ObjectProperty> applicableObjectProperties = new ArrayList<>();
        String resourceRepresentationId = JsonPath.read(resourceRepresentation, "$['@type']");

        /*
         * finding all applicable obj-properties that have the representation id
         * as domain
         */
        List<ObjectProperty> objectProperties = this.ontologyReader.getObjectProperties();
        for (ObjectProperty objectProperty : objectProperties) {
            if (objectProperty.getDomain().getURI().equalsIgnoreCase(resourceRepresentationId)) {
                applicableObjectProperties.add(objectProperty);
            }
        }

        for (ObjectProperty objectProperty : applicableObjectProperties) {
            String objectPropertyRange = objectProperty.getRange().getURI();
            SemanticResource semanticResource = getSemanticResourceByEntity(objectPropertyRange);

            /*
             * in case the range/domain os ah given obj-property have no
             * implementation
             */
            if (semanticResource == null) {
                continue;
            }

            List<UriTemplate> uriTemplates = semanticResource.getUriTemplates();
            for (UriTemplate uriTemplate : uriTemplates) {
                Map<String, String> objectPropertyValue = JsonPath.read(linkedResourceRepresentation, "$");
                Set<String> objectPropertyContent = objectPropertyValue.keySet();
                boolean uriTemplateResolvableFromResourceProperties = objectPropertyContent.containsAll(uriTemplate.getParameters().values());
                if (uriTemplateResolvableFromResourceProperties) {
                    String resolvedLink = resolveLink(semanticResource.getEntity(), objectPropertyValue, uriTemplate);
                    JsonObject updatedRepresentation = new JsonObject();
                    Set<String> listAllPropertyIds = listAllPropertyIds(linkedResourceRepresentation);
                    for (String propertyId : listAllPropertyIds) {
                        String value = JsonPath.read(linkedResourceRepresentation, String.format("$['%s']", propertyId));
                        updatedRepresentation.addProperty(propertyId, value);

                    }
                    updatedRepresentation.addProperty(objectProperty.getURI(), resolvedLink);
                    linkedResourceRepresentation = updatedRepresentation.toString();
                    break;
                }
            }
        }

        return linkedResourceRepresentation;
    }

    private String createExplicitLinks(String resourceRepresentation) {
        String linkedResourceRepresentation = resourceRepresentation;

        List<ObjectProperty> listObjectProperties = listObjectProperties(resourceRepresentation);
        for (ObjectProperty objectProperty : listObjectProperties) {
            OntResource range = objectProperty.getRange();
            SemanticResource semanticResource = getSemanticResourceByEntity(range.getURI());

            boolean isRangeObjectPropertyEqualsEntity = objectProperty.getRange().getURI().equalsIgnoreCase(semanticResource.getEntity());
            if (!isRangeObjectPropertyEqualsEntity) {
                continue;
            }

            String objectPropertyUri = objectProperty.getURI();
            Map<String, String> objectPropertyValue = JsonPath.read(resourceRepresentation, String.format("$['%s']", objectPropertyUri));
            Set<String> objectPropertyContent = objectPropertyValue.keySet();

            List<UriTemplate> uriTemplates = semanticResource.getUriTemplates();
            for (UriTemplate uriTemplate : uriTemplates) {
                boolean uriTemplateResolvableFromResourceProperties = objectPropertyContent.containsAll(uriTemplate.getParameters().values());
                if (uriTemplateResolvableFromResourceProperties) {
                    JsonObject updatedRepresentation = new JsonObject();
                    Set<String> listAllPropertyIds = listAllPropertyIds(resourceRepresentation);
                    for (String propertyId : listAllPropertyIds) {
                        if (propertyId.endsWith(objectPropertyUri)) {
                            String link = resolveLink(range.getURI(), objectPropertyValue, uriTemplate);
                            updatedRepresentation.addProperty(propertyId, link);
                        } else {
                            String value = JsonPath.read(resourceRepresentation, String.format("$['%s']", propertyId));
                            updatedRepresentation.addProperty(propertyId, value);
                        }
                    }
                    linkedResourceRepresentation = updatedRepresentation.toString();
                    break;
                }
            }
        }
        return linkedResourceRepresentation;
    }

    private String resolveLink(String rangeUri, Map<String, String> objectPropertyValue, UriTemplate uriTemplate) {
        String link = String.format("%s/%s", getMicroserviceUriBaseByEntity(rangeUri), uriTemplate.getUri());
        Map<String, String> parameters = uriTemplate.getParameters();
        Set<String> uriTemplateParams = parameters.keySet();
        for (String param : uriTemplateParams) {
            String uriPropertyOfParam = parameters.get(param);
            String paramValuepresentedInResourceRep = objectPropertyValue.get(uriPropertyOfParam);
            link = link.replace("{" + param + "}", paramValuepresentedInResourceRep);
        }
        return link;
    }

    private SemanticResource getSemanticResourceByEntity(String entity) {
        for (SemanticMicroserviceDescription semanticMicroserviceDescription : semanticMicroserviceDescriptions) {
            List<SemanticResource> semanticResources = semanticMicroserviceDescription.getSemanticResources();
            for (SemanticResource semanticResource : semanticResources) {
                if (semanticResource.getEntity().equalsIgnoreCase(entity)) {
                    return semanticResource;
                }
            }
        }

        return null;

    }

    private String getMicroserviceUriBaseByEntity(String entity) {
        for (SemanticMicroserviceDescription semanticMicroserviceDescription : semanticMicroserviceDescriptions) {
            List<SemanticResource> semanticResources = semanticMicroserviceDescription.getSemanticResources();
            for (SemanticResource semanticResource : semanticResources) {
                if (semanticResource.getEntity().equalsIgnoreCase(entity)) {
                    return semanticMicroserviceDescription.getUriBase();
                }
            }
        }

        return null;

    }

    private List<ObjectProperty> listObjectProperties(String resourceRepresentation) {
        List<ObjectProperty> objectProperties = new ArrayList<>();
        Map<String, ?> obj = JsonPath.read(resourceRepresentation, "$");
        Set<String> uriProperty = obj.keySet();
        for (String uri : uriProperty) {
            ObjectProperty objectProperty = ontologyReader.getMapUriObjectProperty().get(uri);
            if (objectProperty != null) {
                objectProperties.add(objectProperty);
            }
        }

        return objectProperties;
    }

    private Set<String> listAllPropertyIds(String resourceRepresentation) {
        Map<String, ?> obj = JsonPath.read(resourceRepresentation, "$");
        return obj.keySet();
    }

}
