/*
 * Copyright (C) 2020-2021 sunilpaulmathew <sunil.kde@gmail.com>
 *
 * This file is part of Package Manager, a simple, yet powerful application
 * to manage other application installed on an android device.
 *
 */

package com.smartpack.packagemanager.utils;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.widget.Toast;

import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatDelegate;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.InterstitialAd;
import com.google.android.gms.ads.MobileAds;
import com.smartpack.packagemanager.BuildConfig;
import com.smartpack.packagemanager.R;
import com.smartpack.packagemanager.utils.root.RootFile;
import com.smartpack.packagemanager.utils.root.RootUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Locale;

/*
 * Created by sunilpaulmathew <sunil.kde@gmail.com> on February 11, 2020
 * Based on the original implementation on Kernel Adiutor by
 * Willi Ye <williye97@gmail.com>
 */

public class Utils {

    private static Utils sInstance;

    public static Utils getInstance() {
        if (sInstance == null) {
            sInstance = new Utils();
        }
        return sInstance;
    }

    private InterstitialAd mInterstitialAd;

    public static boolean isPackageInstalled(String packageID, Context context) {
        try {
            context.getPackageManager().getApplicationInfo(packageID, 0);
            return true;
        } catch (PackageManager.NameNotFoundException ignored) {
            return false;
        }
    }

    public static boolean isNotDonated(Context context) {
        if (BuildConfig.DEBUG) return false;
        return !isPackageInstalled("com.smartpack.donate", context);
    }

