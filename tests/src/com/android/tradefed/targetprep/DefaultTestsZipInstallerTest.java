/*
 * Copyright (C) 2011 The Android Open Source Project
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

package com.android.tradefed.targetprep;

import com.android.ddmlib.FileListingService;
import com.android.tradefed.build.DeviceBuildInfo;
import com.android.tradefed.build.IDeviceBuildInfo;
import com.android.tradefed.device.ITestDevice;
import com.android.tradefed.device.ITestDevice.RecoveryMode;
import com.android.tradefed.device.MockFileUtil;
import com.android.tradefed.util.IRunUtil;

import junit.framework.TestCase;

import org.easymock.EasyMock;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

public class DefaultTestsZipInstallerTest extends TestCase {
    private static final String SKIP_THIS = "skipThis";

    private static final String TEST_STRING = "foo";

    private static final File SOME_PATH_1 = new File("/some/path/1");

    private static final File SOME_PATH_2 = new File("/some/path/2");

    private ITestDevice mMockDevice;
    private IDeviceBuildInfo mDeviceBuild;
    private DefaultTestsZipInstaller mZipInstaller;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mZipInstaller = new DefaultTestsZipInstaller(SKIP_THIS) {
            @Override
            File[] getTestsZipDataFiles(File hostDir) {
                return new File[] { new File("foo") };
            }

            @Override
            Set<File> findDirs(File hostDir, File deviceRootPath) {
                Set<File> files = new HashSet<File>(2);
                files.add(SOME_PATH_1);
                files.add(SOME_PATH_2);
                return files;
            };

            @Override
            IRunUtil getRunUtil() {
                return EasyMock.createNiceMock(IRunUtil.class);
            }
         };

        mMockDevice = EasyMock.createMock(ITestDevice.class);
        EasyMock.expect(mMockDevice.getSerialNumber()).andStubReturn(TEST_STRING);
        EasyMock.expect(mMockDevice.getProductType()).andStubReturn(TEST_STRING);
        EasyMock.expect(mMockDevice.getBuildId()).andStubReturn("1");
        mDeviceBuild = new DeviceBuildInfo("1", TEST_STRING, TEST_STRING);
    }

    public void testCantTouchFilesystem() throws Exception {
        // expect initial android stop
        EasyMock.expect(mMockDevice.getSerialNumber()).andStubReturn("serial_number_stub");
        EasyMock.expect(mMockDevice.getRecoveryMode()).andReturn(RecoveryMode.AVAILABLE);
        mMockDevice.setRecoveryMode(RecoveryMode.ONLINE);

        // turtle!  (return false, for "write failed")
        EasyMock.expect(mMockDevice.pushString((String) EasyMock.anyObject(),
                (String) EasyMock.anyObject())).andReturn(false);

        EasyMock.replay(mMockDevice);
        try {
            mZipInstaller.deleteData(mMockDevice);
            fail("Didn't throw TargetSetupError on failed write test");
        } catch (TargetSetupError e) {
            // expected
        }
        EasyMock.verify(mMockDevice);
    }

    /**
     * Exercise the core logic on a successful scenario.
     */
    public void testPushTestsZipOntoData() throws Exception {
        // mock a filesystem with these contents:
        // /data/app
        // /data/$SKIP_THIS
        MockFileUtil.setMockDirContents(
                mMockDevice, FileListingService.DIRECTORY_DATA, "app", SKIP_THIS);

        // expect initial android stop
        EasyMock.expect(mMockDevice.getSerialNumber()).andStubReturn("serial_number_stub");
        EasyMock.expect(mMockDevice.getRecoveryMode()).andReturn(RecoveryMode.AVAILABLE);
        mMockDevice.setRecoveryMode(RecoveryMode.ONLINE);
        EasyMock.expect(mMockDevice.executeShellCommand("stop")).andReturn("");

        // turtle!  (to make sure filesystem is writable)
        EasyMock.expect(mMockDevice.pushString((String) EasyMock.anyObject(),
                (String) EasyMock.anyObject())).andReturn(true);

        // expect 'rm app' but not 'rm $SKIP_THIS'
        EasyMock.expect(mMockDevice.doesFileExist("data/app")).andReturn(false);
        EasyMock.expect(mMockDevice.executeShellCommand(EasyMock.contains("rm -r data/app")))
                .andReturn("");

        mMockDevice.setRecoveryMode(RecoveryMode.AVAILABLE);

        EasyMock.expect(mMockDevice.syncFiles((File) EasyMock.anyObject(),
                EasyMock.contains(FileListingService.DIRECTORY_DATA)))
                .andReturn(Boolean.TRUE);

        EasyMock.expect(
                mMockDevice.executeShellCommand(EasyMock.startsWith("chown system.system "
                        + SOME_PATH_1.getPath()))).andReturn("");
        EasyMock.expect(
                mMockDevice.executeShellCommand(EasyMock.startsWith("chown system.system "
                        + SOME_PATH_2.getPath()))).andReturn("");

        EasyMock.replay(mMockDevice);
        mZipInstaller.pushTestsZipOntoData(mMockDevice, mDeviceBuild);
        EasyMock.verify(mMockDevice);
    }

    /**
     * Test repeats to delete a dir are aborted
     */
    public void testPushTestsZipOntoData_retry() throws Exception {
        // mock a filesystem with these contents:
        // /data/app
        // /data/$SKIP_THIS
        MockFileUtil.setMockDirContents(
                mMockDevice, FileListingService.DIRECTORY_DATA, "app", SKIP_THIS);

        // expect initial android stop
        EasyMock.expect(mMockDevice.getSerialNumber()).andStubReturn("serial_number_stub");
        EasyMock.expect(mMockDevice.getRecoveryMode()).andReturn(RecoveryMode.AVAILABLE);
        mMockDevice.setRecoveryMode(RecoveryMode.ONLINE);
        EasyMock.expect(mMockDevice.executeShellCommand("stop")).andReturn("");

        // turtle!  (to make sure filesystem is writable)
        EasyMock.expect(mMockDevice.pushString((String) EasyMock.anyObject(),
                (String) EasyMock.anyObject())).andReturn(true);

        // expect 'rm app' but not 'rm $SKIP_THIS'
        EasyMock.expect(mMockDevice.doesFileExist("data/app")).andStubReturn(true);
        EasyMock.expect(mMockDevice.executeShellCommand(EasyMock.contains("rm -r data/app")))
                .andStubReturn("oh noes, rm failed");


        EasyMock.replay(mMockDevice);
        try {
            mZipInstaller.pushTestsZipOntoData(mMockDevice, mDeviceBuild);
            fail("TargetSetupError not thrown");
        } catch (TargetSetupError e) {
            // expected
        }
        EasyMock.verify(mMockDevice);
    }
}
