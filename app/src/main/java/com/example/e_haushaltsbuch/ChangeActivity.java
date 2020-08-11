package com.example.e_haushaltsbuch;

import android.Manifest;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.TextInputEditText;
import android.support.design.widget.TextInputLayout;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;

import com.bumptech.glide.Glide;
import com.example.e_haushaltsbuch.backend.AppDatabase;
import com.example.e_haushaltsbuch.backend.DecimalDigitsInputFilter;
import com.example.e_haushaltsbuch.backend.FinanceItem;
import com.google.gson.Gson;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;

public class ChangeActivity extends AppCompatActivity {

  TextInputLayout nameLayout;
  TextInputEditText name;
  TextInputLayout valueLayout;
  TextInputEditText value;
  TextInputLayout dateLayout;
  TextInputEditText date;
  TextInputEditText time;
  TextInputLayout categoryLayout;
  TextView categoryTextView;
  TextInputLayout noteLayout;
  TextInputEditText note;
  FloatingActionButton save;
  FloatingActionButton cancel;
  Button makePicture;
  Button income;
  Button expenditure;
  Button delete;
  Spinner spinner;
  ImageView imageView;
  String currentPhotoPath;
  boolean statusEarn = true;
  int hour;
  int minute;

  //Extra from BookingDetailsActivity
  String existingBookingItemJson;
  FinanceItem existingBookingItem;

