package com.example.e_haushaltsbuch;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.TextView;

import com.example.e_haushaltsbuch.backend.AppDatabase;
import com.example.e_haushaltsbuch.backend.FinanceDatabaseAdapter;
import com.example.e_haushaltsbuch.backend.FinanceItem;
import com.example.e_haushaltsbuch.backend.SwipeDismissListViewTouchListener;
import com.google.gson.Gson;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;

/**
 * Displays the list view with all database elements.
 */
public class ListFragment extends Fragment {

  private ArrayList<FinanceItem> allTransactions;
  private ArrayList<FinanceItem> filteredList;
  private AppDatabase financeDB;
  private FinanceDatabaseAdapter paymentAdapter;
  ListView list;
  ActionMode mMode;
  ArrayList<Integer> checkedItems = new ArrayList<>();
  View view;
  private LinearLayout emptyStateScreen;
  boolean[] selectedCategories = new boolean[]{false, false, false, false, false, false, false};
  String [] categories;

  @Override
  public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    view = inflater.inflate(R.layout.fragment_list, container, false);
    initList();
    initDatabase();
    initListView();
    initSearch();
    initFilter();
    refreshArrayList();
    return view;
  }

  private void initFilter() {
    categories = view.getResources().getStringArray(R.array.spinnerArray);
    categories = Arrays.copyOf(categories, categories.length - 1);

    //https://www.youtube.com/watch?v=DDwQCtccia8
    final ImageView filterIcon = (ImageView) view.findViewById(R.id.filterIcon);
    filterIcon.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        //reset filteredList
        filteredList = new ArrayList<FinanceItem>();

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
            // What happens if Dialog is closed and nothing is selected
            showFilteredItems();
            dialog.dismiss();
          }
        });
        categoryFilterDialogBuilder.setNegativeButton("Alles anzeigen", new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int which) {
            selectedCategories = new boolean[]{true, true, true, true, true, true, true};
            showFilteredItems();
            dialog.dismiss();
          }
        });
        AlertDialog categoryFilterDialog = categoryFilterDialogBuilder.create();
        categoryFilterDialog.setCanceledOnTouchOutside(true);
        categoryFilterDialog.show();
      }
    });
  }

  private void showFilteredItems() {
    for (int i = 0; i < selectedCategories.length; i++) {
      if (selectedCategories[i]) {
        for (int j = 0; j < allTransactions.size(); j++) {
          FinanceItem item = allTransactions.get(j);
          if (categories[i].equals(item.getCategory())) {
            filteredList.add(item);
          }
        }
      }
    }
    //paymentAdapter.setFilteredList(addSections(sortByDate(filteredList)));
    paymentAdapter.setFilteredList(sortByDate(filteredList));
    showSum(filteredList);
  }

  private void initSearch() {
    SearchView searchView = (SearchView) view.findViewById(R.id.searchBar);
    searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
      @Override
      public boolean onQueryTextChange(String newText) {
        // allTransactions ist die Liste, vgl mit Search Input
        paymentAdapter.getFilter().filter(newText);
        return true;
      }

      @Override
      public boolean onQueryTextSubmit(String query) {
        return false;
      }
    });
  }

  private void initDatabase() {
    financeDB = new AppDatabase(getContext(), "finances.db");
    financeDB.open();
  }

  private void refreshArrayList() {
    ArrayList tempList = financeDB.getAllItems();
    allTransactions.clear();
    //allTransactions.addAll(addSections(sortByDate(tempList)));
    allTransactions.addAll(sortByDate(tempList));
    paymentAdapter.notifyDataSetChanged();
    showSum(allTransactions);

    emptyStateScreen = view.findViewById(R.id.emptyStateScreen);
    if (allTransactions.size() > 0) {
      emptyStateScreen.setVisibility(View.GONE);
      list.setVisibility(View.VISIBLE);
    } else {
      emptyStateScreen.setVisibility(View.VISIBLE);
      list.setVisibility(View.GONE);
    }
  }

  private void initList() {
    allTransactions = new ArrayList<FinanceItem>();
    initListAdapter();
  }

  private void initListAdapter() {
    list = (ListView) view.findViewById(R.id.listView);
    paymentAdapter = new FinanceDatabaseAdapter(getContext(), allTransactions);
    list.setAdapter(paymentAdapter);

    View footerView = ((LayoutInflater) getContext()
        .getSystemService(Context.LAYOUT_INFLATER_SERVICE))
        .inflate(R.layout.list_footer, null, false);
    list.addFooterView(footerView, null, false);
  }

  private void initListView() {
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

    SwipeDismissListViewTouchListener touchListener =
        new SwipeDismissListViewTouchListener(
        list,
        new SwipeDismissListViewTouchListener.DismissCallbacks() {
          @Override
          public boolean canDismiss(int position) {
            return true;
          }

          @Override
          public void onDismiss(ListView listView, int[] reverseSortedPositions) {
            for (int position : reverseSortedPositions) {
              removeTaskAtPosition(position);
              paymentAdapter.notifyDataSetChanged();
            }
          }
        });
    list.setOnTouchListener(touchListener);
  }

  private void removeTaskAtPosition(int position) {
    final FinanceItem item = allTransactions.get(position);
    if (allTransactions.get(position) != null) {
      //Quelle: https://stackoverflow.com/questions/2478517/how-to-display-a-yes-no-dialog-box-on-android
      DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialogInterface, int i) {
          switch (i) {
            case DialogInterface.BUTTON_POSITIVE:
              financeDB.removeItem(item);
              refreshArrayList();
              break;

            case DialogInterface.BUTTON_NEGATIVE:
              break;

            default:
              break;
          }
        }
      };

      android.support.v7.app.AlertDialog.Builder builder = new android.support.v7
          .app.AlertDialog.Builder(getContext());
      builder.setMessage("Diesen Eintrag unwiderruflich löschen?")
          .setPositiveButton("Löschen", dialogClickListener)
          .setNegativeButton("Abbrechen", dialogClickListener).show();
    }
  }

  private void showSum(ArrayList<FinanceItem> sumList) {
    TextView sumRevenue = (TextView) view.findViewById(R.id.sumRevenue);
    String sumRevenueText = "+" + calculateSum(true, sumList);
    sumRevenue.setText(sumRevenueText);
    TextView sumExpenditure = (TextView) view.findViewById(R.id.sumExpenditure);
    sumExpenditure.setText(calculateSum(false, sumList));
  }

  private String calculateSum(boolean kindOfSum, ArrayList<FinanceItem> sumList) {
    float sum = 0;
    for (int i = 0; i < sumList.size(); i++) {
      FinanceItem item = sumList.get(i);
      if (item.getStatus() == kindOfSum && item.getName() != null) {
        Log.d("ausgabe status", String.valueOf(item));
        Log.d("ausgabe value", item.getValue());
        sum += Float.parseFloat(item.getValue());
      }
    }
    return String.valueOf(sum);
  }

  /**sorts the finance items comparing the dates.*/
  private ArrayList<FinanceItem> sortByDate(ArrayList<FinanceItem> unsortedList) {
    Comparator<FinanceItem> comperator = new Comparator<FinanceItem>() {
      @Override
      public int compare(FinanceItem item1, FinanceItem item2) {
        LocalDate date1 = paymentAdapter.getDateFormatted(item1.getDate());
        LocalDate date2 = paymentAdapter.getDateFormatted(item2.getDate());
        return date2.compareTo(date1);
      }
    };
    Collections.sort(unsortedList, comperator);
    return unsortedList;
  }

  @Override
  public void onResume() {
    refreshArrayList();
    super.onResume();
  }


  @Override
  public void onDestroy() {
    super.onDestroy();
    financeDB.close();
  }

  @Override
  public void onDetach() {
    if (mMode != null) {
      mMode.finish();
    }
    super.onDetach();
  }

}