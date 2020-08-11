package com.example.e_haushaltsbuch;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.e_haushaltsbuch.backend.AppDatabase;
import com.example.e_haushaltsbuch.backend.DateRangePicker;
import com.example.e_haushaltsbuch.backend.FinanceItem;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.Hashtable;
import java.util.List;

// https://github.com/lecho/hellocharts-android/blob/master/hellocharts-samples/src/lecho/lib/hellocharts/samples
import lecho.lib.hellocharts.listener.ColumnChartOnValueSelectListener;
import lecho.lib.hellocharts.model.Axis;
import lecho.lib.hellocharts.model.AxisValue;
import lecho.lib.hellocharts.model.Column;
import lecho.lib.hellocharts.model.ColumnChartData;
import lecho.lib.hellocharts.model.SubcolumnValue;
import lecho.lib.hellocharts.view.ColumnChartView;

// https://github.com/psinetron/slycalendarview
import ru.slybeaver.slycalendarview.SlyCalendarDialog;

/**
 * A simple {@link android.app.Fragment} subclass.
 */
public class StatisticFragment extends DialogFragment implements SlyCalendarDialog.Callback {

  private ArrayList<FinanceItem> allTransactions;
  private AppDatabase financeDB;
  private Hashtable<String, Boolean> category;  // stores categories with their status Soll or haben
  private Hashtable<String, Double> filterCategorySums;  // sums of finance items for all
  // categories in filter
  private View view;
  private StatisticFragment statisticFragment;
  private LinearLayout emptyStateScreen;
  private LinearLayout filledStateScreen;

