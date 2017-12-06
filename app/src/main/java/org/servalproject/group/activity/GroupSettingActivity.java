package org.servalproject.group.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ResourceCursorAdapter;
import android.widget.SeekBar;
import android.widget.TextView;

import org.servalproject.R;
import org.servalproject.ServalBatPhoneApplication;
import org.servalproject.group.GroupCommunicationController;
import org.servalproject.group.GroupDAO;
import org.servalproject.group.service.UpdateSubKeyService;
import org.servalproject.servaldna.keyring.KeyringIdentity;

import static org.servalproject.group.service.UpdateSubKeyService.THRESHOLD_VALUE;

public class GroupSettingActivity extends Activity {

    private RadioGroup rgMode;
    private RadioButton rbModePercentage;
    private RadioButton rbModeFix;
    private Button buttonOK;
    private LinearLayout ll;
    private TextView tvCurrent;
    private TextView tvValue;
    private TextView tvCurrentMode;
    private TextView tvCurrentValue;
    private GroupDAO groupDAO;
    private ServalBatPhoneApplication app;
    private KeyringIdentity identity;
    private String mySid;
    private String groupName;
    private String leader;
    private String mode;
    private String value;
    private int groupSize;
    private static final int THRESHOLD_MIN = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_setting);
        Intent intent = getIntent();
        groupName =  intent.getStringExtra("group_name");
        leader = intent.getStringExtra("leader_sid");
        try {
            app = ServalBatPhoneApplication.context;
            identity = app.server.getIdentity();
            mySid = identity.sid.toString();
            groupDAO = new GroupDAO(getApplicationContext(), mySid);
            mode = groupDAO.getGroupThresholdMode(groupName, leader).get("mode");
            value = groupDAO.getGroupThresholdMode(groupName, leader).get("value");
            groupSize = groupDAO.getGroupSize(groupName,leader);
            setupView();
        } catch (Exception e) {
            app.displayToastMessage(e.getMessage());

        }

    }

    private void setupView() {
        buttonOK = (Button) findViewById(R.id.button_ok);
        ll = (LinearLayout) findViewById(R.id.linear_layout_value);
        tvCurrent = (TextView) findViewById(R.id.text_view_current);
        tvValue = (TextView) findViewById(R.id.text_view_setting_value);
        tvCurrentMode = (TextView) findViewById(R.id.text_view_current_mode);
        tvCurrentValue = (TextView) findViewById(R.id.text_view_current_value);
        String actualThreshold = value;
        if(mode.equals("percentage")) {
            Double threshold = Math.ceil(groupSize * Double.valueOf(value));
            actualThreshold = String.valueOf((int) Math.max(2, threshold));
            String contentMode = "Current mode: percentage";
            String contentValue = "Current value: " + (int) (Double.valueOf(value)*100) + "%";
            tvCurrentMode.setText(contentMode);
            tvCurrentValue.setText(contentValue);
        } else {
            String contentMode = "Current mode: fix";
            String contentValue = "Current value: " + value;
            tvCurrentMode.setText(contentMode);
            tvCurrentValue.setText(contentValue);
        }
        String content = " Current threshold: " + actualThreshold;
        tvCurrent.setText(content);
        rgMode = (RadioGroup) findViewById(R.id.radio_group_mode);
        rbModeFix = (RadioButton) findViewById(R.id.radio_button_mode_fix);
        rbModePercentage = (RadioButton) findViewById(R.id.radio_button_mode_percentage);
        rgMode.setOnCheckedChangeListener(
                new RadioGroup.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(RadioGroup group, int checkedId) {
                        switch (checkedId) {
                            case R.id.radio_button_mode_fix:
                                createRadioGroup("fix");
                                break;
                            case R.id.radio_button_mode_percentage:
                                createRadioGroup("percentage");
                                break;
                        }
                    }
                }
        );
        buttonOK.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                groupDAO.setNotUpToDate(groupName, leader);
                groupDAO.setGroupThresholdMode(groupName, leader, mode, value);
                finish();
            }
        });
    }
    private void createRadioGroup(String type){
        ll.removeAllViews();

        if(type.equals("fix")) {
            mode = "fix";
            value = String.valueOf(THRESHOLD_MIN);
            String content = " Current threshold: " + String.valueOf(THRESHOLD_MIN);
            tvCurrent.setText(content);
            tvValue.setText(String.valueOf(THRESHOLD_MIN));
            NumberPicker np = new NumberPicker(this);
            np.setMaxValue(groupSize);
            np.setMinValue(THRESHOLD_MIN);
            np.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
                @Override
                public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
                    String actualThreshold = String.valueOf(newVal);
                    value = actualThreshold;
                    String content = " Current threshold: " + actualThreshold;
                    tvCurrent.setText(content);
                    tvValue.setText(actualThreshold);
                }
            });
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                    100 , LinearLayout.LayoutParams.WRAP_CONTENT);
            layoutParams.gravity = Gravity.CENTER;
            ll.addView(np, layoutParams);
        } else {
            mode = "percentage";
            value = "0";
            SeekBar sk = new SeekBar(this);
            String actualThreshold = String.valueOf(THRESHOLD_MIN);
            String content = " Current threshold: " + actualThreshold;
            tvCurrent.setText(content);
            sk.setMax(100);
            tvValue.setText("0%");
            sk.setOnSeekBarChangeListener(
                    new SeekBar.OnSeekBarChangeListener() {
                        @Override
                        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                            value = String.valueOf(((double) progress)/100D);
                            Double threshold = Math.ceil(groupSize * ((double) progress)/100D);
                            String actualThreshold = String.valueOf((int) Math.max(THRESHOLD_MIN, threshold));
                            String content = " Current threshold: " + actualThreshold;
                            String contentValue = String.valueOf(progress) + "%";
                            tvValue.setText(contentValue);
                            tvCurrent.setText(content);
                        }

                        @Override
                        public void onStartTrackingTouch(SeekBar seekBar) {

                        }

                        @Override
                        public void onStopTrackingTouch(SeekBar seekBar) {

                        }
                    }
            );
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            layoutParams.setMargins(30, 20, 30, 0);
            ll.addView(sk, layoutParams);
        }
    }
}
