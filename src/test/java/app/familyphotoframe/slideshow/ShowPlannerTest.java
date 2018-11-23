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

    @Test
    public void recentPhotosHaveHigherShowRates_oneContact() {
        List<Contact> contacts = generateContacts();
        Contact adam = contacts.get(0);
        Set<Photo> allPhotos = new HashSet<>();

        Date nowDate = makeDate(0);
        Date medDate = makeDate(ShowPlanner.recencyThresholds[0]+1);
        Date oldDate = makeDate(ShowPlanner.recencyThresholds[1]+1);

        allPhotos.add(new Photo("a_old_1", null, null, null, adam, null, oldDate));
        allPhotos.add(new Photo("a_old_2", null, null, null, adam, null, oldDate));
        allPhotos.add(new Photo("a_old_3", null, null, null, adam, null, oldDate));
        allPhotos.add(new Photo("a_med_1", null, null, null, adam, null, medDate));
        allPhotos.add(new Photo("a_med_2", null, null, null, adam, null, medDate));
        allPhotos.add(new Photo("a_med_3", null, null, null, adam, null, medDate));
        allPhotos.add(new Photo("a_new_1", null, null, null, adam, null, nowDate));
        allPhotos.add(new Photo("a_new_2", null, null, null, adam, null, nowDate));
        allPhotos.add(new Photo("a_new_3", null, null, null, adam, null, nowDate));
        allPhotos.add(new Photo("a_new_4", null, null, null, adam, null, nowDate));
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

        Map<Photo,PickCounter> pickCount = new HashMap<>();
        for (int ii=0; ii<numIterations; ii++) {
            List<Photo> chosenPhotos = showPlanner.getPhotosToSchedule(5);
            for (Photo photo : chosenPhotos) {
                if (pickCount.containsKey(photo)) {
                    pickCount.get(photo).increment();
                } else {
                    pickCount.put(photo, new PickCounter(photo));
                }
            }
        }

        validate(pickCount, contacts);
    }

    private List<Contact> generateContacts() {
        return Arrays.asList(new Contact("a", "adam", Relationship.SELF),
                             new Contact("b", "beth", Relationship.FAMILY),
                             new Contact("c", "carl", Relationship.FRIEND));
    }

    private Date makeDate(final int daysAgo) {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DATE, -daysAgo);
        return new Date(calendar.getTime().getTime());
    }

    private void validate(final Map<Photo,PickCounter> pickCount, final List<Contact> contacts) {
        // order from newest to oldest
        List<PickCounter> orderByDate = new ArrayList(pickCount.values());
        orderByDate.sort(new DateComparator());

        System.out.println("contact, photoId, date taken, count");
        for (Contact contact : contacts) {
            boolean foundMed = false;
            boolean foundNow = false;
            for (PickCounter pickCounter : orderByDate) {
                // check photos from each contact separately
                if (!pickCounter.getPhoto().getOwner().equals(contact)) {
                    continue;
                }
                System.out.println(contact.getName() + ", "
                                   + pickCounter.getPhoto().getId() + ", "
                                   + pickCounter.getPhoto().getDateTaken() + ", "
                                   + pickCounter.getCounter());
            }
        }
    }

    class PickCounter {
        private Photo photo;
        private int counter;

        public PickCounter(Photo photo) {
            this.photo = photo;
            counter = 1;
        }

        public Photo getPhoto() {
            return photo;
        }

        public int getCounter() {
            return counter;
        }

        public void increment() {
            counter++;
        }

    }

    class DateComparator implements Comparator<PickCounter> {
        public int compare(PickCounter o1, PickCounter o2) {
            return o2.getPhoto().getDateTaken().compareTo(o1.getPhoto().getDateTaken());
        }

        public boolean equals(Object obj) {
            return false;
        }
    }
}
