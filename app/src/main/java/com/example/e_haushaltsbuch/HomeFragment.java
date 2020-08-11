package com.example.e_haushaltsbuch;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.annotation.Nullable;

import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TableLayout;
import android.widget.TextView;

import com.example.e_haushaltsbuch.backend.AppDatabase;
import com.example.e_haushaltsbuch.backend.FinanceDatabaseAdapter;
import com.example.e_haushaltsbuch.backend.FinanceItem;
import com.google.gson.Gson;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Locale;

public class HomeFragment extends Fragment {

  LinearLayout emptyStateScreen;
  TableLayout filledStateScreen;

  TextView overview;
  EditText monthAndYearInput;
  TextView saldoThisMonth;
  String saldoThisMonthString;
  TextView incomeThisMonth;
  String incomeThisMonthString;
  TextView expenditureThisMonth;
  String expenditureThisMonthString;

  Calendar calendar = Calendar.getInstance();
  int currentYear = calendar.get(Calendar.YEAR);
  int currentMonth = calendar.get(Calendar.MONTH);
  int currentDay = calendar.get(Calendar.DAY_OF_MONTH);

  View view;

  ArrayList<FinanceItem> allTransactions;
  AppDatabase database;
  FinanceDatabaseAdapter transactionsAdapter;
  ListView list;

  @Nullable
  @Override
  public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {

    view = inflater.inflate(R.layout.fragment_home, container, false);

    initList();
    initDB();
    refreshArrayList(Calendar.MONTH, Calendar.YEAR);
    setupUI();
    initCockpit();

    return view;
  }

  private void initDB() {
    database = new AppDatabase(getContext(), "finances.db");
    database.open();
  }

  private void setupUI() {
    emptyStateScreen = view.findViewById(R.id.emptyStateScreen);
    filledStateScreen = view.findViewById(R.id.filledStateScreen);
    saldoThisMonth = view.findViewById(R.id.saldoThisMonth);
    incomeThisMonth = view.findViewById(R.id.incomeThisMonth);
    expenditureThisMonth = view.findViewById(R.id.expenditureThisMonth);
    overview = view.findViewById(R.id.overview);
    monthAndYearInput = view.findViewById(R.id.monthAndYearInput);

    View footerView = ((LayoutInflater) getContext()
        .getSystemService(Context.LAYOUT_INFLATER_SERVICE))
        .inflate(R.layout.list_footer, null, false);
    list.addFooterView(footerView, null, false);
  }

