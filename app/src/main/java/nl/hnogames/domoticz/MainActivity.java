/*
 * Copyright (C) 2015 Domoticz
 *
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */

package nl.hnogames.domoticz;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import hotchemi.android.rate.AppRate;
import nl.hnogames.domoticz.Adapters.NavigationAdapter;
import nl.hnogames.domoticz.Containers.ConfigInfo;
import nl.hnogames.domoticz.Containers.ServerInfo;
import nl.hnogames.domoticz.Domoticz.Domoticz;
import nl.hnogames.domoticz.Fragments.Cameras;
import nl.hnogames.domoticz.Fragments.Dashboard;
import nl.hnogames.domoticz.Fragments.Scenes;
import nl.hnogames.domoticz.Fragments.Switches;
import nl.hnogames.domoticz.Interfaces.ConfigReceiver;
import nl.hnogames.domoticz.Interfaces.UpdateVersionReceiver;
import nl.hnogames.domoticz.Interfaces.VersionReceiver;
import nl.hnogames.domoticz.UI.SortDialog;
import nl.hnogames.domoticz.Utils.PermissionsUtil;
import nl.hnogames.domoticz.Utils.ServerUtil;
import nl.hnogames.domoticz.Utils.SharedPrefUtil;
import nl.hnogames.domoticz.Utils.UsefulBits;
import nl.hnogames.domoticz.Utils.WidgetUtils;
import nl.hnogames.domoticz.Welcome.WelcomeViewActivity;
import nl.hnogames.domoticz.app.AppController;
import nl.hnogames.domoticz.app.DomoticzCardFragment;
import nl.hnogames.domoticz.app.DomoticzFragment;

public class MainActivity extends AppCompatActivity {

    private final int iWelcomeResultCode = 885;
    private final int iSettingsResultCode = 995;

    private String TAG = MainActivity.class.getSimpleName();
    private ActionBarDrawerToggle mDrawerToggle;
    private DrawerLayout mDrawer;
    private String[] fragments;
    private SharedPrefUtil mSharedPrefs;
    private ServerUtil mServerUtil;
    private NavigationAdapter mAdapter;
    private SearchView searchViewAction;

    private ArrayList<String> stackFragments = new ArrayList<>();
    private Domoticz domoticz;
    private boolean onPhone;
    private Timer cameraRefreshTimer = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mServerUtil = new ServerUtil(this);
        mSharedPrefs = new SharedPrefUtil(this);
        domoticz = new Domoticz(this);
        applyLanguage();

