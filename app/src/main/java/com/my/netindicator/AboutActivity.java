package com.my.netindicator;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class AboutActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        buildUI();
    }

    private void buildUI() {
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(Color.parseColor("#111111"));
        LinearLayout main = new LinearLayout(this);
        main.setOrientation(LinearLayout.VERTICAL);
        main.setPadding(40, 60, 40, 40);
        main.setBackgroundColor(Color.parseColor("#111111"));
        main.setGravity(Gravity.CENTER_HORIZONTAL);
        scroll.addView(main);

        TextView appName = new TextView(this);
        appName.setText("True Network");
        appName.setTextColor(Color.parseColor("#00CC44"));
        appName.setTextSize(26);
        appName.setTypeface(null, android.graphics.Typeface.BOLD);
        appName.setPadding(0, 0, 0, 6);
        main.addView(appName);

        TextView version = new TextView(this);
        version.setText("Version 3.0");
        version.setTextColor(Color.parseColor("#AAAAAA"));
        version.setTextSize(14);
        version.setPadding(0, 0, 0, 30);
        main.addView(version);

        TextView developer = new TextView(this);
        developer.setText("Developed by Badal Biswas");
        developer.setTextColor(Color.WHITE);
        developer.setTextSize(16);
        developer.setPadding(0, 0, 0, 30);
        main.addView(developer);

        Button feedbackBtn = new Button(this);
        feedbackBtn.setText("Send Feedback");
        feedbackBtn.setBackgroundColor(Color.parseColor("#00CC44"));
        feedbackBtn.setTextColor(Color.WHITE);
        feedbackBtn.setOnClickListener(v -> sendEmail("True Network - Feedback"));
        LinearLayout.LayoutParams p1 = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        p1.bottomMargin = 10;
        main.addView(feedbackBtn, p1);

        Button bugBtn = new Button(this);
        bugBtn.setText("Report a Bug");
        bugBtn.setBackgroundColor(Color.parseColor("#E63329"));
        bugBtn.setTextColor(Color.WHITE);
        bugBtn.setOnClickListener(v -> sendEmail("True Network - Bug Report"));
        LinearLayout.LayoutParams p2 = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        p2.bottomMargin = 10;
        main.addView(bugBtn, p2);

        Button backBtn = new Button(this);
        backBtn.setText("< Back");
        backBtn.setBackgroundColor(Color.parseColor("#333333"));
        backBtn.setTextColor(Color.WHITE);
        backBtn.setOnClickListener(v -> finish());
        LinearLayout.LayoutParams p3 = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        p3.topMargin = 20;
        main.addView(backBtn, p3);

        setContentView(scroll);
    }

    private void sendEmail(String subject) {
        try {
            Intent intent = new Intent(Intent.ACTION_SENDTO);
            intent.setData(Uri.parse("mailto:"));
            intent.putExtra(Intent.EXTRA_SUBJECT, subject);
            startActivity(intent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