  private ColumnChartView chart;
  private ColumnChartData data;
  private Button rangeButton;
  private ImageView backButton;
  private Calendar fromDate;
  private Calendar toDate;
  private ImageView filterIcon;
  private boolean[] selectedCategories;
  private String[] categories;
  private List<Column> values;
  private List<AxisValue> axisValues;
  private int axisSize;

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
                           final Bundle savedInstanceState) {
    if (view == null) {
      // init view
      view = inflater.inflate(R.layout.fragment_statistic, container, false);
      statisticFragment = this;
    }
    if (filterCategorySums == null) {
      filterCategorySums = new Hashtable<>(); // init value table if necessary
    }

    initCategories();
    initDB();
    initRangeButton();
    initBackButton();

    if (filterIcon == null) {
      initFilter();
    }

    createChart();
    setChartValues();
    handleEmptyState();
    return view;
  }

  private void handleEmptyState() {
    emptyStateScreen = view.findViewById(R.id.emptyStateScreen);
    filledStateScreen = view.findViewById(R.id.filledStateScreen);
    if (allTransactions.size() > 0) {
      emptyStateScreen.setVisibility(View.GONE);
      filledStateScreen.setVisibility(View.VISIBLE);
    } else {
      emptyStateScreen.setVisibility(View.VISIBLE);
      filledStateScreen.setVisibility(View.GONE);
    }
  }

  private void initBackButton() {
    if (backButton == null) {
      backButton = view.findViewById(R.id.backButton);
      backButton.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View view) {
          resetFilter();
          refreshChart();  // update chart with new filters
        }
      });
    }
  }

  private void createChart() {
    chart = view.findViewById(R.id.chart);
    chart.setOnValueTouchListener(new ColumnChartOnValueSelectListener() {
      @Override
      public void onValueSelected(int columnIndex, int subcolumnIndex, SubcolumnValue value) {
        if (filterCategorySums.size() != 0) {
          return;
        }

        // listener for clicks into diagram slices for Einnahmen and Ausgaben
        resetFilter();
        Enumeration<String> categoryNames = category.keys();
        while (categoryNames.hasMoreElements()) {
          String categoryName = categoryNames.nextElement();
          if (columnIndex == 0 && category.get(categoryName)) {
            activateFilter(categoryName);  // adding filter for Haben category
            TextView headline = view.findViewById(R.id.statisticTitle);
            headline.setText(R.string.statPlus);
          } else if (columnIndex == 1 && !category.get(categoryName)) {
            TextView headline = view.findViewById(R.id.statisticTitle);
            headline.setText(R.string.statMinus);
            activateFilter(categoryName);  // adding filter for Soll category
          }
        }
        refreshChart();  // update chart with new filters
      }

      @Override
      public void onValueDeselected() {
      }

    });
  }

  private void initRangeButton() {
    if (rangeButton == null) {

      // init toButton if necessary with current date
      rangeButton = view.findViewById(R.id.rangeButton);
      String s = getString(R.string.slycalendar_save);
      rangeButton.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View view) {
          DateRangePicker dateRangePicker = new DateRangePicker();
          dateRangePicker.setSelectedColor(getResources().getColor(R.color.colorPrimary));
          dateRangePicker.setHeaderColor(getResources().getColor(R.color.colorPrimary));
          dateRangePicker.setSingle(false);
          dateRangePicker.setCallback(statisticFragment);
          dateRangePicker.setStartDate(fromDate.getTime());
          dateRangePicker.setEndDate(toDate.getTime());
          dateRangePicker.show(getFragmentManager(), "TAG_SLYCALENDAR");
        }
      });

      toDate = new GregorianCalendar();  // save end date
      fromDate = new GregorianCalendar();  // save start date
      fromDate.add(Calendar.MONTH, -getResources().getInteger(R.integer.statFilterMonthBefore));
      setRangeButtonText();
    }
  }

  private void initCategories() {
    if (category == null) {
      category = new Hashtable<>(); // init category if necessary with static spinner array
      String[] labels = getResources().getStringArray(R.array.spinnerArray);
      for (int i = 0; i < labels.length - 1; i++) {
        if (i < 4) {
          category.put(labels[i], false);
        } else if (i < 6) {
          category.put(labels[i], true);
        } else if (i == 6) {
          category.put(labels[i], false);
        }
      }
    }
  }

  /**
   * Open the database and creats test data for statistics.
   */
  private void initDB() {
    financeDB = new AppDatabase(getContext(), "finances.db");
    financeDB.open();
  }

  /**
   * calculates chart slices and put them into chart.
   */
  public void setChartValues() {
    data = new ColumnChartData();
    allTransactions = financeDB.getAllItems();
    values = new ArrayList<Column>();
    axisValues = new ArrayList<>();
    axisSize = getResources().getInteger(R.integer.statAxisSizeSmall);

    if (filterCategorySums.size() == 0) {
      // no filter active --> summing up Soll and Haben
      sumUpAllTransactions();

    } else {
      // filter is active --> calculate filter values in valueTable
      // reset old sums
      Enumeration<String> keys = filterCategorySums.keys();
      while (keys.hasMoreElements()) {
        String key = keys.nextElement();
        filterCategorySums.put(key, 0.0);
      }

      calculateSum();
      createColumns();

      backButton.setVisibility(Button.VISIBLE);
    }

    // set data and configure chart
    data = new ColumnChartData(values);
    Axis axisX = new Axis(axisValues);
    axisX.setTextColor(R.color.black);
    axisX.setTextSize(axisSize);
    data.setAxisXBottom(axisX);
    chart.setColumnChartData(data);
  }

  private void createColumns() {
    String[] labels = getResources().getStringArray(R.array.spinnerArray);
    int axisIndex = 0;
    for (int i = 0; i < labels.length - 1; i++) {
      String key = labels[i];
      if (filterCategorySums.get(key) != null) {
        Double val = Math.ceil(filterCategorySums.get(key));
        int rgb = 0;
        if (category.get(key) == false) {
          rgb = Color.rgb(229, 57, 53);
        } else {
          rgb = Color.rgb(67, 160, 71);
        }
        if (i == labels.length - 1) {  // Sonstiges
          if (val >= 0) {
            rgb = Color.rgb(67, 160, 71);
          } else {
            rgb = Color.rgb(229, 57, 53);
          }
        }
        List<SubcolumnValue> colValue = new ArrayList<SubcolumnValue>();
        val = Math.abs(val);
        SubcolumnValue value = new SubcolumnValue(val.intValue(), rgb);
        String valString = "" + val;
        value.setLabel("" + valString + "€");
        colValue.add(value);
        Column sv = new Column(colValue);
        sv.setHasLabels(true);
        values.add(sv);
        AxisValue axisValue = new AxisValue(axisIndex);
        if (filterCategorySums.size() > getResources().getInteger(R.integer.statShortenSize)
                && key.length() > getResources().getInteger(R.integer.statShortenSize) + 2) {
          axisValue.setLabel(key.substring(0, getResources()
                  .getInteger(R.integer.statShortenSize) + 2) + ".");
        } else {
          axisValue.setLabel(key);
        }
        axisValues.add(axisValue);
        axisIndex++;
      }
    }
  }

  private void calculateSum() {
    for (int i = 0; i < allTransactions.size(); i++) {
      FinanceItem financeItem = allTransactions.get(i);
      if (isInDays(financeItem.getDate())) {
        if (isActivateFilter(financeItem.getCategory())) {
          Double sum = filterCategorySums.get(financeItem.getCategory());
          if (financeItem.getStatus()) {
            sum += Double.parseDouble(financeItem.getValue());
          } else {
            sum -= Double.parseDouble(financeItem.getValue());
          }
          filterCategorySums.put(financeItem.getCategory(), sum);
        }
      }
    }
  }

  private void sumUpAllTransactions() {
    Double plus = 0.0;
    Double minus = 0.0;

    TextView headline = view.findViewById(R.id.statisticTitle);
    headline.setText("");

    for (int i = 0; i < allTransactions.size(); i++) {
      FinanceItem financeItem = allTransactions.get(i);
      category.put(financeItem.getCategory(), financeItem.getStatus());
      if (isInDays(financeItem.getDate())) {
        double val = Double.parseDouble(financeItem.getValue());
        if (financeItem.getStatus()) {
          plus += val;
        } else {
          minus += val;
        }
      }
    }

    List<SubcolumnValue> colValue = new ArrayList<SubcolumnValue>();
    SubcolumnValue value = new SubcolumnValue(plus.intValue(), Color.rgb(67,160,71));
    String plusString = "" + plus;
    value.setLabel(plusString + "€");
    colValue.add(value);
    Column sv = new Column(colValue);
    sv.setHasLabels(true);
    values.add(sv);
    AxisValue axisValue = new AxisValue(0);
    axisValue.setLabel(getResources().getString(R.string.statPlus));
    axisValues.add(axisValue);

    colValue = new ArrayList<SubcolumnValue>();
    value = new SubcolumnValue(minus.intValue(), Color.rgb(229,57,53));
    String minusString = "" + minus;
    value.setLabel(minus + "€");
    colValue.add(value);
    sv = new Column(colValue);
    sv.setHasLabels(true);
    values.add(sv);
    axisValue = new AxisValue(1);
    axisValue.setLabel(getResources().getString(R.string.statMinus));
    axisValues.add(axisValue);
    axisSize = getResources().getInteger(R.integer.statAxisSizeLarge);

    backButton.setVisibility(Button.INVISIBLE);
  }

  /**
   * compares whether date is equal oder between from and to date.
   *
   * @param date date to check
   * @return
   */
  private boolean isInDays(String date) {
    try {
      GregorianCalendar dateCal = getCalendar(date);
      return dateCal.compareTo(fromDate) >= 0 && dateCal.compareTo(toDate) <= 0;
    } catch (Exception e) {
      return false;
    }
  }

  /**
   * creates calendar out of string date.
   *
   * @param date date in string to transform
   * @return
   */
  private GregorianCalendar getCalendar(String date) {
    String[] dayMonthYear = date.split("\\.");
    GregorianCalendar res = new GregorianCalendar(Integer.parseInt(dayMonthYear[2]),
            Integer.parseInt(dayMonthYear[1]) - 1, Integer.parseInt(dayMonthYear[0]));
    return res;
  }

  /**
   * repaints chart after changing filter.
   */
  public void refreshChart() {
    setRangeButtonText();
    setChartValues();
  }

  /**
   * calculats filter labels for external filter dialog --> creating an array out of enumeration.
   *
   * @return
   */
  public String[] getFilterLabels() {
    String[] labels = getResources().getStringArray(R.array.spinnerArray);
    String[] res = new String[labels.length - 1];
    for (int i = 0; i < res.length; i++) {
      res[i] = labels[i];
    }
    return res;
  }

  /**
   * external activation of filter category by filter dialog by setting starting value 0 for.
   * category
   *
   * @param categoryName name of category to activate
   */
  public void activateFilter(String categoryName) {
    filterCategorySums.put(categoryName, 0.0);
  }

  /**
   * external reset of filter values by filter dialog --> remove starting values for all categories.
   */
  public void resetFilter() {
    filterCategorySums = new Hashtable<>();
  }

  /**
   * external activation of filter category by filter dialog by setting starting value 0 for
   * category.
   *
   * @param categoryName name of category to check
   * @return
   */
  public boolean isActivateFilter(String categoryName) {
    return filterCategorySums.get(categoryName) != null;
  }

  /**
   * Creates text of range button.
   */
  private void setRangeButtonText() {
    String rangeButtonText = ""
            + fromDate.get(Calendar.DAY_OF_MONTH) + "." + (fromDate.get(Calendar.MONTH) + 1) + "."
            + fromDate.get(Calendar.YEAR) + "   -    "
            + toDate.get(Calendar.DAY_OF_MONTH) + "." + (toDate.get(Calendar.MONTH) + 1) + "."
            + toDate.get(Calendar.YEAR); // month represented by 0 .. 11
    rangeButton.setText(rangeButtonText); // to date stored as button text
  }

  @Override
  public void onDataSelected(Calendar firstDate, Calendar secondDate, int hours, int minutes) {
    fromDate = firstDate;
    toDate = secondDate;
    if (toDate == null) {
      toDate = firstDate;
    }
    refreshChart();
  }

  private void initFilter() {
    // categories = view.getResources().getStringArray(R.array.spinnerArray);

    //https://www.youtube.com/watch?v=DDwQCtccia8
    final ImageView filterIcon = (ImageView) view.findViewById(R.id.filterIcon);
    filterIcon.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        categories = getFilterLabels();
        selectedCategories = new boolean[categories.length];
        for (int i = 0; i < categories.length; i++) {
          if (statisticFragment.isActivateFilter(categories[i])) {
            selectedCategories[i] = true;
          }
        }
        AlertDialog.Builder categoryFilterDialogBuilder = new AlertDialog.Builder(getContext());
        categoryFilterDialogBuilder.setCancelable(true);
        categoryFilterDialogBuilder.setTitle("Kategorie auswählen:");
        categoryFilterDialogBuilder.setMultiChoiceItems(categories, selectedCategories, new DialogInterface.OnMultiChoiceClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int which, boolean isChecked) {
            //set selected items to true or false
            selectedCategories[which] = isChecked;
          }
        });
        categoryFilterDialogBuilder.setPositiveButton("Bestätigen", new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int which) {
            // Was passiert wenn Dialog geschlossen wird und Auswahl getroffen wurde
            activateFilteredItems();
            dialog.dismiss();
            refreshChart();
          }
        });
        categoryFilterDialogBuilder.setNegativeButton("Alles anzeigen", new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int which) {
            for (int i = 0; i < categories.length; i++) {
                selectedCategories[i] = true;
            }
            activateFilteredItems();
            dialog.dismiss();
            refreshChart();
          }
        });
        AlertDialog categoryFilterDialog = categoryFilterDialogBuilder.create();
        categoryFilterDialog.setCanceledOnTouchOutside(true);
        categoryFilterDialog.show();
      }
    });
  }

  private void activateFilteredItems() {
    resetFilter();
    for (int i = 0; i < selectedCategories.length; i++) {
      if (selectedCategories[i]) {
        activateFilter(categories[i]);
      }
    }
  }


  @Override
  public void onCancelled() {
    //Nothing
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    financeDB.close();
  }


}
