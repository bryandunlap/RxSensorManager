# RxSensorManager
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](http://www.apache.org/licenses/LICENSE-2.0)
[![Build Status](https://travis-ci.org/bryandunlap/RxSensorManager.svg?branch=master)](https://travis-ci.org/bryandunlap/RxSensorManager)
[![Code Coverage](https://img.shields.io/codecov/c/github/bryandunlap/RxSensorManager/master.svg)](https://codecov.io/gh/bryandunlap/RxSensorManager)

A reactive interface to the Android [SensorManager](https://developer.android.com/reference/android/hardware/SensorManager.html) API. Based on RxJava 2.

## Usage
A sample project which provides runnable code examples that demonstrate uses of this API is available in the [examples](https://github.com/bryandunlap/RxSensorManager/tree/master/examples) directory.
#### Construction
`RxSensorManager` wraps an Android `SensorManager` instance via constructor injection:
```java
RxSensorManager rxSensorManager = new RxSensorManager((SensorManager) getSystemService(SENSOR_SERVICE));
```

#### Sensor events
Sensor event observation is modeled as a `Flowable` because fundamentally we should be able to customize backpressure support by throttling sensor data through a combination of sampling period and report latency.

For now, the default backpressure strategy is `BackpressureStrategy.LATEST`.
```java
rxSensorManager.observeSensor(Sensor.TYPE_ACCELEROMETER)
        .subscribe(new Observer<SensorEvent>() {
             @Override
            public void onSubscribe(Disposable disposable) {
                // Remember to dispose of this at some point...
            }

            @Override
            public void onNext(SensorEvent event) {
                // Handle sensor event...
            }

            @Override
            public void onError(Throwable e) {
                // Handle error...
            }

            @Override
             public void onComplete() {
                 // This should never complete...
             }
         });
```

#### Trigger events (API level 18)
Trigger sensors are sensors that trigger a single event and are subsequently disabled. As such, trigger sensor observation is modeled as a `Single`:
```java
rxSensorManager.observeTrigger(Sensor.TYPE_SIGNIFICANT_MOTION)
        .subscribe(new SingleObserver<TriggerEvent>() {
             @Override
            public void onSubscribe(Disposable disposable) {
                // Remember to dispose of this at some point...
            }

            @Override
            public void onSuccess(TriggerEvent event) {
                // Handle trigger event...
            }

            @Override
            public void onError(Throwable e) {
                // Handle error...
            }
        });
```

#### Dynamic sensor (dis)connections (API level 24)
Starting with N, Android supports the concept of "dynamic" sensors. Dynamic sensor dis(connections) are modeled as an `Observable` because there is no reasonable way to throttle them.
```java
rxSensorManager.observeDynamicSensorConnections()
        .subscribe(new Observer<Sensor>() {
             @Override
            public void onSubscribe(Disposable disposable) {
                // Remember to dispose of this at some point...
            }

            @Override
            public void onNext(Sensor sensor) {
                // Handle sensor connection...
            }

            @Override
            public void onError(Throwable e) {
                // Handle error...
            }

            @Override
             public void onComplete() {
                 // This should never complete...
             }
         });
```

## Download
I'm working toward an official alpha release. For now, snapshot is available via JitPack:
```groovy
allprojects {
    repositories {
        ...
        maven { url 'https://jitpack.io' }
    }
}
```
```groovy
compile 'com.github.bryandunlap:RxSensorManager:master-SNAPSHOT'
```

## Acknowledgments
Initial inspiration taken from [RxSensor](https://github.com/nvanbenschoten/RxSensor) as an RxJava 1.x implementation. Thanks [nvanbenschoten](https://github.com/nvanbenschoten)!

## License
    Copyright (c) 2016 Bryan Dunlap

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.