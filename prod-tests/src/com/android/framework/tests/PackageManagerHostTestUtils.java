/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.framework.tests;

import com.android.ddmlib.testrunner.IRemoteAndroidTestRunner;
import com.android.ddmlib.testrunner.RemoteAndroidTestRunner;
import com.android.tradefed.device.DeviceNotAvailableException;
import com.android.tradefed.device.ITestDevice;
import com.android.tradefed.log.LogUtil.CLog;
import com.android.tradefed.result.CollectingTestListener;

import java.io.File;
import java.util.Map;
import java.util.Map.Entry;

import junit.framework.Assert;

/**
 * Set of tests that verify host side install cases
 */
public class PackageManagerHostTestUtils extends Assert {
    private ITestDevice mDevice = null;

    // TODO: get this value from Android Environment instead of hardcoding
    private static final String APP_PRIVATE_PATH = "/data/app-private/";
    private static final String DEVICE_APP_PATH = "/data/app/";
    private static final String SDCARD_APP_PATH = "/mnt/secure/asec/";

    private static final int MAX_WAIT_FOR_DEVICE_TIME = 120 * 1000;

    // Install preference on the device-side
    public static enum InstallLocPreference {
        AUTO,
        INTERNAL,
        EXTERNAL
    }

    // Actual install location
    public static enum InstallLocation {
        DEVICE,
        SDCARD
    }

    /**
     * Constructor takes the device to use
     * @param device the {@link ITestDevice} to use when performing operations
     */
    public PackageManagerHostTestUtils(ITestDevice device) {
          mDevice = device;
    }

    /**
     * Returns the path on the device of forward-locked apps.
     *
     * @return path of forward-locked apps on the device
     */
    public static String getAppPrivatePath() {
        return APP_PRIVATE_PATH;
    }

    /**
     * Returns the path on the device of normal apps.
     *
     * @return path of forward-locked apps on the device
     */
    public static String getDeviceAppPath() {
        return DEVICE_APP_PATH;
    }

    /**
     * Returns the path of apps installed on the SD card.
     *
     * @return path of forward-locked apps on the device
     */
    public static String getSDCardAppPath() {
        return SDCARD_APP_PATH;
    }

    /**
     * Helper method to run tests and return the listener that collected the results.
     *
     * For the optional params, pass null to use the default values.

     * @param pkgName Android application package for tests
     * @param className (optional) The class containing the method to test
     * @param methodName (optional) The method in the class of which to test
     * @param runnerName (optional) The name of the TestRunner of the test on the device to be run
     * @param params (optional) Any additional parameters to pass into the Test Runner

     * @return the {@link CollectingTestRunListener}
     * @throws DeviceNotAvailableException
     */
    private CollectingTestListener doRunTests(String pkgName, String className,
            String methodName, String runnerName, Map<String, String> params)
            throws DeviceNotAvailableException  {
        IRemoteAndroidTestRunner testRunner = new RemoteAndroidTestRunner(pkgName, runnerName,
                mDevice.getIDevice());

        if (className != null && methodName != null) {
            testRunner.setMethodName(className, methodName);
        }

        // Add in any additional args to pass into the test
        if (params != null) {
            for (Entry<String, String> argPair : params.entrySet()) {
                testRunner.addInstrumentationArg(argPair.getKey(), argPair.getValue());
            }
        }

        CollectingTestListener listener = new CollectingTestListener();
        mDevice.runInstrumentationTests(testRunner, listener);
        return listener;
    }

    /**
     * Runs the specified packages tests, and returns whether all tests passed or not.
     *
     * @param pkgName Android application package for tests
     * @param className The class containing the method to test
     * @param methodName The method in the class of which to test
     * @param runnerName The name of the TestRunner of the test on the device to be run
     * @param params Any additional parameters to pass into the Test Runner
     * @return true if test passed, false otherwise.
     * @throws DeviceNotAvailableException
     */
    public boolean runDeviceTestsDidAllTestsPass(String pkgName, String className,
            String methodName, String runnerName, Map<String, String> params)
            throws DeviceNotAvailableException  {
        CollectingTestListener listener = doRunTests(pkgName, className, methodName,
                runnerName, params);
        return !listener.hasFailedTests();
    }

