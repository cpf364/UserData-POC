package com.zebra.mdna.userdata_poc;

import androidx.appcompat.app.AppCompatActivity;

import android.app.backup.BackupManager;
import android.app.backup.FullBackupDataOutput;
import android.app.backup.IBackupManager;
import android.app.backup.IFullBackupRestoreObserver;
import android.content.pm.IPackageManager;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.os.ServiceManager;


import android.os.SystemProperties;
import android.os.storage.DiskInfo;
import android.os.storage.StorageManager;
import android.os.storage.VolumeInfo;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.symbol.osx.lite.IOsxLiteManager;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class MainActivity extends AppCompatActivity {
    public static final String TAG = "ZEBRA BACKUP";

    File file;
    IBackupManager bm = null;
    private StorageManager mStorageManager = null;
    EditText pkgName1;
    Button backup, fde_to_fbe, restore,data_system;

    //private ILockSettings mLockSettingsService;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        setContentView(R.layout.activity_main);
        pkgName1 = (EditText) findViewById(R.id.pkgName);

        backup = (Button) findViewById(R.id.backup);
        fde_to_fbe = (Button) findViewById(R.id.fdetofbe);
        restore = (Button) findViewById(R.id.restore);
        data_system = (Button)findViewById(R.id.data_system);
        mStorageManager = getSystemService(StorageManager.class);

        backup.setOnClickListener(BackupOnclick);
        fde_to_fbe.setOnClickListener(FDETOFBEOnclick);
        restore.setOnClickListener(RestoreOnclick);
        data_system.setOnClickListener(DataSystemOnclcik);

        ZebraCheckBypass();
    }

    public void ZebraCheckBypass(){
        Log.d(TAG, "ZebraCheckBypass: ");
        SystemProperties.set("persist.sys.zebra.backup", "true");
     }

    @Override
    protected void onResume() {
        super.onResume();

        String isFBE = SystemProperties.get("ro.boot.fbe", "-1");
        if (isFBE.equals("1")) {
            fde_to_fbe.setVisibility(View.INVISIBLE);
        } else if (isFBE.equals("0")){
            fde_to_fbe.setVisibility(View.VISIBLE);
         }

    }

    public File getFilePath(){
      try {
          List<VolumeInfo> volumes = mStorageManager.getVolumes();
          for(VolumeInfo vol : volumes){
              DiskInfo disk = vol.getDisk();
              if(disk != null && (disk.isSd() && (vol.getState() == VolumeInfo.STATE_MOUNTED))){
                  file = new File(vol.getPath(),"zebra.ab");
                  Log.d(TAG, "onCreate: file path "+file.getPath());
                  return file;

              }
          }
      } catch (Exception e) {
          Log.e(TAG, "unable to mount external storage, "+ e.getMessage());
      }
       return null;
  }

    View.OnClickListener BackupOnclick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {


            file = getFilePath();
            if(file == null){
                Toast.makeText(MainActivity.this, "File Not found at backup", Toast.LENGTH_SHORT).show();
            }
            ParcelFileDescriptor fd = null;
            try {
                Log.e(TAG, "Zebra Backup Start");
                file.createNewFile();

                fd  = ParcelFileDescriptor.open(file,ParcelFileDescriptor.MODE_READ_WRITE);

                String[]pkgList  = pkgName1.getText().toString().split(",");
                for (String s : pkgList){
                    Log.d(TAG, "onClick: PkgName :: "+s);
                }
                bm = IBackupManager.Stub.asInterface(ServiceManager.getService(BACKUP_SERVICE));

                bm.adbBackup(0, fd, false, false, false, false, false,
                        false, false, false, pkgList);


            } catch (RemoteException e) {
                Log.e(TAG, "Unable to invoke backup manager for backup");
                e.printStackTrace();
            }catch (FileNotFoundException e) {
                Log.e(TAG, "FileNotFoundException Unable to invoke backup manager for backup");
                e.printStackTrace();
            }catch (IOException e) {
                Log.e(TAG, "IOException Unable to invoke backup manager for backup");
                e.printStackTrace();
            }  finally {
                if (fd != null) {
                    try {
                        fd.close();
                    } catch (IOException e) {
                        Log.e(TAG, "IO error closing output for backup: " + e.getMessage());
                    }
                }
            }

        }
    };

    View.OnClickListener FDETOFBEOnclick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
                 Log.d(TAG, "onClick: sys.backup_completed change start");
                 SystemProperties.set("sys.backup_completed","1");
                 String backupComplete = SystemProperties.get("sys.backup_completed","-1");
                 Log.d(TAG, "onClick: sys.backup_completed : "+backupComplete);
        }
    };

    View.OnClickListener RestoreOnclick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            file = getFilePath();
            if(file == null){
                Toast.makeText(MainActivity.this, "File Not found at restore ", Toast.LENGTH_SHORT).show();
            }
            ParcelFileDescriptor fd = null;
            try {
                fd  = ParcelFileDescriptor.open(file,ParcelFileDescriptor.MODE_READ_WRITE);
                bm = IBackupManager.Stub.asInterface(ServiceManager.getService(BACKUP_SERVICE));
                bm.adbRestore(0, fd);

            } catch (RemoteException e) {
                Log.e(TAG, "Unable to invoke backup manager for restore");
            }catch (FileNotFoundException e) {
                Log.e(TAG, "FileNotFoundException Unable to invoke restore manager for restore");
                e.printStackTrace();
            }catch (IOException e) {
                Log.e(TAG, "IOException Unable to invoke restore manager for restore");
                e.printStackTrace();
            }  finally {
                if (fd != null) {
                    try {
                        fd.close();
                    } catch (IOException e) {}
                }
            }

        }
    };
    View.OnClickListener DataSystemOnclcik = new View.OnClickListener() {

        @Override
        public void onClick(View v) {

            IOsxLiteManager OsxMgr   = IOsxLiteManager.Stub.asInterface(ServiceManager.getService("osx_lite"));
            try {
                Log.d(TAG, "onClick: deletePrivateFile path :: "+pkgName1.getText().toString());
                OsxMgr.deletePrivateFile(pkgName1.getText().toString());
            } catch (RemoteException e) {
                e.printStackTrace();
            }

        }
    };

}