  private AppDatabase database;
  static final int REQUEST_IMAGE_CAPTURE = 1;
  static final int REQUEST_GALLERY_CAPTURE = 0;
  private DatePickerDialog.OnDateSetListener dateSetListener;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_change);
    initDB();
    setupUI();
  }

  private void initDB() {
    database = new AppDatabase(this, "finances.db");
    database.open();
  }

  // prepare UI
  private void setupUI() {
    income = findViewById(R.id.incomeButton);
    expenditure = findViewById(R.id.expenditureButton);
    income.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        setColors(income, expenditure);
        statusEarn = true;
      }
    });
    expenditure.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        setColors(expenditure, income);
        statusEarn = false;
      }
    });

    name = findViewById(R.id.nameInput);
    nameLayout = findViewById(R.id.nameInputLayout);
    note = findViewById(R.id.noteInput);
    noteLayout = findViewById(R.id.noteInputLayout);

    imageView = findViewById((R.id.imageView));
    imageView.setVisibility(View.GONE);

    spinner = findViewById(R.id.spinner);
    spinner.setVisibility(View.GONE);
    categoryLayout = findViewById(R.id.categoryLayout);
    categoryTextView = findViewById(R.id.categoryTextView);
    initSpinner();

    makePicture = findViewById(R.id.makePicture);
    makePicture.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        String[] permissions = {Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE};
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
          requestPermissions(permissions,2);
        }
        if (ContextCompat.checkSelfPermission(ChangeActivity.this, permissions[0]) == PackageManager.PERMISSION_GRANTED) {
          if (ContextCompat.checkSelfPermission(ChangeActivity.this, permissions[1]) == PackageManager.PERMISSION_GRANTED) {
            DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
              @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                  switch (i) {
                    case DialogInterface.BUTTON_POSITIVE:
                      takePicture();
                      break;

                    case DialogInterface.BUTTON_NEGATIVE:
                      openGallery();
                      break;

                    default:
                      break;
                  }
                }
            };
            AlertDialog.Builder builder = new AlertDialog.Builder(ChangeActivity.this);
            builder.setTitle(R.string.addFoto)
                    .setMessage(R.string.dialogMessage)
                    .setPositiveButton(R.string.camera, dialogClickListener)
                    .setNegativeButton(R.string.gallery, dialogClickListener).show();
          }
        }
      }
    });

    delete = findViewById(R.id.deleteButton);
    delete.setVisibility(View.GONE);
    delete.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        imageView.setImageResource(0);
        currentPhotoPath = "-";
        imageView.setVisibility(View.GONE);
        delete.setVisibility(View.GONE);
      }
    });

    save = findViewById(R.id.saveButton);
    save.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        addInputToDB();
      }
    });

    cancel = findViewById(R.id.cancelButton);
    cancel.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        onCancel();
      }
    });

    date = findViewById(R.id.dateInput);
    date.setOnFocusChangeListener(new View.OnFocusChangeListener() {
      @Override
      public void onFocusChange(View view, boolean hasFocus) {
        if (hasFocus) {
          openDatePicker();
        }
      }
    });
    date.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        openDatePicker();
      }
    });
    dateLayout = findViewById(R.id.dateInputLayout);

    dateSetListener = new DatePickerDialog.OnDateSetListener() {
      @Override
      public void onDateSet(DatePicker datePicker, int year, int month, int day) {
        GregorianCalendar dateCal = new GregorianCalendar(year, month, day);
        DateFormat df = DateFormat.getDateInstance(DateFormat.DEFAULT, Locale.GERMANY);
        String dateString = df.format(dateCal.getTime());
        date.setText(dateString);
      }
    };

    time = findViewById(R.id.timeInput);
    time.setOnFocusChangeListener(new View.OnFocusChangeListener() {
      @Override
      public void onFocusChange(View view, boolean hasFocus) {
        if (hasFocus) {
          openTimePicker();
        }
      }
    });
    time.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        openTimePicker();
      }
    });
    time.setOnLongClickListener(new View.OnLongClickListener() {
      @Override
      public boolean onLongClick(View view) {
        time.setText("");
        return true;
      }
    });

    value = findViewById(R.id.valueInput);
    valueLayout = findViewById(R.id.valueInputLayout);
    value.setFilters(new InputFilter[] {
        new DecimalDigitsInputFilter(7, 2)
    });
    value.addTextChangedListener(new TextWatcher() {
      @Override
      public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

      }

      @Override
      public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
        String enteredString = charSequence.toString();
        if (enteredString.startsWith(".")) {
          String enteredNumber = "0" + enteredString;
          value.setText(enteredNumber);
          value.setSelection(value.getText().length());
        }
        if (enteredString.startsWith("0") && !enteredString.substring(1).startsWith(".")) {
          value.setText(enteredString.substring(1));
          value.setSelection(value.getText().length());
        }
      }

      @Override
      public void afterTextChanged(Editable editable) {

      }
    });

    //Prefill Inputs if user wants to edit existing item
    checkForExistingBookingItemExtra();
  }

  private void openDatePicker() {
    Calendar calendar = Calendar.getInstance();
    int year = calendar.get(Calendar.YEAR);
    int month = calendar.get(Calendar.MONTH);
    int day = calendar.get(Calendar.DAY_OF_MONTH);

    DatePickerDialog dialog = new DatePickerDialog(ChangeActivity.this,
        R.style.DialogTheme,
        dateSetListener, year, month, day);
    dialog.setCanceledOnTouchOutside(true);
    dialog.show();
  }

  private void openTimePicker() {
    Calendar calendar = Calendar.getInstance();
    hour = calendar.get(Calendar.HOUR_OF_DAY);
    minute = calendar.get(Calendar.MINUTE);

    TimePickerDialog timePickerDialog = new TimePickerDialog(ChangeActivity.this,
        new TimePickerDialog.OnTimeSetListener() {

          @Override
          public void onTimeSet(TimePicker view, int ppHour, int ppMinute) {
            hour = ppHour;
            minute = ppMinute;
            String timeString = String.format("%02d:%02d", hour, minute) + " Uhr";
            time.setText(timeString);
          }
        }, hour, minute, true);

    timePickerDialog.show();
  }

  private void setColors(Button selected, Button unselected) {
    selected.setSelected(true);
    unselected.setSelected(false);
  }

  private void initSpinner() {
    ArrayAdapter<CharSequence> spinnerAdapter = ArrayAdapter
        .createFromResource(ChangeActivity.this, R.array.spinnerArray, R.layout.spinner_item);
    spinnerAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
    spinner.setAdapter(spinnerAdapter);
    spinner.setSelection(7);

    spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
      @Override
      public void onItemSelected(AdapterView<?> adapterView, View v, int position, long arg3) {

      }

      @Override
      public void onNothingSelected(AdapterView<?> adapterView) {

      }
    });
  }

  /**returns the selected category name in the spinner.*/
  public String getCategoryName(int spinner) {
    if (spinner == 0) {
      return "Einkauf";
    } else if (spinner == 1) {
      return "Miete";
    } else if (spinner == 2) {
      return "Nebenkosten";
    } else if (spinner == 3) {
      return "Tanken";
    } else if (spinner == 4) {
      return "Lohn";
    } else if (spinner == 5) {
      return "Gutschrift";
    } else if (spinner == 6) {
      return "Sonstiges";
    } else {
      return "keine Auswahl";
    }
  }

  /**returns the selected category number.*/
  public int getCategoryInt(String str) {
    if (str.equals("Einkauf")) {
      return 0;
    } else if (str.equals("Miete")) {
      return 1;
    } else if (str.equals("Nebenkosten")) {
      return 2;
    } else if (str.equals("Tanken")) {
      return 3;
    } else if (str.equals("Lohn")) {
      return 4;
    } else if (str.equals("Gutschrift")) {
      return 5;
    } else if (str.equals("Sonstiges")) {
      return 6;
    } else {
      return 7;
    }
  }

  /** open PictureIntent to take a picture with phone camera. */
  public void takePicture() {
    //opens the camera
    //retrieved from: https://developer.android.com/training/camera/photobasics.html#java
    Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
    if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
      startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
    }
  }

  public void openGallery() {
    Intent openGalleryIntent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
    startActivityForResult(openGalleryIntent, REQUEST_GALLERY_CAPTURE);
  }

  private void addInputToDB() {
    String inputName = name.getText().toString();
    String inputValue = value.getText().toString();
    String inputDate = date.getText().toString();
    String inputNote = note.getText().toString();
    String inputTime = time.getText().toString();

    if (imageView.getVisibility() == View.VISIBLE) {
      Bitmap bitmap = ((BitmapDrawable) imageView.getDrawable()).getBitmap();
      Uri tempUri = getImageUri(getApplicationContext(), bitmap);
      currentPhotoPath = getRealPathFromUri(tempUri);
    } else {
      currentPhotoPath = "-";
    }

    int category;
    if (spinner.getVisibility() == View.VISIBLE) {
      category = spinner.getSelectedItemPosition();
    } else {
      category = 7;
    }

    //instead of getting item number we want the name
    String catName = getCategoryName(category);

    //user needs to enter a name, value, date and category to save entry
    if (!inputName.equals("") && !inputValue.equals("") && !inputDate.equals("") && category != 7) {
      name.setText("");
      value.setText("");
      date.setText("");
      note.setText("");
      time.setText("");
      imageView.setImageResource(0);

      FinanceItem newItem = new FinanceItem(inputName, inputValue, inputDate, inputNote,
          catName, currentPhotoPath, statusEarn, inputTime);
      Log.d("ChangeActivity", "addInputToDB: " + newItem.toString());

      database.removeItem(existingBookingItem);

      Intent openBookingDetailsIntent = new Intent(ChangeActivity.this,
          BookingDetailsActivity.class);
      Gson gson = new Gson();
      String newBookingItemJson = gson.toJson(newItem);
      openBookingDetailsIntent.putExtra("bookingItemJSON", newBookingItemJson);
      openBookingDetailsIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
      startActivity(openBookingDetailsIntent);

      database.insertItem(newItem);
      finish();

    } else {
      if (inputName.isEmpty()) {
        nameLayout.setError(getString(R.string.errorMessage));
      } else {
        nameLayout.setError(null);
      }

      if (inputValue.isEmpty()) {
        valueLayout.setError(getString(R.string.errorMessage));
      } else {
        valueLayout.setError(null);
      }

      if (category == 7) {
        categoryTextView.setTextColor(getResources().getColor(R.color.red));
        TextView errorText = (TextView)spinner.getSelectedView();
        errorText.setError("");
        errorText.setTextColor(getResources().getColor(R.color.red));
        errorText.setText(getString(R.string.mandatoryField) + " ");

      } else {
        categoryLayout.setError(null);
        categoryTextView.setTextColor(getResources().getColor(R.color.textColor));
      }
    }
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
      Bundle extras = data.getExtras();
      Bitmap imageBitmap = (Bitmap) extras.get("data");
      imageView.setImageBitmap(imageBitmap);
      Uri tempUri = getImageUri(getApplicationContext(), imageBitmap);
      currentPhotoPath = getRealPathFromUri(tempUri);
      imageView.setVisibility(View.VISIBLE);
      delete.setVisibility(View.VISIBLE);
    }

    if (requestCode == REQUEST_GALLERY_CAPTURE && resultCode == RESULT_OK) {
      Uri imageUri = data.getData();
      String path = getRealPathFromUri(imageUri);
      currentPhotoPath = path;

      Glide.with(imageView)
        .load(imageUri)
        .into(imageView);

      imageView.setVisibility(View.VISIBLE);
      delete.setVisibility(View.VISIBLE);
    }
  }

  private String getRealPathFromUri(Uri contentUri) {
    String result;
    Cursor cursor = getContentResolver().query(contentUri, null, null,
        null, null);
    if (cursor == null) {
      result = contentUri.getPath();
    } else {
      cursor.moveToFirst();
      int idx = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
      result = cursor.getString(idx);
      cursor.close();
    }
    return result;
  }

  /** get the ImageUri. **/
  public Uri getImageUri(Context inContext, Bitmap inImage) {
    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    inImage.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
    String path = MediaStore.Images.Media.insertImage(inContext.getContentResolver(), inImage,
        "Title", null);
    return Uri.parse(path);
  }

  //check for Extra from BookingDetailsActivity for edit
  private void checkForExistingBookingItemExtra() {
    existingBookingItemJson = getIntent().getStringExtra("existingBookingItemJSON");
    if (existingBookingItemJson != null && !existingBookingItemJson.isEmpty()) {
      Gson gson = new Gson();
      existingBookingItem = gson.fromJson(existingBookingItemJson, FinanceItem.class);

      //prefill Inputs with FinanceItem Data
      if (existingBookingItem.getStatus()) {
        income.performClick();
      } else {
        expenditure.performClick();
      }

      name.setText(existingBookingItem.getName());
      value.setText(existingBookingItem.getValue());
      date.setText(existingBookingItem.getDate());
      time.setText(existingBookingItem.getTime());
      note.setText(existingBookingItem.getNote());

      spinner.setSelection(getCategoryInt(existingBookingItem.getCategory()));
      spinner.setVisibility(View.VISIBLE);

      if (existingBookingItem.getImage() != null && !existingBookingItem.getImage().equals("-")) {
        imageView.setVisibility(View.VISIBLE);
        imageView.setImageURI(Uri.parse(new File(existingBookingItem.getImage()).toString()));
        delete.setVisibility(View.VISIBLE);
      }
    }
  }

  private void onCancel() {
    DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
      @Override
      public void onClick(DialogInterface dialogInterface, int i) {
        switch (i) {
          case DialogInterface.BUTTON_POSITIVE:
            finish();
            break;

          case DialogInterface.BUTTON_NEGATIVE:
            break;

          default:
            break;
        }
      }
    };

    AlertDialog.Builder builder = new AlertDialog.Builder(ChangeActivity.this);
    builder.setMessage("Alle Änderungen verwerfen?")
      .setPositiveButton("Verwerfen", dialogClickListener)
      .setNegativeButton("Abbrechen", dialogClickListener).show();
  }

  @Override
  public void onBackPressed() {
    onCancel();
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    database.close();
  }
}