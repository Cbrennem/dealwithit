package com.example.threegnome.dealwithit;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.app.DialogFragment;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.BitmapDrawable;
import android.media.AudioManager;
import android.media.ExifInterface;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.view.menu.ActionMenuItemView;
import android.support.v7.widget.ShareActionProvider;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.util.SparseArray;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.face.*;

public class MainActivity extends AppCompatActivity {

    //Option data
    int MUSIC_CHOICE_OPTION = 0;

    //for LOG
    private static final String TAG = "Dealwithit";
    private static final boolean DEBUG = true;

    //The hard coded center for the glasses picture and the distances between eyes
    private final static float GLASSES_PIXEL_CENTER_X = 370;
    private final static float GLASSES_PIXEL_CENTER_Y = 50;
    private final static float GLASSES_PIXEL_EYE_DISTANCE_LENGTH = 240;

    //Intent Codes
    private static final int REQUEST_IMAGE_OPEN = 1;
    private static final int REQUEST_AUDIO_OPEN = 2;
    private static final int REQUEST_TAKE_A_PICTURE = 3;


    private ShareActionProvider mShareActionProvider;

    ImageView imgviewPhoto;
    AudioManager mAudioManager;
    AudioManager.OnAudioFocusChangeListener mAfListener;
    MediaPlayer mediaPlayerParty;
    List<ImageView> listImgviewGlasses;

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mAudioManager.abandonAudioFocus(mAfListener);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mediaPlayerParty.setVolume(0, 0);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mediaPlayerParty.setVolume(1, 1);
    }


    //Inflate menus.xml menu list to top toolbar
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        MenuInflater inflater=getMenuInflater();
        inflater.inflate(R.menu.topmenu,menu);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.btnSavePic:
                saveImage();
                return true;
            case R.id.btnShare:
                onShare();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void onShare() {

        if (imgviewPhoto.getDrawable() != null) {

            Intent share = new Intent();

            share.setAction(Intent.ACTION_SEND);
            share.putExtra(Intent.EXTRA_STREAM, Uri.parse(saveImage()));
            share.setType("image/jpeg");

            startActivity(Intent.createChooser(share, "Share image using..."));
        }

    }

    //Return the URI of file
    public String saveImage() {

        if (imgviewPhoto.getDrawable() != null) {


            File path;
            String appDirectoryName = "Dealwithit";

            //Check if there is external storage
            if( Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                //Create a directory for this app.  This uses the public PICTURES folder and
                //creates it there
                path = new File(
                        Environment.getExternalStoragePublicDirectory(
                                Environment.DIRECTORY_PICTURES).getAbsolutePath()
                        + File.separator + appDirectoryName );
                path.mkdir();
            } else {
                path = Environment.getDataDirectory();
            }

            try {

                String filename = File.separator + "dwi" + System.currentTimeMillis() +  ".jpeg";

                //Write a new File at the path specified
                FileOutputStream out = new FileOutputStream(path.getAbsolutePath() + filename);

                //draw the combined layouts of the photo and the glasses
                RelativeLayout pictureLayout = (RelativeLayout) findViewById(R.id.pictureLayout);
                pictureLayout.setDrawingCacheEnabled(true);

                (pictureLayout.getDrawingCache()).compress(Bitmap.CompressFormat.JPEG,100,out );

                pictureLayout.destroyDrawingCache();

                out.close();

                //To update the gallery
                sendBroadcast(
                        new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,
                                Uri.parse( "file://" + path.getAbsolutePath() + filename ))

                );

                Toast.makeText(this, "Picture Saved", Toast.LENGTH_SHORT).show();

                return "file://" + path.getAbsolutePath() + filename;

            } catch (Exception e ) {
                e.printStackTrace();
            }
        }

        Toast.makeText(this, "No Image to save!", Toast.LENGTH_SHORT).show();

        return null;
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Set what the volume keys control
        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        mAudioManager = (AudioManager) this.getSystemService(Context.AUDIO_SERVICE);

        mAfListener =
                new AudioManager.OnAudioFocusChangeListener() {
                    @Override
                    public void onAudioFocusChange(int focusChange) {
                        if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
                            mediaPlayerParty.pause();
                        } else if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {
                            mediaPlayerParty.start();
                        } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS) {

                            mAudioManager.abandonAudioFocus(mAfListener);
                            // Stop playback
                        }
                    }
                };




        Toolbar topToolbar = (Toolbar) findViewById(R.id.topToolbar);
        setSupportActionBar(topToolbar);

        //Code relating to bottom toolbar
        Point P = new Point(0,0);
        getWindowManager().getDefaultDisplay().getSize(P);

        Toolbar btmToolbar = (Toolbar) findViewById(R.id.btmToolbar);
        btmToolbar.inflateMenu(R.menu.menu);
        btmToolbar.setPadding(0
                ,0
                ,P.x/7
                ,0);

        btmToolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {

                switch (item.getItemId()) {
                    case R.id.btnTakeAPic:
                        onTakeaPicture();
                        return true;
                    case R.id.btnPic:
                        onClickChoosePhoto();
                        return true;
                    case R.id.btnAnimate:
                        onAnimate();
                        return true;
                    case R.id.btnChangeMusic:
                        onChangeMusic();
                        return true;
                    case R.id.btnEditText:
                        onEditText();
                        return true;
                }

                return false;
            }
        });

        imgviewPhoto = (ImageView) findViewById(R.id.photoView);

        //used to draw a second layer over the image if needed
        listImgviewGlasses = new ArrayList<>();

        mediaPlayerParty = MediaPlayer.create(this, R.raw.wholikestoparty);
        mediaPlayerParty.setLooping(true);

    }


    public void onTakeaPicture() {


        Intent takepic = new Intent();
        takepic.setAction(MediaStore.ACTION_IMAGE_CAPTURE);

        if (takepic.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takepic, REQUEST_IMAGE_OPEN);
        }


    }

    public void onClickChoosePhoto() {


        resetHomeScreen();

        Intent intentChoosePicture = new Intent();
        intentChoosePicture.setAction(Intent.ACTION_PICK);
        intentChoosePicture.setType("image/*");

        if (intentChoosePicture.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(intentChoosePicture, REQUEST_IMAGE_OPEN);
        }

    }

    public void resetHomeScreen() {

        mediaPlayerParty.stop();

        try {
            mediaPlayerParty.prepare();
            mediaPlayerParty.seekTo(0);

        } catch (IOException e) {
            e.printStackTrace();
        }

        findViewById(R.id.txtDealWithIt).setVisibility(View.INVISIBLE);
        findViewById(R.id.mainLayout).setBackgroundResource(R.color.background);

    }

    public void onChangeMusic() {

        resetHomeScreen();

        DialogFragment changeMusicFragment = new pickMusicDialogFragment();
        changeMusicFragment.show(getFragmentManager(), "Change Music");

    }

    public void doSelectMusic(int which) {

        if (which != 2) {

            AssetFileDescriptor afd = null;

            if (which == 0) {
                afd = this.getResources().openRawResourceFd(R.raw.wholikestoparty);
                MUSIC_CHOICE_OPTION = 0;
            }
            if (which == 1) {
                afd = this.getResources().openRawResourceFd(R.raw.spazzmaticapolka);
                MUSIC_CHOICE_OPTION = 1;
            }

            try {
                mediaPlayerParty.reset();
                mediaPlayerParty.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getDeclaredLength());
                mediaPlayerParty.prepare();
                afd.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (which == 2) {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(intent, REQUEST_AUDIO_OPEN);

            MUSIC_CHOICE_OPTION = 2;
        }


    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == REQUEST_AUDIO_OPEN && resultCode == RESULT_OK) {

            Uri uri = data.getData();

            try {
                mediaPlayerParty.reset();
                mediaPlayerParty.setDataSource(this, uri);
                mediaPlayerParty.prepare();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // Get the picture and put in on the screen then detect faces and prepare glasses
        //for animation
        if (( requestCode == REQUEST_IMAGE_OPEN || requestCode == REQUEST_TAKE_A_PICTURE)
                && resultCode == RESULT_OK) {

            Uri photoUri = data.getData();

            FileDescriptor fd;

            try {

                Bitmap bmp;

                //the exif has tag info on the photo.  Used to get the orientation info of the photo
                ExifInterface exif = new ExifInterface(getRealPathFromURI(photoUri));
                String exifRotation = exif.getAttribute(ExifInterface.TAG_ORIENTATION);

                fd = getContentResolver().openFileDescriptor(photoUri, "r").getFileDescriptor();

                //Get a bitmap that is scaled down based on the size of the picturelayout
                bmp = decodeSampledBitmapFromStream(fd, findViewById(R.id.pictureLayout).getWidth(),
                        findViewById(R.id.pictureLayout).getHeight());

                //rotate the bmp based on Exifinterface data
                bmp = rotateBmpUsingExif(bmp, exifRotation);

                imgviewPhoto.setImageDrawable(new BitmapDrawable(getResources(), bmp));

                detectFacesAndPrepareGlasses();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    public void onEditText() {

        final EditText txtView = (EditText) findViewById(R.id.txtDealWithIt);
        txtView.setEnabled(true);
        txtView.setVisibility(EditText.VISIBLE);
        txtView.setInputType(InputType.TYPE_CLASS_TEXT);
        txtView.selectAll();

        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.showSoftInput(txtView, InputMethodManager.SHOW_IMPLICIT);

        txtView.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {

                if ((event.getAction() == KeyEvent.ACTION_DOWN) &&
                        (keyCode == KeyEvent.KEYCODE_ENTER)) {

                    txtView.setInputType(InputType.TYPE_NULL);
                    txtView.clearFocus();
                    txtView.setSelection(0);
                    txtView.setVisibility(View.INVISIBLE);
                    txtView.setEnabled(false);

                    return true;
                }
                return false;
            }
        });
    }



    //remove from list of glasses and remove from the screen
    public void removeGlassesfromList() {

        if (!listImgviewGlasses.isEmpty()) {
            ImageView I;

            while (listImgviewGlasses.size() != 0) {
                I = listImgviewGlasses.get(0);
                ((ViewGroup) I.getParent()).removeView(I);
                listImgviewGlasses.remove(0);
            }
        }
    }


    public void detectFacesAndPrepareGlasses() {

        Bitmap bmp = ((BitmapDrawable)imgviewPhoto.getDrawable()).getBitmap();


        if (bmp != null) {
            Frame.Builder frameBuilder = new Frame.Builder();

            frameBuilder.setBitmap(bmp);

            Frame frame = frameBuilder.build();

            FaceDetector.Builder fdb =
                    new FaceDetector.Builder(this);
            fdb.setLandmarkType(FaceDetector.ALL_LANDMARKS)
                    .setMinFaceSize((float) 0.05)
                    .setTrackingEnabled(false);
            FaceDetector faceDetector = fdb.build();
            SparseArray<Face> facesList = faceDetector.detect(frame);
            faceDetector.release();


            drawGlassesAtLocation(facesList);


        }
    }

    //Return Point Array of Midpoint of all faces detected
    //Returns null if no faces detected
    public PointF getPointBetweenEyes(Face face) {


        if (face != null) {

            PointF P = new PointF();

            // Left Eye is the Photos Left Eye, not your left
            PointF leftEye = new PointF(1, 1);
            PointF rightEye = new PointF(0, 0);

            for (Landmark landmark : face.getLandmarks()) {

                if (landmark.getType() == Landmark.LEFT_EYE) {
                    leftEye = landmark.getPosition();
                } else if (landmark.getType() == Landmark.RIGHT_EYE) {
                    rightEye = landmark.getPosition();
                }
            }

            float x = rightEye.x + ((leftEye.x - rightEye.x) / 2);
            float y = rightEye.y + ((leftEye.y - rightEye.y) / 2);


            return (new PointF(x, y));
        }

        return null;
    }

    public float getEyeDistance(Face face) {

        if( face != null ) {

            float leftEye = 0;
            float rightEye = 0;

            for (Landmark landmark : face.getLandmarks()) {

                if (landmark.getType() == Landmark.LEFT_EYE) {
                    leftEye = landmark.getPosition().x;
                } else if (landmark.getType() == Landmark.RIGHT_EYE) {
                    rightEye = landmark.getPosition().x;
                }
            }

            return leftEye - rightEye;
        }

        return 0;
    }


    public void drawGlassesAtLocation(SparseArray<Face> facesList) //PointF[] P, float[] eyeCenterPoints)
    {


        //First remove all the glasses from the list if there are any
        removeGlassesfromList();

        RelativeLayout pictureLayout = (RelativeLayout) findViewById(R.id.pictureLayout);
        ImageView imgviewGlasses;




        for (int i = 0; i != facesList.size(); i++) {


            PointF eyeCenterPoint = getPointBetweenEyes(facesList.valueAt(i));


            if (eyeCenterPoint != null) {

                float eyeDistanceLength = getEyeDistance(facesList.valueAt(i));

                float[] imgviewPhotoMatrix = new float[9];
                imgviewPhoto.getImageMatrix().getValues(imgviewPhotoMatrix);

                imgviewGlasses = new ImageView(this);

                //I had to move the drawable into the drawable-nodpi so I can do scaling on my own... see density
                imgviewGlasses.setImageResource(R.drawable.dealwithitglasses);

                Matrix m = new Matrix();

                //Scale the glasses image to the size using the ratio between the eyes
                //This is used to scale the glasses to the size of the actual face
                //Makes the glasses smaller or bigger depending on this number
                float scaledGlasses = (eyeDistanceLength / GLASSES_PIXEL_EYE_DISTANCE_LENGTH)
                        * imgviewPhotoMatrix[Matrix.MSCALE_X];
                m.setScale(scaledGlasses, scaledGlasses);


                imgviewGlasses.setImageMatrix(m);
                imgviewGlasses.setScaleType(ImageView.ScaleType.MATRIX);

                pictureLayout.addView(imgviewGlasses);

                // The position of the glasses
                // We must get the scaled image in the Imageview and the Left and Top position of
                // that image within the imageview.
                //Scale the position with the image in the imageView and then the top position of
                // the image will be the empty space from the imageview to the image.

                //Draw the glasses facting in the locaiton of the image and its scale and center the
                // glasses at the center of the eyes.
                imgviewGlasses.setTranslationX(
                        (eyeCenterPoint.x * imgviewPhotoMatrix[Matrix.MSCALE_X]
                                + imgviewPhotoMatrix[Matrix.MTRANS_X])
                                - (GLASSES_PIXEL_CENTER_X * scaledGlasses));

                imgviewGlasses.setTranslationY(
                        (eyeCenterPoint.y * imgviewPhotoMatrix[Matrix.MSCALE_Y]
                                + imgviewPhotoMatrix[Matrix.MTRANS_Y])
                                - (GLASSES_PIXEL_CENTER_Y * scaledGlasses));

                imgviewGlasses.setVisibility(ImageView.INVISIBLE);

                listImgviewGlasses.add(imgviewGlasses);




            }
        }
    }

    public String getRealPathFromURI(Uri uri) {
        try {
            String[] proj = {MediaStore.Images.Media.DATA};
            Cursor cursor = getContentResolver().query(uri, proj, null, null, null);
            int colIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            return cursor.getString(colIndex);
        } catch (Exception e) {
            return uri.getPath();
        }
    }


    private Bitmap rotateBmpUsingExif(Bitmap origBmp, String exifRotation) {

        if (exifRotation != null) {
            Matrix m = new Matrix();

            switch (Integer.parseInt(exifRotation)) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    m.setRotate(90);
                    return Bitmap.createBitmap(origBmp, 0, 0, origBmp.getWidth(), origBmp.getHeight(), m, true);
                case ExifInterface.ORIENTATION_ROTATE_180:
                    m.setRotate(180);
                    return Bitmap.createBitmap(origBmp, 0, 0, origBmp.getWidth(), origBmp.getHeight(), m, true);
                case ExifInterface.ORIENTATION_ROTATE_270:
                    m.setRotate(270);
                    return Bitmap.createBitmap(origBmp, 0, 0, origBmp.getWidth(), origBmp.getHeight(), m, true);
            }
        }

        // If there is no change, return the original image
        return origBmp;
    }

    public static Bitmap decodeSampledBitmapFromStream(FileDescriptor res, int reqWidth, int reqHeight) {
        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();


        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFileDescriptor(res, null, options);

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

        //Now return the bitmap!
        options.inJustDecodeBounds = false;

        return BitmapFactory.decodeFileDescriptor(res, null, options);
    }

    public static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            // Calculate the largest inSampleSize value that is a power of 2 and
            // height and width ARE NOT larger than the requested height and width.
            while ((height / inSampleSize) > reqHeight
                    || (width / inSampleSize) > reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }

    public void onAnimate() {

        RelativeLayout pictureLayout = (RelativeLayout) findViewById(R.id.pictureLayout);

        EditText t = (EditText) findViewById(R.id.txtDealWithIt);
        t.setVisibility(View.VISIBLE);
        flashBackground();





        int result = mAudioManager.requestAudioFocus(mAfListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);


        if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            mediaPlayerParty.start();
        }

        if (!listImgviewGlasses.isEmpty()) {
            float lowestGlassesYPosition = 0;

            for (int i = 0; i < listImgviewGlasses.size();i++) {
                float temp = listImgviewGlasses.get(i).getTranslationY();

                if( temp > lowestGlassesYPosition )
                    lowestGlassesYPosition = temp;

            }

            for (int i = 0; i < listImgviewGlasses.size(); i++) {
                ImageView glasses = listImgviewGlasses.get(i);

                float startPos = glasses.getTranslationY() - lowestGlassesYPosition - glasses.getHeight(); //- pictureLayout.getHeight();
                float endPos = glasses.getTranslationY();

                glasses.setVisibility(ImageView.VISIBLE);
                ObjectAnimator glassesAnimator = ObjectAnimator.ofFloat(glasses, "TranslationY", startPos, endPos);
                glassesAnimator.setDuration(3000);

                glassesAnimator.addListener(new Animator.AnimatorListener() {
                    @Override
                    public void onAnimationStart(Animator animation) {
                        ActionMenuItemView btn = (ActionMenuItemView) findViewById(R.id.btnAnimate);
                        btn.setEnabled(false);
                    }

                    @Override
                    public void onAnimationEnd(Animator animation) {
                        ActionMenuItemView btn = (ActionMenuItemView) findViewById(R.id.btnAnimate);
                        btn.setEnabled(true);
                    }

                    @Override
                    public void onAnimationCancel(Animator animation) {

                        ActionMenuItemView btn = (ActionMenuItemView) findViewById(R.id.btnAnimate);
                        btn.setEnabled(true);
                    }

                    @Override
                    public void onAnimationRepeat(Animator animation) {

                    }
                });

                glassesAnimator.start();

            }
        } else {
            Toast.makeText(this, "No faces detected!", Toast.LENGTH_SHORT).show();
        }

    }

    public void flashBackground() {
        LinearLayout mainLayout = (LinearLayout) findViewById(R.id.mainLayout);
        mainLayout.setBackgroundResource(R.drawable.flashcolor);
        AnimationDrawable anim = (AnimationDrawable) mainLayout.getBackground();
        anim.start();
    }
}