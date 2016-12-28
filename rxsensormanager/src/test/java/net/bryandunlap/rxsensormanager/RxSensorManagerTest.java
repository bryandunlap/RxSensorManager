/*
 * Copyright (c) 2016 Bryan Dunlap
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.bryandunlap.rxsensormanager;

import android.annotation.TargetApi;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.TriggerEvent;
import android.hardware.TriggerEventListener;
import android.os.Build;
import io.reactivex.disposables.Disposable;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// TODO: DRY REFACTOR
public class RxSensorManagerTest {
    private static final int INVALID_SENSOR_TYPE = -1;

    private SensorManager mockSensorManager;
    private Sensor mockSensor;
    private RxSensorManager rxSensorManager;

    @Before
    public void before() {
        mockSensorManager = mock(SensorManager.class);
        mockSensor = mock(Sensor.class);
        rxSensorManager = new RxSensorManager(mockSensorManager);
    }

    @Test
    public void testObserveSensor() throws Exception {
        setFinalStatic(Build.VERSION.class.getField("SDK_INT"), 9);
        when(mockSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)).thenReturn(mockSensor);
        final int numberOfEventsToSend = 5;
        final ArgumentCaptor<SensorEventListener> argumentCaptor = ArgumentCaptor.forClass(SensorEventListener.class);
        when(mockSensorManager.registerListener(
                argumentCaptor.capture(),
                eq(mockSensor),
                eq(0)
        )).thenAnswer(new Answer<Boolean>() {
            @Override
            public Boolean answer(InvocationOnMock invocation) throws Throwable {
                for (int i = 0; i < numberOfEventsToSend; ++i) {
                    argumentCaptor.getValue().onSensorChanged(mock(SensorEvent.class));
                }
                return true;
            }
        });
        rxSensorManager.observeSensor(Sensor.TYPE_ACCELEROMETER, 0)
                .test()
                .assertValueCount(numberOfEventsToSend);
    }

    @Test
    public void testObserveSensorOnErrorSensorNotFoundException() {
        when(mockSensorManager.getDefaultSensor(INVALID_SENSOR_TYPE)).thenReturn(null);
        rxSensorManager.observeSensor(INVALID_SENSOR_TYPE, 0)
                .test()
                .assertError(SensorNotFoundException.class);
    }

    @Test
    public void testObserveSensorOnErrorSensorListenerException() throws Exception {
        setFinalStatic(Build.VERSION.class.getField("SDK_INT"), 9);
        when(mockSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)).thenReturn(mockSensor);
        when(mockSensorManager.registerListener(
                any(SensorEventListener.class),
                eq(mockSensor),
                eq(0)
        )).thenReturn(false);
        rxSensorManager.observeSensor(Sensor.TYPE_ACCELEROMETER, 0)
                .test()
                .assertError(SensorListenerException.class);
    }

    @Test
    @TargetApi(Build.VERSION_CODES.KITKAT)
    public void testObserveSensorOnErrorSensorListenerExceptionApi19() throws Exception {
        setFinalStatic(Build.VERSION.class.getField("SDK_INT"), 19);
        when(mockSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)).thenReturn(mockSensor);
        when(mockSensorManager.registerListener(
                any(SensorEventListener.class),
                eq(mockSensor),
                eq(0),
                eq(0)
        )).thenReturn(false);
        rxSensorManager.observeSensor(Sensor.TYPE_ACCELEROMETER, 0)
                .test()
                .assertError(SensorListenerException.class);
    }

    @Test
    public void testObserveSensorDisposable() throws Exception {
        setFinalStatic(Build.VERSION.class.getField("SDK_INT"), 9);
        when(mockSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)).thenReturn(mockSensor);
        when(mockSensorManager.registerListener(
                any(SensorEventListener.class),
                eq(mockSensor),
                eq(0)
        )).thenReturn(true);
        Disposable disposable = rxSensorManager.observeSensor(Sensor.TYPE_ACCELEROMETER, 0).test();
        disposable.dispose();
        verify(mockSensorManager).unregisterListener(any(SensorEventListener.class));
        assertEquals(disposable.isDisposed(), true);
    }

    @Test
    public void testObserveSensorAccuracy() throws Exception {
        setFinalStatic(Build.VERSION.class.getField("SDK_INT"), 9);
        when(mockSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)).thenReturn(mockSensor);
        final int numberOfEventsToSend = 5;
        final ArgumentCaptor<SensorEventListener> argumentCaptor = ArgumentCaptor.forClass(SensorEventListener.class);
        when(mockSensorManager.registerListener(
                argumentCaptor.capture(),
                eq(mockSensor),
                eq(0)
        )).thenAnswer(new Answer<Boolean>() {
            @Override
            public Boolean answer(InvocationOnMock invocation) throws Throwable {
                for (int i = 0; i < numberOfEventsToSend; ++i) {
                    argumentCaptor.getValue().onAccuracyChanged(mockSensor, SensorManager.SENSOR_STATUS_ACCURACY_HIGH);
                }
                return true;
            }
        });
        rxSensorManager.observeSensorAccuracy(Sensor.TYPE_ACCELEROMETER, 0)
                .test()
                .assertValueCount(numberOfEventsToSend);
    }

    @Test
    public void testObserveSensorAccuracyOnErrorSensorNotFoundException() {
        when(mockSensorManager.getDefaultSensor(INVALID_SENSOR_TYPE)).thenReturn(null);
        rxSensorManager.observeSensorAccuracy(INVALID_SENSOR_TYPE, 0)
                .test()
                .assertError(SensorNotFoundException.class);
    }

    @Test
    public void testObserveSensorAccuracyOnErrorSensorListenerException() throws Exception {
        setFinalStatic(Build.VERSION.class.getField("SDK_INT"), 9);
        when(mockSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)).thenReturn(mockSensor);
        when(mockSensorManager.registerListener(
                any(SensorEventListener.class),
                eq(mockSensor),
                eq(0)
        )).thenReturn(false);
        rxSensorManager.observeSensorAccuracy(Sensor.TYPE_ACCELEROMETER, 0)
                .test()
                .assertError(SensorListenerException.class);
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    @Test
    public void testObserveSensorAccuracyOnErrorSensorListenerExceptionApi19() throws Exception {
        setFinalStatic(Build.VERSION.class.getField("SDK_INT"), 19);
        when(mockSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)).thenReturn(mockSensor);
        when(mockSensorManager.registerListener(
                any(SensorEventListener.class),
                eq(mockSensor),
                eq(0),
                eq(0)
        )).thenReturn(false);
        rxSensorManager.observeSensorAccuracy(Sensor.TYPE_ACCELEROMETER, 0)
                .test()
                .assertError(SensorListenerException.class);
    }

    @Test
    public void testObserveSensorAccuracyDisposable() throws Exception {
        setFinalStatic(Build.VERSION.class.getField("SDK_INT"), 9);
        when(mockSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)).thenReturn(mockSensor);
        when(mockSensorManager.registerListener(
                any(SensorEventListener.class),
                eq(mockSensor),
                eq(0)
        )).thenReturn(true);
        Disposable disposable = rxSensorManager.observeSensorAccuracy(Sensor.TYPE_ACCELEROMETER, 0).test();
        disposable.dispose();
        verify(mockSensorManager, times(1)).unregisterListener(any(SensorEventListener.class));
        assertEquals(disposable.isDisposed(), true);
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    @Test
    public void testObserveTrigger() throws Exception {
        when(mockSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)).thenReturn(mockSensor);
        final ArgumentCaptor<TriggerEventListener> argumentCaptor = ArgumentCaptor.forClass(TriggerEventListener.class);
        when(mockSensorManager.requestTriggerSensor(
                argumentCaptor.capture(),
                eq(mockSensor)
        )).thenAnswer(new Answer<Boolean>() {
            @Override
            public Boolean answer(InvocationOnMock invocation) throws Throwable {
                argumentCaptor.getValue().onTrigger(mock(TriggerEvent.class));
                return true;
            }
        });
        rxSensorManager.observeTrigger(Sensor.TYPE_ACCELEROMETER)
                .test()
                .assertValueCount(1);
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    @Test
    public void testObserveTriggerOnErrorSensorNotFoundException() {
        when(mockSensorManager.getDefaultSensor(INVALID_SENSOR_TYPE)).thenReturn(null);
        rxSensorManager.observeTrigger(INVALID_SENSOR_TYPE)
                .test()
                .assertError(SensorNotFoundException.class);
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    @Test
    public void testObserveTriggerOnErrorSensorListenerException() throws Exception {
        when(mockSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)).thenReturn(mockSensor);
        when(mockSensorManager.requestTriggerSensor(
                any(TriggerEventListener.class),
                eq(mockSensor)
        )).thenReturn(false);
        rxSensorManager.observeTrigger(Sensor.TYPE_ACCELEROMETER)
                .test()
                .assertError(SensorListenerException.class);
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    @Test
    public void testObserveTriggerDisposable() throws Exception {
        when(mockSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)).thenReturn(mockSensor);
        when(mockSensorManager.requestTriggerSensor(any(TriggerEventListener.class), eq(mockSensor))).thenReturn(true);
        Disposable disposable = rxSensorManager.observeTrigger(Sensor.TYPE_ACCELEROMETER).test();
        disposable.dispose();
        verify(mockSensorManager).cancelTriggerSensor(
                any(TriggerEventListener.class), eq(mockSensor)
        );
        assertEquals(disposable.isDisposed(), true);
    }

    @TargetApi(Build.VERSION_CODES.N)
    @Test
    public void testObserveDynamicSensorConnections() throws Exception {
        final int numberOfEventsToSend = 5;
        when(mockSensorManager.isDynamicSensorDiscoverySupported()).thenReturn(true);
        final ArgumentCaptor<SensorManager.DynamicSensorCallback> argumentCaptor =
                ArgumentCaptor.forClass(SensorManager.DynamicSensorCallback.class);
        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                for (int i = 0; i < numberOfEventsToSend; ++i) {
                    argumentCaptor.getValue().onDynamicSensorConnected(mockSensor);
                }
                return null;
            }
        }).when(mockSensorManager).registerDynamicSensorCallback(argumentCaptor.capture());
        rxSensorManager.observeDynamicSensorConnections()
                .test()
                .assertValueCount(numberOfEventsToSend);
    }

    @TargetApi(Build.VERSION_CODES.N)
    @Test
    public void testObserveDynamicSensorDisconnections() throws Exception {
        final int numberOfEventsToSend = 5;
        when(mockSensorManager.isDynamicSensorDiscoverySupported()).thenReturn(true);
        final ArgumentCaptor<SensorManager.DynamicSensorCallback> argumentCaptor =
                ArgumentCaptor.forClass(SensorManager.DynamicSensorCallback.class);
        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                for (int i = 0; i < numberOfEventsToSend; ++i) {
                    argumentCaptor.getValue().onDynamicSensorDisconnected(mockSensor);
                }
                return null;
            }
        }).when(mockSensorManager).registerDynamicSensorCallback(argumentCaptor.capture());
        rxSensorManager.observeDynamicSensorDisconnections()
                .test()
                .assertValueCount(numberOfEventsToSend);
    }

    @TargetApi(Build.VERSION_CODES.N)
    @Test
    public void testObserveDynamicSensorConnectionsOnErrorSensorDiscoveryException() throws Exception {
        when(mockSensorManager.isDynamicSensorDiscoverySupported()).thenReturn(false);
        rxSensorManager.observeDynamicSensorConnections().test().assertError(SensorDiscoveryException.class);
    }

    @TargetApi(Build.VERSION_CODES.N)
    @Test
    public void testObserveDynamicSensorConnectionsDisposable() throws Exception {
        when(mockSensorManager.isDynamicSensorDiscoverySupported()).thenReturn(true);
        Disposable disposable = rxSensorManager.observeDynamicSensorConnections().test();
        disposable.dispose();
        verify(mockSensorManager).unregisterDynamicSensorCallback(any(SensorManager.DynamicSensorCallback.class));
        assertEquals(disposable.isDisposed(), true);
    }

    // http://stackoverflow.com/questions/38074224/stub-value-of-build-version-sdk-int-in-local-unit-test
    private static void setFinalStatic(Field field, Object newValue) throws Exception {
        field.setAccessible(true);
        Field modifiersField = Field.class.getDeclaredField("modifiers");
        modifiersField.setAccessible(true);
        modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);
        field.set(null, newValue);
    }
}
