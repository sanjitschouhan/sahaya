package hitam.epics.sahaya.services;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.TaskStackBuilder;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import hitam.epics.sahaya.R;
import hitam.epics.sahaya.Sahaya;
import hitam.epics.sahaya.support.AnnouncementItem;
import hitam.epics.sahaya.volunteer.AnnouncementsActivity;

/**
 * @author sanjit
 */
public class AnnouncementService extends Service {
    private DatabaseReference discussionRef;

    /*Constructor*/
    public AnnouncementService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        /*Get Firebase Authentication Instance*/
        FirebaseAuth auth = FirebaseAuth.getInstance();

        /*Get Firebase Database Instance*/
        FirebaseDatabase database = FirebaseDatabase.getInstance();

        /*Get discussion database reference*/
        discussionRef = database.getReference("announcements");

        /*Get Default Shared Preferences associated with app*/
        final SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);

        /*ChildEventListener to handle discussion database changes*/
        final ChildEventListener childEventListener = new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                /*Retrieve last read announcementItem time from preferences*/
                long lastMessageTime = sharedPref.getLong("last_announcement", System.currentTimeMillis());

                /*get announcementItem from database*/
                AnnouncementItem announcementItem = dataSnapshot.getValue(AnnouncementItem.class);

                /*compare time of last read announcementItem and announcementItem from database*/
                if (announcementItem.getTime() > lastMessageTime) {
                    /*builder for push notification*/
                    Notification.Builder builder = new Notification.Builder(AnnouncementService.this)
                            .setSmallIcon(R.mipmap.ic_launcher)
                            .setContentTitle("Announcement")
                            .setContentText("There is a new announcement");

                    /*intent for push notification*/
                    Intent resultIntent = new Intent(AnnouncementService.this, AnnouncementsActivity.class);

                    /*build stack for intent calls*/
                    TaskStackBuilder stackBuilder = TaskStackBuilder.create(AnnouncementService.this);
                    stackBuilder.addParentStack(Sahaya.class);
                    stackBuilder.addNextIntent(resultIntent);
                    PendingIntent resultPendingIntent =
                            stackBuilder.getPendingIntent(
                                    0,
                                    PendingIntent.FLAG_UPDATE_CURRENT
                            );

                    /*add resultPendingIndent to builder*/
                    builder.setContentIntent(resultPendingIntent);

                    /*build push notification*/
                    Notification notification = builder.build();
                    notification.defaults |= Notification.DEFAULT_SOUND;
                    notification.defaults |= Notification.DEFAULT_VIBRATE;

                    /*get notification manager*/
                    NotificationManager notificationManager = (NotificationManager) getSystemService(Service.NOTIFICATION_SERVICE);

                    /*show notification*/
                    notificationManager.notify(0, notification);
                }

                /*set last read message time as current time*/
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putLong("last_announcement", System.currentTimeMillis());
                editor.apply();

            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {

            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        };

        /*set childListener to database reference based on user*/
        auth.addAuthStateListener(new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user != null) {
                    discussionRef.addChildEventListener(childEventListener);
                } else {
                    discussionRef.removeEventListener(childEventListener);
                }
            }
        });
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        /*start service in STICKY mode*/
        return START_STICKY;
    }
}
