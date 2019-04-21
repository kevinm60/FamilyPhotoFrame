package app.familyphotoframe.slideshow;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Date;
import java.util.Calendar;
import java.util.Comparator;
import java.text.DecimalFormat;
import java.util.Random;

import org.junit.*;
import org.mockito.MockitoAnnotations;
import org.mockito.Mock;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import app.familyphotoframe.model.Photo;
import app.familyphotoframe.model.Contact;
import app.familyphotoframe.model.Relationship;
import app.familyphotoframe.repository.PhotoCollection;
import app.familyphotoframe.PhotoFrameActivity;
import app.familyphotoframe.exception.DiscoveryFailureException;

// run with "gradle testDebug --info"
// gradle will only run tests if the test class has changed

public class ShowPlannerTest {
    // miliseconds per day
    private static final long MS_PER_DAY = 60 * 60 * 24 * 1000;

    private static final int NUM_PHOTOS_PER_CONTACT = 120;
    private static final int NUM_ITERATIONS = 10000;
    private static final int NUM_PHOTOS_PER_ITERATION = 30;

    @Mock
    private PhotoCollection mockPhotoCollection;

    /** object under test */
    private ShowPlanner showPlanner;

    private Random random = new Random();

    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);
    }

    // TODO: Add test for cases where fewer photos are available (groups are
    //       empty or only partially populated)

    @Test
    public void photosShownAtExpectedRates() throws DiscoveryFailureException {

        // create contacts
        Contact[] contacts =
            { new Contact("a", "adam", Relationship.SELF),
              new Contact("b", "beth", Relationship.FAMILY),
              new Contact("c", "carl", Relationship.FAMILY),
              new Contact("d", "don", Relationship.FAMILY),
              new Contact("e", "erin", Relationship.FRIEND) };

        // create photos with random dates for each contact
        Date now = new Date();
        Set<Photo> allPhotos = new HashSet<>();
        for (Contact contact : contacts) {
            // System.out.println("contact: " + contact);
            for (int ii = 0; ii<NUM_PHOTOS_PER_CONTACT; ++ii) {
                int daysAgo = random.nextInt(800);
                Date date = new Date(now.getTime() - daysAgo * MS_PER_DAY);
                // System.out.println("  " + daysAgo + " " + date);
                allPhotos.add(new Photo(contact.getName() + "_" + date.getTime() + "_" + ii,
                                        null, null, null, contact, null, date, null));
            }
        }

        when(mockPhotoCollection.getPhotos())
            .thenReturn(allPhotos);
        when(mockPhotoCollection.getTimeOfLastDiscovery())
            .thenReturn(makeDate(-1));

        showPlanner = new ShowPlanner(mockPhotoCollection);

        // run the ShowPlanner several times and count which photos come up

        Map<Photo,PickCounter> pickCount = new HashMap<>();
        for (Photo photo : allPhotos) {
            pickCount.put(photo, new PickCounter());
        }
        for (int ii=0; ii<NUM_ITERATIONS; ii++) {
            List<Photo> chosenPhotos = showPlanner.getPhotosToSchedule(NUM_PHOTOS_PER_ITERATION);
            for (Photo photo : chosenPhotos) {
                pickCount.get(photo).increment();
            }
        }

        // analyze the results

        // we expect that all photos are shown at least once

        // int numExpectedPhotos = allPhotos.size();
        // int numMissingPhotos = numExpectedPhotos - pickCount.size();
        // if (numMissingPhotos > 0) {
        //     System.out.println("FAIL: " + numMissingPhotos + " of " + numExpectedPhotos +
        //                        " photos were never shown");
        // } else {
        //     System.out.println("PASS: all " + numExpectedPhotos + " photos were shown at least once");
        // }

        // count how many photos in each group
        int[][][] numPhotosByGroup = new int[ShowPlanner.numRelationshipIntervals][ShowPlanner.numRecencyIntervals][ShowPlanner.numSeasonalityIntervals];
        // count how many time each photo is picked by each dimension
        int[][][] numPicksByGroup = new int[ShowPlanner.numRelationshipIntervals][ShowPlanner.numRecencyIntervals][ShowPlanner.numSeasonalityIntervals];
        for (Map.Entry<Photo,PickCounter> pair : pickCount.entrySet()) {
            Photo photo = pair.getKey();
            int count = pair.getValue().getCounter();
            int iRelationship = showPlanner.determineRelationship(photo);
            int iRecency = showPlanner.determineRecency(now, photo);
            int iSeasonality = showPlanner.determineSeasonality(now, photo);
            numPhotosByGroup[iRelationship][iRecency][iSeasonality] += 1;
            numPicksByGroup[iRelationship][iRecency][iSeasonality] += count;
            // System.out.println(String.format("index: %s [%d,%d,%d]", photo.getOwner().getName(), iRelationship, iRecency, iSeasonality));
        }

        DecimalFormat df = new DecimalFormat("#.00");
        final double tolerance = 0.5;

        // check that higher nominal relationship likelihood results in more average picks.
        // counts should be monotonically decreasing as the index decreases for each dimension
        // System.out.println("self, family, friend");
        // for (int iRecency=0; iRecency<ShowPlanner.numRecencyIntervals; iRecency++) {
        //     for (int iSeasonality=0; iSeasonality<ShowPlanner.numSeasonalityIntervals; iSeasonality++) {
        //         int[] photos = new int[ShowPlanner.numRelationshipIntervals];
        //         int[] picks = new int[ShowPlanner.numRelationshipIntervals];
        //         String status = "PASS";
        //         int lastPickAvg = Integer.MAX_VALUE;
        //         for (int iRelationship=0; iRelationship<ShowPlanner.numRelationshipIntervals; iRelationship++) {
        //             int thisPhotoCount = numPhotosByGroup[iRelationship][iRecency][iSeasonality];
        //             photos[iRelationship] = thisPhotoCount;
        //             int thisPickCount = numPicksByGroup[iRelationship][iRecency][iSeasonality];
        //             if (thisPickCount == 0) {
        //                 continue;
        //             }
        //             int thisPickAvg = (int)Math.round((thisPickCount*1.0)/thisPhotoCount);
        //             picks[iRelationship] = thisPickAvg;
        //             if (thisPickAvg > lastPickAvg) {
        //                 status = "FAIL";
        //                 // System.out.println(String.format("FAIL: numPicksByGroup[%d][%d][%d]=%d, numPicksByGroup[%d][%d][%d]=%d",
        //                 //                                  iRelationship, iRecency, iSeasonality, thisCount,
        //                 //                                  iRelationship-1, iRecency, iSeasonality, lastCount));
        //             }
        //             lastPickAvg = thisPickAvg;
        //         }
        //         int[] margin = new int[ShowPlanner.numRelationshipIntervals];
        //         for (int ii=0; ii<photos.length; ii++) {
        //             margin[ii] = photos[ii] - ShowPlanner.nominalRelationshipLikelihood[ii] *
        //                 ShowPlanner.nominalRecencyLikelihood[iRecency] *
        //                 ShowPlanner.nominalSeasonalLikelihood[iSeasonality];
        //         }
        //         System.out.println(String.format("%s: [*,rec=%d,ses=%d] = %22s %18s %18s", status, iRecency, iSeasonality, Arrays.toString(picks),
        //                                          Arrays.toString(photos), Arrays.toString(margin)));
        //     }
        // }

        // check that higher nominal relationship likelihood results in more average picks.
        // counts should be monotonically decreasing as the index decreases for each dimension
        System.out.println("sameDay, inSeason, outOfSeason, oppositeSeason");
        for (int iRelationship=0; iRelationship<ShowPlanner.numRelationshipIntervals; iRelationship++) {
            for (int iRecency=0; iRecency<ShowPlanner.numRecencyIntervals; iRecency++) {
                int[] photos = new int[ShowPlanner.numSeasonalityIntervals];
                int[] picks = new int[ShowPlanner.numSeasonalityIntervals];
                String status = "PASS";
                int lastPickAvg = Integer.MAX_VALUE;
                for (int iSeasonality=0; iSeasonality<ShowPlanner.numSeasonalityIntervals; iSeasonality++) {
                    int thisPhotoCount = numPhotosByGroup[iRelationship][iRecency][iSeasonality];
                    photos[iSeasonality] = thisPhotoCount;
                    int thisPickCount = numPicksByGroup[iRelationship][iRecency][iSeasonality];
                    if (thisPickCount == 0) {
                        continue;
                    }
                    int thisPickAvg = (int)Math.round((thisPickCount*1.0)/thisPhotoCount);
                    picks[iSeasonality] = thisPickAvg;
                    if (thisPickAvg > lastPickAvg) {
                        status = "FAIL";
                        // System.out.println(String.format("FAIL: numPicksByGroup[%d][%d][%d]=%d, numPicksByGroup[%d][%d][%d]=%d",
                        //                                  iRelationship, iRecency, iSeasonality, thisCount,
                        //                                  iRelationship-1, iRecency, iSeasonality, lastCount));
                    }
                    lastPickAvg = thisPickAvg;
                }
                int[] margin = new int[ShowPlanner.numSeasonalityIntervals];
                for (int ii=0; ii<photos.length; ii++) {
                    margin[ii] = photos[ii] - ShowPlanner.nominalRelationshipLikelihood[iRelationship] *
                        ShowPlanner.nominalRecencyLikelihood[iRecency] *
                        ShowPlanner.nominalSeasonalLikelihood[ii];
                }
                System.out.println(String.format("%s: [rel=%d,rec=%d,ses=*] = %22s %18s %18s", status, iRelationship, iRecency, Arrays.toString(picks),
                                                 Arrays.toString(photos), Arrays.toString(margin)));
            }
        }
    }

    private Date makeDate(final int daysAgo) {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DATE, -daysAgo);
        return new Date(calendar.getTime().getTime());
    }

    class PickCounter {
        private int counter;

        public PickCounter() {
            counter = 0;
        }

        public int getCounter() {
            return counter;
        }

        public void increment() {
            counter++;
        }
    }
}
