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
import android.support.annotation.NonNull;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.FlowableEmitter;
import io.reactivex.FlowableOnSubscribe;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.Single;
import io.reactivex.SingleEmitter;
import io.reactivex.SingleOnSubscribe;
import io.reactivex.disposables.Disposable;

/**
 * RxSensorManager exposes a reactive interface to the Android {@link SensorManager} API.
 *
 * @author    Bryan Dunlap
 * @see       SensorManager
 * @since     0.8.0-alpha
 */
public class RxSensorManager {
    private final SensorManager sensorManager;
    private enum DynamicSensorCallbackType { CONNECTED, DISCONNECTED }

    /**
     * Public constructor.
     *
     * @param    sensorManager  a {@link SensorManager} instance to wrap
     * @since    0.8.0-alpha
     */
    public RxSensorManager(@NonNull final SensorManager sensorManager) {
        this.sensorManager = sensorManager;
    }

    /**
     * Create a {@link Flowable} that notifies subscribers of a change in a given {@link Sensor}.
     *
     * @param    type  the {@link Sensor} type to request updates for
     * @param    samplingPeriodUs    the desired delay between two consecutive events in microseconds
     * @return   A {@link Flowable} that notifies subscribers of a change in a given {@link Sensor}.
     * @since    0.8.0-alpha
     */
    @NonNull
    public Flowable<SensorEvent> observeSensor(final int type, final int samplingPeriodUs) {
        return observeSensor(type, samplingPeriodUs, 0);
    }

