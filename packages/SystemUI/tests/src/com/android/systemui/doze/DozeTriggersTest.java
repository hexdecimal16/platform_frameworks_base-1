/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.systemui.doze;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Instrumentation;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.support.test.InstrumentationRegistry;

import com.android.internal.hardware.AmbientDisplayConfiguration;
import com.android.systemui.statusbar.phone.DozeParameters;
import com.android.systemui.util.wakelock.WakeLock;
import com.android.systemui.util.wakelock.WakeLockFake;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

public class DozeTriggersTest {
    private Context mContext;
    private DozeTriggers mTriggers;
    private DozeMachine mMachine;
    private DozeHostFake mHost;
    private AmbientDisplayConfiguration mConfig;
    private DozeParameters mParameters;
    private SensorManagerFake mSensors;
    private Handler mHandler;
    private WakeLock mWakeLock;
    private Instrumentation mInstrumentation;

    @BeforeClass
    public static void setupSuite() {
        // We can't use KeyguardUpdateMonitor from tests.
        DozeLog.setRegisterKeyguardCallback(false);
    }

    @Before
    public void setUp() throws Exception {
        mInstrumentation = InstrumentationRegistry.getInstrumentation();
        mContext = InstrumentationRegistry.getContext();
        mMachine = mock(DozeMachine.class);
        mHost = new DozeHostFake();
        mConfig = DozeConfigurationUtil.createMockConfig();
        mParameters = DozeConfigurationUtil.createMockParameters();
        mSensors = new SensorManagerFake(mContext);
        mHandler = new Handler(Looper.getMainLooper());
        mWakeLock = new WakeLockFake();

        mInstrumentation.runOnMainSync(() -> {
            mTriggers = new DozeTriggers(mContext, mMachine, mHost,
                    mConfig, mParameters, mSensors, mHandler, mWakeLock, true);
        });
    }

    @Test
    @Ignore("setup crashes on virtual devices")
    public void testOnNotification_stillWorksAfterOneFailedProxCheck() throws Exception {
        when(mMachine.getState()).thenReturn(DozeMachine.State.DOZE);

        mInstrumentation.runOnMainSync(()->{
            mTriggers.transitionTo(DozeMachine.State.UNINITIALIZED, DozeMachine.State.INITIALIZED);
            mTriggers.transitionTo(DozeMachine.State.UNINITIALIZED, DozeMachine.State.DOZE);

            mHost.callback.onNotificationHeadsUp();
        });

        mInstrumentation.runOnMainSync(() -> {
            mSensors.PROXIMITY.sendProximityResult(false); /* Near */
        });

        verify(mMachine, never()).requestState(any());

        mInstrumentation.runOnMainSync(()->{
            mHost.callback.onNotificationHeadsUp();
        });

        mInstrumentation.runOnMainSync(() -> {
            mSensors.PROXIMITY.sendProximityResult(true); /* Far */
        });

        verify(mMachine).requestPulse(anyInt());
    }

}