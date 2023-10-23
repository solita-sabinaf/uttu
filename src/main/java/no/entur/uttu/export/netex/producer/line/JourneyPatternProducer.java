/*
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by
 * the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 *   https://joinup.ec.europa.eu/software/page/eupl
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and
 * limitations under the Licence.
 */

package no.entur.uttu.export.netex.producer.line;

import java.math.BigInteger;
import java.util.List;
import java.util.stream.Collectors;
import javax.xml.bind.JAXBElement;
import no.entur.uttu.export.netex.NetexExportContext;
import no.entur.uttu.export.netex.producer.NetexIdProducer;
import no.entur.uttu.export.netex.producer.NetexObjectFactory;
import no.entur.uttu.model.BookingArrangement;
import no.entur.uttu.model.FlexibleStopPlace;
import no.entur.uttu.model.HailAndRideArea;
import no.entur.uttu.model.JourneyPattern;
import no.entur.uttu.model.Ref;
import no.entur.uttu.model.StopPointInJourneyPattern;
import no.entur.uttu.model.job.SeverityEnumeration;
import no.entur.uttu.stopplace.StopPlaceRegistry;
import org.rutebanken.netex.model.BookingAccessEnumeration;
import org.rutebanken.netex.model.BookingArrangementsStructure;
import org.rutebanken.netex.model.BookingMethodEnumeration;
import org.rutebanken.netex.model.DestinationDisplayRefStructure;
import org.rutebanken.netex.model.JourneyPatternRefStructure;
import org.rutebanken.netex.model.NoticeAssignment;
import org.rutebanken.netex.model.PointInLinkSequence_VersionedChildStructure;
import org.rutebanken.netex.model.PointsInJourneyPattern_RelStructure;
import org.rutebanken.netex.model.PurchaseMomentEnumeration;
import org.rutebanken.netex.model.PurchaseWhenEnumeration;
import org.rutebanken.netex.model.RouteRefStructure;
import org.rutebanken.netex.model.ScheduledStopPoint;
import org.rutebanken.netex.model.ScheduledStopPointRefStructure;
import org.rutebanken.netex.model.StopPointInJourneyPatternRefStructure;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class JourneyPatternProducer {

  private final NetexObjectFactory objectFactory;
  private final ContactStructureProducer contactStructureProducer;
  private final StopPlaceRegistry stopPlaceRegistry;

  @Autowired
  public JourneyPatternProducer(
    NetexObjectFactory objectFactory,
    ContactStructureProducer contactStructureProducer,
    StopPlaceRegistry stopPlaceRegistry
  ) {
    this.objectFactory = objectFactory;
    this.contactStructureProducer = contactStructureProducer;
    this.stopPlaceRegistry = stopPlaceRegistry;
  }

  public org.rutebanken.netex.model.JourneyPattern produce(
    JourneyPattern local,
    List<NoticeAssignment> noticeAssignments,
    NetexExportContext context
  ) {
    List<PointInLinkSequence_VersionedChildStructure> netexStopPoints = local
      .getPointsInSequence()
      .stream()
      .map(spinjp -> mapStopPointInJourneyPattern(spinjp, noticeAssignments, context))
      .collect(Collectors.toList());
    PointsInJourneyPattern_RelStructure pointsInJourneyPattern_relStructure =
      new PointsInJourneyPattern_RelStructure()
        .withPointInJourneyPatternOrStopPointInJourneyPatternOrTimingPointInJourneyPattern(
          netexStopPoints
        );

    RouteRefStructure routeRef = objectFactory.populateRefStructure(
      new RouteRefStructure(),
      local.getRef(),
      true
    );

    noticeAssignments.addAll(
      objectFactory.createNoticeAssignments(
        local,
        JourneyPatternRefStructure.class,
        local.getNotices(),
        context
      )
    );
    context.notices.addAll(local.getNotices());

    return objectFactory
      .populate(new org.rutebanken.netex.model.JourneyPattern(), local)
      .withRouteRef(routeRef)
      .withName(objectFactory.createMultilingualString(local.getName()))
      .withPointsInSequence(pointsInJourneyPattern_relStructure);
  }

  private org.rutebanken.netex.model.StopPointInJourneyPattern mapStopPointInJourneyPattern(
    StopPointInJourneyPattern local,
    List<NoticeAssignment> noticeAssignments,
    NetexExportContext context
  ) {
    DestinationDisplayRefStructure destinationDisplayRefStructure = null;
    if (local.getDestinationDisplay() != null) {
      context.destinationDisplays.add(local.getDestinationDisplay());
      destinationDisplayRefStructure =
        objectFactory.populateRefStructure(
          new DestinationDisplayRefStructure(),
          local.getDestinationDisplay().getRef(),
          false
        );
    }

    // Create ref to scheduledStopPoint referring to either a flexible stop place or a NSR QuayRef
    Ref stopRef;
    FlexibleStopPlace flexibleStopPlace = local.getFlexibleStopPlace();
    if (flexibleStopPlace != null) {
      context.flexibleStopPlaces.add(flexibleStopPlace);
      stopRef = flexibleStopPlace.getRef();

      HailAndRideArea hailAndRideArea = flexibleStopPlace.getHailAndRideArea();
      if (hailAndRideArea != null) {
        addQuayRef(hailAndRideArea.getStartQuayRef(), context);
        addQuayRef(hailAndRideArea.getEndQuayRef(), context);
        Ref startQuayRef = objectFactory.createScheduledStopPointRefFromQuayRef(
          hailAndRideArea.getStartQuayRef(),
          context
        );
        context.scheduledStopPointRefs.add(startQuayRef);
        Ref endQuayRef = objectFactory.createScheduledStopPointRefFromQuayRef(
          hailAndRideArea.getEndQuayRef(),
          context
        );
        context.scheduledStopPointRefs.add(endQuayRef);
      }
    } else {
      addQuayRef(local.getQuayRef(), context);
      stopRef =
        objectFactory.createScheduledStopPointRefFromQuayRef(local.getQuayRef(), context);
    }

    Ref scheduledStopPointRef = NetexIdProducer.replaceEntityName(
      stopRef,
      ScheduledStopPoint.class.getSimpleName()
    );
    context.scheduledStopPointRefs.add(scheduledStopPointRef);
    JAXBElement<ScheduledStopPointRefStructure> scheduledStopPointRefStructure =
      objectFactory.wrapAsJAXBElement(
        new ScheduledStopPointRefStructure().withRef(scheduledStopPointRef.id)
      );

    noticeAssignments.addAll(
      objectFactory.createNoticeAssignments(
        local,
        StopPointInJourneyPatternRefStructure.class,
        local.getNotices(),
        context
      )
    );
    context.notices.addAll(local.getNotices());

    return objectFactory
      .populateId(
        new org.rutebanken.netex.model.StopPointInJourneyPattern(),
        local.getRef()
      )
      .withBookingArrangements(mapBookingArrangement(local.getBookingArrangement()))
      .withForAlighting(local.getForAlighting())
      .withForBoarding(local.getForBoarding())
      .withOrder(BigInteger.valueOf(local.getOrder()))
      .withDestinationDisplayRef(destinationDisplayRefStructure)
      .withScheduledStopPointRef(scheduledStopPointRefStructure);
  }

  private void addQuayRef(String quayRef, NetexExportContext context) {
    if (stopPlaceRegistry.getStopPlaceByQuayRef(quayRef).isEmpty()) {
      context.addExportMessage(
        SeverityEnumeration.ERROR,
        "{0} is not a valid quayRef",
        quayRef
      );
    }

    context.quayRefs.add(quayRef);
  }

  private BookingArrangementsStructure mapBookingArrangement(BookingArrangement local) {
    if (local == null) {
      return null;
    }
    return new BookingArrangementsStructure()
      .withBookingAccess(
        objectFactory.mapEnum(local.getBookingAccess(), BookingAccessEnumeration.class)
      )
      .withBookingMethods(
        objectFactory.mapEnums(local.getBookingMethods(), BookingMethodEnumeration.class)
      )
      .withBookWhen(
        objectFactory.mapEnum(local.getBookWhen(), PurchaseWhenEnumeration.class)
      )
      .withBuyWhen(
        objectFactory.mapEnums(local.getBuyWhen(), PurchaseMomentEnumeration.class)
      )
      .withLatestBookingTime(local.getLatestBookingTime())
      .withMinimumBookingPeriod(local.getMinimumBookingPeriod())
      .withBookingNote(objectFactory.createMultilingualString(local.getBookingNote()))
      .withBookingContact(
        contactStructureProducer.mapContactStructure(local.getBookingContact())
      );
  }
}
