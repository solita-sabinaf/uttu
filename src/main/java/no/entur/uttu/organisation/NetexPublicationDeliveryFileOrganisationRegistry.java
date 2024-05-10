package no.entur.uttu.organisation;

import static jakarta.xml.bind.JAXBContext.newInstance;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.annotation.PostConstruct;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import no.entur.uttu.error.codederror.CodedError;
import no.entur.uttu.error.codes.ErrorCodeEnumeration;
import no.entur.uttu.util.Preconditions;
import org.rutebanken.netex.model.GeneralOrganisation;
import org.rutebanken.netex.model.Organisation;
import org.rutebanken.netex.model.PublicationDeliveryStructure;
import org.rutebanken.netex.model.ResourceFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnMissingBean(
  value = OrganisationRegistry.class,
  ignored = NetexPublicationDeliveryFileOrganisationRegistry.class
)
public class NetexPublicationDeliveryFileOrganisationRegistry
  implements OrganisationRegistry {

  private List<GeneralOrganisation> organisations = List.of();

  private static final Logger log = LoggerFactory.getLogger(
    NetexPublicationDeliveryFileOrganisationRegistry.class
  );

  private static final JAXBContext publicationDeliveryContext = createContext(
    PublicationDeliveryStructure.class,
    Organisation.class
  );

  @Value("${uttu.organisations.netex-file-uri}")
  String netexFileUri;

  @PostConstruct
  public void init() {
    PublicationDeliveryStructure publicationDeliveryStructure = readFromSource(
      new StreamSource(new File(netexFileUri))
    );
    publicationDeliveryStructure
      .getDataObjects()
      .getCompositeFrameOrCommonFrame()
      .forEach(frame -> {
        var frameValue = frame.getValue();
        if (frameValue instanceof ResourceFrame resourceFrame) {
          organisations =
            resourceFrame
              .getOrganisations()
              .getOrganisation_()
              .stream()
              .map(org -> (GeneralOrganisation) org.getValue())
              .toList();
        }
      });
  }

  @Override
  public List<GeneralOrganisation> getOrganisations() {
    return organisations;
  }

  @Override
  public Optional<GeneralOrganisation> getOrganisation(String id) {
    return organisations.stream().filter(org -> org.getId().equals(id)).findFirst();
  }

  @Override
  public String getVerifiedOperatorRef(String operatorRef) {
    Preconditions.checkArgument(
      organisations.stream().anyMatch(org -> org.getId().equals(operatorRef)),
      CodedError.fromErrorCode(
        ErrorCodeEnumeration.ORGANISATION_NOT_IN_ORGANISATION_REGISTRY
      ),
      "Organisation with ref %s not found in organisation registry",
      operatorRef
    );
    return operatorRef;
  }

  @Override
  public String getVerifiedAuthorityRef(String authorityRef) {
    Preconditions.checkArgument(
      organisations.stream().anyMatch(org -> org.getId().equals(authorityRef)),
      CodedError.fromErrorCode(
        ErrorCodeEnumeration.ORGANISATION_NOT_IN_ORGANISATION_REGISTRY
      ),
      "Organisation with ref %s not found in organisation registry",
      authorityRef
    );

    return authorityRef;
  }

  private <T> T readFromSource(Source source) {
    try {
      JAXBElement<T> element = (JAXBElement<T>) getUnmarshaller().unmarshal(source);
      return element.getValue();
    } catch (JAXBException e) {
      throw new RuntimeException(e);
    }
  }

  private Unmarshaller getUnmarshaller() throws JAXBException {
    return publicationDeliveryContext.createUnmarshaller();
  }

  private static JAXBContext createContext(Class... clazz) {
    try {
      JAXBContext jaxbContext = newInstance(clazz);
      log.info("Created context {}", jaxbContext.getClass());
      return jaxbContext;
    } catch (JAXBException e) {
      String message = "Could not create instance of jaxb context for class " + clazz;
      log.warn(message, e);
      throw new RuntimeException(
        "Could not create instance of jaxb context for class " + clazz,
        e
      );
    }
  }
}
