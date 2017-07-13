package com.example.furszy.contactsapp.ui.settings_restore;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckedTextView;
import android.widget.Spinner;
import android.widget.Toast;

import com.example.furszy.contactsapp.BaseActivity;
import com.example.furszy.contactsapp.R;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by Neoperol on 6/22/17.
 */

public class SettingsRestoreActivity extends BaseActivity {
    private static final int OPTIONS_RESTORE = 200;
    private static final int MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 500;
    private Spinner spinner_files;
    private List<File> fileList = new ArrayList<>();
    private ArrayAdapter<String> adapter;


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        MenuItem menuItem = menu.add(0,OPTIONS_RESTORE,0,"Restore");
        menuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId()==OPTIONS_RESTORE){
            int selected = spinner_files.getSelectedItemPosition();
            if (selected < 0) {
                return true;
            }
            anRedtooth.restoreFrom(fileList.get(selected),null);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onCreateView(Bundle savedInstanceState, ViewGroup container) {
        View root = getLayoutInflater().inflate(R.layout.setttings_restore_activity, container);
        setTitle("Restore profile");
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        checkPermissions();


        //Open File Folder
        spinner_files = (Spinner) root.findViewById(R.id.spinner_files);
        fileList = listFiles();
        List<String> list = new ArrayList<>();
        for (File file : fileList) {
            list.add(file.getName());
        }
        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item,list){
            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                CheckedTextView view = (CheckedTextView) super.getDropDownView(position, convertView, parent);
                view.setTextColor(Color.BLACK);
                return view;
            }
        };
        spinner_files.setAdapter(adapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        fileList = listFiles();
        List<String> list = new ArrayList<>();
        for (File file : fileList) {
            list.add(file.getName());
        }
        adapter.clear();
        adapter.addAll(list);
        adapter.notifyDataSetChanged();
    }

    private List<File> listFiles() {
        File backupDir = app.getBackupDir();
        if (backupDir.isDirectory()){
            File[] fileArray = backupDir.listFiles();
            if (fileArray!=null){
                for (File file : fileArray) {
                    if (PROFILE_FILE_FILTER.accept(file)) {
                        fileList.add(file);
                    }
                }
            }
        }
        for (final String filename : fileList())
            if (filename.startsWith("backup_iop_connect"))
                fileList.add(new File(getFilesDir(), filename));

        // sort
        Collections.sort(fileList, new Comparator<File>() {
            @Override
            public int compare(final File lhs, final File rhs)
            {
                return lhs.getName().compareToIgnoreCase(rhs.getName());
            }
        });
        return fileList;
    }

    private void checkPermissions() {
        // Assume thisActivity is the current activity
        if (Build.VERSION.SDK_INT > 22) {

            int permissionCheck = ContextCompat.checkSelfPermission(this,
                    Manifest.permission.READ_EXTERNAL_STORAGE);

            // Here, thisActivity is the current activity
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {

                // Should we show an explanation?
                if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.READ_EXTERNAL_STORAGE)) {

                    // Show an expanation to the user *asynchronously* -- don't block
                    // this thread waiting for the user's response! After the user
                    // sees the explanation, try again to request the permission.

                } else {

                    // No explanation needed, we can request the permission.

                    ActivityCompat.requestPermissions(this,
                            new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                            MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);

                    // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                    // app-defined int constant. The callback method gets the
                    // result of the request.
                }
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 1: {

                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    Toast.makeText(this, "Permission denied to read your External storage", Toast.LENGTH_SHORT).show();
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    public static final FileFilter PROFILE_FILE_FILTER = new FileFilter() {
        @Override
        public boolean accept(File pathname) {
            if (pathname.getAbsolutePath().contains("backup_iop_connect")){
                return true;
            }
            return false;
        }
    };
}