        if (mSharedPrefs.isFirstStart()) {
            mSharedPrefs.setNavigationDefaults();
            Intent welcomeWizard = new Intent(this, WelcomeViewActivity.class);
            startActivityForResult(welcomeWizard, iWelcomeResultCode);
            mSharedPrefs.setFirstStart(false);
        } else {
            WidgetUtils.RefreshWidgets(this);

            //noinspection ConstantConditions
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeButtonEnabled(true);

            // Only start Geofences when not started
            // Geofences are already started on device boot up by the BootUpReceiver
            if (!mSharedPrefs.isGeofencingStarted()) {
                mSharedPrefs.setGeofencingStarted(true);
                mSharedPrefs.enableGeoFenceService();
            }

            buildScreen();
        }
    }

    public void buildScreen() {
        if (mSharedPrefs.isWelcomeWizardSuccess()) {
            setupMobileDevice();
            checkDomoticzServerUpdate();
            saveServerConfigToSharedPreferences();
            appRate();
            drawNavigationMenu();
        } else {
            Intent welcomeWizard = new Intent(this, WelcomeViewActivity.class);
            startActivityForResult(welcomeWizard, iWelcomeResultCode);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        }
    }

    public void drawNavigationMenu() {
        TextView usingTabletLayout = (TextView) findViewById(R.id.tabletLayout);
        if (usingTabletLayout == null)
            onPhone = true;

        addDrawerItems();
        addFragment();
    }

    private void setScreenOn() {
        if (mSharedPrefs.getAwaysOn())
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        else
            getWindow().clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    private void applyLanguage() {
        if (!UsefulBits.isEmpty(mSharedPrefs.getLanguage())) {
            UsefulBits.setLocale(this, mSharedPrefs.getLanguage());
        }
    }

    /* Called when the second activity's finished */
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (data != null && resultCode == RESULT_OK) {
            switch (requestCode) {
                case iWelcomeResultCode:
                    Bundle res = data.getExtras();
                    if (!res.getBoolean("RESULT", false))
                        this.finish();
                    else {
                        buildScreen();
                    }
                    break;
                case iSettingsResultCode:
                    drawNavigationMenu();
                    refreshFragment();
                    updateDrawerItems();
                    invalidateOptionsMenu();
                    break;
            }
        }
    }

    public void refreshFragment() {
        Fragment f = getVisibleFragment();
        if (f instanceof DomoticzFragment) {
            ((DomoticzFragment) f).refreshFragment();
        } else if (f instanceof DomoticzCardFragment)
            ((DomoticzCardFragment) f).refreshFragment();
    }

    public void removeFragmentStack(String fragment) {
        if (stackFragments != null) {
            if (stackFragments.contains(fragment))
                stackFragments.remove(fragment);
        }
    }

    public void addFragmentStack(String fragment) {
        if (stackFragments == null)
            stackFragments = new ArrayList<>();

        if (!stackFragments.contains(fragment)) {
            if (stackFragments.size() > 1)
                stackFragments.remove(stackFragments.size() - 1);
            stackFragments.add(fragment);
        }
    }

    public void changeFragment(String fragment) {
        FragmentTransaction tx = getSupportFragmentManager().beginTransaction();
        // tx.setCustomAnimations(R.anim.enter_from_left, R.anim.exit_to_right, R.anim.enter_from_right, R.anim.exit_to_left);
        tx.replace(R.id.main, Fragment.instantiate(MainActivity.this, fragment));
        tx.commitAllowingStateLoss();
        addFragmentStack(fragment);
        saveScreenToAnalytics(fragment);
    }

    private void addFragment() {
        int screenIndex = mSharedPrefs.getStartupScreenIndex();
        FragmentTransaction tx = getSupportFragmentManager().beginTransaction();
        //tx.setCustomAnimations(R.anim.enter_from_left, R.anim.exit_to_right, R.anim.enter_from_right, R.anim.exit_to_left);
        tx.replace(R.id.main, Fragment.instantiate(MainActivity.this, getResources().getStringArray(R.array.drawer_fragments)[screenIndex]));
        tx.commitAllowingStateLoss();
        addFragmentStack(getResources().getStringArray(R.array.drawer_fragments)[screenIndex]);
        saveScreenToAnalytics(getResources().getStringArray(R.array.drawer_fragments)[screenIndex]);
    }

    private void saveScreenToAnalytics(String screen) {
        try {
            AppController application = (AppController) getApplication();
            Tracker mTracker = application.getDefaultTracker();
            mTracker.setScreenName(screen);
            mTracker.send(new HitBuilders.ScreenViewBuilder().build());
        } catch (Exception ignored) {
        }
    }

    private void updateDrawerItems() {
        String[] drawerActions = mSharedPrefs.getNavigationActions();
        fragments = mSharedPrefs.getNavigationFragments();
        int ICONS[] = mSharedPrefs.getNavigationIcons();
        mAdapter.updateData(drawerActions, ICONS);
    }

    /**
     * Adds the items to the drawer and registers a click listener on the items
     */
    private void addDrawerItems() {
        String[] drawerActions = mSharedPrefs.getNavigationActions();
        fragments = mSharedPrefs.getNavigationFragments();
        int ICONS[] = mSharedPrefs.getNavigationIcons();

        String NAME = getString(R.string.app_name_domoticz);
        String WEBSITE = getString(R.string.domoticz_url);
        int PROFILE = R.drawable.ic_launcher;

        mDrawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        RecyclerView mRecyclerView = (RecyclerView) findViewById(R.id.RecyclerView);
        mRecyclerView.setHasFixedSize(true);                            // Letting the system know that the list objects are of fixed size

        mAdapter = new NavigationAdapter(drawerActions, ICONS, NAME, WEBSITE, PROFILE, this);
        mAdapter.onClickListener(new NavigationAdapter.ClickListener() {
            @Override
            public void onClick(View child, int position) {
                if (child != null) {
                    try {
                        searchViewAction.setQuery("", false);
                        searchViewAction.clearFocus();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    try {
                        FragmentTransaction tx = getSupportFragmentManager().beginTransaction();
                        tx.replace(R.id.main,
                                Fragment.instantiate(MainActivity.this,
                                        fragments[position - 1]));
                        tx.commitAllowingStateLoss();
                        addFragmentStack(fragments[position - 1]);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    invalidateOptionsMenu();
                    if (onPhone)
                        mDrawer.closeDrawer(GravityCompat.START);
                }
            }
        });

        mRecyclerView.setAdapter(mAdapter);
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);

        setupDrawer();
    }

    /**
     * Sets the drawer with listeners for open and closed
     */
    private void setupDrawer() {
        if (onPhone) {
            mDrawerToggle = new ActionBarDrawerToggle(
                    this, mDrawer, R.string.drawer_open, R.string.drawer_close) {
                /**
                 * Called when a mDrawer has settled in a completely open state.
                 */
                public void onDrawerOpened(View drawerView) {
                    super.onDrawerOpened(drawerView);

                    try {
                        if (searchViewAction != null)
                            searchViewAction.clearFocus();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    //getSupportActionBar().setTitle(R.string.drawer_navigation_title);
                    invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
                }

                /**
                 * Called when a mDrawer has settled in a completely closed state.
                 */
                public void onDrawerClosed(View view) {
                    super.onDrawerClosed(view);
                    //getSupportActionBar().setTitle(currentTitle);
                    invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
                }
            };

            mDrawerToggle.setDrawerIndicatorEnabled(true); // hamburger menu icon
            mDrawer.setDrawerListener(mDrawerToggle); // attach hamburger menu icon to drawer
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        if (mDrawerToggle != null) mDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (mDrawerToggle != null) mDrawerToggle.onConfigurationChanged(newConfig);
    }

    public Fragment getVisibleFragment() {
        try {
            FragmentManager fragmentManager = MainActivity.this.getSupportFragmentManager();
            List<Fragment> fragments = fragmentManager.getFragments();
            for (Fragment fragment : fragments) {
                if (fragment != null && fragment.isVisible())
                    return fragment;
            }

            return null;
        } catch (Exception ex) {
            return null;
        }
    }

    private void appRate() {
        AppRate.with(this)
                .setInstallDays(0) // default 10, 0 means install day.
                .setLaunchTimes(3) // default 10
                .setRemindInterval(2) // default 1
                .monitor();

        // Show a dialog if meets conditions
        AppRate.showRateDialogIfMeetsConditions(this);
    }

    private void setupMobileDevice() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!PermissionsUtil.canAccessDeviceState(this)) {
                requestPermissions(PermissionsUtil.INITIAL_DEVICE_PERMS, PermissionsUtil.INITIAL_DEVICE_REQUEST);
            } else {
                AppController.getInstance().StartEasyGCM();
            }
        } else {
            AppController.getInstance().StartEasyGCM();
        }
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case PermissionsUtil.INITIAL_DEVICE_REQUEST:
                if (PermissionsUtil.canAccessDeviceState(this))
                    AppController.getInstance().StartEasyGCM();
                break;
        }
    }

    private void checkDomoticzServerUpdate() {

        // Get latest Domoticz version update
        domoticz.getUpdate(new UpdateVersionReceiver() {
            @Override
            public void onReceiveUpdate(String version, boolean haveUpdate) {
                // Write update version to shared preferences
                mSharedPrefs.setUpdateVersionAvailable(version);
                mSharedPrefs.setServerUpdateAvailable(haveUpdate);
                if (haveUpdate) getCurrentServerVersion();
            }

            @Override
            public void onError(Exception error) {
                String message = String.format(
                        getString(R.string.error_couldNotCheckForUpdates),
                        domoticz.getErrorMessage(error));
                showSimpleSnackbar(message);
                mSharedPrefs.setUpdateVersionAvailable("");
            }
        });
    }

    private void getCurrentServerVersion() {
        // Get current Domoticz server version
        domoticz.getServerVersion(new VersionReceiver() {
            @Override
            public void onReceiveVersion(String serverVersion) {
                if (!UsefulBits.isEmpty(serverVersion)) {
                    mSharedPrefs.setServerVersion(serverVersion);

                    String[] version
                            = serverVersion.split("\\.");
                    // Update version is only revision number
                    String updateVersion =
                            version[0] + "." + mSharedPrefs.getUpdateVersionAvailable();
                    String message
                            = String.format(getString(R.string.update_available_enhanced),
                            serverVersion,
                            updateVersion);
                    showSnackBarToUpdateServer(message);
                }
            }

            @Override
            public void onError(Exception error) {
                String message = String.format(
                        getString(R.string.error_couldNotCheckForUpdates),
                        domoticz.getErrorMessage(error));
                showSimpleSnackbar(message);
            }
        });
    }

    private void showSnackBarToUpdateServer(String message) {
        View layout = getFragmentCoordinatorLayout();
        if (layout != null) {
            Snackbar.make(layout, message, Snackbar.LENGTH_LONG)
                    .setAction("Update server", new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            startActivity(new Intent(MainActivity.this, UpdateActivity.class));
                        }
                    })
                    .show();
        }
    }

    private void saveServerConfigToSharedPreferences() {
        // Get Domoticz server configuration
        domoticz.GetConfig(new ConfigReceiver() {
            @Override
            public void onReceiveConfig(ConfigInfo settings) {
                if (settings != null)
                    mSharedPrefs.saveConfig(settings);
            }

            @Override
            public void onError(Exception error) {
                String message = String.format(
                        getString(R.string.error_couldNotCheckForConfig),
                        domoticz.getErrorMessage(error));
                showSimpleSnackbar(message);
            }
        });
    }

    private void showSimpleSnackbar(String message) {
        View layout = getFragmentCoordinatorLayout();
        if (layout != null) Snackbar.make(layout, message, Snackbar.LENGTH_SHORT).show();
        else Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
    }

    public View getFragmentCoordinatorLayout() {
        View layout = null;
        try {
            layout = getVisibleFragment().getView().findViewById(R.id.coordinatorLayout);
        } catch (Exception ex) {
            Log.e(TAG, "Unable to get the coordinator layout of visible fragment");
            ex.printStackTrace();
        }
        return layout;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Fragment f = getVisibleFragment();
        if (!(f instanceof DomoticzFragment)) {
            if ((f instanceof Cameras)) {
                if (cameraRefreshTimer != null)
                    getMenuInflater().inflate(R.menu.menu_camera_pause, menu);
                else
                    getMenuInflater().inflate(R.menu.menu_camera, menu);
            } else
                getMenuInflater().inflate(R.menu.menu_simple, menu);
        } else {
            if ((f instanceof Dashboard) || (f instanceof Scenes) || (f instanceof Switches))
                getMenuInflater().inflate(R.menu.menu_main_sort, menu);
            else
                getMenuInflater().inflate(R.menu.menu_main, menu);

            MenuItem searchMenuItem = menu.findItem(R.id.search);
            searchViewAction = (SearchView) MenuItemCompat
                    .getActionView(searchMenuItem);
            searchViewAction.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String query) {
                    return false;
                }

                @Override
                public boolean onQueryTextChange(String newText) {
                    Fragment n = getVisibleFragment();
                    if (n instanceof DomoticzFragment) {
                        ((DomoticzFragment) n).Filter(newText);
                    }
                    return false;
                }
            });
        }

        if (mSharedPrefs.isMultiServerEnabled()) {
            //set multi server actionbar item
            MenuItem searchMenuItem = menu.findItem(R.id.action_switch_server);
            if (searchMenuItem != null && mServerUtil.getEnabledServerList() != null && mServerUtil.getEnabledServerList().size() > 1) {
                searchMenuItem.setVisible(true);
            } else {
                searchMenuItem.setVisible(false);
            }
        }

        return super.onCreateOptionsMenu(menu);
    }

    @SuppressWarnings("SimplifiableIfStatement")
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        try {
            switch (item.getItemId()) {
                case R.id.action_camera_play:
                    if (cameraRefreshTimer == null) {
                        cameraRefreshTimer = new Timer("camera", true);
                        cameraRefreshTimer.scheduleAtFixedRate(new TimerTask() {
                            @Override
                            public void run() {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        //call refresh fragment
                                        Fragment f = getVisibleFragment();
                                        if (f instanceof Cameras) {
                                            ((Cameras) f).refreshFragment();
                                        } else {
                                            //we're not at the camera fragment? stop timer!
                                            stopCameraTimer();
                                            invalidateOptionsMenu();
                                        }
                                    }
                                });
                            }
                        }, 0, 5000);//schedule in 5 seconds
                    }
                    invalidateOptionsMenu();//set pause button
                    return true;
                case R.id.action_camera_pause:
                    stopCameraTimer();
                    invalidateOptionsMenu();//set pause button
                    return true;
                case R.id.action_settings:
                    stopCameraTimer();
                    startActivityForResult(new Intent(this, SettingsActivity.class), this.iSettingsResultCode);
                    return true;
                case R.id.action_sort:
                    SortDialog infoDialog = new SortDialog(
                            this,
                            R.layout.dialog_switch_logs);
                    infoDialog.onDismissListener(new SortDialog.DismissListener() {
                        @Override
                        public void onDismiss(String selectedSort) {
                            Log.i(TAG, "Sorting: " + selectedSort);
                            Fragment f = getVisibleFragment();
                            if (f instanceof DomoticzFragment) {
                                ((DomoticzFragment) f).sortFragment(selectedSort);
                            }
                        }
                    });
                    infoDialog.show();
                    return true;
                case R.id.action_switch_server:
                    showServerDialog();
                    return true;
            }

            // Activate the navigation drawer toggle
            if (mDrawerToggle.onOptionsItemSelected(item)) {
                return true;
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return super.onOptionsItemSelected(item);
    }

    public void showServerDialog() {
        String[] serverNames = new String[mServerUtil.getServerList().size()];
        int count = 0;
        for (ServerInfo s : mServerUtil.getEnabledServerList()) {
            serverNames[count] = s.getServerName();
            count++;
        }

        //show dialog with servers
        new MaterialDialog.Builder(this)
                .title(R.string.choose_server)
                .items(serverNames)
                .itemsCallback(new MaterialDialog.ListCallback() {
                    @Override
                    public void onSelection(MaterialDialog dialog, View view, int which, CharSequence text) {
                        ServerInfo setNew = null;
                        for (ServerInfo s : mServerUtil.getEnabledServerList()) {
                            if (s.getServerName().equals(text)) {
                                showSimpleSnackbar("Switching server to " + s.getServerName());
                                setNew = s;
                            }
                        }
                        if (setNew != null) {
                            mServerUtil.setActiveServer(setNew);
                            buildScreen();
                            invalidateOptionsMenu();
                        }
                    }
                })
                .show();
    }

    @Override
    public void onResume() {
        super.onResume();
        setScreenOn();
        refreshFragment();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopCameraTimer();
    }

    private void stopCameraTimer() {
        if (cameraRefreshTimer != null) {
            cameraRefreshTimer.cancel();
            cameraRefreshTimer.purge();
            cameraRefreshTimer = null;
        }
    }

    @Override
    public void onBackPressed() {
        if (stackFragments == null || stackFragments.size() <= 1) {
            MainActivity.super.onBackPressed();
        } else {
            String currentFragment = stackFragments.get(stackFragments.size() - 1);
            String previousFragment = stackFragments.get(stackFragments.size() - 2);
            changeFragment(previousFragment);
            stackFragments.remove(currentFragment);
        }

        stopCameraTimer();
        invalidateOptionsMenu();
    }
}