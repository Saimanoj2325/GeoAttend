package com.geoattend.admin;

import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.os.Bundle;
import android.os.Environment;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.geoattend.databinding.ActivityAttendanceAnalyticsBinding;
import com.geoattend.model.AttendanceRecord;
import com.geoattend.utils.FirebaseHelper;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.PercentFormatter;
import com.google.firebase.firestore.DocumentSnapshot;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.geoattend.R;
import com.geoattend.utils.AdminNavigationHelper;

public class AttendanceAnalyticsActivity extends AppCompatActivity {
    private ActivityAttendanceAnalyticsBinding binding;
    private List<AttendanceRecord> allRecords = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAttendanceAnalyticsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.btnBack.setOnClickListener(v -> finish());
        binding.btnExportPdf.setOnClickListener(v -> exportToPDF());

        AdminNavigationHelper.init(this, R.id.nav_analytics);

        setupLineChart();
        setupPieChart();
        loadData();
    }

    private void setupLineChart() {
        binding.lineChart.getDescription().setEnabled(false);
        binding.lineChart.getLegend().setTextColor(Color.WHITE);
        binding.lineChart.getXAxis().setTextColor(Color.WHITE);
        binding.lineChart.getAxisLeft().setTextColor(Color.WHITE);
        binding.lineChart.getAxisRight().setEnabled(false);
        binding.lineChart.setNoDataText("Loading data...");
        binding.lineChart.setNoDataTextColor(Color.WHITE);
    }

    private void setupPieChart() {
        binding.pieChart.getDescription().setEnabled(false);
        binding.pieChart.setUsePercentValues(true);
        binding.pieChart.setHoleColor(Color.TRANSPARENT);
        binding.pieChart.setCenterTextColor(Color.WHITE);
        binding.pieChart.getLegend().setTextColor(Color.WHITE);
    }

    private void loadData() {
        FirebaseHelper.getAttendanceRef().get().addOnSuccessListener(query -> {
            allRecords.clear();
            for (DocumentSnapshot doc : query.getDocuments()) {
                AttendanceRecord r = doc.toObject(AttendanceRecord.class);
                if (r != null) allRecords.add(r);
            }
            updateCharts();
        });
    }

    private void updateCharts() {
        // Line Chart: Attendance per day for last 7 days
        Map<Integer, Integer> dayCounts = new HashMap<>();
        Calendar cal = Calendar.getInstance();
        for (int i = 0; i < 7; i++) {
            dayCounts.put(cal.get(Calendar.DAY_OF_YEAR), 0);
            cal.add(Calendar.DAY_OF_YEAR, -1);
        }

        for (AttendanceRecord r : allRecords) {
            if (r.getTimestamp() == null || !"IN".equals(r.getType())) continue;
            Calendar rCal = Calendar.getInstance();
            rCal.setTime(r.getTimestamp().toDate());
            int day = rCal.get(Calendar.DAY_OF_YEAR);
            if (dayCounts.containsKey(day)) {
                dayCounts.put(day, dayCounts.get(day) + 1);
            }
        }

        List<Entry> lineEntries = new ArrayList<>();
        cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, -6);
        for (int i = 0; i < 7; i++) {
            int day = cal.get(Calendar.DAY_OF_YEAR);
            lineEntries.add(new Entry(i, dayCounts.getOrDefault(day, 0)));
            cal.add(Calendar.DAY_OF_YEAR, 1);
        }

        LineDataSet lineDataSet = new LineDataSet(lineEntries, "Check-ins");
        lineDataSet.setColor(Color.parseColor("#3B82F6"));
        lineDataSet.setCircleColor(Color.parseColor("#3B82F6"));
        lineDataSet.setLineWidth(2f);
        lineDataSet.setValueTextColor(Color.WHITE);
        binding.lineChart.setData(new LineData(lineDataSet));
        binding.lineChart.invalidate();

        // Pie Chart: Status distribution
        int present = 0, flagged = 0, rejected = 0;
        for (AttendanceRecord r : allRecords) {
            if (r.getStatus() == null) continue;
            switch (r.getStatus()) {
                case "PRESENT": present++; break;
                case "FLAGGED": flagged++; break;
                case "REJECTED": rejected++; break;
            }
        }

        List<PieEntry> pieEntries = new ArrayList<>();
        if (present > 0) pieEntries.add(new PieEntry(present, "Present"));
        if (flagged > 0) pieEntries.add(new PieEntry(flagged, "Flagged"));
        if (rejected > 0) pieEntries.add(new PieEntry(rejected, "Rejected"));

        PieDataSet pieDataSet = new PieDataSet(pieEntries, "Attendance Status");
        pieDataSet.setColors(new int[]{Color.parseColor("#10B981"), Color.parseColor("#F59E0B"), Color.parseColor("#EF4444")});
        pieDataSet.setValueTextColor(Color.WHITE);
        pieDataSet.setValueTextSize(12f);
        
        PieData pieData = new PieData(pieDataSet);
        pieData.setValueFormatter(new PercentFormatter(binding.pieChart));
        binding.pieChart.setData(pieData);
        binding.pieChart.invalidate();
    }

    private void exportToPDF() {
        PdfDocument document = new PdfDocument();
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 842, 1).create();
        PdfDocument.Page page = document.startPage(pageInfo);
        
        android.graphics.Canvas canvas = page.getCanvas();
        Paint paint = new Paint();
        
        paint.setTextSize(18f);
        paint.setFakeBoldText(true);
        canvas.drawText("GeoAttend - Employee Attendance Report", 50, 50, paint);
        
        paint.setTextSize(12f);
        paint.setFakeBoldText(false);
        int y = 100;
        
        canvas.drawText("Date", 50, y, paint);
        canvas.drawText("Employee Name", 150, y, paint);
        canvas.drawText("Type", 350, y, paint);
        canvas.drawText("Status", 450, y, paint);
        
        y += 20;
        canvas.drawLine(50, y, 550, y, paint);
        y += 20;

        for (AttendanceRecord r : allRecords) {
            if (y > 800) break; // Simple page break logic omitted for brevity
            String date = r.getTimestamp() != null ? r.getTimestamp().toDate().toString() : "N/A";
            canvas.drawText(date.substring(0, 10), 50, y, paint);
            canvas.drawText(r.getUserName() != null ? r.getUserName() : "Unknown", 150, y, paint);
            canvas.drawText(r.getType() != null ? r.getType() : "N/A", 350, y, paint);
            canvas.drawText(r.getStatus() != null ? r.getStatus() : "N/A", 450, y, paint);
            y += 20;
        }

        document.finishPage(page);

        File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        File file = new File(downloadsDir, "AttendanceReport_" + System.currentTimeMillis() + ".pdf");

        try {
            document.writeTo(new FileOutputStream(file));
            Toast.makeText(this, "PDF Exported to Downloads", Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error exporting PDF: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }

        document.close();
    }
}
