package net.bryandunlap.rxsensormanager.examples;

import android.annotation.TargetApi;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorManager;
import android.hardware.TriggerEvent;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.Observer;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.Locale;
import net.bryandunlap.rxsensormanager.RxSensorManager;

public class MainActivity extends AppCompatActivity {
    private CompositeDisposable compositeDisposable;
    private RxSensorManager rxSensorManager;

    @BindView(R.id.sensor_switch)
    Switch sensorSwitch;

    @BindView(R.id.trigger_switch)
    Switch triggerSwitch;

    @BindView(R.id.dynamic_switch)
    Switch dynamicSwitch;

    @BindView(R.id.sensor_x_text)
    TextView sensorXTextView;

    @BindView(R.id.sensor_y_text)
    TextView sensorYTextView;

    @BindView(R.id.sensor_z_text)
    TextView sensorZTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        compositeDisposable = new CompositeDisposable();
        rxSensorManager = new RxSensorManager((SensorManager) getSystemService(SENSOR_SERVICE));

        bindSensorSwitch();
        bindTriggerSwitch();
        bindDynamicSwitch();
    }

    @Override
    protected void onDestroy() {
        compositeDisposable.clear();
        super.onDestroy();
    }

    private Observable<Boolean> checkedChanges(final CompoundButton button) {
        return Observable.create(new ObservableOnSubscribe<Boolean>() {
            @Override
            public void subscribe(final ObservableEmitter<Boolean> emitter) throws Exception {
                final CompoundButton.OnCheckedChangeListener checkedChangeListener =
                        new CompoundButton.OnCheckedChangeListener() {
                            @Override
                            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                                emitter.onNext(isChecked);
                            }
                        };
                button.setOnCheckedChangeListener(checkedChangeListener);
                emitter.setDisposable(new Disposable() {
                    boolean isDisposed;

                    @Override
                    public void dispose() {
                        button.setOnCheckedChangeListener(null);
                    }

                    @Override
                    public boolean isDisposed() {
                        return isDisposed;
                    }
                });
            }
        });
    }

    private void bindSensorSwitch() {
        final NumberFormat formatter = NumberFormat.getInstance(Locale.US);
        formatter.setMinimumFractionDigits(10);
        formatter.setMaximumFractionDigits(10);
        formatter.setRoundingMode(RoundingMode.HALF_UP);
        checkedChanges(sensorSwitch)
                .doOnNext(new Consumer<Boolean>() {
                    @Override
                    public void accept(Boolean isChecked) {
                        if (isChecked) {
                            showSensorData();
                        } else {
                            hideSensorData();
                        }
                    }
                })
                .switchMap(new Function<Boolean, Observable<SensorEvent>>() {
                    @Override
                    public Observable<SensorEvent> apply(Boolean isChecked) {
                        if (isChecked) {
                            return rxSensorManager.observeSensor(
                                    Sensor.TYPE_ACCELEROMETER,
                                    SensorManager.SENSOR_DELAY_NORMAL
                            ).toObservable();
                        } else {
                            return Observable.empty();
                        }
                    }
                })
                .subscribe(new Observer<SensorEvent>() {
                    @Override
                    public void onSubscribe(Disposable disposable) {
                        compositeDisposable.add(disposable);
                    }

                    @Override
                    public void onNext(SensorEvent event) {
                        sensorXTextView.setText(formatter.format(event.values[0]));
                        sensorYTextView.setText(formatter.format(event.values[1]));
                        sensorZTextView.setText(formatter.format(event.values[2]));
                    }

                    @Override
                    public void onError(Throwable e) {
                        toast("Sensor event error: " + e.getMessage());
                    }

                    @Override
                    public void onComplete() {
                        toast("This should not have just completed! UNPOSSIBLE!");
                    }
                });
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    private void bindTriggerSwitch() {
                checkedChanges(triggerSwitch)
                .switchMap(new Function<Boolean, Observable<TriggerEvent>>() {
                    @Override
                    public Observable<TriggerEvent> apply(Boolean isChecked) {
                        if (isChecked) {
                            return rxSensorManager.observeTrigger(Sensor.TYPE_SIGNIFICANT_MOTION)
                                    .toObservable();
                        } else {
                            return Observable.empty();
                        }
                    }
                })
                .subscribe(new Observer<TriggerEvent>() {
                    @Override
                    public void onSubscribe(Disposable disposable) {
                        compositeDisposable.add(disposable);
                    }

                    @Override
                    public void onNext(TriggerEvent event) {
                        toast("Trigger event: " + event.toString());
                        triggerSwitch.setChecked(false);
                    }

                    @Override
                    public void onError(Throwable e) {
                        toast("Trigger event error: " + e.getMessage());
                    }

                    @Override
                    public void onComplete() {
                        toast("This should not have just completed! UNPOSSIBLE!");
                    }
                });
    }

    @TargetApi(Build.VERSION_CODES.N)
    private void bindDynamicSwitch() {
        checkedChanges(dynamicSwitch)
                .switchMap(new Function<Boolean, Observable<Sensor>>() {
                    @Override
                    public Observable<Sensor> apply(Boolean isChecked) {
                        if (isChecked) {
                            return rxSensorManager.observeDynamicSensorConnections();
                        } else {
                            return Observable.empty();
                        }
                    }
                })
                .subscribe(new Observer<Sensor>() {
                    @Override
                    public void onSubscribe(Disposable disposable) {
                        compositeDisposable.add(disposable);
                    }

                    @Override
                    public void onNext(Sensor sensor) {
                        toast("Dynamic sensor connected: " + sensor.toString());
                    }

                    @Override
                    public void onError(Throwable e) {
                        toast("Dynamic sensor error: " + e.getMessage());
                        dynamicSwitch.setChecked(false);
                        dynamicSwitch.setEnabled(false);
                    }

                    @Override
                    public void onComplete() {
                        toast("This should not have just completed! UNPOSSIBLE!");
                    }
                });
    }

    private void hideSensorData() {
        sensorXTextView.setVisibility(View.INVISIBLE);
        sensorYTextView.setVisibility(View.INVISIBLE);
        sensorZTextView.setVisibility(View.INVISIBLE);
    }

    private void showSensorData() {
        sensorXTextView.setVisibility(View.VISIBLE);
        sensorYTextView.setVisibility(View.VISIBLE);
        sensorZTextView.setVisibility(View.VISIBLE);
    }

    private void toast(@NonNull final String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG)
                .show();
    }
}
