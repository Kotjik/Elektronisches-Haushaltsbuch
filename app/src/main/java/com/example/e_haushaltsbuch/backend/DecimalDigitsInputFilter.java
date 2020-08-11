package com.example.e_haushaltsbuch.backend;

import android.text.InputFilter;
import android.text.Spanned;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

//retrieved from https://stackoverflow.com/questions/5357455/limit-decimal-places-in-android-edittext
//enables the user to enter a value with a specific number of decimal places
public class DecimalDigitsInputFilter implements InputFilter {
  private Pattern pattern;

  public DecimalDigitsInputFilter(int digitsBeforeZero, int digitsAfterZero) {
    pattern = Pattern.compile("[0-9]{0," + (digitsBeforeZero - 1) + "}+((\\.[0-9]{0," + (digitsAfterZero - 1) + "})?)||(\\.)?");
  }

  @Override
  public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {

    Matcher matcher = pattern.matcher(dest);
    if (!matcher.matches()) {
      return "";
    }
    return null;
  }
}