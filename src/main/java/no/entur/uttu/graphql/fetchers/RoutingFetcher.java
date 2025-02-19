package no.entur.uttu.graphql.fetchers;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import no.entur.uttu.graphql.model.ServiceLink;
import no.entur.uttu.routing.RouteGeometry;
import no.entur.uttu.routing.RoutingService;
import no.entur.uttu.stopplace.spi.StopPlaceRegistry;
import org.rutebanken.netex.model.Quay;
import org.springframework.stereotype.Service;

@Service("routingFetcher")
public class RoutingFetcher implements DataFetcher<ServiceLink> {

  private final RoutingService routingService;
  private final StopPlaceRegistry stopPlaceRegistry;

  public RoutingFetcher(
    RoutingService routingService,
    StopPlaceRegistry stopPlaceRegistry
  ) {
    this.routingService = routingService;
    this.stopPlaceRegistry = stopPlaceRegistry;
  }

  @Override
  public ServiceLink get(DataFetchingEnvironment environment) {
    String quayRefFrom = environment.getArgument("quayRefFrom");
    String quayRefTo = environment.getArgument("quayRefTo");
    Quay quayFrom = getQuay(quayRefFrom);
    Quay quayTo = getQuay(quayRefTo);

    if (quayFrom == null || quayTo == null) {
      return new ServiceLink(quayRefFrom + "_" + quayRefTo, null, null, null);
    }

    RouteGeometry routeGeometry = routingService.getRouteGeometry(
      quayFrom.getCentroid().getLocation().getLongitude(),
      quayFrom.getCentroid().getLocation().getLatitude(),
      quayTo.getCentroid().getLocation().getLongitude(),
      quayTo.getCentroid().getLocation().getLatitude()
    );

    return new ServiceLink(
      quayRefFrom + "_" + quayRefTo,
      quayRefFrom,
      quayRefTo,
      routeGeometry
    );
  }

  private Quay getQuay(String quayRef) {
    return stopPlaceRegistry.getQuayById(quayRef).orElse(null);
  }
}
