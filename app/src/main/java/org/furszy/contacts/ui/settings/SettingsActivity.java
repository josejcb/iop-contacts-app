package org.furszy.contacts.ui.settings;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import org.libertaria.world.profile_server.DatabaseCollector;
import org.furszy.contacts.App;
import org.furszy.contacts.AppConstants;
import org.furszy.contacts.BaseDrawerActivity;
import org.furszy.contacts.BuildConfig;
import org.furszy.contacts.CrashReporter;
import org.furszy.contacts.R;
import org.furszy.contacts.app_base.ReportIssueDialogBuilder;
import org.furszy.contacts.ui.settings_backup.SettingsBackupActivity;
import org.furszy.contacts.ui.settings_restore.SettingsRestoreActivity;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Neoperol on 6/21/17.
 */

public class SettingsActivity  extends BaseDrawerActivity implements DatabaseCollector {
    private View root;
    private Button buttonRestore;
    private Button buttonBackup;
    private Button btn_report;
    private String versionName = "";
    TextView text_version ;
    @Override
    protected void onCreateView(Bundle savedInstanceState, ViewGroup container) {
        root = getLayoutInflater().inflate(R.layout.settings_activity, container);
        setTitle("Settings");

        // Open Restore
        buttonRestore = (Button) root.findViewById(R.id.btn_restore);
        buttonRestore.setOnClickListener(this);

        // Backup Profile
        buttonBackup = (Button) root.findViewById(R.id.btn_backup);
        buttonBackup.setOnClickListener(this);

        root.findViewById(R.id.btn_delete_contacts).setOnClickListener(this);
        root.findViewById(R.id.btn_delete_contacts).setVisibility(View.GONE);
        root.findViewById(R.id.btn_delete_requests).setOnClickListener(this);
        root.findViewById(R.id.btn_delete_requests).setVisibility(View.GONE);

        Switch switchView = ((Switch)root.findViewById(R.id.switch_background_service));
        switchView.setChecked(app.createProfSerConfig().getBackgroundServiceEnable());
        switchView.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                app.createProfSerConfig().setBackgroundServiceEnable(isChecked);
            }
        });

        btn_report = (Button) root.findViewById(R.id.btn_report);
        btn_report.setOnClickListener(this);

        // APP Version
        text_version = (TextView) root.findViewById(R.id.text_version);
        try {
            versionName = getApplicationContext().getPackageManager().getPackageInfo(getApplicationContext().getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        text_version.setText(versionName);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.btn_delete_contacts){
            //anRedtooth.deteleContacts();
            Toast.makeText(this,"Method cancelled..",Toast.LENGTH_LONG).show();
        }else if(id == R.id.btn_delete_requests) {
            //anRedtooth.deletePairingRequests();
            Toast.makeText(this, "Method cancelled..", Toast.LENGTH_LONG).show();
        }else if(id == R.id.btn_backup) {
            Intent myIntent = new Intent(v.getContext(), SettingsBackupActivity.class);
            startActivity(myIntent);
        }else if(id == R.id.btn_restore) {
            Intent myIntent = new Intent(v.getContext(), SettingsRestoreActivity.class);
            startActivity(myIntent);
        }else if(id == R.id.btn_report){
            launchReportDialog();
        }else
            super.onClick(v);
    }

    private void launchReportDialog() {
        ReportIssueDialogBuilder dialog = new ReportIssueDialogBuilder(
                this,
                "org.furszy.contacts.myfileprovider",
                R.string.report_issuea_dialog_title,
                R.string.report_issue_dialog_message_issue,
                this)
        {
            @Nullable
            @Override
            protected CharSequence subject() {
                return AppConstants.REPORT_SUBJECT_ISSUE+" "+ BuildConfig.VERSION_NAME;
            }

            @Nullable
            @Override
            protected CharSequence collectApplicationInfo() throws IOException {
                final StringBuilder applicationInfo = new StringBuilder();
                CrashReporter.appendApplicationInfo(applicationInfo, (App) getApplication());
                return applicationInfo;
            }

            @Nullable
            @Override
            protected CharSequence collectStackTrace() throws IOException {
                return null;
            }

            @Nullable
            @Override
            protected CharSequence collectDeviceInfo() throws IOException {
                final StringBuilder deviceInfo = new StringBuilder();
                CrashReporter.appendDeviceInfo(deviceInfo, SettingsActivity.this);
                return deviceInfo;
            }
        };
        dialog.show();
    }

    @Override
    public void onResume() {
        super.onResume();
        // to check current activity in the navigation drawer
        setNavigationMenuItemChecked(1);
    }

    @Override
    public List collectData() {
        List<Object> list = new ArrayList<>();
        //// TODO: 7/25/17  
        /*Collection<PairingRequest> list1 = anRedtooth.listAllPairingRequests();
        if (list1 != null) {
            list.addAll(list1);
            list.addAll(anRedtooth.listAllProfileInformation());
        }*/
        return list;
    }
}
