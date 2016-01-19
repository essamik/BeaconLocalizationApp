package ch.swisscom.beaconlocalizationapp;

import android.os.Environment;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;

public class PersistUtils {
    public static void saveMeasure(String content, int nbBeacon) {
        String filename = "localization_"+nbBeacon+"_" + System.currentTimeMillis() + ".csv";
        String root = Environment.getExternalStorageDirectory().toString();
        File myDir = new File(root + "/localization_delta");
        myDir.mkdirs();
        File file = new File (myDir, filename);
        if (file.exists ()) file.delete ();

        try {
            OutputStreamWriter out = new OutputStreamWriter(new FileOutputStream(file));
            out.write(content);
            out.flush();
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}