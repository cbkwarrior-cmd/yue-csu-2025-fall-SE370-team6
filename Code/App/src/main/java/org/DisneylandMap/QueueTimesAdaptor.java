package org.DisneylandMap;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;

import java.io.IOException;

// Adaptor for the Queue-Times API. It requests, receives, and parses through a JSON of current attraction
//data of the park from Queue-Time.com
public class QueueTimesAdaptor implements IQueueTime {

    //Disneyland California has the id '16' in Queue-Times API
    private static final int DISNEYLANDID = 16;
    //Initializes a reusable, static ObjectMapper instance to improve runtime
    private static final ObjectMapper objectMapper = new ObjectMapper();
    //Initializes a reusable, static httpClient instance to improve runtime
    private static final HttpClient httpClient = HttpClient.newHttpClient();
    //Makes a httpRequest for Queue-Times API to get data about Disneyland CA
    private static final HttpRequest httpRequest = HttpRequest.newBuilder()
            .GET()
            .uri(URI.create("https://queue-times.com/parks/" + DISNEYLANDID + "/queue_times.json"))
            .build();


    //Constructor for class Attraction
    public QueueTimesAdaptor() {

    }

    //Returns the current wait time of the attraction using the current data that is retrieved from Queue-Times API,
    //or it returns -1 if the attraction is closed, or -2 if an error has occurred. The parameter 'landID' indicates
    //the land in which the desired attraction is from, and the parameter 'attractionID' indicates the desired attraction
    //that the method will return the wait time from
    public int getWaitTime(int landID, int attractionID) throws IOException, InterruptedException {

        //Sends and gets a httpResponse from Queue-Times API
        HttpResponse<byte[]> httpResponse;
        try {
            httpResponse = httpClient.send(httpRequest, BodyHandlers.ofByteArray());
        } catch(IOException | InterruptedException e) {
            //Checks if the status code given by httpResponse indicates an error has occurred

            e.printStackTrace();
            return -2;
        }

        JsonNode rootNode = objectMapper.readTree(httpResponse.body());
        JsonNode lands = rootNode.path("lands");

        JsonNode land;
        JsonNode rides;
        JsonNode attraction;

        //Iterates through the lands of the park in the API data
        for (int i = 0; i < lands.size(); ++i) {

            land = lands.get(i);

            //Checks if the id of the land is the same as the given land id of the attraction
            if (landID == land.get("id").asInt()) {

                rides = lands.get(i).path("rides");

                //Iterates through the rides in the land of the park in the API data
                for (int j = 0; j < rides.size(); ++j) {

                    //Gets the data of a ride from the API data
                    attraction = rides.get(j);

                    //Checks if the id of the ride is the same as the given id of the attraction
                    if (attractionID == attraction.get("id").asInt()) {

                        //Checks if the attraction is currently open
                        if (attraction.get("is_open").asBoolean()) {

                            //Returns the current wait time of the attraction
                            return attraction.get("wait_time").asInt();
                        }
                        //Returns -1 if the attraction is closed
                        return -1;
                    }
                }

                //Gives error message if no matching attraction id was found in given json data
                System.err.println("Attraction id not found in Queue-Times API json data in method getWaitTime() of class QueueTimesAdaptor");
                //Returns -2 since an error has occurred
                return -2;

            }

            //Gives error message if no matching land id was found in given json data
            if (i == lands.size() - 1) {
                System.err.println("Land id not found in Queue-Times API json data in method getWaitTime() of class QueueTimesAdaptor");
                //Returns -2 since an error has occurred
                return -2;
            }
        }

        //Returns -2 if an error has occurred
        return -2;
    }
}
