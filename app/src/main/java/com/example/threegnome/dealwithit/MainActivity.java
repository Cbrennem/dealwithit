package com.example.threegnome.dealwithit;

import android.animation.ObjectAnimator;
import android.app.ActionBar;
import android.app.DialogFragment;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.media.ExifInterface;
import android.media.FaceDetector;
import android.media.Image;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.w3c.dom.Attr;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity  {



    private static final String TAG = "Dealwithit";
    private static final boolean DEBUG = true;

    private final static float GLASSES_PIXEL_CENTER_X = 370;
    private final static float GLASSES_PIXEL_CENTER_Y = 50;
    private final static float GLASSES_PIXEL_EYE_DISTANCE_LENGTH = 240;

    private static final int REQUEST_IMAGE_OPEN = 1;

    ImageView imgviewPhoto;
    Bitmap bmpPhoto;
    LayerDrawable lyrdrwPhoto;
    MediaPlayer mediaPlayerParty;

    List<ImageView> listImgviewGlasses;

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

        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar topToolbar = (Toolbar) findViewById(R.id.topToolbar);
        setSupportActionBar(topToolbar);

        Toolbar btmToolbar = (Toolbar) findViewById(R.id.btmToolbar);
        btmToolbar.inflateMenu(R.menu.menu);


        imgviewPhoto = (ImageView) findViewById(R.id.photoView);

        //used to draw a second layer over the image if needed
        Drawable[] layers = {new ColorDrawable(Color.TRANSPARENT), new ColorDrawable(Color.TRANSPARENT)};
        lyrdrwPhoto = new LayerDrawable(layers);
        listImgviewGlasses = new ArrayList<>();
        mediaPlayerParty = MediaPlayer.create(this, R.raw.wholikestoparty);

    }


    public void onClickChoosePhoto(View v)
    {

        Intent test = new Intent();
        test.setAction(Intent.ACTION_PICK);
        test.setType("image/*");

        if (test.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(test, REQUEST_IMAGE_OPEN);
        }

        removeGlassesfromList();
        ((TextView) findViewById(R.id.txtDealWithIt)).setVisibility(View.INVISIBLE);
        ((LinearLayout) findViewById(R.id.mainLayout)).setBackgroundResource(R.color.background);

    }

    public void onChangeMusic(View view) {
        DialogFragment changeMusicFragment = new pickMusicDialogFragment();
        changeMusicFragment.show(getFragmentManager(), "Change Music");
    }

    public void doSelectMusic(int which) {

        AssetFileDescriptor afd = null;

        if(which == 0 ) {
            afd = this.getResources().openRawResourceFd(R.raw.wholikestoparty);
        }
        if(which == 1) {
            afd = this.getResources().openRawResourceFd(R.raw.spazzmaticapolka);
        }

        try {
            mediaPlayerParty.reset();
            mediaPlayerParty.setDataSource(afd.getFileDescriptor(),afd.getStartOffset(),afd.getDeclaredLength());
            mediaPlayerParty.prepare();
            afd.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void onEditText(View view) {
        DialogFragment changeTextFragment = new changeTextDialogFragment();
        changeTextFragment.show(getFragmentManager(), "Change Text");


    }

    public void onEdit(View view) {


    }

    //remove from list of glasses and remove from the screen
    public void removeGlassesfromList()
    {
        if( !listImgviewGlasses.isEmpty() ) {
            ImageView I;

            while( listImgviewGlasses.size() != 0 ) {
                I = listImgviewGlasses.get(0);
                ((ViewGroup) I.getParent()).removeView(I);
                listImgviewGlasses.remove(0);
            }
        }
    }


    public void detectFacesAndPrepareGlasses()
    {

        if (bmpPhoto != null) {

            faceDetect fd = new faceDetect(bmpPhoto, 10);

            drawGlassesAtLocation(fd); // fd.getFacePoints(), fd.getEyeDistance());

        }
    }



    public void onClickDetectFaces(View v)
    {
        if( bmpPhoto != null) {

            faceDetect fd = new faceDetect(bmpPhoto, 10);

            drawGlassesAtLocation(fd); // fd.getFacePoints(), fd.getEyeDistance());

        }
    }


    public void drawGlassesAtLocation(faceDetect faceInfo) //PointF[] P, float[] eyeCenterPoints)
    {
        PointF[] eyeCenterPoints = faceInfo.getFacePoints();
        float[] eyeDistanceLength = faceInfo.getEyeDistance();

        if (eyeCenterPoints != null )
        {

            RelativeLayout pictureLayout = (RelativeLayout) findViewById(R.id.pictureLayout);
            ImageView g1;

//             debugging
//            Bitmap b = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888);
//            b.eraseColor(Color.GREEN);

            for (int i = 0; i < eyeCenterPoints.length; i++) {

                float[] imageMatrix = new float[9];
                imgviewPhoto.getImageMatrix().getValues(imageMatrix);

                g1 = new ImageView(this);

                //I had to move the drawable into the drawable-nodpi so I can do scaling on my own... see density
                g1.setImageResource(R.drawable.dealwithitglasses);

                Matrix m = new Matrix();

                //Scale the glasses image to the size using the ratio between the eyes
                float scaledGlasses = (eyeDistanceLength[i]/GLASSES_PIXEL_EYE_DISTANCE_LENGTH)*imageMatrix[Matrix.MSCALE_X];
                m.setScale(scaledGlasses, scaledGlasses);
                g1.setImageMatrix(m);
                g1.setScaleType(ImageView.ScaleType.MATRIX);

                pictureLayout.addView(g1);

                // The position of the glasses
                // We must get the scaled image in the Imageview and the Left and Top position of
                // that image within the imageview.
                //Scale the position with the image in the imageView and then the top position of
                // the image will be the empty space from the imageview to the image.

                //Draw the glasses facting in the locaiton of the image and its scale and center the glasses at the center of the eyes.

                g1.setTranslationX((eyeCenterPoints[i].x * imageMatrix[Matrix.MSCALE_X] + imageMatrix[Matrix.MTRANS_X])
                        - (GLASSES_PIXEL_CENTER_X * scaledGlasses));

                g1.setTranslationY((eyeCenterPoints[i].y * imageMatrix[Matrix.MSCALE_Y] + imageMatrix[Matrix.MTRANS_Y])
                        - (GLASSES_PIXEL_CENTER_Y * scaledGlasses));

                g1.setVisibility(ImageView.INVISIBLE);

                listImgviewGlasses.add(g1);
            }
        }
    }

    public String getRealPathFromURI(Uri uri)
    {
        try {
            String[] proj = {MediaStore.Images.Media.DATA};
            Cursor cursor = getContentResolver().query(uri,proj,null,null,null);
            int colIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            return cursor.getString(colIndex);
            }
            catch (Exception e) {
                return uri.getPath();
            }
    }

    @Override
    protected void onActivityResult (int requestCode, int resultCode, Intent data)
    {
        if (requestCode == REQUEST_IMAGE_OPEN && resultCode == RESULT_OK) {

            Uri photoUri = data.getData();

            FileDescriptor fd;

            try {

                //the exif has tag info on the photo.  Used to get the orientation info of the photo
                ExifInterface exif = new ExifInterface(getRealPathFromURI(photoUri));
                String exifRotation = exif.getAttribute(ExifInterface.TAG_ORIENTATION);

                fd = getContentResolver().openFileDescriptor(photoUri,"r").getFileDescriptor();

                //Get a bitmap that is scaled down based on the size of the picturelayout
                bmpPhoto = decodeSampledBitmapFromStream(fd, findViewById(R.id.pictureLayout).getWidth(),
                        findViewById(R.id.pictureLayout).getHeight());

                //rotate the bmp based on Exifinterface data
                bmpPhoto = rotateBmpUsingExif(bmpPhoto, exifRotation);



                updateImgviewPhoto(new BitmapDrawable(getResources(), bmpPhoto),
                        new ColorDrawable(Color.TRANSPARENT));

                detectFacesAndPrepareGlasses();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private Bitmap rotateBmpUsingExif(Bitmap origBmp, String exifRotation)
    {

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

    public static Bitmap decodeSampledBitmapFromStream(FileDescriptor res, int reqWidth, int reqHeight)
    {
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

    public static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight)
    {
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


    private void updateImgviewPhoto(Drawable d1, Drawable d2)
    {
        Drawable[] ds = {d1, d2};

        lyrdrwPhoto = new LayerDrawable(ds);

        //SetDrawableByLayerID does work properly,  the previous call always draws on top of the layer
        // or I'm doing something wrong.
        //lyrdrwPhoto.setDrawableByLayerId(lyrdrwPhoto.getId(0), d1);
        //lyrdrwPhoto.setDrawableByLayerId(lyrdrwPhoto.getId(1), new ColorDrawable(Color.TRANSPARENT));

        imgviewPhoto.setImageDrawable(lyrdrwPhoto);

    }

    public void onAnimate(View view)
    {
        RelativeLayout pictureLayout = (RelativeLayout) findViewById(R.id.pictureLayout);

        TextView t = (TextView) findViewById(R.id.txtDealWithIt);
        t.setVisibility(View.VISIBLE);
        flashBackground();
        mediaPlayerParty.start();

        if(!listImgviewGlasses.isEmpty())
        {
            for(int i = 0; i < listImgviewGlasses.size(); i++ ) {
                ImageView glasses = listImgviewGlasses.get(i);

                float startPos = glasses.getTranslationY() - pictureLayout.getHeight();
                float endPos = glasses.getTranslationY();

                glasses.setVisibility(ImageView.VISIBLE);
                ObjectAnimator glassesAnimator = ObjectAnimator.ofFloat(glasses, "TranslationY", startPos, endPos);
                glassesAnimator.setDuration(4000);
                glassesAnimator.start();


            }
        }

    }

    public void flashBackground()
    {
        LinearLayout mainLayout = (LinearLayout) findViewById(R.id.mainLayout);
        mainLayout.setBackgroundResource(R.drawable.flashcolor);
        AnimationDrawable anim = (AnimationDrawable)mainLayout.getBackground();
        anim.start();
    }



}

class faceDetect{

    protected Bitmap bmpFaces;
    protected int MAX_FACES;
    protected FaceDetector.Face[] faces;
    protected int faceCount = 0;

    //Return Point Array of Midpoint of all faces detected
    //Returns null if no faces detected
    public PointF[] getFacePoints() {

        if (faceCount != 0) {

            PointF P[] = new PointF[faceCount];
            PointF tempF = new PointF();

            for (int i = 0; faces[i] != null; i++) {

                faces[i].getMidPoint(tempF);
                P[i] = new PointF(tempF.x, tempF.y);

            }

            return P;
        }

        return null;
    }

    public float[] getEyeDistance()
    {
        if (faceCount != 0) {

            float f[] = new float[faceCount];

            for (int i = 0; faces[i] != null; i++) {
                f[i] = faces[i].eyesDistance();
            }

            return f;
        }

        return null;

    }


    //Constructor
    public faceDetect(Bitmap bmp, int maxFaces){

        if (bmp != null)
        {
            bmpFaces = bmp.copy(Bitmap.Config.RGB_565, true);
            MAX_FACES = maxFaces;
            faces = new FaceDetector.Face[MAX_FACES];
            detectFaces();
        }
    }

    private void detectFaces() {

        FaceDetector fd = new FaceDetector(bmpFaces.getWidth(), bmpFaces.getHeight(), MAX_FACES);
        faceCount = fd.findFaces(bmpFaces, faces);

    }

    public BitmapDrawable getBoxFaceOverlay()
    {

        Bitmap bmpOverlay = Bitmap.createBitmap(bmpFaces.getWidth(),bmpFaces.getHeight(), Bitmap.Config.ARGB_8888);

        Canvas c = new Canvas(bmpOverlay);
        Paint p = new Paint();
        p.setStrokeWidth(10);
        p.setStyle(Paint.Style.STROKE);

        for( int i =0; faces[i] != null; i++)
        {

            PointF pf = new PointF();
            faces[i].getMidPoint(pf);



            c.drawRect(pf.x - (faces[i].eyesDistance()), pf.y - faces[i].eyesDistance()/2,
                    pf.x + (faces[i].eyesDistance()), pf.y + faces[i].eyesDistance()/2, p);


        }

        return new BitmapDrawable( Resources.getSystem() ,bmpOverlay);
    }

}

