package com.example.jhuncho.secureone.activities;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.Toast;
import android.widget.ToggleButton;
import com.example.jhuncho.secureone.services.APictureCapturingService;
import com.example.jhuncho.secureone.listeners.PictureCapturingListener;
import com.example.jhuncho.secureone.services.PictureCapturingServiceImpl;
import com.example.jhuncho.secureone.R;
import com.example.jhuncho.secureone.util.Encrypt;
import com.example.jhuncho.secureone.util.RecorderService;
import com.example.jhuncho.secureone.util.RequestHttpURLConnection;

import javax.crypto.Cipher;
import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

public class MainActivity extends Activity {

    private static final String[] requiredPermissions = {
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.CAMERA,
            Manifest.permission.INTERNET,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.MODIFY_AUDIO_SETTINGS,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.SYSTEM_ALERT_WINDOW,
            Manifest.permission.VIBRATE,
            Manifest.permission.ACCESS_NETWORK_STATE,
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
        showToast("앱 실행");
        checkPermissions();

        Button encrypt = (Button) findViewById(R.id.encrypt);
        Button decrypt = (Button) findViewById(R.id.decrypt);
        ToggleButton video = (ToggleButton) findViewById(R.id.video);
        ToggleButton photo = (ToggleButton) findViewById(R.id.photo);
        video.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                if(isChecked){
                    videoCapture();
                }else{
                    videoStop();
                }
            }
        });

        photo.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                if(isChecked){
                    videoCapture();
                }else{
                    videoStop();
                }
            }
        });

        encrypt.setOnClickListener(v -> {
            /*showToast("Photo encrypt ");
            File path = new File(Environment.getExternalStorageDirectory() + "/DCIM/CameraApp");
            for (File file : path.listFiles()) {
                try {
                    Encrypt.crypt(Cipher.ENCRYPT_MODE,seed_text, file, new File(Environment.getExternalStorageDirectory() + "/DCIM/CameraApp/" + "EN_" + file.getName()));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }*/
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
                    Encrypt.crypt(Cipher.DECRYPT_MODE, seed_text, file, new File(Environment.getExternalStorageDirectory() + "/DCIM/CameraApp/" + "DE_" + file.getName()));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            //uploadBackPhoto.setImageBitmap(Encrypt.decrypt());
        });
        // getAuth
        /*ContentValues param = new ContentValues();
        NetworkTask networkTask = new NetworkTask(SERVER_AUTH_URL, null);
        networkTask.execute();*/
    }

    private void showToast(final String text) {
        runOnUiThread(() ->
                Toast.makeText(getApplicationContext(), text, Toast.LENGTH_SHORT).show()
        );
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
            seed_text = s;
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        showToast("앱 종료");
        finish();
    }

    public void videoCapture(){
        // video 촬영 코드
        if(!Settings.canDrawOverlays(this)){
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()));
            startActivity(intent);
        }else{
            Intent intent = new Intent(this, RecorderService.class);
            intent.putExtra(RecorderService.INTENT_VIDEO_PATH, "/DCIM/CameraApp/");
            startService(intent);
            showToast("비디오 촬영 시작");
        }
    }

    public void videoStop(){
        stopService(new Intent(this, RecorderService.class));
        showToast("비디오 촬영 종료");
    }
}