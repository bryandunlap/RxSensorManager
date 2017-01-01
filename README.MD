# RxSensorManager
[![Build Status](https://travis-ci.org/bryandunlap/RxSensorManager.svg?branch=master)](https://travis-ci.org/bryandunlap/RxSensorManager)
&nbsp;
[![Code Coverage](https://img.shields.io/codecov/c/github/bryandunlap/RxSensorManager/master.svg)](https://codecov.io/gh/bryandunlap/RxSensorManager)

A reactive interface to the Android [SensorManager](https://developer.android.com/reference/android/hardware/SensorManager.html) API. Based on RxJava 2.

## Usage
RxSensorManager wraps an Android `SensorManager` instance via constructor injection:
```java
RxSensorManager rxSensorManager = new RxSensorManager((SensorManager) getSystemService(SENSOR_SERVICE));
```

#### Sensor events
#### Trigger sensors (API level 18)
Trigger sensors are sensors that trigger a single event and are subsequently disabled. As such, trigger sensor observation is modeled as an RxJava 2 `Single`:
```java
rxSensorManager.observeTrigger(Sensor.TYPE_SIGNIFICANT_MOTION)
        .subscribe(new SingleObserver<TriggerEvent>() {
             @Override
            public void onSubscribe(Disposable disposable) {
                // noop
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

## Download
Snapshot available via JitPack:
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
Initial inspiration from [RxSensor](https://github.com/nvanbenschoten/RxSensor) as an RxJava 1.x implementation. Thanks [nvanbenschoten](https://github.com/nvanbenschoten)!

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