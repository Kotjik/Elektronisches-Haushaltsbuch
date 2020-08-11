package com.example.e_haushaltsbuch.backend;

public class FinanceItem implements Comparable<FinanceItem> {

  private String name;
  private String value;
  private String date;
  private String time;
  private String note;
  private String category;
  private String image;
  private boolean status;
  private boolean isSectionHeader;

  /**represents a entry in the database.*/
  public FinanceItem(String name, String value, String date, String note, String category, String image, Boolean status, String time) {
    this.name = name;
    this.value = value;
    this.date = date;
    this.time = time;
    this.note = note;
    this.category = category;
    this.image = image;
    this.status = status;
    isSectionHeader = false;
  }

  public String getName() {
    return name;
  }

  public String getValue() {
    return value;
  }

  public String getDate() {
    return date;
  }

  public String getTime() {
    return time;
  }

  public String getNote() {
    return note;
  }

  public String getCategory() {
    return category;
  }

  public String getImage() {
    return image;
  }

  public Boolean getStatus() {
    return status;
  }

  @Override
  public String toString() {
    return "Name: " + getName() + ", Value: " + getValue() + ", Date: " + getDate() + ", Time: " + getTime() + ", Note: " + getNote() + ", Category: "
            + getCategory() + ", Image: " + getImage() + ", Status: " + getStatus();
  }

  @Override
  public int compareTo(FinanceItem item) {
    return this.date.compareTo(item.date);
  }

}