package no.entur.uttu.export.netex.producer.common;

import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.rutebanken.netex.model.GeneralOrganisation;
import org.rutebanken.netex.model.KeyListStructure;
import org.rutebanken.netex.model.KeyValueStructure;
import org.rutebanken.netex.model.Organisation;

public class OrganisationProducerTest {

  @Test
  public void testExtractLegacyId() {
    Assertions.assertEquals(
      "TST:Operator:2",
      OrganisationProducer.extractLegacyId(
        new Organisation()
          .withId("notThis")
          .withKeyList(
            new KeyListStructure()
              .withKeyValue(
                new KeyValueStructure()
                  .withKey("LegacyId")
                  .withValue("TST:Authority:1,TST:Operator:2")
              )
          ),
        "Operator"
      )
    );

    Assertions.assertEquals(
      "TST:Authority:1",
      OrganisationProducer.extractLegacyId(
        new Organisation()
          .withId("notThis")
          .withKeyList(
            new KeyListStructure()
              .withKeyValue(
                new KeyValueStructure()
                  .withKey("LegacyId")
                  .withValue("TST:Authority:1,TST:Operator:2")
              )
          ),
        "Authority"
      )
    );

    Assertions.assertNull(
      OrganisationProducer.extractLegacyId(
        new Organisation()
          .withId("TST:Operator:2")
          .withKeyList(
            new KeyListStructure()
              .withKeyValue(
                new KeyValueStructure().withKey("LegacyId").withValue("TST:Authority:1")
              )
          ),
        "Operator"
      )
    );

    Assertions.assertNull(
      OrganisationProducer.extractLegacyId(
        new Organisation().withId("TST:Operator:2"),
        "Operator"
      )
    );
  }
}
