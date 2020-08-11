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
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
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
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;

import com.bumptech.glide.Glide;
import com.example.e_haushaltsbuch.backend.AppDatabase;
import com.example.e_haushaltsbuch.backend.DecimalDigitsInputFilter;
import com.example.e_haushaltsbuch.backend.FinanceItem;

import java.io.ByteArrayOutputStream;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.regex.Pattern;

public class InputActivity extends AppCompatActivity {

  EditText name;
  EditText value;
  EditText date;
  EditText time;
  EditText note;
  Button save;
  Button previous;
  ImageButton cancel;
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
  LinearLayout inputOutput;
  LinearLayout nameMenu;
  LinearLayout valueMenu;
  LinearLayout dateMenu;
  LinearLayout timeMenu;
  LinearLayout noteMenu;
  LinearLayout catMenu;
  LinearLayout picMenu;
  TextInputLayout editName;
  TextInputLayout editValue;
  TextInputLayout editDate;
  TextView choseButton;
  String inputName;
  String inputValue;
  String inputDate;
  String inputNote;
  String inputTime;
  String catName;
  private AppDatabase database;
  static final int REQUEST_IMAGE_CAPTURE = 1;
  static final int REQUEST_GALLERY_CAPTURE = 0;
  private DatePickerDialog.OnDateSetListener dateSetListener;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_input);
    initDB();
    setupUI();
  }

  private void initDB() {
    database = new AppDatabase(this, "finances.db");
    database.open();
  }

  private void setupUI() {
    inputOutput = findViewById(R.id.inputOutput);
    nameMenu = findViewById(R.id.nameMenu);
    valueMenu = findViewById(R.id.valueMenu);
    dateMenu = findViewById(R.id.dateMenu);
    timeMenu = findViewById(R.id.timeMenu);
    noteMenu = findViewById(R.id.noteMenu);
    catMenu = findViewById(R.id.categoryMenu);
    picMenu = findViewById(R.id.pictureMenu);
    openFirstView();

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
    note = findViewById(R.id.noteInput);

    currentPhotoPath = "-";
    imageView = findViewById((R.id.imageView));
    imageView.setVisibility(View.GONE);

    spinner = findViewById(R.id.spinner);
    initSpinner();

    makePicture = findViewById(R.id.makePicture);
    makePicture.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        //retrieved from: https://stackoverflow.com/questions/48948156/android-studio-reading-local-file-permission-denied-despite-having-permissio
        String[] permissions = {Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE};
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
          requestPermissions(permissions,2);
        }
        if (ContextCompat.checkSelfPermission(InputActivity.this, permissions[0]) == PackageManager.PERMISSION_GRANTED) {
          if (ContextCompat.checkSelfPermission(InputActivity.this, permissions[1]) == PackageManager.PERMISSION_GRANTED) {
            openDialog();
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
        showNextPage();
      }
    });

    previous = findViewById(R.id.previousButton);
    previous.setVisibility(View.GONE);
    previous.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        showPrevPage();
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
    GregorianCalendar dateCal = new GregorianCalendar();
    DateFormat df = DateFormat.getDateInstance(DateFormat.DEFAULT, Locale.GERMANY);
    String dateString = df.format(dateCal.getTime());
    date.setText(dateString);
    date.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        openDatePicker();
      }
    });
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
    editName = findViewById(R.id.editName);
    editDate = findViewById(R.id.editDate);
    editValue = findViewById(R.id.editValue);
    choseButton = findViewById(R.id.choseButton);
  }

  /** shows the previous view. */
  private void showPrevPage() {
    if (nameMenu.getVisibility() == View.VISIBLE) {
      nameMenu.setVisibility(View.GONE);
      inputOutput.setVisibility(View.VISIBLE);
      previous.setVisibility(View.GONE);
    } else if (valueMenu.getVisibility() == View.VISIBLE) {
      valueMenu.setVisibility(View.GONE);
      nameMenu.setVisibility(View.VISIBLE);
    } else if (dateMenu.getVisibility() == View.VISIBLE) {
      dateMenu.setVisibility(View.GONE);
      valueMenu.setVisibility(View.VISIBLE);
    } else if (timeMenu.getVisibility() == View.VISIBLE) {
      timeMenu.setVisibility(View.GONE);
      dateMenu.setVisibility(View.VISIBLE);
    } else if (noteMenu.getVisibility() == View.VISIBLE) {
      noteMenu.setVisibility(View.GONE);
      timeMenu.setVisibility(View.VISIBLE);
    } else if (catMenu.getVisibility() == View.VISIBLE) {
      catMenu.setVisibility(View.GONE);
      noteMenu.setVisibility(View.VISIBLE);
    } else if (picMenu.getVisibility() == View.VISIBLE) {
      picMenu.setVisibility(View.GONE);
      catMenu.setVisibility(View.VISIBLE);
      save.setText(getString(R.string.next));
    }
  }

  /** shows the next view or saves data if last view is visible. */
  private void showNextPage() {
    if (inputOutput.getVisibility() == View.VISIBLE) {
      if (!income.isSelected() && !expenditure.isSelected()) {
        choseButton.setError("");
        choseButton.setTextColor(Color.RED);
        choseButton.setText(getString(R.string.mandatoryField) + " ");
      } else {
        choseButton.setError(null);
        choseButton.setText("");
        inputOutput.setVisibility(View.GONE);
        nameMenu.setVisibility(View.VISIBLE);
        previous.setVisibility(View.VISIBLE);
      }
    } else if (nameMenu.getVisibility() == View.VISIBLE) {
      if (name.getText().toString().trim().isEmpty()) {
        editName.setError(getString(R.string.errorMessage));
      } else {
        editName.setError(null);
        nameMenu.setVisibility(View.GONE);
        valueMenu.setVisibility(View.VISIBLE);
      }
    } else if (valueMenu.getVisibility() == View.VISIBLE) {
      if (value.getText().toString().trim().isEmpty() || Pattern.matches("0([.]?0*)*", value.getText().toString())) {
        editValue.setError(getString(R.string.errorMessage));
      } else {
        editValue.setError(null);
        valueMenu.setVisibility(View.GONE);
        dateMenu.setVisibility(View.VISIBLE);
      }
    } else if (dateMenu.getVisibility() == View.VISIBLE) {
      if (date.getText().toString().trim().isEmpty()) {
        editDate.setError(getString(R.string.errorMessage));
      } else {
        editDate.setError(null);
        dateMenu.setVisibility(View.GONE);
        timeMenu.setVisibility(View.VISIBLE);
      }
    } else if (timeMenu.getVisibility() == View.VISIBLE) {
      timeMenu.setVisibility(View.GONE);
      noteMenu.setVisibility(View.VISIBLE);
    } else if (noteMenu.getVisibility() == View.VISIBLE) {
      noteMenu.setVisibility(View.GONE);
      catMenu.setVisibility(View.VISIBLE);
    } else if (catMenu.getVisibility() == View.VISIBLE) {
      if (spinner.getSelectedItemPosition() == 7) {
        TextView errorText = (TextView)spinner.getSelectedView();
        errorText.setError("");
        errorText.setTextColor(Color.RED);
        errorText.setText(getString(R.string.mandatoryField) + " ");
      } else {
        catMenu.setVisibility(View.GONE);
        picMenu.setVisibility(View.VISIBLE);
        save.setText(getString(R.string.save));
      }
    } else {
      addInputToDB();
    }
  }

  private void openFirstView() {
    nameMenu.setVisibility(View.GONE);
    valueMenu.setVisibility(View.GONE);
    dateMenu.setVisibility(View.GONE);
    timeMenu.setVisibility(View.GONE);
    noteMenu.setVisibility(View.GONE);
    catMenu.setVisibility(View.GONE);
    picMenu.setVisibility(View.GONE);
  }

  private void setColors(Button selected, Button unselected) {
    //sets the color depending on the selection
    selected.setSelected(true);
    unselected.setSelected(false);
  }

  /**opens dialog to get a picture from the camera or gallery.*/
  private void openDialog() {
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
    AlertDialog.Builder builder = new AlertDialog.Builder(InputActivity.this);
    builder.setTitle(R.string.addFoto)
            .setMessage(R.string.dialogMessage)
            .setPositiveButton(R.string.camera, dialogClickListener)
            .setNegativeButton(R.string.gallery, dialogClickListener).show();
  }

  private void openDatePicker() {
    Calendar calendar = Calendar.getInstance();
    int year = calendar.get(Calendar.YEAR);
    int month = calendar.get(Calendar.MONTH);
    int day = calendar.get(Calendar.DAY_OF_MONTH);

    DatePickerDialog dialog = new DatePickerDialog(InputActivity.this, R.style.DialogTheme,
            dateSetListener, year, month, day);
    dialog.setCanceledOnTouchOutside(true);
    dialog.show();
  }

  private void openTimePicker() {
    Calendar calendar = Calendar.getInstance();
    hour = calendar.get(Calendar.HOUR_OF_DAY);
    minute = calendar.get(Calendar.MINUTE);

    TimePickerDialog timePickerDialog = new TimePickerDialog(InputActivity.this,
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

  private void initSpinner() {
    ArrayAdapter<CharSequence> spinnerAdapter = ArrayAdapter.createFromResource(InputActivity.this, R.array.spinnerArray,
            R.layout.spinner_item);
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

  /** takePicture action. */
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

  /**saves data in the database.*/
  private void addInputToDB() {
    inputName = name.getText().toString();
    inputValue = value.getText().toString();
    inputDate = date.getText().toString();
    inputNote = note.getText().toString();
    inputTime = time.getText().toString();

    int category;
    if (spinner.getVisibility() == View.VISIBLE) {
      category = spinner.getSelectedItemPosition();
    } else {
      category = 7;
    }

    //instead of getting item number we want the name
    catName = getCategoryName(category);

    name.setText("");
    value.setText("");
    date.setText("");
    note.setText("");
    time.setText("");
    imageView.setImageResource(0);

    FinanceItem newItem = new FinanceItem(inputName, inputValue, inputDate, inputNote, catName, currentPhotoPath, statusEarn, inputTime);
    Log.d("InputActivity", "addInputToDB: " + newItem.toString());

    database.insertItem(newItem);
    finish();
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

  //retrieved from: https://stackoverflow.com/questions/2789276/android-get-real-path-by-uri-getpath
  private String getRealPathFromUri(Uri contentUri) {
    String result;
    Cursor cursor = getContentResolver().query(contentUri, null, null, null, null);
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

  /**gets ImageUri.*/
  public Uri getImageUri(Context inContext, Bitmap inImage) {
    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    inImage.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
    String path = MediaStore.Images.Media.insertImage(inContext.getContentResolver(), inImage, "Title", null);
    return Uri.parse(path);
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

    AlertDialog.Builder builder = new AlertDialog.Builder(InputActivity.this);
    builder.setMessage("Die Eingabe verwerfen?")
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