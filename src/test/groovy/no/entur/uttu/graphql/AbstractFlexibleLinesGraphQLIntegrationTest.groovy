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

package no.entur.uttu.graphql

import io.restassured.response.ValidatableResponse

import java.time.LocalDate

abstract class AbstractFlexibleLinesGraphQLIntegrationTest extends AbstractGraphQLResourceIntegrationTest {

    ValidatableResponse createDayType(LocalDate date) {
        String query = """
            mutation MutateDayType(\$input: DayTypeInput!) {
                mutateDayType(input: \$input) {
                    id
                    name
                    daysOfWeek
                    dayTypeAssignments {
                        date
                        isAvailable
                        operatingPeriod {
                            fromDate
                            toDate
                        }
                    }
                }
            }
        """

        String variables = """
            {
                "input": {
                    "name": "Test day type name",
                    "dayTypeAssignments": {
                        "date": "$date"
                    }
                }
            }
        """

        executeGraphQL(query, variables)
    }

    protected String getUrl() {
        return "/services/flexible-lines/tst/graphql"
    }

    String getFlexibleStopPlaceId(ValidatableResponse response) {
        extractId(response, "mutateFlexibleStopPlace")
    }

    String createStopPlaceQuery = """
 mutation mutateFlexibleStopPlace(\$flexibleStopPlace: FlexibleStopPlaceInput!) {
  mutateFlexibleStopPlace(input: \$flexibleStopPlace) {
    id
    name
    keyValues {
      key
      values
    }
    flexibleArea {
      polygon {
        type
        coordinates
      }
    }
    flexibleAreas {
      keyValues {
        key
        values
      }
      polygon {
        type
        coordinates
      }
    }
    hailAndRideArea {
      startQuayRef
      endQuayRef 
    }
  }
  }
         """


    ValidatableResponse createFlexibleStopPlaceWithFlexibleArea(String name) {


        String variables = """    
{
  "flexibleStopPlace": {
    "name": "$name",
    "description": "flexible area desc",
    "transportMode": "water",
    "keyValues": [{
      "key": "foo",
      "values": ["bar", "baz"]
    }],
    "flexibleArea": {
      "polygon": {
        "coordinates": [
          [
            2.1,
            3.3
          ],
          [
            4.1,
            5.2
          ],
          [
            4.9,
            5.9
          ],
          [
            2.1,
            3.3
          ]
        ],
        "type": "Polygon"
      }
    }
  }
}
        """
        executeGraphQL(createStopPlaceQuery, variables)
    }

    ValidatableResponse createFlexibleStopPlaceWithFlexibleAreas(String name) {


        String variables = """
{
  "flexibleStopPlace": {
    "name": "$name",
    "description": "flexible area desc",
    "transportMode": "water",
    "flexibleAreas": [{
      "keyValues": [{
        "key": "foo",
        "values": ["bar", "baz"]
      }],
      "polygon": {
        "coordinates": [
          [
            2.1,
            3.3
          ],
          [
            4.1,
            5.2
          ],
          [
            4.9,
            5.9
          ],
          [
            2.1,
            3.3
          ]
        ],
        "type": "Polygon"
      }
    }]
  }
}
        """
        executeGraphQL(createStopPlaceQuery, variables)
    }

    ValidatableResponse createFlexibleStopPlaceWithHailAndRideArea(String name) {
        String variables = """    
        {
        "flexibleStopPlace": {
        "name": "$name",
        "description": "hail and ride desc",
        "transportMode": "bus",
        "keyValues": [{
          "key": "foo",
          "values": ["bar", "baz"]
        }],
        "hailAndRideArea": {"startQuayRef": "NSR:Quay:565","endQuayRef": "NSR:Quay:494"}
    }
        }"""

        executeGraphQL(createStopPlaceQuery, variables)
    }

    ValidatableResponse createFlexibleLine(String name) {
        return createFlexibleLine(name, 'NOG:Operator:1')
    }


    ValidatableResponse createFlexibleLine(String name, String operatorRef) {
        String networkId = getNetworkId(createNetwork(name))
        String flexAreaStopPlaceId = getFlexibleStopPlaceId(createFlexibleStopPlaceWithFlexibleArea(name + "FlexArea"))
        String hailAndRideStopPlaceId = getFlexibleStopPlaceId(createFlexibleStopPlaceWithHailAndRideArea(name + "HailAndRide"))
        return createFlexibleLine(name, operatorRef, networkId, flexAreaStopPlaceId, hailAndRideStopPlaceId)
    }