    /**
     * Runs the specified packages tests, and returns whether all tests passed or not.
     *
     * @param pkgName Android application package for tests

     * @return true if every test passed, false otherwise.
     * @throws DeviceNotAvailableException
     */
    public boolean runDeviceTestsDidAllTestsPass(String pkgName)
            throws DeviceNotAvailableException   {
        CollectingTestListener listener = doRunTests(pkgName, null, null, null, null);
        return !listener.hasFailedTests();
    }

    /**
     * Helper method to install a file
     * @param localFile the {@link File} to install
     * @param reinstall set to <code>true</code> if re-install of app should be performed
     * @throws DeviceNotAvailableException
     */
    public void installFile(final File localFile, final boolean replace)
            throws DeviceNotAvailableException {
        String result = mDevice.installPackage(localFile, replace);
        assertEquals(null, result);
    }

    /**
     * Helper method to install a file to device as forward locked.
     *
     * @param apkFile the {@link File} to install
     * @param replace set to <code>true</code> if re-install of app should be performed
     * @throws DeviceNotAvailableException if communication with device is lost
     */
    public String installFileForwardLocked(final File apkFile, final boolean replace)
            throws DeviceNotAvailableException {
        return mDevice.installPackage(apkFile, replace, "-l");
    }

    /**
     * Helper method to determine if file exists on the device containing a given string.
     *
     * @param destPath the absolute path of the file
     * @return <code>true</code> if file exists containing given string,
     *         <code>false</code> otherwise.
     * @throws DeviceNotAvailableException
     */
    public boolean doesRemoteFileExistContainingString(String destPath, String searchString)
            throws DeviceNotAvailableException {
        String lsResult = mDevice.executeShellCommand(String.format("ls %s", destPath));
        return lsResult.contains(searchString);
    }

    /**
     * Helper method to determine if package on device exists.
     *
     * @param packageName the Android manifest package to check.
     * @return <code>true</code> if package exists, <code>false</code> otherwise

     * @throws DeviceNotAvailableException
     */
    public boolean doesPackageExist(String packageName) throws DeviceNotAvailableException {
        String pkgGrep = mDevice.executeShellCommand(String.format("pm path %s", packageName));
        return pkgGrep.contains("package:");
    }

    /**
     * Determines if app was installed on device.
     *
     * @param packageName package name to check for
     * @return <code>true</code> if file exists, <code>false</code> otherwise.
     * @throws DeviceNotAvailableException
     */
    public boolean doesAppExistOnDevice(String packageName) throws DeviceNotAvailableException {
        return doesRemoteFileExistContainingString(DEVICE_APP_PATH, packageName);
    }

    /**
     * Determines if app was installed on SD card.
     *
     * @param packageName package name to check for
     * @return <code>true</code> if file exists, <code>false</code> otherwise.
     * @throws DeviceNotAvailableException
     */
    public boolean doesAppExistOnSDCard(String packageName) throws DeviceNotAvailableException {
        return doesRemoteFileExistContainingString(SDCARD_APP_PATH, packageName);
    }

    /**
     * Helper method to determine if app was installed on SD card.
     *
     * @param packageName package name to check for
     * @return <code>true</code> if file exists, <code>false</code> otherwise.
     * @throws DeviceNotAvailableException
     */
    public boolean doesAppExistAsForwardLocked(String packageName)
            throws DeviceNotAvailableException {
        return doesRemoteFileExistContainingString(APP_PRIVATE_PATH, packageName);
    }

    /**
     * Waits for device's package manager to respond.
     *
     * @throws DeviceNotAvailableException
     */
    public void waitForPackageManager() throws DeviceNotAvailableException {
       CLog.i("waiting for device");
       mDevice.waitForDeviceAvailable(MAX_WAIT_FOR_DEVICE_TIME);
    }

    /**
     * Helper method for installing an app to wherever is specified in its manifest, and
     * then verifying the app was installed onto SD Card.
     * <p/>
     * Assumes adb is running as root in device under test.
     *
     * @param the path of the apk to install
     * @param the name of the package
     * @param <code>true</code> if the app should be overwritten, <code>false</code> otherwise

     * @throws DeviceNotAvailableException
     */
    public void installAppAndVerifyExistsOnSDCard(File apkPath, String pkgName, boolean overwrite)
            throws DeviceNotAvailableException {
        // Start with a clean slate if we're not overwriting
        if (!overwrite) {
            // cleanup test app just in case it already exists
            mDevice.uninstallPackage(pkgName);
            // grep for package to make sure its not installed
            assertFalse(doesPackageExist(pkgName));
        }

        installFile(apkPath, overwrite);
        assertTrue(doesAppExistOnSDCard(pkgName));
        assertFalse(doesAppExistOnDevice(pkgName));
        // TODO: is this necessary?
        waitForPackageManager();

        // grep for package to make sure it is installed
        assertTrue(doesPackageExist(pkgName));
    }

