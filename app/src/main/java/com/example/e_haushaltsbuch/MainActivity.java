package com.example.e_haushaltsbuch;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;

public class MainActivity extends AppCompatActivity {

  FloatingActionButton addButton;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    BottomNavigationView bottomNav = findViewById(R.id.bottom_nav);
    bottomNav.setOnNavigationItemSelectedListener(navListener);

    //keep the selected fragment when rotating the device
    if (savedInstanceState == null) {
      getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container,
        new HomeFragment()).commit();
    }

    setupUI();
  }

  private void setupUI() {
    addButton = findViewById(R.id.button_add);

    addButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        Intent openInput = new Intent(MainActivity.this, InputActivity.class);
        startActivity(openInput);
      }
    });
  }

  private BottomNavigationView.OnNavigationItemSelectedListener navListener =
      new BottomNavigationView.OnNavigationItemSelectedListener() {
      @Override
      public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
        Fragment selectedFragment = null;

        switch (menuItem.getItemId()) {
          case R.id.navigation_home:
            selectedFragment = new HomeFragment();
            break;
          case R.id.navigation_statistic:
            selectedFragment = new StatisticFragment();
            break;
          case R.id.navigation_list:
            selectedFragment = new ListFragment();
            break;
          default:
            selectedFragment = new HomeFragment();
        }
        getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container,
            selectedFragment).commit();

        return true;
      }
      };


}
