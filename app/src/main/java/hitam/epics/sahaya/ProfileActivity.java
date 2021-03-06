package hitam.epics.sahaya;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.facebook.AccessToken;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.login.LoginManager;
import com.firebase.ui.storage.images.FirebaseImageLoader;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;

import hitam.epics.sahaya.support.UserDetails;

public class ProfileActivity extends Activity {
    private FirebaseUser user;
    private TextView ProfileName;
    private TextView ProfileEmail;
    private TextView ProfilePhone;
    private TextView ProfileAttendance;
    private TextView ProfileRole;
    private ImageView ProfilePic;
    private DatabaseReference detailReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        ProfileName = (TextView) findViewById(R.id.profile_name);
        ProfileEmail = (TextView) findViewById(R.id.profile_email);
        ProfilePhone = (TextView) findViewById(R.id.profile_phone);
        ProfileAttendance = (TextView) findViewById(R.id.profile_attendance);
        ProfileRole = (TextView) findViewById(R.id.profile_role);
        ProfilePic = (ImageView) findViewById(R.id.profile_pic);

        FirebaseAuth auth = FirebaseAuth.getInstance();
        user = auth.getCurrentUser();

        if (user != null) {
            ProfileName.setText(user.getDisplayName());
        }
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference roleReference = database.getReference("/user_types/" + user.getEmail()
                .replace(".", "(dot)")
                .replace("#", "(hash)")
                .replace("$", "(dollar)")
                .replace("[", "(bropen)")
                .replace("]", "(brclose)"));
        roleReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                ProfileRole.setText(dataSnapshot.getValue(String.class));
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        detailReference = database.getReference("/user_details/" + user.getUid());
        detailReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                UserDetails userDetails = dataSnapshot.getValue(UserDetails.class);
                if (userDetails == null) {
                    userDetails = new UserDetails(user.getUid(), user.getDisplayName(), user.getEmail(), "Phone Number Not Available", "NA", System.currentTimeMillis());
                    detailReference.setValue(userDetails);

                }
                ProfileName.setText(userDetails.getName());
                ProfileEmail.setText(userDetails.getEmail());
                ProfilePhone.setText(userDetails.getPhone());
                ProfileAttendance.setText(String.valueOf(userDetails.getAttendance()));
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        ProfileRole.setText(R.string.loading);
        if (user.getPhotoUrl() != null) {

            if (user.getProviders().get(0).equals("password")) {
                StorageReference storageReference = FirebaseStorage.getInstance().getReferenceFromUrl(user.getPhotoUrl().toString());
                if (user.getPhotoUrl() != null) {
                    Glide.with(this)
                            .using(new FirebaseImageLoader())
                            .load(storageReference)
                            .diskCacheStrategy(DiskCacheStrategy.NONE)
                            .into(ProfilePic);
                }
            } else if (!user.getPhotoUrl().equals("")) {
                Glide.with(this)
                        .load(user.getPhotoUrl().toString())
                        .diskCacheStrategy(DiskCacheStrategy.NONE)
                        .into(ProfilePic);
            }
        }

    }

    public void logout(View view) {
        FirebaseAuth.getInstance().signOut();
        if (AccessToken.getCurrentAccessToken() != null) {
            new GraphRequest(
                    AccessToken.getCurrentAccessToken(), "/me/permissions/", null, null, new GraphRequest.Callback() {
                @Override
                public void onCompleted(GraphResponse response) {
                    LoginManager.getInstance().logOut();
                }
            }
            ).executeAsync();
        }
        finish();
    }

    public void EditName(View view) {
        findViewById(R.id.name_view).setVisibility(View.GONE);
        findViewById(R.id.name_edit).setVisibility(View.VISIBLE);

        ((EditText) findViewById(R.id.profile_name_edit)).setText(user.getDisplayName());
    }

    public void SaveName(View view) {
        String newName = ((EditText) findViewById(R.id.profile_name_edit)).getText().toString().trim();

        if (!newName.equals("")) {
            UserProfileChangeRequest userProfileChangeRequest = new UserProfileChangeRequest.Builder()
                    .setDisplayName(newName)
                    .build();
            user.updateProfile(userProfileChangeRequest);
            FirebaseDatabase.getInstance().getReference("user_details")
                    .child(user.getUid())
                    .child("name")
                    .setValue(newName);
            ((TextView) findViewById(R.id.profile_name)).setText(newName);
        }

        findViewById(R.id.name_view).setVisibility(View.VISIBLE);
        findViewById(R.id.name_edit).setVisibility(View.GONE);
    }

    public void CancelNameEdit(View view) {
        findViewById(R.id.name_view).setVisibility(View.VISIBLE);
        findViewById(R.id.name_edit).setVisibility(View.GONE);
    }

    public void ChangeProfilePic(View view) {

        Intent intent = new Intent(Intent.ACTION_PICK,
                MediaStore.Images.Media.INTERNAL_CONTENT_URI);
        intent.setType("image/*");
        intent.putExtra("crop", "true");
        intent.putExtra("scale", true);
        intent.putExtra("outputX", 256);
        intent.putExtra("outputY", 256);
        intent.putExtra("aspectX", 1);
        intent.putExtra("aspectY", 1);
        intent.putExtra("return-data", true);
        startActivityForResult(intent, 1);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 1 && resultCode == RESULT_OK) {
            final Bundle extras = data.getExtras();
            if (extras != null) {
                //Get image
                Bitmap newProfilePic = extras.getParcelable("data");
                ProfilePic.setImageBitmap(newProfilePic);
                uploadPic();
            } else {
                Log.e("onActivityResult: ", "error");
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void uploadPic() {
        StorageReference mStorageRef = FirebaseStorage.getInstance().getReference();

        StorageReference ProfilePicRef = mStorageRef.child("users/" + user.getUid() + "/pp.jpg");

        ProfilePic.setDrawingCacheEnabled(true);
        ProfilePic.buildDrawingCache();
        Bitmap bitmap = ProfilePic.getDrawingCache();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        byte[] data = baos.toByteArray();

        UploadTask uploadTask = ProfilePicRef.putBytes(data);
        uploadTask.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                AlertDialog.Builder builder = new AlertDialog.Builder(ProfileActivity.this, R.style.AppThemeAlert);
                builder.setTitle("Error")
                        .setMessage("Unable to update profile picture. Please try again later.")
                        .setPositiveButton("Close", null);
                builder.create().show();
            }
        }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                @SuppressWarnings("VisibleForTests") Uri photoUrl = taskSnapshot.getDownloadUrl();
                UserProfileChangeRequest profileChangeRequest = new UserProfileChangeRequest.Builder()
                        .setPhotoUri(photoUrl)
                        .build();
                user.updateProfile(profileChangeRequest);
            }
        });
    }
}
