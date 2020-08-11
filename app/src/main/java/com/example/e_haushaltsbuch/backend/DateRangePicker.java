package com.example.e_haushaltsbuch.backend;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.e_haushaltsbuch.R;

import java.util.Calendar;
import java.util.Date;

// https://github.com/psinetron/slycalendarview
import ru.slybeaver.slycalendarview.SlyCalendarDialog;
import ru.slybeaver.slycalendarview.SlyCalendarView;
import ru.slybeaver.slycalendarview.listeners.DialogCompleteListener;

public class DateRangePicker extends SlyCalendarDialog {

  /** erstelle DateRangePicker. */
  @SuppressLint("ResourceAsColor")
  public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    SlyCalendarView calendarView = (SlyCalendarView)super.onCreateView(inflater, container, savedInstanceState);
    TextView f = calendarView.findViewById(R.id.txtSave);
    calendarView.setBackgroundColor(R.color.colorPrimary);
    f.setText(R.string.statOK);
    f = calendarView.findViewById(R.id.txtCancel);
    f.setText(R.string.statCancel);
    return calendarView;
  }

}