    public static void initializeAppTheme(Context context) {
        if (getBoolean("dark_theme", true, context)) {
            AppCompatDelegate.setDefaultNightMode(
                    AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(
                    AppCompatDelegate.MODE_NIGHT_NO);
        }
    }

    public void initializeGoogleAds(Context context) {
        MobileAds.initialize(context, "ca-app-pub-7791710838910455~4399535899");
        mInterstitialAd = new InterstitialAd(context);
        mInterstitialAd.setAdUnitId("ca-app-pub-7791710838910455/7664681843");
        mInterstitialAd.loadAd(new AdRequest.Builder().build());
    }

    public void showInterstitialAd(Context context) {
        if (getBoolean("allow_ads", true, context) && mInterstitialAd.isLoaded()) {
            mInterstitialAd.show();
        }
    }

    public static boolean isTablet(Context context) {
        return (context.getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK)
                >= Configuration.SCREENLAYOUT_SIZE_LARGE;
    }

    public static void toast(String message, Context context) {
        toast(message, context, Toast.LENGTH_SHORT);
    }

    public static void toast(@StringRes int id, Context context) {
        toast(context.getString(id), context);
    }

    private static void toast(String message, Context context, int duration) {
        Toast.makeText(context, message, duration).show();
    }

    public static void launchUrl(String url, Context context) {
        if (!isNetworkAvailable(context)) {
            Utils.toast(R.string.no_internet, context);
            return;
        }
        try {
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setData(Uri.parse(url));
            context.startActivity(i);
        } catch (ActivityNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static int getOrientation(Activity activity) {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && activity.isInMultiWindowMode() ?
                Configuration.ORIENTATION_PORTRAIT : activity.getResources().getConfiguration().orientation;
    }

    static String readFile(String file) {
        return readFile(file, true);
    }

    private static String readFile(String file, boolean root) {
        if (root) {
            return new RootFile(file).readFile();
        }

        BufferedReader buf = null;
        try {
            buf = new BufferedReader(new FileReader(file));

            StringBuilder stringBuilder = new StringBuilder();
            String line;
            while ((line = buf.readLine()) != null) {
                stringBuilder.append(line).append("\n");
            }

            return stringBuilder.toString().trim();
        } catch (IOException ignored) {
        } finally {
            try {
                if (buf != null) buf.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public static boolean existFile(String file) {
        return existFile(file, true);
    }

    private static boolean existFile(String file, boolean root) {
        return !root ? new File(file).exists() : new RootFile(file).exists();
    }

    static void copy(String source, String dest) {
        RootUtils.runCommand("cp -r " + source + " " + dest);
    }

    static void create(String text, String path) {
        try {
            File logFile = new File(path);
            logFile.createNewFile();
            FileOutputStream fOut = new FileOutputStream(logFile);
            OutputStreamWriter myOutWriter =
                    new OutputStreamWriter(fOut);
            myOutWriter.append(text);
            myOutWriter.close();
            fOut.close();
        } catch (Exception ignored) {
        }
    }

    static void delete(String path) {
        if (Utils.existFile(path)) {
            RootUtils.runCommand("rm -r " + path);
        }
    }

    public static void sleep(int sec) {
        RootUtils.runCommand("sleep " + sec);
    }

    public static String getPath(File file) {
        String path = file.getAbsolutePath();
        if (path.startsWith("/document/raw:")) {
            path = path.replace("/document/raw:", "");
        } else if (path.startsWith("/document/primary:")) {
            path = (Environment.getExternalStorageDirectory() + ("/") + path.replace("/document/primary:", ""));
        } else if (path.startsWith("/document/")) {
            path = path.replace("/document/", "/storage/").replace(":", "/");
        }
        if (path.startsWith("/storage_root/storage/emulated/0")) {
            path = path.replace("/storage_root/storage/emulated/0", "/storage/emulated/0");
        } else if (path.startsWith("/storage_root")) {
            path = path.replace("storage_root", "storage/emulated/0");
        }
        if (path.startsWith("/external")) {
            path = path.replace("external", "storage/emulated/0");
        } if (path.startsWith("/root/")) {
            path = path.replace("/root", "");
        }
        if (path.contains("file%3A%2F%2F%2F")) {
            path = path.replace("file%3A%2F%2F%2F", "").replace("%2F", "/");
        }
        return path;
    }

    public static boolean isDocumentsUI(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    private static boolean isNetworkAvailable(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        assert cm != null;
        return (cm.getActiveNetworkInfo() != null) && cm.getActiveNetworkInfo().isConnectedOrConnecting();
    }

    /*
     * Taken and used almost as such from the following stackoverflow discussion
     * Ref: https://stackoverflow.com/questions/7203668/how-permission-can-be-checked-at-runtime-without-throwing-securityexception
     */
    public static boolean isStorageWritePermissionDenied(Context context) {
        String permission = android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
        int res = context.checkCallingOrSelfPermission(permission);
        return (res != PackageManager.PERMISSION_GRANTED);
    }

    public static boolean getBoolean(String name, boolean defaults, Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(name, defaults);
    }

    public static void saveBoolean(String name, boolean value, Context context) {
        PreferenceManager.getDefaultSharedPreferences(context).edit().putBoolean(name, value).apply();
    }

    public static boolean languageDefault(Context context) {
        return !Utils.getBoolean("use_english", false, context)
                && !Utils.getBoolean("use_korean", false, context)
                && !Utils.getBoolean("use_am", false, context)
                && !Utils.getBoolean("use_el", false, context)
                && !Utils.getBoolean("use_ml", false, context);
    }

    public static void setLanguage(Context context) {
        String lang;
        if (getBoolean("use_english", false, context)) {
            lang = "en_US";
        } else if (getBoolean("use_korean", false, context)) {
            lang = "ko";
        } else if (getBoolean("use_am", false, context)) {
            lang = "am";
        } else if (getBoolean("use_el", false, context)) {
            lang = "el";
        } else if (getBoolean("use_ml", false, context)) {
            lang = "ml";
        } else {
            lang = java.util.Locale.getDefault().getLanguage();
        }
        Locale myLocale = new Locale(lang);
        Resources res = context.getResources();
        DisplayMetrics dm = res.getDisplayMetrics();
        Configuration conf = res.getConfiguration();
        conf.locale = myLocale;
        res.updateConfiguration(conf, dm);
    }

}