    /**
     * Helper method for installing an app to wherever is specified in its manifest, and
     * then verifying the app was installed onto device.
     * <p/>
     * Assumes adb is running as root in device under test.
     *
     * @param apkFile the {@link File} of the apk to install
     * @param the name of the package
     * @param <code>true</code> if the app should be overwritten, <code>false</code> otherwise
     * @throws DeviceNotAvailableException
     */
    public void installAppAndVerifyExistsOnDevice(File apkFile, String pkgName, boolean overwrite)
            throws  DeviceNotAvailableException {
        // Start with a clean slate if we're not overwriting
        if (!overwrite) {
            // cleanup test app just in case it already exists
            mDevice.uninstallPackage(pkgName);
            // grep for package to make sure its not installed
            assertFalse(doesPackageExist(pkgName));
        }

        installFile(apkFile, overwrite);
        assertFalse(doesAppExistOnSDCard(pkgName));
        assertTrue(doesAppExistOnDevice(pkgName));
        // TODO: is this necessary?
        waitForPackageManager();

        // grep for package to make sure it is installed
        assertTrue(doesPackageExist(pkgName));
    }

    /**
     * Helper method for installing an app as forward-locked, and
     * then verifying the app was installed in the proper forward-locked location.
     * <p/>
     * Assumes adb is running as root in device under test.
     *
     * @param apkFile the {@link File} of the apk to install
     * @param pkgName the name of the package
     * @param overwrite <code>true</code> if the app should be overwritten,
     * <code>false</code> otherwise
     * @throws Exception if failed to install app
     */
    public void installFwdLockedAppAndVerifyExists(File apkFile,
            String pkgName, boolean overwrite) throws Exception {
        // Start with a clean slate if we're not overwriting
        if (!overwrite) {
            // cleanup test app just in case it already exists
            mDevice.uninstallPackage(pkgName);
            // grep for package to make sure its not installed
            assertFalse(doesPackageExist(pkgName));
        }

        String result = installFileForwardLocked(apkFile, overwrite);
        assertEquals(null, result);
        assertTrue(doesAppExistAsForwardLocked(pkgName));
        assertFalse(doesAppExistOnSDCard(pkgName));
        waitForPackageManager();

        // grep for package to make sure it is installed
        assertTrue(doesPackageExist(pkgName));
    }

    /**
     * Helper method for uninstalling an app.
     * <p/>
     * Assumes adb is running as root in device under test.
     *
     * @param pkgName package name to uninstall
     * @throws DeviceNotAvailableException
     */
    public void uninstallApp(String pkgName) throws DeviceNotAvailableException {
        mDevice.uninstallPackage(pkgName);
        // make sure its not installed anymore
        assertFalse(doesPackageExist(pkgName));
    }

    /**
     * Sets the device's install location preference.
     *
     * <p/>
     * Assumes adb is running as root in device under test.
     * @throws DeviceNotAvailableException
     */
    public void setDevicePreferredInstallLocation(InstallLocPreference pref)
            throws DeviceNotAvailableException {
        String command = "pm setInstallLocation %d";
        int locValue = 0;
        switch (pref) {
            case INTERNAL:
                locValue = 1;
                break;
            case EXTERNAL:
                locValue = 2;
                break;
            default: // AUTO
                locValue = 0;
                break;
        }
        mDevice.executeShellCommand(String.format(command, locValue));
    }

    /**
     * Gets the device's install location preference.
     *
     * <p/>
     * Assumes adb is running as root in device under test.
     * @throws DeviceNotAvailableException
     */
    public InstallLocPreference getDevicePreferredInstallLocation()
            throws DeviceNotAvailableException {
        String result = mDevice.executeShellCommand("pm getInstallLocation");
        if (result.indexOf('0') != -1) {
            return InstallLocPreference.AUTO;
        }
        else if (result.indexOf('1') != -1) {
            return InstallLocPreference.INTERNAL;
        }
        else {
            return InstallLocPreference.EXTERNAL;
        }
    }
}