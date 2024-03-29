package com.example.haru;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;
import java.util.List;

public class Map extends AppCompatActivity implements OnMapReadyCallback {

    private GpsTracker gpsTracker;
    private static final int GPS_ENABLE_REQUEST_CODE = 400;
    private GoogleMap googleMap;

    View dialogview;
    EditText edt_md;

    Button mapbtn1;

    SQLiteDatabase sqlDB;
    MemoDBHelper memoHelper;
    String d_name;
    Double d_latitude;
    Double d_longitude;
    List<MarkerData> mlist;
    MarkerOptions markerOptions;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        mapbtn1 = findViewById(R.id.mapbtn1);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        // 뒤로가기
        mapbtn1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setResult(RESULT_CANCELED);
                finish();
            }
        });

        try {
            mlist = new ArrayList();
            MemoDBHelper memoHelper = new MemoDBHelper(this);
            SQLiteDatabase db = memoHelper.getReadableDatabase();
            Cursor cursor = db.rawQuery("SELECT * FROM marker", null);
            if (cursor != null){
                while (cursor.moveToNext()) {
                    MarkerData markerData = new MarkerData();
                    markerData.setId(cursor.getInt(0));
                    markerData.setName(cursor.getString(1));
                    markerData.setLatitude(cursor.getString(2));
                    markerData.setLongitude(cursor.getString(3));

                    mlist.add(markerData);
                }
            }
        }catch (SQLException mSQLException){
            throw mSQLException;
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        this.googleMap = googleMap;

        dialogview=(View)View.inflate(Map.this, R.layout.map_dialog, null);
        edt_md=dialogview.findViewById(R.id.edt_md);

        LatLng latLng = new LatLng(37.351756, 126.742844);
        googleMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
        googleMap.moveCamera(CameraUpdateFactory.zoomTo(15));
        markerOptions = new MarkerOptions().position(latLng).title("정왕역");
        googleMap.addMarker(markerOptions);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED){
            googleMap.setMyLocationEnabled(true);
        }
        memoHelper= new MemoDBHelper(this);

        //맵 터치 시 마커생성
        googleMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(@NonNull LatLng point) {
                Double latitude = point.latitude; //위도
                Double longitude = point.longitude;  //경도
                markerOptions.position(new LatLng(latitude, longitude));
                new AlertDialog.Builder(Map.this)
                        .setTitle("마커 이름을 입력하세요.")
                        .setView(dialogview)
                        .setPositiveButton("확인", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                ViewGroup dialogParentView = (ViewGroup) dialogview.getParent();
                                ContentValues row;
                                sqlDB = memoHelper.getWritableDatabase();
                                row = new ContentValues();
                                row.put("name", edt_md.getText().toString());
                                row.put("latitude", latitude.toString());
                                row.put("longitude", longitude.toString());
                                sqlDB.insert("marker", null, row);
                                memoHelper.close();
                                Toast.makeText(getApplicationContext(), "마커가 저장되었습니다.", Toast.LENGTH_SHORT).show();

                                dialogParentView.removeView(dialogview);
                                LatLng latLng2 = new LatLng(Double.valueOf(latitude.toString()), Double.valueOf(longitude.toString()));
                                googleMap.moveCamera(CameraUpdateFactory.newLatLng(latLng2));
                                googleMap.moveCamera(CameraUpdateFactory.zoomTo(15));
                                markerOptions = new MarkerOptions().position(latLng2).title(edt_md.getText().toString());
                                googleMap.addMarker(markerOptions);
                            }
                        })
                        .setNegativeButton("취소", null)
                        .show();
            }
        });
        for(int i=0; i<mlist.size();i++) {
            String d_name = mlist.get(i).name;
            Double d_latitude = Double.valueOf(mlist.get(i).latitude);
            Double d_longitude = Double.valueOf(mlist.get(i).longitude);

            LatLng latLng2 = new LatLng(d_latitude, d_longitude);
            googleMap.moveCamera(CameraUpdateFactory.newLatLng(latLng2));
            googleMap.moveCamera(CameraUpdateFactory.zoomTo(15));
            markerOptions = new MarkerOptions().position(latLng2).title(d_name);
            googleMap.addMarker(markerOptions);
        }
    }

    // 권한 요청
    public static final int MY_PERMISSIONS_REQUEST_LOCATION = 99;
    @Override
    public void onRequestPermissionsResult(int permsRequestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grandResults) {

        super.onRequestPermissionsResult(permsRequestCode, permissions, grandResults);
        switch (permsRequestCode) {
            case MY_PERMISSIONS_REQUEST_LOCATION: {
                if (grandResults.length > 0 && grandResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                            == PackageManager.PERMISSION_GRANTED) {
                        googleMap.setMyLocationEnabled(true);
                    }
                } else {
                    Toast.makeText(this, "permission denied", Toast.LENGTH_LONG).show();
                }
                return;
            }
        }
    }

    // 서비스 권한 체크
    public boolean checkLocationServicesStatus() {
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case GPS_ENABLE_REQUEST_CODE:
                    if (checkLocationServicesStatus()) {
                        if (checkLocationServicesStatus()) {
                            Log.d("@@@", "onActivityResult : GPS 활성화 되있음");
                            return;
                        }
                    }
                    break;
            }
        }else {
            return;
        }
    }
}