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

import org.junit.*;
// import org.junit.runner.RunWith;
// import org.powermock.core.classloader.annotations.PrepareForTest;
// import org.powermock.modules.junit4.PowerMockRunner;
// import org.powermock.api.mockito.PowerMockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Mock;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import android.widget.Toast;

import app.familyphotoframe.model.Photo;
import app.familyphotoframe.model.Contact;
import app.familyphotoframe.model.Relationship;
import app.familyphotoframe.repository.PhotoCollection;
import app.familyphotoframe.PhotoFrameActivity;

// @RunWith(PowerMockRunner.class)
// @PrepareForTest(Toast.class)
public class ShowPlannerTest {
    @Mock
    PhotoCollection mockPhotoCollection;

    @Mock
    PhotoFrameActivity mockPhotoFrameActivity;

    // @Mock
    // Toast mockToast;

    /** object under test */
    ShowPlanner showPlanner;

    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);
    }

    // TODO: Add test for cases where fewer photos are available (groups are
    //       empty or only partially populated)
    
    @Test
    public void photosShownAtExpectedRates() {

        Contact[] contacts =
            { new Contact("a", "adam", Relationship.SELF),
              new Contact("b", "beth", Relationship.FAMILY),
              new Contact("c", "carl", Relationship.FRIEND) };

        Date[] dates =
            { makeDate(0),
              makeDate(ShowPlanner.recencyThresholds[0]+1),
              makeDate(ShowPlanner.recencyThresholds[1]+1) };

        Set<Photo> allPhotos = new HashSet<>();

        int numPhotosPerGroup = 100;
        for (int i = 0; i<numPhotosPerGroup; ++i) {
            for (Contact c : contacts) {
                for (Date d : dates) {
                    allPhotos.add(new Photo(c.getName() + "_" + d.getTime() + "_" + i,
                                            null, null, null, c, null, d));
                }
            }
        }

        when(mockPhotoCollection.getPhotos())
            .thenReturn(allPhotos);
        when(mockPhotoCollection.getTimeOfLastDiscovery())
            .thenReturn(makeDate(-1));
        // PowerMockito.mockStatic(Toast.class);
        // PowerMockito.when(Toast.makeText(Matchers.<TestActivity>anyObject(),
        //                                  Mockito.anyString(), Mockito.anyShort()))
        //     .thenReturn(mockToast);

        showPlanner = new ShowPlanner(mockPhotoFrameActivity, mockPhotoCollection);

        int numIterations = 10000;
        int numPhotosPerIteration = 10;

        Map<Photo,PickCounter> pickCount = new HashMap<>();
        for (int ii=0; ii<numIterations; ii++) {
            List<Photo> chosenPhotos = showPlanner.getPhotosToSchedule(numPhotosPerIteration);
            for (Photo photo : chosenPhotos) {
                if (pickCount.containsKey(photo)) {
                    pickCount.get(photo).increment();
                } else {
                    pickCount.put(photo, new PickCounter());
                }
            }
        }

        int numExpectedPhotos = allPhotos.size();
        int numMissingPhotos = numExpectedPhotos - pickCount.size();
        if (numMissingPhotos > 0) {
            System.out.println("FAIL: " + numMissingPhotos + " of " + numExpectedPhotos +
                               " photos were never shown");
        } else {
            System.out.println("PASS: all " + numExpectedPhotos + " photos were shown at least once");
        }

        int totalNumPicks = 0;
        int totalNumPicksByRelationship[] = new int[contacts.length];
        int totalNumPicksByRecency[] = new int[dates.length];
        for (Map.Entry<Photo,PickCounter> pair : pickCount.entrySet()) {
            Photo photo = pair.getKey();
            int count = pair.getValue().getCounter();
            int iRelationship = photo.getOwner().getRelationship().getValue();
            int iRecency;
            for (iRecency = 0; iRecency < dates.length; ++iRecency) {
                if (photo.getDateTaken() == dates[iRecency]) {
                    break;
                }
            }
            totalNumPicks += count;
            totalNumPicksByRelationship[iRelationship] += count;
            totalNumPicksByRecency[iRecency] += count;
        }

        int numExpectedPicks = numIterations * numPhotosPerIteration;
        if (totalNumPicks != numExpectedPicks) {
            System.out.println("Warning: totalNumPicks (" + totalNumPicks + ") != " +
                               " numExpectedPicks(" + numExpectedPicks + "}");
        }

        DecimalFormat df = new DecimalFormat("#.00");
        final double tolerance = 0.5;

        int totalRelationshipLikelihood = 0;
        for (int nominalLikelihood : showPlanner.nominalRelationshipLikelihood)
            totalRelationshipLikelihood += nominalLikelihood;

        for (int i = 0; i < totalNumPicksByRelationship.length; ++i) {
            double expectedPercentage = 100.0 * showPlanner.nominalRelationshipLikelihood[i] /
                totalRelationshipLikelihood;
            double measuredPercentage = 100.0 * totalNumPicksByRelationship[i] / totalNumPicks;
            if (Math.abs(expectedPercentage - measuredPercentage) < tolerance) {
                System.out.print("PASS: ");
            } else {
                System.out.print("FAIL: ");
            }
            System.out.println(df.format(measuredPercentage) + "% of the photos were taken by " +
                               contacts[i].getName() + ". " + df.format(expectedPercentage) + "% expected");

            // TODO: print median/min/max count of a photo
        }

        int totalRecencyLikelihood = 0;
        for (int nominalLikelihood : showPlanner.nominalRecencyLikelihood)
            totalRecencyLikelihood += nominalLikelihood;

        for (int i = 0; i < totalNumPicksByRecency.length; ++i) {
            double expectedPercentage = 100.0 * showPlanner.nominalRecencyLikelihood[i] /
                totalRecencyLikelihood;
            double measuredPercentage = 100.0 * totalNumPicksByRecency[i] / totalNumPicks;
            if (Math.abs(expectedPercentage - measuredPercentage) < tolerance) {
                System.out.print("PASS: ");
            } else {
                System.out.print("FAIL: ");
            }
            System.out.println(df.format(measuredPercentage) + "% of the photos were taken on " +
                               dates[i] + ". " + df.format(expectedPercentage) + "% expected");

            // TODO: print median/min/max count of a photo
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
            counter = 1;
        }

        public int getCounter() {
            return counter;
        }

        public void increment() {
            counter++;
        }
    }
}
