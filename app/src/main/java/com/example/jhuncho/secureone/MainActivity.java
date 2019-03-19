package com.example.jhuncho.secureone;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;
import com.example.jhuncho.secureone.util.Encrypt;
import com.example.jhuncho.secureone.util.RequestHttpURLConnection;

import javax.crypto.Cipher;
import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

public class MainActivity extends AppCompatActivity implements PictureCapturingListener, ActivityCompat.OnRequestPermissionsResultCallback {

    private static final String[] requiredPermissions = {
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.CAMERA,
    };
    private static final int MY_PERMISSIONS_REQUEST_ACCESS_CODE = 1;
    private static final String SERVER_AUTH_URL = "http://10.23.5.235:8080/auth";
    private String seed_text;
    private ImageView uploadBackPhoto;
    private ImageView uploadFrontPhoto;

    //The capture service
    private APictureCapturingService pictureService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        checkPermissions();
        uploadBackPhoto = (ImageView) findViewById(R.id.backIV);
        uploadFrontPhoto = (ImageView) findViewById(R.id.frontIV);
        final Button btn = (Button) findViewById(R.id.startCaptureBtn);
        // getting instance of the Service from PictureCapturingServiceImpl
        pictureService = PictureCapturingServiceImpl.getInstance(this);
        btn.setOnClickListener(v -> {
            showToast("Starting capture!");
            pictureService.startCapturing(this);
        });

        final Button encrypt = (Button) findViewById(R.id.encrypt);
        final Button decrypt = (Button) findViewById(R.id.decrypt);
        encrypt.setOnClickListener(v -> {
            showToast("Photo encrypt ");
            File path = new File(Environment.getExternalStorageDirectory() + "/DCIM/CameraApp");
            for (File file : path.listFiles()) {
                try {
                    Encrypt.crypt(Cipher.ENCRYPT_MODE,seed_text, file, new File(Environment.getExternalStorageDirectory() + "/DCIM/CameraApp/" + "EN_" + file.getName()));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        decrypt.setOnClickListener(v -> {
            File path = new File(Environment.getExternalStorageDirectory() + "/DCIM/CameraApp");
            for (File file : path.listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    return name.startsWith("EN_");
                }
            })) {
                try {
                    Encrypt.crypt(Cipher.DECRYPT_MODE,seed_text, file, new File(Environment.getExternalStorageDirectory() + "/DCIM/CameraApp/" + "DE_" + file.getName()));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            //uploadBackPhoto.setImageBitmap(Encrypt.decrypt());
        });
        // getAuth
        ContentValues param = new ContentValues();
        NetworkTask networkTask = new NetworkTask(SERVER_AUTH_URL, null);
        networkTask.execute();
    }

    private void showToast(final String text) {
        runOnUiThread(() ->
                Toast.makeText(getApplicationContext(), text, Toast.LENGTH_SHORT).show()
        );
    }

    /**
     * We've finished taking pictures from all phone's cameras
     */
    @Override
    public void onDoneCapturingAllPhotos(TreeMap<String, byte[]> picturesTaken) {
        if (picturesTaken != null && !picturesTaken.isEmpty()) {
            showToast("Done capturing all photos!");
            return;
        }
        showToast("No camera detected!");
    }

    /**
     * Displaying the pictures taken.
     */
    @Override
    public void onCaptureDone(String pictureUrl, byte[] pictureData) {
        /*if (pictureData != null && pictureUrl != null) {
            runOnUiThread(() -> {
                final Bitmap bitmap = BitmapFactory.decodeByteArray(pictureData, 0, pictureData.length);
                final int nh = (int) (bitmap.getHeight() * (512.0 / bitmap.getWidth()));
                final Bitmap scaled = Bitmap.createScaledBitmap(bitmap, 512, nh, true);
                if (pictureUrl.contains("0_pic.jpg")) {
                    uploadBackPhoto.setImageBitmap(scaled);
                } else if (pictureUrl.contains("1_pic.jpg")) {
                    uploadFrontPhoto.setImageBitmap(scaled);
                }
            });
            showToast("Picture saved to " + pictureUrl);
        }*/
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_ACCESS_CODE: {
                if (!(grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    checkPermissions();
                }
            }
        }
    }

    /**
     * checking  permissions at Runtime.
     */
    @TargetApi(Build.VERSION_CODES.M)
    private void checkPermissions() {
        final List<String> neededPermissions = new ArrayList<>();
        for (final String permission : requiredPermissions) {
            if (ContextCompat.checkSelfPermission(getApplicationContext(),
                    permission) != PackageManager.PERMISSION_GRANTED) {
                neededPermissions.add(permission);
            }
        }
        if (!neededPermissions.isEmpty()) {
            requestPermissions(neededPermissions.toArray(new String[]{}),
                    MY_PERMISSIONS_REQUEST_ACCESS_CODE);
        }
    }

    public class NetworkTask extends AsyncTask<Void, Void, String> {

        private String url;
        private ContentValues values;

        public NetworkTask(String url, ContentValues values) {

            this.url = url;
            this.values = values;
        }

        @Override
        protected String doInBackground(Void... params) {

            String result; // 요청 결과를 저장할 변수.
            RequestHttpURLConnection requestHttpURLConnection = new RequestHttpURLConnection();
            result = requestHttpURLConnection.request(url, values); // 해당 URL로 부터 결과물을 얻어온다.

            return result;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);

            //doInBackground()로 부터 리턴된 값이 onPostExecute()의 매개변수로 넘어오므로 s를 출력한다.
            seed_text=s;
        }
    }
}
