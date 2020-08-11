package com.example.e_haushaltsbuch.backend;

/**
 * Adapter for finance database.
 * Generates the associated list item for each element in the database and sets all information.
 */

import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import com.example.e_haushaltsbuch.R;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Locale;

public class FinanceDatabaseAdapter extends BaseAdapter implements Filterable {
  private ArrayList<FinanceItem> paymentList;
  private Context context;
  private ArrayList<FinanceItem> filteredList;
  private FinanceFilter financeFilter;
  ArrayList<String> months = new ArrayList<String>();
  private View v;
  LayoutInflater inflater;


  /**
  * Constructor.
  */
  public FinanceDatabaseAdapter(Context context, ArrayList<FinanceItem> listItems) {
    this.context = context;
    this.paymentList = listItems;
    this.filteredList = listItems;
    //super(context, 0, listItems);
    inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
  }

  /**
  * Get custom filter.
  * @return filter.
  */
  @Override
  public Filter getFilter() {
    if (financeFilter == null) {
      financeFilter = new FinanceFilter();
    }

    return financeFilter;
  }

  @Override
  public int getCount() {
    return filteredList.size();
  }

  @Override
  public FinanceItem getItem(int position) {
    return filteredList.get(position);
  }

  public void setFilteredList(ArrayList<FinanceItem> list) {
    filteredList = list;
    notifyDataSetChanged();
  }

  @Override
  public long getItemId(int position) {
    return position;
  }

  private boolean getStatus(int position) {
    return filteredList.get(position).getStatus();
  }

  @Override
  public View getView(int position, View convertView, ViewGroup parent) {
    v = convertView;

    FinanceItem cell = (FinanceItem) getItem(position);

    v = inflater.inflate(R.layout.list_item, null);
    setInput(position, cell);
    return v;
  }

  private void setInput(int position, FinanceItem item) {
    TextView paymentAmount = (TextView) v.findViewById(R.id.paymentAmount);
    TextView paymentCurrency = (TextView) v.findViewById(R.id.currency);
    TextView status = (TextView) v.findViewById(R.id.status);

    //Changes text color depending on the type of booking
    if (!getStatus(position)) {
      paymentAmount.setTextColor(ContextCompat.getColor(context, R.color.red));
      paymentCurrency.setTextColor(ContextCompat.getColor(context, R.color.red));
      status.setText("-");
      status.setTextColor(ContextCompat.getColor(context, R.color.red));
      status.setVisibility(View.VISIBLE);
    } else {
      paymentAmount.setTextColor(ContextCompat.getColor(context, R.color.green));
      paymentCurrency.setTextColor(ContextCompat.getColor(context, R.color.green));
      status.setText("+");
      status.setTextColor(ContextCompat.getColor(context, R.color.green));
      status.setVisibility(View.VISIBLE);
    }

    TextView paymentKategoryName = (TextView) v.findViewById(R.id.paymentKategoryName);
    paymentKategoryName.setText(item.getCategory());
    TextView paymentDay = (TextView) v.findViewById(R.id.paymentDay);
    paymentDay.setText(getElementFromDate(item.getDate(), "d"));
    TextView paymentMY = (TextView) v.findViewById(R.id.paymentMY);
    paymentMY.setText(getElementFromDate(item.getDate(), "m") + " " + getElementFromDate(item.getDate(), "y"));
    TextView paymentName = (TextView) v.findViewById(R.id.paymentName);
    paymentName.setText(item.getName());
    paymentAmount.setText(item.getValue());
  }

  /**
  * Splits the date string into year, month and day.
  */
  public String getElementFromDate(String date, String e) {
    String dateElement = "";
    LocalDate localDate = getDateFormatted(date);
    if (e == "d") {
      dateElement = String.valueOf(localDate.getDayOfMonth());
    } else if (e == "m") {
      dateElement = String.valueOf(localDate.getMonth().getDisplayName(TextStyle.FULL, Locale.GERMAN));
    } else if (e == "y") {
      dateElement = String.valueOf(localDate.getYear());
    }
    return dateElement;
  }

  public LocalDate getDateFormatted(String date) {
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    return LocalDate.parse(date, formatter);
  }

  /**
  * Custom filter for friend list
  * Filter content in friend list according to the search text
  * https://github.com/erangaeb/dev-notes/blob/master/android-search-list/FriendListAdapter.java
  */
  private class FinanceFilter extends Filter {
    @Override
    protected FilterResults performFiltering(CharSequence constraint) {
      FilterResults filterResults = new FilterResults();
      if (constraint != null && constraint.length() > 0) {
        ArrayList<FinanceItem> tempList = new ArrayList<FinanceItem>();
        // search content in payment list
        for (FinanceItem item : paymentList) {
          if (item.getName().toLowerCase().contains(constraint.toString().toLowerCase())) {
            tempList.add(item);
          }
        }
        filterResults.count = tempList.size();
        filterResults.values = tempList;
      } else {
        filterResults.count = paymentList.size();
        filterResults.values = paymentList;
      }

      return filterResults;
    }

    /**
    * Notify about filtered list to ui.
    * @param constraint text.
    * @param results filtered result.
    */
    @SuppressWarnings("unchecked")
    @Override
    protected void publishResults(CharSequence constraint, FilterResults results) {
      filteredList = (ArrayList<FinanceItem>) results.values;
      notifyDataSetChanged();
    }
  }
}