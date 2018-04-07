package com.yidea.divvyup;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import com.divvyup.jayson.divvyup.R;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "Settings";
    private ArrayList<View> viewArray = new ArrayList<>();
    private int tipTotal = 0;
    private int entries = 0;
    private int average = 0;
    private double hoursTotal = 0.00;
    private double hourly = 0.00;
    private int leftOver = 0;
    private int low = 0;
    private long lastPress;
    private modeType currentMode;
    private int defaultMode = modeType.INDIVIDUAL.ordinal();
    private CheckBox cbxHours;
    private boolean cbxChecked = false;
    private enum modeType {INDIVIDUAL, TIPBUCKET}
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i("MainActivity", "onCreate()");
        super.onCreate(savedInstanceState);


        setContentView(R.layout.layout);

        //Load saved mode
        currentMode = getSavedMode();
        //Load hourly status
        SharedPreferences sharedPref = getSharedPreferences(PREFS_NAME, 0);
        cbxChecked = sharedPref.getBoolean("checkbox", false);

        //Checkbox
        cbxHours = (CheckBox) findViewById(R.id.cbxHours);

        //Set layouts
        if (currentMode == modeType.INDIVIDUAL) {
            showIndividualLayout();
        } else {
            showTipBucketLayout();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    protected void onStart() {
        super.onStart();

        //Buttons
        final Button btnAddEntry = (Button) findViewById(R.id.btnAddEntry);
        Button btnClearAll = (Button) findViewById(R.id.btnClearAll);
        Button btnAbout1 = (Button) findViewById(R.id.btnAbout1);
        Button btnAbout2 = (Button) findViewById(R.id.btnAbout2);
        Button btnRefresh = (Button) findViewById(R.id.btnRefresh);
        RadioButton radIndividually = (RadioButton) findViewById(R.id.radIndividually);
        RadioButton radTipBucket = (RadioButton) findViewById(R.id.radTipBucket);

        //About Buttons
        btnAbout1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openAboutActivity();
            }
        });
        btnAbout2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openAboutActivity();
            }
        });

        //Add Entry Button
        btnAddEntry.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (currentMode == modeType.INDIVIDUAL) {
                    createEntryIndividual();
                } else if (currentMode == modeType.TIPBUCKET) {
                    createEntryTipBucket();
                }
            }
        });

        //Show or hide hourly
        if (!cbxChecked) {
            hideHourFunctions();
        } else {
            showHourFunctions();
        }

        //Radio Buttons - Collection
        radTipBucket.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showTipBucketLayout();
                clearAll();
            }
        });
        radIndividually.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showIndividualLayout();
                clearAll();
            }
        });

        //Checkbox listener
        cbxHours.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                checkBoxLayout();
            }
        });

        //Clear All Button
        btnClearAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                clearAll();
            }
        });

        //Refresh Button
        btnRefresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                checkHourlyTipBucket();
                logTakeHomeTipBucket();
            }
        });

        //IME ActionGo
        EditText tipsEnter = (EditText) findViewById(R.id.txtTipsInput);
        tipsEnter.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                boolean handled = false;
                if (actionId == EditorInfo.IME_ACTION_GO) {
                    if (cbxHours.isChecked()) {
                        final TextView hoursInput = (TextView) findViewById(R.id.txtHoursInput);
                        //Move selection to hours
                        hoursInput.requestFocus();
                        //Open soft keyboard
                        hoursInput.postDelayed(new Runnable() {
                                                   @Override
                                                   public void run() {
                                                       InputMethodManager keyboard = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                                                       keyboard.showSoftInput(hoursInput, 0);
                                                   }
                                               }
                                , 25);
                    } else {
                        btnAddEntry.performClick();
                        handled = true;
                    }
                }
                return handled;
            }
        });
        final EditText hoursEnter = (EditText) findViewById(R.id.txtHoursInput);
        hoursEnter.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                boolean handled = false;
                if (actionId == EditorInfo.IME_ACTION_GO) {
                    btnAddEntry.performClick();
                    //Move selection to tips (may not be necessary)
                    TextView tipsInput = (TextView) findViewById(R.id.txtTipsInput);
                    double value = 0;
                    if (!hoursEnter.getText().toString().equals("")) {
                        value = Double.parseDouble(hoursEnter.getText().toString());
                    }
                    if (value > 0) {
                        tipsInput.requestFocus();
                    }
                    handled = true;
                }
                return handled;
            }
        });
        final EditText tipBucketHours = (EditText) findViewById(R.id.txtPersonsHours);
        tipBucketHours.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                boolean handled = false;
                if (actionId == EditorInfo.IME_ACTION_GO) {
                    btnAddEntry.performClick();
                    //Highlight hours
                    tipBucketHours.requestFocus();
                    tipBucketHours.selectAll();
                    handled = true;
                }
                return handled;
            }
        });

        final EditText totalTips = (EditText) findViewById(R.id.txtBucketTipsInput);
        totalTips.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                boolean handled = false;
                if (actionId == EditorInfo.IME_ACTION_GO) {
                    //Move selection to hours
                    final TextView hoursInput = (TextView) findViewById(R.id.txtPersonsHours);
                    hoursInput.requestFocus();
                    hoursInput.postDelayed(new Runnable() {
                                               @Override
                                               public void run() {
                                                   InputMethodManager keyboard = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                                                   keyboard.showSoftInput(hoursInput, 0);
                                               }
                                           }
                            , 25);
                    handled = true;
                }
                return handled;
            }
        });
        //TODO: Configure save-able settings
    }

    @Override
    protected void onPause() {
        super.onPause();


    }

    @Override
    public void onBackPressed() {
        //Wait three seconds for another back button press before exiting
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastPress > 3000) {
            Toast.makeText(getBaseContext(), "Press back again to exit", Toast.LENGTH_LONG).show();
            lastPress = currentTime;
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onStop() {
        super.onStop();

        //Save mode status
        SharedPreferences sharedPref = getSharedPreferences(PREFS_NAME, 0);
        int sharedMode = currentMode.ordinal();
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putInt("current_mode", sharedMode);

        //Save checkbox status
        editor.putBoolean("checkbox", cbxChecked);

        editor.commit();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        //Exit normally
        System.exit(0);
    }
    //TODO Design and implement better getters and setters
    /**
     * Gets the mode that was last used.
     *
     * @return the modeType last used.
     */
    private modeType getSavedMode() {
        SharedPreferences sharedPref = getSharedPreferences(PREFS_NAME, 0);
        int sharedMode = sharedPref.getInt("current_mode", defaultMode);
        modeType intMode;
        switch (sharedMode) {
            case 0:
                intMode = modeType.INDIVIDUAL;
                break;
            case 1:
                intMode = modeType.TIPBUCKET;
                break;
            default:
                intMode = modeType.INDIVIDUAL;
        }
        return intMode;
    }
    
    /**
     * Sets the UI layout based on Tip Bucket mode. Hour functions are shown in this mode.
     * Low and High stats are hidden.
     */
    private void showTipBucketLayout() {
        clearAll();

        LinearLayout layTipBucket = (LinearLayout) findViewById(R.id.layTipBucket);
        LinearLayout layTipsAndHours = (LinearLayout) findViewById(R.id.layTipsAndHours);
        LinearLayout layPersonsHours = (LinearLayout) findViewById(R.id.layPersonsHours);
        LinearLayout layDisplay = (LinearLayout) findViewById(R.id.layDisplay);

        layTipBucket.setVisibility(View.VISIBLE);
        layPersonsHours.setVisibility(View.VISIBLE);
        layTipsAndHours.setVisibility(View.GONE);
        layDisplay.setVisibility(View.GONE);

        RadioButton radTipBucket = (RadioButton) findViewById(R.id.radTipBucket);
        radTipBucket.setChecked(true);

        showHourFunctions();
        hideLowHigh();

        currentMode = modeType.TIPBUCKET;
    }

    /**
     * Configure the UI to hide the Low and High stats.
     */
    private void hideLowHigh() {
        TextView txtLow = (TextView) findViewById(R.id.txtLow);
        TextView txtHigh = (TextView) findViewById(R.id.txtHigh);
        TextView txtLowOutput = (TextView) findViewById(R.id.txtLowOutput);
        TextView txtHighOUtput = (TextView) findViewById(R.id.txtHighOutput);

        txtLow.setTextColor(Color.GRAY);
        txtHigh.setTextColor(Color.GRAY);
        txtLowOutput.setTextColor(Color.GRAY);
        txtHighOUtput.setTextColor(Color.GRAY);
    }

    /**
     * Configure the UI to show the Low and High stats.
     */
    private void showLowHigh() {
        TextView txtLow = (TextView) findViewById(R.id.txtLow);
        TextView txtHigh = (TextView) findViewById(R.id.txtHigh);
        TextView txtLowOutput = (TextView) findViewById(R.id.txtLowOutput);
        TextView txtHighOUtput = (TextView) findViewById(R.id.txtHighOutput);

        txtLow.setTextColor(Color.rgb(187, 187, 187));
        txtHigh.setTextColor(Color.rgb(187, 187, 187));
        txtLowOutput.setTextColor(Color.rgb(187, 187, 187));
        txtHighOUtput.setTextColor(Color.rgb(187, 187, 187));
    }

    /**
     * Sets the UI layout based on Individual mode. Hour functions are hidden in this mode.
     * Low and High stats are shown.
     */
    private void showIndividualLayout() {
        clearAll();

        LinearLayout layTipBucket = (LinearLayout) findViewById(R.id.layTipBucket);
        LinearLayout layTipsAndHours = (LinearLayout) findViewById(R.id.layTipsAndHours);
        LinearLayout layPersonsHours = (LinearLayout) findViewById(R.id.layPersonsHours);
        LinearLayout layDisplay = (LinearLayout) findViewById(R.id.layDisplay);

        layTipBucket.setVisibility(View.GONE);
        layPersonsHours.setVisibility(View.GONE);
        layTipsAndHours.setVisibility(View.VISIBLE);
        layDisplay.setVisibility(View.VISIBLE);

        RadioButton radIndividually = (RadioButton) findViewById(R.id.radIndividually);

        showLowHigh();

        if (!cbxChecked) {
            hideHourFunctions();
        }

        radIndividually.setChecked(true);

        currentMode = modeType.INDIVIDUAL;
    }

    /**
     * Assigns the appropriate checked box based on current mode. Shows or Hides appropriate stats based on
     * what the current mode is set to.
     */
    private void checkBoxLayout() {
        if (cbxHours.isChecked()) {
            //Dialog box to add default hours value or clear all fields
            clearAll();
            showHourFunctions();
            cbxChecked = true;
        } else if (currentMode == modeType.TIPBUCKET) {
            clearAll();
            showHourFunctions();
            cbxChecked = true;
        } else {
            cbxChecked = false;
            hideHourFunctions();
            clearAll();
        }
    }

    /**
     * Add a new entry for Tip Bucket mode.
     */
    private void createEntryTipBucket() {
        EditText totalTipsInput = (EditText) findViewById(R.id.txtBucketTipsInput);
        EditText hoursText = (EditText) findViewById(R.id.txtPersonsHours);
        hoursText.setHighlightColor(1719716804);
        String hoursValue = hoursText.getText().toString();
        String totalTipsValue = totalTipsInput.getText().toString();

        //Don't allow empty value
        if (hoursValue.equals("")) {
            hoursValue = "0";
            hoursText.append("0");
        }

        //Prevent empty value
        if (totalTipsValue.equals("")) {
            totalTipsValue = "0";
        }

        //Make hours selected if there is a value already in total tips
        if (Integer.parseInt(totalTipsValue) > 0) {
            hoursText.requestFocus();
            getEntryTipBucket();
            clearFields();
        }
    }

    /**
     * Add a new entry for Individual mode.
     */
    private void createEntryIndividual() {
        EditText tipText = (EditText) findViewById(R.id.txtTipsInput);
        final EditText hoursText = (EditText) findViewById(R.id.txtHoursInput);
        hoursText.setHighlightColor(1719716804);
        boolean zeroHour = false;
        String hoursValue = hoursText.getText().toString();

        //Prevent blanks
        if (hoursValue.equals("")) {
            hoursValue = "0";
            hoursText.append("0");
        }

        if (tipText.getText().length() > 0) {
            if (cbxChecked) {
                //Make sure hours is not 0
                if (Double.parseDouble(hoursValue) > 0) {
                    getEntryIndividual();
                    checkTotal();
                    clearFields();
                } else {
                    //Highlight value and set the color to red to notify invalid input
                    hoursText.setHighlightColor(Color.RED);
                    hoursText.requestFocus();
                    hoursText.selectAll();
                    zeroHour = true;
                }
            } else {
                getEntryIndividual();
                checkTotal();
                clearFields();
            }
        }
        //Make txtTipsInput selected
        if (!zeroHour) {
            tipText.requestFocus();
        }
    }

    /**
     * Opens the activity that contains "about" information.
     */
    private void openAboutActivity() {
        Intent aboutIntent = new Intent(this, AboutActivity.class);
        //Open activity
        startActivityForResult(aboutIntent, 1);
    }

    /**
     * Configures the layout to hide hourly stats.
     */
    private void hideHourFunctions() {
        TextView txtHoursInput = (TextView) findViewById(R.id.txtHoursInput);
        TextView txtHoursLabel = (TextView) findViewById(R.id.txtHoursLabel);
        TextView txtTotalHours = (TextView) findViewById(R.id.txtTotalHours);
        TextView txtTotalHoursOutput = (TextView) findViewById(R.id.txtTotalHoursOutput);
        TextView txtHourly = (TextView) findViewById(R.id.txtHourly);
        TextView txtHourlyOutput = (TextView) findViewById(R.id.txtHourlyOutput);

        txtHoursInput.setVisibility(View.GONE);
        txtHoursLabel.setVisibility(View.GONE);
        txtTotalHours.setTextColor(Color.GRAY);
        txtTotalHoursOutput.setTextColor(Color.GRAY);
        txtHourly.setTextColor(Color.GRAY);
        txtHourlyOutput.setTextColor(Color.GRAY);

        //Ensure that the appropriate checkbox is checked
        cbxHours.setChecked(false);
    }

    /**
     * Configures teh layout to show hourly stats.
     */
    private void showHourFunctions() {
        TextView txtHoursInput = (TextView) findViewById(R.id.txtHoursInput);
        TextView txtHoursLabel = (TextView) findViewById(R.id.txtHoursLabel);
        TextView txtTotalHours = (TextView) findViewById(R.id.txtTotalHours);
        TextView txtTotalHoursOutput = (TextView) findViewById(R.id.txtTotalHoursOutput);
        TextView txtHourly = (TextView) findViewById(R.id.txtHourly);
        TextView txtHourlyOutput = (TextView) findViewById(R.id.txtHourlyOutput);

        txtHoursInput.setVisibility(View.VISIBLE);
        txtHoursLabel.setVisibility(View.VISIBLE);
        txtTotalHours.setTextColor(Color.rgb(187, 187, 187));
        txtTotalHoursOutput.setTextColor(Color.rgb(187, 187, 187));
        txtHourly.setTextColor(Color.rgb(187, 187, 187));
        txtHourlyOutput.setTextColor(Color.rgb(187, 187, 187));

        //Ensure that the appropriate checkbox is checked
        cbxHours.setChecked(true);
    }

    /**
     * Gets the entry for Individual mode.
     */
    private void getEntryIndividual() {
        EditText tipText = (EditText) findViewById(R.id.txtTipsInput);
        int tipAmount = 0; //Default value

        //Test to make sure the value isn't empty
        if (!tipText.getText().toString().equals("")) {
            tipAmount = Integer.parseInt(tipText.getText().toString());
        }

        addToLogIndividual(tipAmount);
        checkEntries();
        checkHours();
        checkHourlyIndividual();
        checkAvg();
        checkLeftOver();
        checkLow();
        checkHigh();
        logTakeHomeIndividual();
    }

    /**
     * Gets the entry for Tip Bucket mode.
     */
    private void getEntryTipBucket() {
        EditText hoursText = (EditText) findViewById(R.id.txtPersonsHours);
        double hourAmount = 0; //Default value

        //Test to make sure the value isn't empty
        if (!hoursText.getText().toString().equals("")) {
            hourAmount = Double.parseDouble(hoursText.getText().toString());
        }

        addToLogTipBucket(hourAmount);
        checkEntries();
        checkHours();
        checkHourlyTipBucket();
        checkTotalTipBucket();
        checkAvgTipBucket();
        checkLeftOverTipBucket();
        logTakeHomeTipBucket();
    }

    /**
     * Calculates the "left over" value for Tip Bucket mode.
     */
    private void checkLeftOverTipBucket() {
        int totalValue = 0;
        leftOver = 0;

        for (int i = viewArray.size(); i > 0; i--) {
            TextView hourValue = (TextView) viewArray.get(i - 1).findViewById(R.id.txtLogHours);
            double value = 0;
            if (!hourValue.getText().toString().equals("0")) {
                String valueString = hourValue.getText().toString();
                value = Double.parseDouble(valueString) * hourly;

                Log.d("RESULT: ", Double.toString(value));
            }
            totalValue += value;
        }
        if (totalValue > 1 && entries > 1) {
            leftOver = tipTotal - totalValue;
            Log.d("LEFTOVER: ", Integer.toString(leftOver));
        }
        TextView txtLeftOverOutput = (TextView) findViewById(R.id.txtLeftOverOutput);
        txtLeftOverOutput.setText(null); //Clear
        txtLeftOverOutput.append(Integer.toString(leftOver));
    }

    /**
     * Calculates the average for Tip Bucket mode.
     */
    private void checkAvgTipBucket() {
        double totalValue = tipTotal;
        double totalEntries = entries;
        double tipBucketAverage = 0.0;

        //Make sure nothing gets divided by 0
        if (totalEntries > 0 && totalValue > 0) {
            tipBucketAverage = totalValue / totalEntries;
        }

        //Using NumberFormat allows desired formatting
        NumberFormat currency = NumberFormat.getCurrencyInstance();
        String amount = currency.format(tipBucketAverage);

        TextView txtAvgOutput = (TextView) findViewById(R.id.txtAvgOutput);
        txtAvgOutput.setText(null); //Clear
        txtAvgOutput.append(amount);
    }

    /**
     * Calculates the total for Tip Bucket mode.
     */
    private void checkTotalTipBucket() {
        TextView totalTipsOutput = (TextView) findViewById(R.id.txtTotalTipsOutput);
        TextView totalTipsInput = (TextView) findViewById(R.id.txtBucketTipsInput);

        if (!totalTipsInput.getText().toString().equals("")) {
            tipTotal = Integer.parseInt(totalTipsInput.getText().toString());
        } else {
            tipTotal = 0;
        }

        totalTipsOutput.setText(null); //clear
        totalTipsOutput.append(Integer.toString(tipTotal)); //set
    }

    /**
     * Adds the entry and its details to the log for Tip Bucket mode.
     * Also inserts a functional remove button with each entry.
     * @param hourAmount the amount of hours worked for this entry
     */
    private void addToLogTipBucket(double hourAmount) {
        final LinearLayout statistics = (LinearLayout) findViewById(R.id.statistics);
        final View child = getLayoutInflater().inflate(R.layout.userlayouttipbucket, null);

        TextView txtLogHours = (TextView) child.findViewById(R.id.txtLogHours);
        txtLogHours.append(Double.toString(hourAmount));

        TextView txtHoursLabel = (TextView) child.findViewById(R.id.txtHoursLabel);
        txtHoursLabel.setVisibility(View.VISIBLE);


        viewArray.add(child);
        final View entry = viewArray.get(viewArray.size() - 1);
        statistics.addView(entry);

        //Remove button next to each entry
        entry.findViewById(R.id.btnLogRemove).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                viewArray.remove(child);
                statistics.removeView(entry);
                checkEntries();
                checkHours();
                checkHourlyTipBucket();
                checkTotalTipBucket();
                checkAvgTipBucket();
                checkLeftOverTipBucket();
                logTakeHomeTipBucket();
            }
        });
    }

    /**
     * Calculates the hourly value for Tip Bucket mode.
     */
    private void checkHourlyTipBucket() {
        double totalTips = 0;
        double totalHours = 0;
        TextView totalTipsValue = (TextView) findViewById(R.id.txtBucketTipsInput);

        //Avoid empty values
        if (!totalTipsValue.getText().toString().equals("")) {
            totalTips = Double.parseDouble(totalTipsValue.getText().toString());
        }
        
        for (int i = viewArray.size(); i > 0; i--) {
            TextView hoursValue = (TextView) viewArray.get(i - 1).findViewById(R.id.txtLogHours);

            double hours = Double.parseDouble(hoursValue.getText().toString());
            totalHours += hours;
        }
        
        if (totalTips > 0 && totalHours > 0) {
            hourly = (totalTips / totalHours);
        } else {
            hourly = 0.00;
        }

        DecimalFormat formattedOutput = new DecimalFormat(".####");
        String output = formattedOutput.format(hourly);

        TextView txtHourlyOutput = (TextView) findViewById(R.id.txtHourlyOutput);
        txtHourlyOutput.setText(null); //Clear
        txtHourlyOutput.append(output);
    }

    /**
     * Calculates the take home value for Tip Bucket mode.
     */
    private void logTakeHomeTipBucket() {
        for (int i = viewArray.size(); i > 0; i--) {
            double hours = 0;
            TextView txtLogHours = (TextView) viewArray.get(i - 1).findViewById(R.id.txtLogHours);
            TextView txtTakeHome = (TextView) viewArray.get(i - 1).findViewById(R.id.txtTakeHome);

            if (!txtLogHours.getText().toString().equals("")) {
                hours = Double.parseDouble(txtLogHours.getText().toString());
            }

            NumberFormat currency = NumberFormat.getCurrencyInstance();
            currency.setRoundingMode(RoundingMode.DOWN);

            double takeHome = hourly * hours;
            String amount = currency.format(takeHome);

            txtTakeHome.setText(null);
            txtTakeHome.append(amount);
        }
    }

    /**
     * Calculates the total hours of all entries.
     */
    private void checkHours() {
        if (cbxChecked) {
            hoursTotal = 0;

            for (int i = viewArray.size(); i > 0; i--) {
                TextView hoursValue = (TextView) viewArray.get(i - 1).findViewById(R.id.txtLogHours);
                double value = 0;
                if (!hoursValue.getText().toString().equals("")) {
                    value = Double.parseDouble(hoursValue.getText().toString());
                }
                hoursTotal += value;
            }

            TextView totalHoursOutput = (TextView) findViewById(R.id.txtTotalHoursOutput);
            totalHoursOutput.setText(null); //clear
            totalHoursOutput.append(Double.toString(hoursTotal)); //set
        }
    }

    /**
     * Calculates the total amount for all entries.
     */
    private void checkTotal() {
        tipTotal = 0;

        for (int i = viewArray.size(); i > 0; i--) {
            TextView tipValue = (TextView) viewArray.get(i - 1).findViewById(R.id.txtLogTips);
            int value = 0;
            if (!tipValue.getText().toString().equals("")) {
                value = Integer.parseInt(tipValue.getText().toString());
            }
            tipTotal += value;
        }

        TextView totalTipsOutput = (TextView) findViewById(R.id.txtTotalTipsOutput);
        totalTipsOutput.setText(null); //clear
        totalTipsOutput.append(Integer.toString(tipTotal)); //set
    }

    /**
     * Adds the entry to the log for Individual mode.
     * @param tips the input amount associated with the tip value
     */
    private void addToLogIndividual(int tips) {
        String tipsString = Integer.toString(tips);
        final LinearLayout statistics = (LinearLayout) findViewById(R.id.statistics);
        final View child = getLayoutInflater().inflate(R.layout.userlayout, null);

        TextView txtLogTips = (TextView) child.findViewById(R.id.txtLogTips);
        txtLogTips.append(tipsString);

        //Determine if the user wants to use hourly values or not
        if (cbxChecked) {
            EditText hoursText = (EditText) findViewById(R.id.txtHoursInput);
            double hoursAmount = 0;
            if (!hoursText.getText().toString().equals("")) {
                hoursAmount = Double.parseDouble(hoursText.getText().toString());
            }

            TextView txtLogHours = (TextView) child.findViewById(R.id.txtLogHours);
            txtLogHours.append(Double.toString(hoursAmount));

            TextView txtHoursLabel = (TextView) child.findViewById(R.id.txtHoursLabel);
            txtHoursLabel.setVisibility(View.VISIBLE);
        } else {
            TextView txtHoursLabel = (TextView) child.findViewById(R.id.txtHoursLabel);
            txtHoursLabel.setVisibility(View.INVISIBLE);
        }

        viewArray.add(child);
        final View entry = viewArray.get(viewArray.size() - 1);
        statistics.addView(entry);

        //Remove button
        entry.findViewById(R.id.btnLogRemove).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                viewArray.remove(child);
                statistics.removeView(entry);
                checkTotal();
                checkHours();
                checkHourlyIndividual();
                checkAvg();
                checkLeftOver();
                checkLow();
                checkHigh();
                checkEntries();
                logTakeHomeIndividual();
            }
        });
    }

    /**
     * Clear all entries
     */
    private void clearAll() {
        LinearLayout statistics = (LinearLayout) findViewById(R.id.statistics);
        statistics.removeAllViews();
        viewArray.clear();

        //Make all stats 0
        resetLogValues();
    }

    /**
     * Sets all stats to 0 and reflects the values with each layout view.
     */
    private void resetLogValues() {
        tipTotal = 0;
        entries = 0;
//        highValue = 0;
        average = 0;
        hoursTotal = 0.00;
        hourly = 0.00;
        leftOver = 0;
        low = 0;

        TextView txtHighOutput = (TextView) findViewById(R.id.txtHighOutput);
        TextView txtEntriesOutput = (TextView) findViewById(R.id.txtEntriesOutput);
        TextView totalTipsOutput = (TextView) findViewById(R.id.txtTotalTipsOutput);
        TextView txtLeftOverOutput = (TextView) findViewById(R.id.txtLeftOverOutput);
        TextView txtLowOutput = (TextView) findViewById(R.id.txtLowOutput);
        TextView txtAvgOutput = (TextView) findViewById(R.id.txtAvgOutput);
        TextView txtTotalHoursOutput = (TextView) findViewById(R.id.txtTotalHoursOutput);
        TextView txtHourlyOutput = (TextView) findViewById(R.id.txtHourlyOutput);

        txtHighOutput.setText("0");
        txtEntriesOutput.setText("0");
        totalTipsOutput.setText("0");
        txtLeftOverOutput.setText("0");
        txtLowOutput.setText("0");
        txtAvgOutput.setText("0");
        txtTotalHoursOutput.setText("0.00");
        txtHourlyOutput.setText("0.00");
    }

    /**
     * Calculates the number of entries.
     */
    private void checkEntries() {
        int countEntries = 0;

        for (int i = viewArray.size(); i > 0; i--) {
            countEntries++;
        }
        entries = countEntries;

        TextView txtEntriesOutput = (TextView) findViewById(R.id.txtEntriesOutput);
        txtEntriesOutput.setText(null); //Clear
        txtEntriesOutput.append(Integer.toString(countEntries));
        entries = countEntries;
    }

    /**
     * Calculates the highest value.
     */
    private void checkHigh() {
        int highestValue = 0;

        for (int i = viewArray.size(); i > 0; i--) {
            TextView tipValue = (TextView) viewArray.get(i - 1).findViewById(R.id.txtLogTips);
            int value = 0;
            if (!tipValue.getText().toString().equals("")) {
                value = Integer.parseInt(tipValue.getText().toString());
            }
            if (value > highestValue) {
                highestValue = value;
            }
        }

        TextView txtHighOutput = (TextView) findViewById(R.id.txtHighOutput);
        txtHighOutput.setText(null); //clear
        txtHighOutput.append(Integer.toString(highestValue)); //set
//        highValue = highestValue;
    }

    /**
     * Calculates the "left over" value. The whole dollar amount remaining when change isn't distributed.
     * (Individual mode)
     */
    private void checkLeftOver() {
        int totalValue = 0;
        int totalEntries = 0;
        leftOver = 0;

        for (int i = viewArray.size(); i > 0; i--) {
            TextView tipValue = (TextView) viewArray.get(i - 1).findViewById(R.id.txtLogTips);
            int value = 0;
            if (!tipValue.getText().toString().equals("")) {
                value = Integer.parseInt(tipValue.getText().toString());
            }
            totalValue += value;
            totalEntries++;
        }
        if (totalValue > 1 && totalEntries > 1) {
            leftOver = totalValue % totalEntries;
        }

        TextView txtLeftOverOutput = (TextView) findViewById(R.id.txtLeftOverOutput);
        txtLeftOverOutput.setText(null); //Clear
        txtLeftOverOutput.append(Integer.toString(leftOver));
    }

    /**
     * Calculate the lowest value.
     */
    private void checkLow() {
        if (viewArray.size() > 0) {
            TextView firstValue = (TextView) viewArray.get(0).findViewById(R.id.txtLogTips);
            low = Integer.parseInt(firstValue.getText().toString());
        } else if (viewArray.size() == 0) {
            low = 0;
        }

        for (int i = viewArray.size(); i > 0; i--) {
            TextView tipValue = (TextView) viewArray.get(i - 1).findViewById(R.id.txtLogTips);
            int value = 0;
            if (!tipValue.getText().toString().equals("")) {
                value = Integer.parseInt(tipValue.getText().toString());
            }
            if (value < low) {
                low = value;
            }
        }

        TextView txtLowOutput = (TextView) findViewById(R.id.txtLowOutput);
        txtLowOutput.setText(null); //clear
        txtLowOutput.append(Integer.toString(low)); //set
    }

    /**
     * Calculate the average value. (Individual mode)
     */
    private void checkAvg() {
        //TODO: Make functions for each calculation separately for cleaner code. Return values.
        int totalValue = 0;
        int totalEntries = 0;
        average = 0;

        for (int i = viewArray.size(); i > 0; i--) {
            TextView tipValue = (TextView) viewArray.get(i - 1).findViewById(R.id.txtLogTips);
            int tips = 0;
            if (!tipValue.getText().toString().equals("")) {
                tips = Integer.parseInt(tipValue.getText().toString());
            }
            totalValue += tips;
            totalEntries++;
        }

        if (totalEntries > 0 && totalValue > 0) {
            average = totalValue / totalEntries;
        }

        TextView txtAvgOutput = (TextView) findViewById(R.id.txtAvgOutput);
        txtAvgOutput.setText(null); //Clear
        txtAvgOutput.append(Integer.toString(average));
    }

    /**
     * Calculate the hourly value for Individual mode.
     */
    private void checkHourlyIndividual() {
        if (cbxChecked) {
            double totalTips = 0;
            double totalHours = 0;

            for (int i = viewArray.size(); i > 0; i--) {
                TextView tipValue = (TextView) viewArray.get(i - 1).findViewById(R.id.txtLogTips);
                TextView hoursValue = (TextView) viewArray.get(i - 1).findViewById(R.id.txtLogHours);
                double tips = 0;
                if (!tipValue.getText().toString().equals("")) {
                    tips = Integer.parseInt(tipValue.getText().toString());
                }
                double hours = 0;
                if (!hoursValue.getText().toString().equals("")) {
                    hours = Double.parseDouble(hoursValue.getText().toString());
                }
                totalTips += tips;
                totalHours += hours;
            }
            if (totalTips > 0 && totalHours > 0) {
                hourly = (totalTips / totalHours);
            } else {
                hourly = 0.00;
            }

            DecimalFormat formattedOutput = new DecimalFormat(".####");
            String output = formattedOutput.format(hourly);

            TextView txtHourlyOutput = (TextView) findViewById(R.id.txtHourlyOutput);
            txtHourlyOutput.setText(null); //Clear
            txtHourlyOutput.append(output);
        }
    }

    /**
     * Calculate the "take home" value. The amount an individual will receive after tips are distributed evenly.
     * (Individual mode)
     */
    private void logTakeHomeIndividual() {
        double takeHome = 0;

        if (cbxChecked) {
            for (int i = viewArray.size(); i > 0; i--) {
                TextView txtLogHours = (TextView) viewArray.get(i - 1).findViewById(R.id.txtLogHours);
                TextView txtTakeHome = (TextView) viewArray.get(i - 1).findViewById(R.id.txtTakeHome);

                double hours = 0;
                if (!txtLogHours.getText().toString().equals("")) {
                    hours = Double.parseDouble(txtLogHours.getText().toString());
                }

                takeHome = hourly * hours;

                NumberFormat currency = NumberFormat.getCurrencyInstance();
                currency.setRoundingMode(RoundingMode.DOWN);
                String amount = currency.format(takeHome);

                txtTakeHome.setText(null);
                txtTakeHome.append(amount);
            }
        } else {
            for (int i = viewArray.size(); i > 0; i--) {
                TextView txtTakeHome = (TextView) viewArray.get(i - 1).findViewById(R.id.txtTakeHome);

                NumberFormat currency = NumberFormat.getCurrencyInstance();
                currency.setRoundingMode(RoundingMode.UNNECESSARY);

                //TODO: Make these functions proper getters
                checkEntries();
                checkTotal();

                takeHome = tipTotal / entries;

                String amount = currency.format(takeHome);

                txtTakeHome.setText(null);
                txtTakeHome.append(amount);
            }
        }
    }

    /**
     * Clears the input fields.
     */
    private void clearFields() {
        EditText tipText = (EditText) findViewById(R.id.txtTipsInput);
        tipText.getText().clear();
    }



}