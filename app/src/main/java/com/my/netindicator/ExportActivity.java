package com.my.netindicator;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileWriter;
import android.content.Intent;
import androidx.core.content.FileProvider;
import android.graphics.pdf.PdfDocument;
import android.graphics.Paint;
import android.graphics.Canvas;

public class ExportActivity extends Activity {
    private NetworkLogger logger;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        logger = new NetworkLogger(this);
        buildUI();
    }

    private void buildUI() {
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(Color.parseColor("#111111"));
        LinearLayout main = new LinearLayout(this);
        main.setOrientation(LinearLayout.VERTICAL);
        main.setPadding(40, 60, 40, 40);
        main.setBackgroundColor(Color.parseColor("#111111"));
        scroll.addView(main);

        TextView title = new TextView(this);
        title.setText("Export Data");
        title.setTextColor(Color.WHITE);
        title.setTextSize(22);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        title.setPadding(0, 0, 0, 20);
        main.addView(title);

        TextView info = new TextView(this);
        info.setText("Export your network history as CSV file to Downloads folder.");
        info.setTextColor(Color.parseColor("#AAAAAA"));
        info.setTextSize(14);
        info.setPadding(0, 0, 0, 20);
        main.addView(info);

        Button exportBtn = new Button(this);
        exportBtn.setText("Export as CSV");
        exportBtn.setBackgroundColor(Color.parseColor("#00CC44"));
        exportBtn.setTextColor(Color.WHITE);
        exportBtn.setOnClickListener(v -> exportCSV(false));
        main.addView(exportBtn);

        Button shareCsvBtn = new Button(this);
        shareCsvBtn.setText("Export & Share CSV");
        shareCsvBtn.setBackgroundColor(Color.parseColor("#0099FF"));
        shareCsvBtn.setTextColor(Color.WHITE);
        shareCsvBtn.setOnClickListener(v -> exportCSV(true));
        main.addView(shareCsvBtn);

        Button exportJsonBtn = new Button(this);
        exportJsonBtn.setText("Export as JSON");
        exportJsonBtn.setBackgroundColor(Color.parseColor("#00CC44"));
        exportJsonBtn.setTextColor(Color.WHITE);
        exportJsonBtn.setOnClickListener(v -> exportJSON(false));
        main.addView(exportJsonBtn);

        Button shareJsonBtn = new Button(this);
        shareJsonBtn.setText("Export & Share JSON");
        shareJsonBtn.setBackgroundColor(Color.parseColor("#0099FF"));
        shareJsonBtn.setTextColor(Color.WHITE);
        shareJsonBtn.setOnClickListener(v -> exportJSON(true));
        main.addView(shareJsonBtn);

        Button exportPdfBtn = new Button(this);
        exportPdfBtn.setText("Export as PDF");
        exportPdfBtn.setBackgroundColor(Color.parseColor("#00CC44"));
        exportPdfBtn.setTextColor(Color.WHITE);
        exportPdfBtn.setOnClickListener(v -> exportPDF(false));
        main.addView(exportPdfBtn);

        Button sharePdfBtn = new Button(this);
        sharePdfBtn.setText("Export & Share PDF");
        sharePdfBtn.setBackgroundColor(Color.parseColor("#0099FF"));
        sharePdfBtn.setTextColor(Color.WHITE);
        sharePdfBtn.setOnClickListener(v -> exportPDF(true));
        main.addView(sharePdfBtn);

        Button backBtn = new Button(this);
        backBtn.setText("< Back");
        backBtn.setBackgroundColor(Color.parseColor("#333333"));
        backBtn.setTextColor(Color.WHITE);
        backBtn.setOnClickListener(v -> finish());
        main.addView(backBtn);

        setContentView(scroll);
    }

    private void exportCSV(boolean share) {
        try {
            JSONArray logs = logger.getLogs();
            StringBuilder sb = new StringBuilder();
            sb.append("Date,Time,Grade,Ping(ms),Data(KB),Signal(dBm)\n");
            for (int i = 0; i < logs.length(); i++) {
                JSONObject obj = logs.getJSONObject(i);
                sb.append(obj.optString("date", "")).append(",")
                  .append(obj.optString("time", "")).append(",")
                  .append(obj.optString("grade", "")).append(",")
                  .append(obj.optLong("ping", 0)).append(",")
                  .append(obj.optLong("data", 0)).append(",")
                  .append(obj.optInt("signal", 0)).append("\n");
            }

            File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            File file = new File(dir, "TrueNetwork_export.csv");
            FileWriter fw = new FileWriter(file);
            fw.write(sb.toString());
            fw.close();

            if (share) {
                shareFile(file, "text/csv");
            } else {
                Toast.makeText(this, "Exported to Downloads/TrueNetwork_export.csv", Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Export failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void exportJSON(boolean share) {
        try {
            JSONArray logs = logger.getLogs();
            File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            File file = new File(dir, "TrueNetwork_export.json");
            FileWriter fw = new FileWriter(file);
            fw.write(logs.toString(2));
            fw.close();

            if (share) {
                shareFile(file, "application/json");
            } else {
                Toast.makeText(this, "Exported to Downloads/TrueNetwork_export.json", Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Export failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void exportPDF(boolean share) {
        try {
            JSONArray logs = logger.getLogs();
            PdfDocument document = new PdfDocument();
            Paint paint = new Paint();
            Paint titlePaint = new Paint();
            titlePaint.setTextSize(16);
            titlePaint.setFakeBoldText(true);
            paint.setTextSize(11);

            int pageWidth = 595, pageHeight = 842;
            int rowsPerPage = 45;
            int totalRows = logs.length();
            int pageCount = Math.max(1, (int) Math.ceil(totalRows / (double) rowsPerPage));

            for (int p = 0; p < pageCount; p++) {
                PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, p + 1).create();
                PdfDocument.Page page = document.startPage(pageInfo);
                Canvas canvas = page.getCanvas();
                int y = 40;
                canvas.drawText("True Network - Export Report (Page " + (p + 1) + "/" + pageCount + ")", 30, y, titlePaint);
                y += 30;
                canvas.drawText(String.format("%-10s %-10s %-8s %-8s %-8s", "Date", "Time", "Grade", "Ping", "Data"), 30, y, paint);
                y += 16;

                int start = p * rowsPerPage;
                int end = Math.min(start + rowsPerPage, totalRows);
                for (int i = start; i < end; i++) {
                    JSONObject obj = logs.getJSONObject(i);
                    String row = String.format("%-10s %-10s %-8s %-8s %-8s",
                            obj.optString("date", ""),
                            obj.optString("time", ""),
                            obj.optString("grade", ""),
                            obj.optLong("ping", 0) + "ms",
                            obj.optLong("data", 0) + "KB");
                    canvas.drawText(row, 30, y, paint);
                    y += 14;
                }
                document.finishPage(page);
            }

            File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            File file = new File(dir, "TrueNetwork_export.pdf");
            document.writeTo(new java.io.FileOutputStream(file));
            document.close();

            if (share) {
                shareFile(file, "application/pdf");
            } else {
                Toast.makeText(this, "Exported to Downloads/TrueNetwork_export.pdf", Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "PDF export failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void shareFile(File file, String mimeType) {
        try {
            android.net.Uri uri = FileProvider.getUriForFile(this, "com.my.netindicator.fileprovider", file);
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType(mimeType);
            shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(shareIntent, "Share network data"));
        } catch (Exception e) {
            Toast.makeText(this, "Share failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}
