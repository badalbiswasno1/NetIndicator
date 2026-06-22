package com.my.netindicator;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.*;
import android.view.*;
import android.graphics.Color;
public class SettingsActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        SharedPreferences prefs=getSharedPreferences("settings",MODE_PRIVATE);
        ScrollView scroll=new ScrollView(this);
        scroll.setBackgroundColor(Color.parseColor("#111111"));
        LinearLayout main=new LinearLayout(this);
        main.setOrientation(LinearLayout.VERTICAL);
        main.setPadding(40,80,40,40);
        scroll.addView(main);
        TextView title=new TextView(this);
        title.setText("সেটিংস");
        title.setTextColor(Color.WHITE);
        title.setTextSize(24);
        title.setTypeface(null,android.graphics.Typeface.BOLD);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0,0,0,20);
        main.addView(title);
        addLine(main,"#FFD700");
        TextView floatTitle=new TextView(this);
        floatTitle.setText("Floating Widget");
        floatTitle.setTextColor(Color.parseColor("#FFD700"));
        floatTitle.setTextSize(16);
        floatTitle.setTypeface(null,android.graphics.Typeface.BOLD);
        floatTitle.setPadding(0,20,0,5);
        main.addView(floatTitle);
        Button floatBtn=new Button(this);
        floatBtn.setText("Overlay Permission দাও");
        floatBtn.setBackgroundColor(Color.parseColor("#E63329"));
        floatBtn.setTextColor(Color.WHITE);
        floatBtn.setOnClickListener(v->{
            Intent intent=new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:"+getPackageName()));
            startActivity(intent);
        });
        main.addView(floatBtn);
        addLine(main,"#333333");
        TextView langTitle=new TextView(this);
        langTitle.setText("ভাষা (Language)");
        langTitle.setTextColor(Color.parseColor("#FFD700"));
        langTitle.setTextSize(16);
        langTitle.setTypeface(null,android.graphics.Typeface.BOLD);
        langTitle.setPadding(0,20,0,5);
        main.addView(langTitle);
        String[] langs={"বাংলা","English","हिन्दी"};
        String[] langCodes={"bn","en","hi"};
        String savedLang=prefs.getString("lang","bn");
        RadioGroup rg=new RadioGroup(this);
        for(int i=0;i<langs.length;i++){
            RadioButton rb=new RadioButton(this);
            rb.setText(langs[i]);
            rb.setTextColor(Color.WHITE);
            rb.setTextSize(14);
            rb.setTag(langCodes[i]);
            if(langCodes[i].equals(savedLang))rb.setChecked(true);
            rg.addView(rb);
        }
        rg.setOnCheckedChangeListener((group,checkedId)->{
            RadioButton rb=(RadioButton)group.findViewById(checkedId);
            if(rb!=null){
                String lang=(String)rb.getTag();
                prefs.edit().putString("lang",lang).apply();
                Toast.makeText(this,"ভাষা পরিবর্তন হবে পরের বার খুললে",Toast.LENGTH_SHORT).show();
            }
        });
        main.addView(rg);
        addLine(main,"#333333");
        TextView permTitle=new TextView(this);
        permTitle.setText("Phone Permission (Signal দেখার জন্য)");
        permTitle.setTextColor(Color.parseColor("#FFD700"));
        permTitle.setTextSize(16);
        permTitle.setTypeface(null,android.graphics.Typeface.BOLD);
        permTitle.setPadding(0,20,0,5);
        main.addView(permTitle);
        Button permBtn=new Button(this);
        permBtn.setText("App Settings খোলো");
        permBtn.setBackgroundColor(Color.parseColor("#00CC44"));
        permBtn.setTextColor(Color.WHITE);
        permBtn.setOnClickListener(v->{
            Intent intent=new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.parse("package:"+getPackageName()));
            startActivity(intent);
        });
        main.addView(permBtn);
        addLine(main,"#333333");
        TextView about=new TextView(this);
        about.setText("True Network v1.0\nDeveloped by badalbiswasno1");
        about.setTextColor(Color.parseColor("#AAAAAA"));
        about.setTextSize(13);
        about.setGravity(Gravity.CENTER);
        about.setPadding(0,30,0,0);
        main.addView(about);
        setContentView(scroll);
    }
    private void addLine(LinearLayout parent,String color){
        View line=new View(this);
        line.setBackgroundColor(Color.parseColor(color));
        line.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,2));
        parent.addView(line);
    }
}