  private void initCockpit() {
    setMonthAndYearOnTextView(currentMonth, currentYear);

    if (databaseEmpty()) {
      emptyStateScreen.setVisibility(View.VISIBLE);
      filledStateScreen.setVisibility(View.GONE);
      incomeThisMonth.setText("-.--€");
      expenditureThisMonth.setText("-.--€");
      //saldoThisMonth.setText("-.--€");
      saldoThisMonth.setText(getResources().getString(R.string.databaseEmpty));

    } else {
      refreshCalculatedData(currentMonth, currentYear);
      emptyStateScreen.setVisibility(View.GONE);
      filledStateScreen.setVisibility(View.VISIBLE);
    }

    monthAndYearInput.setOnFocusChangeListener(new View.OnFocusChangeListener() {
      @Override
      public void onFocusChange(View view, boolean hasFocus) {
        if (hasFocus) {
          openDatePicker();
        }
      }
    });
    monthAndYearInput.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        openDatePicker();
      }
    });
  }

  private void refreshArrayList(int month, int year) {
    ArrayList<FinanceItem> tempList = new ArrayList<>();

    //Nur anzeigen der items, welche dem ausgewähltem Monat zugehören
    for (int i = 0; i < database.getAllItems().size(); i++) {
      FinanceItem financeItem = database.getAllItems().get(i);
      if (financeItem.getDate().contains(getMonthAndYearString(month, year))) {
        tempList.add(financeItem);
      }
    }

    allTransactions.clear();
    allTransactions.addAll(sortByDate(tempList));
    transactionsAdapter.notifyDataSetChanged();
  }

  private void initList() {
    allTransactions = new ArrayList<FinanceItem>();
    initListAdapter();
  }

  private void initListAdapter() {
    list = view.findViewById(R.id.homeListView);
    transactionsAdapter = new FinanceDatabaseAdapter(getContext(), allTransactions);
    list.setAdapter(transactionsAdapter);

    list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
      @Override
      public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Intent openBookingDetailsIntent = new Intent(getActivity(), BookingDetailsActivity.class);

        /*
        Quelle: https://stackoverflow.com/questions/5571092/convert-object-to-json-in-android &
        https://github.com/google/gson
        */
        Gson gson = new Gson();
        String bookingItemJson = gson.toJson(allTransactions.get(position));

        openBookingDetailsIntent.putExtra("bookingItemJSON", bookingItemJson);
        startActivity(openBookingDetailsIntent);
      }
    });

  }

  private void refreshCalculatedData(int month, int year) {
    incomeThisMonthString = "+" + getIncome(month, year) + "€";
    incomeThisMonth.setText(incomeThisMonthString);

    expenditureThisMonthString = "-" + getExpenditure(month, year) + "€";
    expenditureThisMonth.setText(expenditureThisMonthString);

    if (Double.parseDouble(getSaldo(month, year)) > 0) {
      saldoThisMonthString = "+" + getSaldo(month, year) + "€";
    } else {
      saldoThisMonthString = getSaldo(month, year) + "€";
    }
    saldoThisMonth.setText(saldoThisMonthString);
  }

  private String getIncome(int month, int year) {
    double plus = 0.0;

    for (int i = 0; i < database.getAllItems().size(); i++) {
      FinanceItem financeItem = database.getAllItems().get(i);
      if (financeItem.getDate().contains(getMonthAndYearString(month, year))) {
        if (financeItem.getStatus()) {
          plus += Double.parseDouble(financeItem.getValue());
        }
      }
    }

    return Double.toString(plus);
  }

  private String getExpenditure(int month, int year) {
    double minus = 0.0;

    for (int i = 0; i < database.getAllItems().size(); i++) {
      FinanceItem financeItem = database.getAllItems().get(i);
      if (financeItem.getDate().contains(getMonthAndYearString(month, year))) {
        if (!financeItem.getStatus()) {
          minus += Double.parseDouble(financeItem.getValue());
        }
      }
    }

    return Double.toString(minus);
  }

  private String getSaldo(int month, int year) {
    double income = Double.parseDouble(getIncome(month, year));
    double expenditure = Double.parseDouble(getExpenditure(month, year));
    double saldo = income - expenditure;

    return String.valueOf(saldo);
  }

  private String getMonthAndYearString(int month, int year) {
    month += 1;
    String monthString;
    if (month < 10) {
      monthString = "0" + month;
    } else {
      monthString = Integer.toString(month);
    }

    return "." + monthString + "." + year;
  }

  private ArrayList<FinanceItem> sortByDate(ArrayList<FinanceItem> unsortedList) {
    Comparator<FinanceItem> comperator = new Comparator<FinanceItem>() {
      @Override
      public int compare(FinanceItem item1, FinanceItem item2) {
        LocalDate date1 = transactionsAdapter.getDateFormatted(item1.getDate());
        LocalDate date2 = transactionsAdapter.getDateFormatted(item2.getDate());
        return date2.compareTo(date1);
      }
    };
    Collections.sort(unsortedList, comperator);
    return unsortedList;
  }

  private void setMonthAndYearOnTextView(int month, int year) {

    String currentMonthString;

    switch (month + 1) {
      case 1:
        currentMonthString = "Januar";
        break;
      case 2:
        currentMonthString = "Februar";
        break;
      case 3:
        currentMonthString = "März";
        break;
      case 4:
        currentMonthString = "April";
        break;
      case 5:
        currentMonthString = "Mai";
        break;
      case 6:
        currentMonthString = "Juni";
        break;
      case 7:
        currentMonthString = "Juli";
        break;
      case 8:
        currentMonthString = "August";
        break;
      case 9:
        currentMonthString = "September";
        break;
      case 10:
        currentMonthString = "Oktober";
        break;
      case 11:
        currentMonthString = "November";
        break;
      case 12:
        currentMonthString = "Dezember";
        break;
      default:
        currentMonthString = "Monat";
        break;
    }

    String result = currentMonthString + " " + year;
    monthAndYearInput.setText(result);
  }

  private void openDatePicker() {
    //Quelle: https://learnlinky.com/2016/12/02/disable-day-selection-android-datepicker/
    Locale locale = getResources().getConfiguration().locale;
    Locale.setDefault(locale);

    DatePickerDialog datePickerDialog = new DatePickerDialog(getContext(),
        AlertDialog.THEME_HOLO_LIGHT,
        new DatePickerDialog.OnDateSetListener() {

          @Override
          public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {

            setMonthAndYearOnTextView(monthOfYear, year);
            refreshArrayList(monthOfYear, year);
            refreshCalculatedData(monthOfYear, year);
            currentMonth = monthOfYear;
            currentYear = year;
          }
        }, currentYear, currentMonth, currentDay);
    (datePickerDialog.getDatePicker()).findViewById(Resources.getSystem()
            .getIdentifier("day", "id", "android"))
            .setVisibility(View.GONE);
    datePickerDialog.show();
  }

  private boolean databaseEmpty() {
    ArrayList<FinanceItem> tempList = database.getAllItems();
    return tempList.isEmpty();
  }

  @Override
  public void onResume() {
    initList();
    initDB();
    refreshArrayList(currentMonth, currentYear);
    initCockpit();
    super.onResume();
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    database.close();
  }
}