    ValidatableResponse createFlexibleLine(String name, String operatorRef, String networkId, String flexAreaStopPlaceId, String hailAndRideStopPlaceId) {
        ValidatableResponse dayTypeResponse = createDayType(TODAY)
        String dayTypeRef = dayTypeResponse.extract().body().path("data.mutateDayType.id")
        String timestamp = System.currentTimeMillis().toString()
        String variables = """
{
  "flexibleLine": {
    "name": "$name",
    "publicCode": "pubCode",
    "description": "FlexibleLine desc",
    "privateCode": "privateCode",
    "operatorRef": "$operatorRef",
    "flexibleLineType": "flexibleAreasOnly",
    "networkRef": "$networkId",
    "transportMode": "bus",
    "transportSubmode": "expressBus",
    "notices": [
      {
        "text": "lineNotice1"
      },
      {
        "text": "lineNotice2"
      }
    ],
    "bookingArrangement": {
      "minimumBookingPeriod": "PT2H",
      "bookingNote": "Notis for booking av linje",
      "bookingMethods": [
        "online",
        "callDriver"
      ],
      "bookingAccess": "authorisedPublic",
      "buyWhen": [
        "afterBoarding",
        "beforeBoarding"
      ],
      "bookingContact": {
        "contactPerson": "Linjemann Book-Jensen",
        "furtherDetails": "Linje: Ytterligere detaljer",
        "email": "line@booking.com",
        "phone": "line + 577",
        "url": "http://line.booking.com"
      }
    },
    "journeyPatterns": [
      {
        "directionType": "inbound",
        "pointsInSequence": [
          {
            "flexibleStopPlaceRef": "$hailAndRideStopPlaceId",
            "destinationDisplay": {
              "frontText": "krokkus"
            }
          },
          {
            "flexibleStopPlaceRef": "$flexAreaStopPlaceId"
          },
          {
            "quayRef": "NSR:Quay:563"
          }
        ],
        "serviceJourneys": [
          {
            "name": "SJ-$timestamp-1",
            "notices": {
              "text": "koko"
            },
            "dayTypesRefs": ["$dayTypeRef"],
            "privateCode": "500",
            "passingTimes": [
              {
                "departureTime": "16:00"
              },
              {
                "latestArrivalTime":"16:10",
                "earliestDepartureTime": "16:20"
              },
              {
                "arrivalTime": "16:30"
              }
            ]
          }
        ]
      },
      {
        "directionType": "inbound",
        "pointsInSequence": [
                {
            "flexibleStopPlaceRef": "$hailAndRideStopPlaceId",
             "destinationDisplay": {
              "frontText": "direkte"
            }
        }
        ,
        
        {
            "quayRef": "NSR:Quay:494"
         }
        ],
        "serviceJourneys": [
          {
            "name": "SJ-$timestamp-2",
            "dayTypesRefs": ["$dayTypeRef"],
            "privateCode": "501",
            "passingTimes": [
              {
                "departureTime": "18:00"
              },
              {
                "arrivalTime": "18:30"
              }
            ]
          }
        ]
      }
    ]
  }
}
"""

        executeGraphQL(createFlexibleLineQuery, variables)
    }



    String fullFlexibleLineFieldSet = """
                    id
                    version
                    name
                    created
                    changed
                    createdBy
                    changedBy
                    publicCode
                    privateCode
                    description
                    transportMode
                    transportSubmode
                    network {
                        id
                    }
                    operatorRef
                    notices {
                        version
                        id
                        text
                    }
                    bookingArrangement {
                        buyWhen
                        bookWhen
                        bookingNote
                        bookingAccess
                        latestBookingTime
                        minimumBookingPeriod
                        bookingContact {
                            contactPerson
                            email
                            phone
                            url
                            contactPerson
                        }
                    }
                    journeyPatterns {
                        name
                        description
                        privateCode
                        directionType
                        notices {
                            id
                            text
                        }
                        pointsInSequence {
                            id
                            version
                            forBoarding
                            forAlighting
                            quayRef
                            flexibleStopPlace {
                                id
                            }
                            destinationDisplay {
                                id
                                frontText
                            }
                            notices {
                                id
                                text
                            }
                        }
                        serviceJourneys {
                            name
                            dayTypes {
                                name
                                daysOfWeek
                                dayTypeAssignments {
                                    operatingPeriod {
                                        fromDate
                                        toDate
                                    }
                                    isAvailable
                                    date
                                }
                            }
                            passingTimes {
                                    arrivalTime
                                    arrivalDayOffset
                                    departureTime
                                    departureDayOffset
                                    latestArrivalTime
                                    latestArrivalDayOffset
                                    earliestDepartureTime
                                    earliestDepartureDayOffset
                             }
                            notices {
                                text
                            }
                        }
                    }
"""


    String createFlexibleLineQuery = """
 mutation mutateFlexibleLine(\$flexibleLine: FlexibleLineInput!) {
  mutateFlexibleLine(input: \$flexibleLine) {
    $fullFlexibleLineFieldSet    
  }
  }
         """

}


