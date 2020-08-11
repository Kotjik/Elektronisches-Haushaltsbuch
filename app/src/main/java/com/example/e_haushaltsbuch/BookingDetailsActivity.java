package com.example.e_haushaltsbuch;

import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageView;

import android.widget.TextView;

import com.example.e_haushaltsbuch.backend.AppDatabase;
import com.example.e_haushaltsbuch.backend.FinanceItem;
import com.google.gson.Gson;

import java.io.File;

public class BookingDetailsActivity extends AppCompatActivity {

  TextView name;
  TextView value;
  TextView date;
  TextView note;
  TextView category;
  TextView time;
  ImageView image;
  TextView imageTextView;

  FloatingActionButton optionsButton;
  FloatingActionButton editButton;
  FloatingActionButton deleteButton;
  boolean optionsButtonsVisible;

  //Extra from ListFragment
  String bookingItemJson;
  FinanceItem bookingItem;

  private AppDatabase database;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_bookingdetails);
    initDB();
    setupUI();
  }

  private void initDB() {
    database = new AppDatabase(this, "finances.db");
    database.open();
  }

  // fill UI with data of clicked financeItem
  private void setupUI() {
    name = findViewById(R.id.nameTextView);
    value = findViewById(R.id.valueTextView);
    date = findViewById(R.id.dateTextView);
    time = findViewById(R.id.timeTextView);
    note = findViewById(R.id.noteTextView);
    category = findViewById(R.id.categoryTextView);
    image = findViewById(R.id.imageView);
    imageTextView = findViewById(R.id.imageTextView);

    optionsButton = findViewById(R.id.button_options);
    editButton = findViewById(R.id.button_edit);
    deleteButton = findViewById(R.id.button_delete);
    setOptionsButtonsVisibility(false);

    bookingItemJson = getIntent().getStringExtra("bookingItemJSON");
    Gson gson = new Gson();
    bookingItem = gson.fromJson(bookingItemJson, FinanceItem.class);


    name.setText(bookingItem.getName());

    String valueText;
    if (bookingItem.getStatus()) {
      valueText = "+" + bookingItem.getValue() + "€";
      value.setText(valueText);
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        value.setTextColor(getResources().getColor(R.color.green, null));
      }
    } else {
      valueText = "-" + bookingItem.getValue() + "€";
      value.setText(valueText);
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        value.setTextColor(getResources().getColor(R.color.red, null));
      }
    }

    date.setText(bookingItem.getDate());

    if (bookingItem.getTime() == null || bookingItem.getTime().equals("")) {
      time.setText(getResources().getString(R.string.nothing_available));
    } else {
      time.setText(bookingItem.getTime());
    }

    if (bookingItem.getNote().isEmpty()) {
      note.setText(getResources().getString(R.string.nothing_available));
    } else {
      note.setText(bookingItem.getNote());
    }

    category.setText(bookingItem.getCategory());

    if (bookingItem.getImage() == null || bookingItem.getImage().equals("-")) {
      image.setVisibility(View.GONE);
      imageTextView.setVisibility(View.VISIBLE);
    } else {
      //Bitmap bmp = BitmapFactory.decodeByteArray(bookingItem.getImage(),
      // 0, bookingItem.getImage().length);

      image.setImageURI(Uri.parse(new File(bookingItem.getImage()).toString()));
      imageTextView.setVisibility(View.GONE);
      image.setVisibility(View.VISIBLE);
    }

    optionsButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        if (optionsButtonsVisible) {
          setOptionsButtonsVisibility(false);
          optionsButton.setImageDrawable(getResources().getDrawable(R.drawable.ic_more));
        } else {
          setOptionsButtonsVisibility(true);
          optionsButton.setImageDrawable(getResources().getDrawable(R.drawable.ic_expand_less));
        }
      }
    });
    editButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        setOptionsButtonsVisibility(false);
        Intent openInputActivityIntent = new Intent(BookingDetailsActivity.this,
                ChangeActivity.class);
        openInputActivityIntent.putExtra("existingBookingItemJSON", bookingItemJson);
        startActivityForResult(openInputActivityIntent, RESULT_OK);
      }
    });

    deleteButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {

        //Quelle:
        //https://stackoverflow.com/questions/2478517/how-to-display-a-yes-no-dialog-box-on-android
        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialogInterface, int i) {
            switch (i) {
              case DialogInterface.BUTTON_POSITIVE:
                database.removeItem(bookingItem);
                finish();
                break;

              case DialogInterface.BUTTON_NEGATIVE:
                break;

              default:
                break;
            }
          }
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(BookingDetailsActivity.this);
        builder.setMessage("Diesen Eintrag unwiderruflich löschen?")
          .setPositiveButton("Löschen", dialogClickListener)
          .setNegativeButton("Abbrechen", dialogClickListener).show();

        /* Code, welcher das Item löscht und die Activity schließt

        database.removeItem(bookingItem);
        finish();
        */
      }
    });
  }

  private void setOptionsButtonsVisibility(Boolean visibility) {
    if (visibility) {
      optionsButtonsVisible = true;
      optionsButton.setImageDrawable(getResources().getDrawable(R.drawable.ic_expand_less));
      editButton.show();
      deleteButton.show();
    } else {
      optionsButtonsVisible = false;
      optionsButton.setImageDrawable(getResources().getDrawable(R.drawable.ic_more));
      editButton.hide();
      deleteButton.hide();
    }
  }

  @Override
  public void onBackPressed() {
    if (optionsButtonsVisible) {
      setOptionsButtonsVisibility(false);
      return;
    }
    super.onBackPressed();
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    database.close();
  }
}