    /**
     * Create a {@link Flowable} that notifies subscribers of a change in a given {@link Sensor}.
     * <p>
     * <code>maxReportLatencyUs</code> is the maximum time in microseconds that events can be delayed before being
     * reported. <b>Works for API level >= KITKAT ONLY.</b>
     *
     * @param    type  the {@link Sensor} type to request updates for
     * @param    samplingPeriodUs    the desired delay between two consecutive events in microseconds
     * @param    maxReportLatencyUs  maximum time in microseconds that events can be delayed before being reported
     * @return   A {@link Flowable} that notifies subscribers of a change in a given {@link Sensor}.
     * @since    0.8.0-alpha
     */
    @NonNull
    public Flowable<SensorEvent> observeSensor(
            final int type,
            final int samplingPeriodUs,
            final int maxReportLatencyUs
    ) {
        return Flowable.create(new FlowableOnSubscribe<SensorEvent>() {
            @Override
            @TargetApi(Build.VERSION_CODES.KITKAT)
            public void subscribe(final FlowableEmitter<SensorEvent> emitter) {
                final Sensor sensor = sensorManager.getDefaultSensor(type);
                if (sensor == null) {
                    emitter.onError(new SensorNotFoundException(type));
                    return;
                }
                final SensorEventListener sensorEventListener = new SensorEventListener() {
                    @Override
                    public void onSensorChanged(SensorEvent sensorEvent) {
                        emitter.onNext(sensorEvent);
                    }

                    @Override
                    public void onAccuracyChanged(Sensor sensor, int accuracy) {
                        // noop
                    }
                };
                final boolean sensorEnabled;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    sensorEnabled = sensorManager.registerListener(
                            sensorEventListener,
                            sensor,
                            samplingPeriodUs,
                            maxReportLatencyUs
                    );
                } else {
                    sensorEnabled = sensorManager.registerListener(
                            sensorEventListener,
                            sensor,
                            samplingPeriodUs
                    );
                }
                if (!sensorEnabled) {
                    emitter.onError(new SensorListenerException(sensor));
                    return;
                }
                emitter.setDisposable(new Disposable() {
                    boolean disposed = false;

                    @Override
                    public void dispose() {
                        sensorManager.unregisterListener(sensorEventListener);
                        disposed = true;
                    }

                    @Override
                    public boolean isDisposed() {
                        return disposed;
                    }
                });
            }
        }, BackpressureStrategy.LATEST);
    }

    /**
     * Create a {@link Flowable} that notifies subscribers of a change in the accuracy of a given {@link Sensor}.
     * <p>
     * Accuracy change is modeled as a {@link SensorAccuracyEvent}, which contains the sensor under observation
     * as well as the accuracy value.
     *
     * @param    type  the {@link Sensor} type to request accuracy updates for
     * @param    samplingPeriodUs    the desired delay between two consecutive events in microseconds
     * @return   A {@link Flowable} that notifies subscribers of a change in the accuracy of a given {@link Sensor}.
     * @since    0.8.0-alpha
     */
    @NonNull
    public Flowable<SensorAccuracyEvent> observeSensorAccuracy(final int type, final int samplingPeriodUs) {
        return observeSensorAccuracy(type, samplingPeriodUs, 0);
    }

    /**
     * Create a {@link Flowable} that notifies subscribers of a change in the accuracy of a given {@link Sensor}.
     * <p>
     * Accuracy change is modeled as a {@link SensorAccuracyEvent}, which contains the sensor under observation
     * as well as the accuracy value.
     * <p>
     * <code>maxReportLatencyUs</code> is the maximum time in microseconds that events can be delayed before being
     * reported. <b>Works for API level >= KITKAT ONLY.</b>
     *
     * @param    type  the {@link Sensor} type to request accuracy updates for
     * @param    samplingPeriodUs    the desired delay between two consecutive events in microseconds
     * @param    maxReportLatencyUs  maximum time in microseconds that events can be delayed before being reported
     * @return   A {@link Flowable} that notifies subscribers of a change in the accuracy of a given {@link Sensor}.
     * @since    0.8.0-alpha
     */
    @NonNull
    public Flowable<SensorAccuracyEvent> observeSensorAccuracy(
            final int type,
            final int samplingPeriodUs,
            final int maxReportLatencyUs
    ) {
        return Flowable.create(new FlowableOnSubscribe<SensorAccuracyEvent>() {
            @Override
            @TargetApi(Build.VERSION_CODES.KITKAT)
            public void subscribe(final FlowableEmitter<SensorAccuracyEvent> emitter) {
                final Sensor sensor = sensorManager.getDefaultSensor(type);
                if (sensor == null) {
                    emitter.onError(new SensorNotFoundException(type));
                    return;
                }
                final SensorEventListener sensorEventListener = new SensorEventListener() {
                    @Override
                    public void onSensorChanged(SensorEvent sensorEvent) {
                        // noop
                    }

                    @Override
                    public void onAccuracyChanged(Sensor sensor, int accuracy) {
                        emitter.onNext(new SensorAccuracyEvent(sensor, accuracy));
                    }
                };
                final boolean sensorEnabled;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    sensorEnabled = sensorManager.registerListener(
                            sensorEventListener,
                            sensor,
                            samplingPeriodUs,
                            maxReportLatencyUs
                    );
                } else {
                    sensorEnabled = sensorManager.registerListener(
                            sensorEventListener,
                            sensor,
                            samplingPeriodUs
                    );
                }
                if (!sensorEnabled) {
                    emitter.onError(new SensorListenerException(sensor));
                    return;
                }
                emitter.setDisposable(new Disposable() {
                    boolean disposed = false;

                    @Override
                    public void dispose() {
                        sensorManager.unregisterListener(sensorEventListener);
                        disposed = true;
                    }

                    @Override
                    public boolean isDisposed() {
                        return disposed;
                    }
                });
            }
        }, BackpressureStrategy.LATEST);
    }

    /**
     * Create a {@link Single} that notifies subscribers of a {@link TriggerEvent} on a given {@link Sensor}.
     * <p>
     * This is modeled as a {@link Single} because once the sensor detects a trigger event condition, the provided
     * {@link TriggerEventListener} will be invoked once and then cancelled. To continue receiving trigger events, the
     * caller must subscribe to a new {@link Single} for each event received.
     *
     * @param    type  the {@link Sensor} type to request {@link TriggerEvent}s for
     * @return   A {@link Single} that notifies subscribers of a {@link TriggerEvent} on a given {@link Sensor}.
     * @since    0.8.0-alpha
     */
    @NonNull
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    public Single<TriggerEvent> observeTrigger(final int type) {
        return Single.create(new SingleOnSubscribe<TriggerEvent>() {
            @Override
            public void subscribe(final SingleEmitter<TriggerEvent> emitter) {
                final Sensor sensor = sensorManager.getDefaultSensor(type);
                if (sensor == null) {
                    emitter.onError(new SensorNotFoundException(type));
                    return;
                }
                final TriggerEventListener triggerEventListener = new TriggerEventListener() {
                    @Override
                    public void onTrigger(TriggerEvent triggerEvent) {
                        emitter.onSuccess(triggerEvent);
                    }
                };
                final boolean sensorEnabled = sensorManager.requestTriggerSensor(
                        triggerEventListener,
                        sensor
                );
                if (!sensorEnabled) {
                    emitter.onError(new SensorListenerException(sensor));
                    return;
                }
                emitter.setDisposable(new Disposable() {
                    boolean disposed = false;

                    @Override
                    public void dispose() {
                        sensorManager.cancelTriggerSensor(triggerEventListener, sensor);
                        disposed = true;
                    }

                    @Override
                    public boolean isDisposed() {
                        return disposed;
                    }
                });
            }
        });
    }

    /**
     * Create an {@link Observable} that notifies subscribers of dynamic sensor connections.
     * <p>
     * Every time a dynamic sensor is connected, a corresponding {@link Sensor} instance is pushed to
     * all subscribers.
     *
     * @return   An {@link Observable} that notifies subscribers of dynamic sensor connections.
     * @since    0.8.0-alpha
     */
    @NonNull
    @TargetApi(Build.VERSION_CODES.N)
    public Observable<Sensor> observeDynamicSensorConnections() {
        return createDynamicSensorObservable(DynamicSensorCallbackType.CONNECTED);
    }

    /**
     * Create an {@link Observable} that notifies subscribers of dynamic sensor disconnections.
     * <p>
     * Every time a dynamic sensor is disconnected, a corresponding {@link Sensor} instance is pushed to
     * all subscribers.
     *
     * @return   An {@link Observable} that notifies subscribers of dynamic sensor disconnections.
     * @since    0.8.0-alpha
     */
    @NonNull
    @TargetApi(Build.VERSION_CODES.N)
    public Observable<Sensor> observeDynamicSensorDisconnections() {
        return createDynamicSensorObservable(DynamicSensorCallbackType.DISCONNECTED);
    }

    @NonNull
    @TargetApi(Build.VERSION_CODES.N)
    private Observable<Sensor> createDynamicSensorObservable(final DynamicSensorCallbackType type) {
        return Observable.create(new ObservableOnSubscribe<Sensor>() {
            @Override
            public void subscribe(final ObservableEmitter<Sensor> emitter) {
                // No dynamic sensor discovery support is an obvious show stopper.
                if (!sensorManager.isDynamicSensorDiscoverySupported()) {
                    emitter.onError(new SensorDiscoveryException());
                    return;
                }

                // Create and register an appropriate DynamicSensorCallback instance based on the type requested.
                final SensorManager.DynamicSensorCallback callback;
                switch (type) {
                    case CONNECTED:
                        callback = new SensorManager.DynamicSensorCallback() {
                            @Override
                            public void onDynamicSensorConnected(Sensor sensor) {
                                emitter.onNext(sensor);
                            }
                        };
                        break;
                    case DISCONNECTED:
                        callback = new SensorManager.DynamicSensorCallback() {
                            @Override
                            public void onDynamicSensorDisconnected(Sensor sensor) {
                                emitter.onNext(sensor);
                            }
                        };
                        break;
                    default:
                        emitter.onError(new SensorDiscoveryException());
                        return;
                }
                sensorManager.registerDynamicSensorCallback(callback);

                // Unregister the DynamicSensorCallback instance when a subscription is disposed of.
                emitter.setDisposable(new Disposable() {
                    boolean disposed = false;

                    @Override
                    public void dispose() {
                        sensorManager.unregisterDynamicSensorCallback(callback);
                        disposed = true;
                    }

                    @Override
                    public boolean isDisposed() {
                        return disposed;
                    }
                });
            }
        });
    }
}
