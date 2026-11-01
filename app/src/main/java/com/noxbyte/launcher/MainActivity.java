package com.noxbyte.launcher;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.imageview.ShapeableImageView;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    static class AppInfo {
        final String label, pkg;
        final Drawable icon;
        AppInfo(String l, Drawable i, String p) { label = l; icon = i; pkg = p; }
    }

    static class VH extends RecyclerView.ViewHolder {
        final ShapeableImageView icon;
        final TextView label;
        VH(View v) {
            super(v);
            icon = v.findViewById(R.id.icon);
            label = v.findViewById(R.id.label);
        }
    }

    private final List<AppInfo> apps = new ArrayList<>();
    private final Handler handler = new Handler();
    private TextView clock, dateView;
    private SimpleDateFormat timeFmt = new SimpleDateFormat("HH:mm", Locale.getDefault());
    private SimpleDateFormat dateFmt = new SimpleDateFormat("EEEE d MMMM", Locale.getDefault());
    private final Runnable tick = new Runnable() {
        @Override public void run() { updateClock(); handler.postDelayed(this, 15000); }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        clock = findViewById(R.id.clock);
        dateView = findViewById(R.id.date);
        RecyclerView grid = findViewById(R.id.grid);
        grid.setLayoutManager(new GridLayoutManager(this, 4));
        loadApps();
        grid.setAdapter(new AppAdapter());
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateClock();
        handler.postDelayed(tick, 15000);
    }

    @Override
    protected void onPause() {
        handler.removeCallbacks(tick);
        super.onPause();
    }

    private void updateClock() {
        Date now = new Date();
        clock.setText(timeFmt.format(now));
        dateView.setText(dateFmt.format(now));
    }

    private void loadApps() {
        apps.clear();
        PackageManager pm = getPackageManager();
        Intent main = new Intent(Intent.ACTION_MAIN);
        main.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> ris = pm.queryIntentActivities(main, 0);
        String self = getPackageName();
        for (ResolveInfo r : ris) {
            String pkg = r.activityInfo != null ? r.activityInfo.packageName : r.resolvePackageName;
            if (pkg == null || pkg.equals(self)) continue;
            Intent launch = pm.getLaunchIntentForPackage(pkg);
            if (launch == null) continue;
            apps.add(new AppInfo(
                    r.loadLabel(pm).toString(),
                    r.loadIcon(pm),
                    pkg));
        }
        Collections.sort(apps, (a, b) -> a.label.compareToIgnoreCase(b.label));
    }

    private class AppAdapter extends RecyclerView.Adapter<VH> {
        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_app, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int position) {
            final AppInfo app = apps.get(position);
            h.icon.setImageDrawable(app.icon);
            h.label.setText(app.label);
            h.itemView.setOnClickListener(v -> {
                Intent launch = getPackageManager().getLaunchIntentForPackage(app.pkg);
                if (launch != null) {
                    launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(launch);
                }
            });
            h.itemView.setOnLongClickListener(v -> {
                Intent info = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.parse("package:" + app.pkg));
                info.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(info);
                return true;
            });
        }

        @Override
        public int getItemCount() { return apps.size(); }
    }
}
