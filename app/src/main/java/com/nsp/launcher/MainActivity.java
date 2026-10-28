package com.nsp.launcher;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Set;

public class MainActivity extends Activity {

    private static final String PREFS = "nsp";

    private static final int C_BG_DARK = 0xFF000000;
    private static final int C_BG_LIGHT = 0xFFFFFFFF;
    private static final int C_TEXT_DARK = 0xFFFFFFFF;
    private static final int C_TEXT_LIGHT = 0xFF111111;
    private static final int C_SEC_DARK = 0xFF9E9E9E;
    private static final int C_SEC_LIGHT = 0xFF666666;
    private static final int C_ACCENT = 0xFF1BA1E2;
    private static final int C_WHITE = 0xFFFFFFFF;
    private static final int COLUMNS = 5;
    private static final int TILE_DP = 104;

    private SharedPreferences prefs;
    private boolean isLight;
    private AppRepository repo;
    private final Handler ui = new Handler(Looper.getMainLooper());

    private FrameLayout root;
    private LinearLayout content;
    private final ArrayList<Section> sections = new ArrayList<Section>();
    private ArrayList<AppEntry> apps = new ArrayList<AppEntry>();

    private int focusSection = 0;
    private int focusIndex = 0;

    private boolean dialogOpen = false;
    private boolean scanning = false;
    private int stickDir = 0;
    private long lastStickTime = 0;
    private String pendingFocus = null;

    private Dashboard dashboard;
    private WeatherData weather;
    private boolean resumed = false;

    private LinearLayout btcChip;
    private TextView btcValue;
    private TextView btcStatus;
    private ImageView settingsGear;
    private ScrollView mainScroll;
    private boolean btcLoading;
    private String btcError;
    private final ArrayList<View> headerItems = new ArrayList<View>();
    private boolean headerFocus;
    private int headerPos;
    private Tile agendaTile;
    private TextView agendaLine1;
    private TextView agendaLine2;
    private TextView newsLine1;
    private TextView newsLine2;
    private boolean newsLoading;
    private ImageView searchIcon;
    private CaseView caseMark;
    private TextView updateChip;
    private String updateVersion;
    private boolean updateChecking;
    private boolean weatherLoading = false;
    private boolean recentsDirty = false;
    private BroadcastReceiver packageReceiver;
    private IconStore iconStore;
    private Bitmap customBackgroundBitmap;
    private Tile notificationTile;

    private ClockView headerClock;
    private TextView headerDate;
    private TextView headerTime;
    private TextView headerWeekday;
    private TextView headerDay;
    private TextView headerMonth;
    private int clockStyle;
    private TextView hintOverlay;
    private boolean hintShown;
    private BroadcastReceiver batteryReceiver;
    private int batteryPercent = 0;
    private boolean batteryCharging;
    private String batteryStatus = "";
    private final SimpleDateFormat headerDateFormat =
            new SimpleDateFormat("EEEE d 'de' MMMM", new Locale("es", "ES"));
    private final SimpleDateFormat headerTimeFormat =
            new SimpleDateFormat("HH:mm", new Locale("es", "ES"));
    private final SimpleDateFormat headerTimeFormatSeconds =
            new SimpleDateFormat("HH:mm:ss", new Locale("es", "ES"));
    private final SimpleDateFormat headerWeekdayFormat =
            new SimpleDateFormat("EEEE", new Locale("es", "ES"));
    private final SimpleDateFormat headerMonthFormat =
            new SimpleDateFormat("MMMM", new Locale("es", "ES"));

    private final Runnable packageReload = new Runnable() {
        public void run() {
            if (!scanning && !dialogOpen) scanApps(false);
        }
    };

    private static class Section {
        String title;
        ArrayList<Tile> tiles = new ArrayList<Tile>();
        int lastIndex = 0;
        LinearLayout container;
        LinearLayout row;
        LinearLayout grid;
        TextView header;
    }

    private static class Tile {
        AppEntry entry;
        Runnable action;
        View view;
        ImageView icon;
        TextView label;
        TextView badge;
        GradientDrawable normal;
        GradientDrawable focused;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        boolean effectiveLight = BackgroundManager.effectiveLight(this);
        if (prefs.getBoolean("light", false) != effectiveLight) {
            prefs.edit().putBoolean("light", effectiveLight).apply();
        }
        isLight = effectiveLight;
        clockStyle = prefs.getInt("clock_style", 0);
        setTheme(isLight ? R.style.AppThemeLight : R.style.AppThemeDark);
        super.onCreate(savedInstanceState);

        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        repo = new AppRepository(this);
        iconStore = new IconStore(this);
        weather = WeatherService.loadCached(this);

        hintOverlay = new TextView(this);
        hintOverlay.setText("A abrir  |  X favorito  |  Y mover  |  SELECT tema  |  START recargar");
        hintOverlay.setTextSize(9);
        hintOverlay.setTextColor(C_WHITE);
        hintOverlay.setLetterSpacing(0.06f);
        hintOverlay.setGravity(Gravity.CENTER);
        hintOverlay.setPadding(dp(14), dp(8), dp(14), dp(8));
        GradientDrawable pill = new GradientDrawable();
        pill.setShape(GradientDrawable.RECTANGLE);
        pill.setColor(0xCC101010);
        pill.setCornerRadius(dp(18));
        hintOverlay.setBackgroundDrawable(pill);
        hintOverlay.setVisibility(View.GONE);
        if (savedInstanceState != null) {
            pendingFocus = savedInstanceState.getString("focusPkg");
        }

        root = new FrameLayout(this);
        root.setBackgroundColor(bgColor());
        root.setClipChildren(false);
        setContentView(root);

        applyImmersive();

        ArrayList<AppEntry> cached = repo.readCache();
        if (!cached.isEmpty()) {
            apps = cached;
            String focus = pendingFocus;
            pendingFocus = null;
            buildUi(cached, focus);
            scanApps(false);
        } else {
            showLoading();
            scanApps(true);
        }
    }

    private int dp(float value) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value,
                getResources().getDisplayMetrics()));
    }

    private int bgColor() {
        return isLight ? C_BG_LIGHT : C_BG_DARK;
    }

    private int textColor() {
        return isLight ? C_TEXT_LIGHT : C_TEXT_DARK;
    }

    private int secondaryColor() {
        return isLight ? C_SEC_LIGHT : C_SEC_DARK;
    }

    private void applyImmersive() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) applyImmersive();
    }

    private void showLoading() {
        root.removeAllViews();
        TextView t = new TextView(this);
        t.setText("CARGANDO APLICACIONES");
        t.setTextSize(13);
        t.setLetterSpacing(0.25f);
        t.setTextColor(C_ACCENT);
        t.setGravity(Gravity.CENTER);
        root.addView(t, new FrameLayout.LayoutParams(-1, -1));
    }

    private void scanApps(final boolean initial) {
        if (scanning) return;
        scanning = true;
        new Thread(new Runnable() {
            public void run() {
                final ArrayList<AppEntry> list = repo.load();
                ui.post(new Runnable() {
                    public void run() {
                        scanning = false;
                        applyScan(list, initial);
                    }
                });
            }
        }).start();
    }

    private void applyScan(ArrayList<AppEntry> list, boolean initial) {
        for (AppEntry e : list) {
            AppEntry old = findApp(apps, e.pkg);
            if (old != null && old.updated != e.updated) {
                iconStore.invalidate(e.pkg);
            }
        }

        repo.writeCache(list);

        boolean changed = !sameApps(apps, list);
        if (changed || apps.isEmpty()) {
            String focus = focusedPackage();
            if (focus == null) focus = pendingFocus;
            pendingFocus = null;
            buildUi(list, focus);
        }
    }

    private AppEntry findApp(ArrayList<AppEntry> list, String pkg) {
        for (AppEntry e : list) {
            if (e.pkg.equals(pkg)) return e;
        }
        return null;
    }

    private boolean sameApps(ArrayList<AppEntry> a, ArrayList<AppEntry> b) {
        if (a.size() != b.size()) return false;
        for (int i = 0; i < a.size(); i++) {
            AppEntry x = a.get(i);
            AppEntry y = b.get(i);
            if (!x.pkg.equals(y.pkg) || !x.activity.equals(y.activity)
                    || !x.label.equals(y.label) || x.updated != y.updated) {
                return false;
            }
        }
        return true;
    }

    private void reloadApps() {
        if (scanning) return;
        scanApps(false);
    }

    private void buildUi(ArrayList<AppEntry> list, final String focusPkg) {
        apps = list;
        sections.clear();
        if (dashboard != null) {
            dashboard.stop();
            dashboard = null;
        }
        root.removeAllViews();

        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(24), dp(6), dp(24), dp(6));

        TextView title = new TextView(this);
        title.setText("NSP");
        title.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));
        title.setTextSize(28);
        title.setLetterSpacing(0.04f);
        title.setTextColor(textColor());
        title.setSingleLine(true);
        header.addView(title, new LinearLayout.LayoutParams(0, -2, 1f));

        LinearLayout headerRight = new LinearLayout(this);
        headerRight.setOrientation(LinearLayout.HORIZONTAL);
        headerRight.setGravity(Gravity.CENTER_VERTICAL);

        headerItems.clear();
        headerFocus = false;
        buildBtcChip(headerRight);

        if (clockStyle == 2) {
            LinearLayout dateBlock = new LinearLayout(this);
            dateBlock.setOrientation(LinearLayout.VERTICAL);
            dateBlock.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);

            headerWeekday = new TextView(this);
            headerWeekday.setTextSize(9);
            headerWeekday.setTextColor(isLight ? 0xFF0050EF : 0xFF1BA1E2);
            headerWeekday.setLetterSpacing(0.12f);
            headerWeekday.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
            headerWeekday.setSingleLine(true);
            dateBlock.addView(headerWeekday, new LinearLayout.LayoutParams(-2, -2));

            headerDay = new TextView(this);
            headerDay.setTextSize(28);
            headerDay.setTextColor(textColor());
            headerDay.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));
            headerDay.setSingleLine(true);
            dateBlock.addView(headerDay, new LinearLayout.LayoutParams(-2, -2));

            headerMonth = new TextView(this);
            headerMonth.setTextSize(9);
            headerMonth.setTextColor(secondaryColor());
            headerMonth.setLetterSpacing(0.12f);
            headerMonth.setSingleLine(true);
            dateBlock.addView(headerMonth, new LinearLayout.LayoutParams(-2, -2));

            headerRight.addView(dateBlock, new LinearLayout.LayoutParams(-2, -2));
        } else {
            LinearLayout headerText = new LinearLayout(this);
            headerText.setOrientation(LinearLayout.VERTICAL);
            headerText.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);

            headerDate = new TextView(this);
            headerDate.setTextSize(9);
            headerDate.setTextColor(secondaryColor());
            headerDate.setLetterSpacing(0.10f);
            headerDate.setSingleLine(true);
            headerText.addView(headerDate, new LinearLayout.LayoutParams(-2, -2));

            if (clockStyle == 1) {
                headerTime = new TextView(this);
                headerTime.setTextSize(22);
                headerTime.setTextColor(textColor());
                headerTime.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));
                headerTime.setSingleLine(true);
                headerText.addView(headerTime, new LinearLayout.LayoutParams(-2, -2));
            }

            headerRight.addView(headerText, new LinearLayout.LayoutParams(-2, -2));

            if (clockStyle == 0) {
                headerClock = new ClockView(this);
                headerClock.setColors(secondaryColor(), textColor(),
                        isLight ? 0xFF0050EF : 0xFF1BA1E2);
                headerClock.setShowSeconds(prefs.getBoolean("clock_seconds", false));
                headerClock.setRunning(true);
                LinearLayout.LayoutParams clockLp = new LinearLayout.LayoutParams(dp(44), dp(44));
                clockLp.leftMargin = dp(8);
                headerRight.addView(headerClock, clockLp);
            }
        }

        buildSearchButton(headerRight);
        buildUpdateChip(headerRight);
        buildSettingsGear(headerRight);
        buildCaseMark(headerRight);

        header.addView(headerRight, new LinearLayout.LayoutParams(-2, -2));

        page.addView(header, new LinearLayout.LayoutParams(-1, -2));

        ScrollView scroll = new ScrollView(this);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.setClipChildren(false);
        scroll.setClipToPadding(false);
        mainScroll = scroll;
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setClipChildren(false);
        content.setClipToPadding(false);
        scroll.addView(content, new LinearLayout.LayoutParams(-1, -2));
        page.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1f));

        int backgroundMode = BackgroundManager.mode(this);
        if (backgroundMode != BackgroundManager.MODE_NONE) {
            ImageView background = new ImageView(this);
            Bitmap custom = null;
            if (backgroundMode == BackgroundManager.MODE_CUSTOM) {
                if (customBackgroundBitmap == null) {
                    customBackgroundBitmap = BackgroundManager.load(this);
                }
                custom = customBackgroundBitmap;
            }
            if (custom != null) {
                background.setImageBitmap(custom);
            } else {
                background.setImageResource(isLight ? R.drawable.bg_light : R.drawable.bg_dark);
            }
            background.setScaleType(ImageView.ScaleType.CENTER_CROP);
            root.addView(background, new FrameLayout.LayoutParams(-1, -1));

            View scrim = new View(this);
            scrim.setBackgroundColor(isLight ? 0xB3FFFFFF : 0x99000000);
            root.addView(scrim, new FrameLayout.LayoutParams(-1, -1));
        }

        root.addView(page, new FrameLayout.LayoutParams(-1, -1));

        if (hintOverlay != null) {
            FrameLayout.LayoutParams hintLp = new FrameLayout.LayoutParams(-2, -2,
                    Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
            hintLp.bottomMargin = dp(18);
            root.addView(hintOverlay, hintLp);
            if (!prefs.getBoolean("hint_shown", false)) {
                prefs.edit().putBoolean("hint_shown", true).apply();
                showHint();
            }
        }

        addSystemSection();
        addRecentSection();

        ArrayList<AppEntry> favs = new ArrayList<AppEntry>();
        for (AppEntry e : apps) {
            if (repo.isFavorite(e.pkg)) favs.add(e);
        }
        addSection(AppRepository.FAV, favs);

        for (String cat : AppRepository.CATS) {
            ArrayList<AppEntry> catApps = new ArrayList<AppEntry>();
            for (AppEntry e : apps) {
                if (cat.equals(e.category)) catApps.add(e);
            }
            addSection(cat, catApps);
        }

        root.post(new Runnable() {
            public void run() {
                boolean restored = false;
                if (focusPkg != null) restored = focusByPackage(focusPkg);
                if (!restored) {
                    focusFirstApp();
                    scrollToTop();
                }
            }
        });

        if (dashboard != null) {
            dashboard.setWeather(weather);
            dashboard.setBattery(batteryPercent, batteryCharging, batteryStatus);
            dashboard.refreshNow();
            if (resumed) dashboard.start();
        }
        maybeRefreshWeather();
    }

    private void addSystemSection() {
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);

        TextView header = new TextView(this);
        header.setText("SISTEMA");
        header.setTextSize(12);
        header.setLetterSpacing(0.2f);
        header.setTextColor(secondaryColor());
        header.setPadding(dp(24), dp(14), dp(24), dp(2));
        container.addView(header, new LinearLayout.LayoutParams(-1, -2));

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setClipChildren(false);
        row.setClipToPadding(false);
        row.setPadding(dp(20), dp(4), dp(24), dp(12));

        LinearLayout topRow = new LinearLayout(this);
        topRow.setOrientation(LinearLayout.HORIZONTAL);

        Section section = new Section();
        section.title = "Sistema";

        notificationTile = makeSystemTile("Avisos", R.mipmap.ic_notifications, 0xFF825A2C,
                new Runnable() {
                    public void run() {
                        showNotificationDialog();
                    }
                });
        LinearLayout.LayoutParams notificationParams = new LinearLayout.LayoutParams(dp(96), dp(96));
        notificationParams.rightMargin = dp(6);
        topRow.addView(notificationTile.view, notificationParams);
        section.tiles.add(notificationTile);
        updateNotificationBadge();

        dashboard = new Dashboard(this, isLight,
                new Runnable() {
                    public void run() {
                        fetchWeather(true);
                    }
                });
        topRow.addView(dashboard, new LinearLayout.LayoutParams(0, dp(96), 1f));
        row.addView(topRow, new LinearLayout.LayoutParams(-1, dp(96)));

        LinearLayout bottomRow = new LinearLayout(this);
        bottomRow.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams agendaParams = new LinearLayout.LayoutParams(0, dp(96), .85f);
        agendaParams.topMargin = dp(6); agendaParams.rightMargin = dp(6);
        Tile agenda = makeAgendaTile(new Runnable() { public void run() { openCalendarApp(); } });
        bottomRow.addView(agenda.view, agendaParams); section.tiles.add(agenda); agendaTile = agenda;
        Tile news = makeNewsTile();
        LinearLayout.LayoutParams newsParams = new LinearLayout.LayoutParams(0, dp(96), 1.15f);
        newsParams.topMargin = dp(6);
        bottomRow.addView(news.view, newsParams); section.tiles.add(news);
        row.addView(bottomRow, new LinearLayout.LayoutParams(-1, dp(102)));
        refreshAgenda();
        refreshNewsPreview();

        container.addView(row, new LinearLayout.LayoutParams(-1, -2));
        content.addView(container, new LinearLayout.LayoutParams(-1, -2));

        section.container = container;
        section.header = header;
        section.row = row;
        sections.add(section);
    }

    private Section createAppSection(String title) {
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);

        TextView header = new TextView(this);
        header.setText(title.toUpperCase(new Locale("es", "ES")));
        header.setTextSize(12);
        header.setLetterSpacing(0.2f);
        header.setTextColor(secondaryColor());
        header.setPadding(dp(24), dp(14), dp(24), dp(2));
        container.addView(header, new LinearLayout.LayoutParams(-1, -2));

        LinearLayout grid = new LinearLayout(this);
        grid.setOrientation(LinearLayout.VERTICAL);
        grid.setClipChildren(false);
        grid.setClipToPadding(false);
        grid.setPadding(dp(20), dp(4), dp(24), dp(12));
        container.addView(grid, new LinearLayout.LayoutParams(-1, -2));

        Section section = new Section();
        section.title = title;
        section.container = container;
        section.header = header;
        section.grid = grid;
        return section;
    }

    private void insertSection(Section section, int index) {
        if (index < 0) index = 0;
        if (index > sections.size()) index = sections.size();
        content.addView(section.container, index);
        sections.add(index, section);
    }

    private void addSection(String title, ArrayList<AppEntry> list) {
        if (list.isEmpty()) return;
        Section section = createAppSection(title);
        int color = AppRepository.colorFor(title);
        for (AppEntry e : list) {
            section.tiles.add(makeTile(e, color));
        }
        buildGrid(section);
        insertSection(section, sections.size());
    }

    private void addRecentSection() {
        ArrayList<AppEntry> recents = resolveRecents();
        if (recents.isEmpty()) return;
        Section section = createAppSection(AppRepository.RECENT);
        int color = AppRepository.colorFor(AppRepository.RECENT);
        for (AppEntry entry : recents) {
            section.tiles.add(makeTile(entry, color));
        }
        buildGrid(section);
        insertSection(section, 1);
    }

    private ArrayList<AppEntry> resolveRecents() {
        ArrayList<AppEntry> out = new ArrayList<AppEntry>();
        ArrayList<String> packages = repo.recentPackages();
        for (String pkg : packages) {
            AppEntry entry = findApp(apps, pkg);
            if (entry != null) out.add(entry);
            if (out.size() >= 8) break;
        }
        return out;
    }

    private void buildGrid(Section section) {
        if (section.grid == null) return;
        for (Tile tile : section.tiles) {
            ViewParent parent = tile.view.getParent();
            if (parent instanceof ViewGroup) {
                ((ViewGroup) parent).removeView(tile.view);
            }
        }
        section.grid.removeAllViews();
        for (int i = 0; i < section.tiles.size(); i += COLUMNS) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setClipChildren(false);
            row.setClipToPadding(false);
            for (int j = i; j < section.tiles.size() && j < i + COLUMNS; j++) {
                LinearLayout.LayoutParams params =
                        new LinearLayout.LayoutParams(dp(TILE_DP), dp(TILE_DP));
                params.rightMargin = dp(8);
                row.addView(section.tiles.get(j).view, params);
            }
            LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(-1, -2);
            rowParams.bottomMargin = dp(8);
            section.grid.addView(row, rowParams);
        }
    }

    private void insertTile(Section section, AppEntry entry, boolean append) {
        int color = AppRepository.colorFor(section.title);
        Tile tile = makeTile(entry, color);
        if (append) {
            section.tiles.add(tile);
        } else {
            int position = section.tiles.size();
            for (int i = 0; i < section.tiles.size(); i++) {
                if (entry.label.compareToIgnoreCase(section.tiles.get(i).entry.label) < 0) {
                    position = i;
                    break;
                }
            }
            section.tiles.add(position, tile);
        }
        buildGrid(section);
    }

    private Section findSection(String title) {
        for (Section section : sections) {
            if (section.title.equals(title)) return section;
        }
        return null;
    }

    private Tile findTile(Section section, String pkg) {
        for (Tile tile : section.tiles) {
            if (tile.entry != null && tile.entry.pkg.equals(pkg)) return tile;
        }
        return null;
    }

    private void removeTile(Section section, Tile tile) {
        section.tiles.remove(tile);
        if (section.tiles.isEmpty()) {
            removeSection(section);
        } else {
            buildGrid(section);
        }
    }

    private void removeSection(Section section) {
        int index = sections.indexOf(section);
        if (index < 0) return;
        content.removeView(section.container);
        sections.remove(index);
        if (focusSection > index) {
            focusSection--;
        } else if (focusSection == index) {
            focusSection = Math.max(0, index - 1);
        }
    }

    private void restoreFocus(String pkg) {
        if (pkg != null && focusByPackage(pkg)) return;
        focusFirstApp();
    }

    private void addFavoriteInPlace(AppEntry entry) {
        Section favorites = findSection(AppRepository.FAV);
        if (favorites == null) {
            favorites = createAppSection(AppRepository.FAV);
            int position = 1;
            if (findSection(AppRepository.RECENT) != null) position = 2;
            insertSection(favorites, position);
        }
        if (findTile(favorites, entry.pkg) == null) {
            insertTile(favorites, entry, false);
        }
    }

    private void removeFavoriteInPlace(AppEntry entry) {
        Section favorites = findSection(AppRepository.FAV);
        if (favorites == null) return;
        Tile tile = findTile(favorites, entry.pkg);
        if (tile != null) removeTile(favorites, tile);
    }

    private int categoryRank(String title) {
        for (int i = 0; i < AppRepository.CATS.length; i++) {
            if (AppRepository.CATS[i].equals(title)) return i;
        }
        return -1;
    }

    private int categoryInsertPosition(String category) {
        int rank = categoryRank(category);
        for (int i = 0; i < sections.size(); i++) {
            int other = categoryRank(sections.get(i).title);
            if (other >= 0 && other > rank) return i;
        }
        return sections.size();
    }

    private void moveCategoryInPlace(AppEntry entry, String oldCategory, String newCategory) {
        Section oldSection = findSection(oldCategory);
        if (oldSection != null) {
            Tile oldTile = findTile(oldSection, entry.pkg);
            if (oldTile != null) removeTile(oldSection, oldTile);
        }
        Section target = findSection(newCategory);
        if (target == null) {
            target = createAppSection(newCategory);
            insertSection(target, categoryInsertPosition(newCategory));
        }
        if (findTile(target, entry.pkg) == null) {
            insertTile(target, entry, false);
        }
    }

    private void refreshRecents() {
        recentsDirty = false;
        if (apps.isEmpty()) return;
        String focused = focusedPackage();
        Section old = findSection(AppRepository.RECENT);
        if (old != null) removeSection(old);
        ArrayList<AppEntry> recents = resolveRecents();
        if (!recents.isEmpty()) {
            Section section = createAppSection(AppRepository.RECENT);
            int color = AppRepository.colorFor(AppRepository.RECENT);
            for (AppEntry entry : recents) {
                section.tiles.add(makeTile(entry, color));
            }
            buildGrid(section);
            insertSection(section, 1);
        }
        restoreFocus(focused);
    }

    private Tile makeTile(AppEntry e, int color) {
        final Tile t = new Tile();
        t.entry = e;

        t.normal = new GradientDrawable();
        t.normal.setShape(GradientDrawable.RECTANGLE);
        t.normal.setColor(color);

        t.focused = new GradientDrawable();
        t.focused.setShape(GradientDrawable.RECTANGLE);
        t.focused.setColor(color);
        t.focused.setStroke(dp(3), isLight ? 0xFF111111 : C_WHITE);

        FrameLayout tile = new FrameLayout(this);
        tile.setBackgroundDrawable(t.normal);
        tile.setFocusable(true);
        tile.setFocusableInTouchMode(true);
        tile.setClickable(true);
        tile.setClipChildren(false);
        tile.setClipToPadding(false);

        ImageView icon = new ImageView(this);
        icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
        FrameLayout.LayoutParams iconLp = new FrameLayout.LayoutParams(dp(42), dp(42));
        iconLp.gravity = Gravity.TOP | Gravity.LEFT;
        iconLp.leftMargin = dp(11);
        iconLp.topMargin = dp(10);
        tile.addView(icon, iconLp);

        TextView label = new TextView(this);
        label.setText(e.label);
        label.setTextSize(11);
        label.setTextColor(C_WHITE);
        label.setSingleLine(true);
        label.setEllipsize(TextUtils.TruncateAt.END);
        label.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        FrameLayout.LayoutParams labelLp = new FrameLayout.LayoutParams(-1, -2);
        labelLp.gravity = Gravity.BOTTOM;
        labelLp.leftMargin = dp(9);
        labelLp.rightMargin = dp(9);
        labelLp.bottomMargin = dp(8);
        tile.addView(label, labelLp);

        TextView badge = new TextView(this);
        badge.setTextSize(9);
        badge.setTextColor(C_WHITE);
        badge.setGravity(Gravity.CENTER);
        badge.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        GradientDrawable bubble = new GradientDrawable();
        bubble.setShape(GradientDrawable.OVAL);
        bubble.setColor(0xFFE51400);
        badge.setBackgroundDrawable(bubble);
        badge.setVisibility(View.GONE);
        FrameLayout.LayoutParams badgeLp = new FrameLayout.LayoutParams(dp(20), dp(20));
        badgeLp.gravity = Gravity.TOP | Gravity.RIGHT;
        badgeLp.topMargin = dp(6);
        badgeLp.rightMargin = dp(6);
        tile.addView(badge, badgeLp);

        t.view = tile;
        t.icon = icon;
        t.label = label;
        t.badge = badge;

        tile.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                open(t.entry);
            }
        });

        tile.setOnLongClickListener(new View.OnLongClickListener() {
            public boolean onLongClick(View v) {
                showAppMenu(t.entry);
                return true;
            }
        });

        tile.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    v.setBackgroundDrawable(t.focused);
                    v.animate().scaleX(1.05f).scaleY(1.05f).setDuration(90).start();
                } else {
                    v.setBackgroundDrawable(t.normal);
                    v.animate().scaleX(1f).scaleY(1f).setDuration(90).start();
                }
            }
        });

        Bitmap cachedIcon = iconStore.get(e.pkg);
        if (cachedIcon != null) icon.setImageBitmap(cachedIcon);
        applyBadge(t);
        requestIcon(e);
        return t;
    }

    private void applyBadge(Tile tile) {
        if (tile == null || tile.badge == null) return;
        int count = tile.entry == null ? 0 : NotificationStore.count(tile.entry.pkg);
        if (count > 0) {
            tile.badge.setText(count > 99 ? "99+" : String.valueOf(count));
            tile.badge.setVisibility(View.VISIBLE);
        } else {
            tile.badge.setVisibility(View.GONE);
        }
    }

    private void updateBadges(String pkg) {
        for (Section section : sections) {
            for (Tile tile : section.tiles) {
                if (tile.entry != null && tile.entry.pkg.equals(pkg)) {
                    applyBadge(tile);
                }
            }
        }
    }

    private void applyAllBadges() {
        for (Section section : sections) {
            for (Tile tile : section.tiles) {
                applyBadge(tile);
            }
        }
    }

    private final NotificationStore.Listener badgeListener = new NotificationStore.Listener() {
        public void onBadgeChanged(String pkg, int count) {
            updateBadges(pkg);
            updateNotificationBadge();
        }
    };

    private void requestIcon(final AppEntry entry) {
        Bitmap cached = iconStore.get(entry.pkg);
        if (cached != null) {
            updateTilesIcon(entry.pkg, cached, entry.updated);
            return;
        }
        final long updated = entry.updated;
        final String pkg = entry.pkg;
        iconStore.request(pkg, entry.activity, updated, new IconStore.Listener() {
            public void onIcon(String name, Bitmap bitmap) {
                updateTilesIcon(name, bitmap, updated);
            }
        });
    }

    private void updateTilesIcon(String pkg, Bitmap bitmap, long updated) {
        for (Section section : sections) {
            for (Tile tile : section.tiles) {
                if (tile.entry != null && tile.entry.pkg.equals(pkg)
                        && tile.entry.updated == updated) {
                    tile.icon.setImageBitmap(bitmap);
                }
            }
        }
    }

    private Tile makeSystemTile(String name, int iconRes, int color, final Runnable action) {
        final Tile t = new Tile();
        t.action = action;

        t.normal = new GradientDrawable();
        t.normal.setShape(GradientDrawable.RECTANGLE);
        t.normal.setColor(color);

        t.focused = new GradientDrawable();
        t.focused.setShape(GradientDrawable.RECTANGLE);
        t.focused.setColor(color);
        t.focused.setStroke(dp(3), isLight ? 0xFF111111 : C_WHITE);

        FrameLayout tile = new FrameLayout(this);
        tile.setBackgroundDrawable(t.normal);
        tile.setFocusable(true);
        tile.setFocusableInTouchMode(true);
        tile.setClickable(true);
        tile.setClipChildren(false);
        tile.setClipToPadding(false);

        ImageView icon = new ImageView(this);
        icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
        icon.setImageResource(iconRes);
        FrameLayout.LayoutParams iconLp = new FrameLayout.LayoutParams(dp(42), dp(42));
        iconLp.gravity = Gravity.TOP | Gravity.LEFT;
        iconLp.leftMargin = dp(11);
        iconLp.topMargin = dp(10);
        tile.addView(icon, iconLp);

        TextView label = new TextView(this);
        label.setText(name);
        label.setTextSize(11);
        label.setTextColor(C_WHITE);
        label.setSingleLine(true);
        label.setEllipsize(TextUtils.TruncateAt.END);
        label.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        FrameLayout.LayoutParams labelLp = new FrameLayout.LayoutParams(-1, -2);
        labelLp.gravity = Gravity.BOTTOM;
        labelLp.leftMargin = dp(9);
        labelLp.rightMargin = dp(9);
        labelLp.bottomMargin = dp(8);
        tile.addView(label, labelLp);

        t.view = tile;
        t.icon = icon;
        t.label = label;

        TextView badge = new TextView(this);
        badge.setTextSize(9);
        badge.setTextColor(C_WHITE);
        badge.setGravity(Gravity.CENTER);
        badge.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        GradientDrawable bubble = new GradientDrawable();
        bubble.setShape(GradientDrawable.OVAL);
        bubble.setColor(0xFFE51400);
        badge.setBackgroundDrawable(bubble);
        badge.setVisibility(View.GONE);
        FrameLayout.LayoutParams badgeLp = new FrameLayout.LayoutParams(dp(20), dp(20));
        badgeLp.gravity = Gravity.TOP | Gravity.RIGHT;
        badgeLp.topMargin = dp(6);
        badgeLp.rightMargin = dp(6);
        tile.addView(badge, badgeLp);
        t.badge = badge;

        tile.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (t.action != null) t.action.run();
            }
        });

        tile.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    v.setBackgroundDrawable(t.focused);
                    v.animate().scaleX(1.05f).scaleY(1.05f).setDuration(90).start();
                } else {
                    v.setBackgroundDrawable(t.normal);
                    v.animate().scaleX(1f).scaleY(1f).setDuration(90).start();
                }
            }
        });

        return t;
    }

    private Tile makeAgendaTile(final Runnable action) {
        final Tile t = new Tile();
        t.action = action;

        t.normal = new GradientDrawable();
        t.normal.setShape(GradientDrawable.RECTANGLE);
        t.normal.setColor(0xFFFA6800);

        t.focused = new GradientDrawable();
        t.focused.setShape(GradientDrawable.RECTANGLE);
        t.focused.setColor(0xFFFA6800);
        t.focused.setStroke(dp(3), isLight ? 0xFF111111 : C_WHITE);

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundDrawable(t.normal);
        card.setClickable(true);
        card.setPadding(dp(9), dp(8), dp(9), dp(8));

        TextView title = new TextView(this);
        title.setText("AGENDA");
        title.setTextSize(9);
        title.setTextColor(0xCCFFFFFF);
        title.setLetterSpacing(0.15f);
        title.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        card.addView(title, new LinearLayout.LayoutParams(-2, -2));

        agendaLine1 = new TextView(this);
        agendaLine1.setTextSize(12.5f);
        agendaLine1.setTextColor(C_WHITE);
        agendaLine1.setSingleLine(true);
        agendaLine1.setEllipsize(TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams line1Params = new LinearLayout.LayoutParams(-1, -2);
        line1Params.topMargin = dp(5);
        card.addView(agendaLine1, line1Params);

        agendaLine2 = new TextView(this);
        agendaLine2.setTextSize(12.5f);
        agendaLine2.setTextColor(0xE6FFFFFF);
        agendaLine2.setSingleLine(true);
        agendaLine2.setEllipsize(TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams line2Params = new LinearLayout.LayoutParams(-1, -2);
        line2Params.topMargin = dp(2);
        card.addView(agendaLine2, line2Params);

        t.view = card;

        card.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (t.action != null) t.action.run();
            }
        });

        card.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    v.setBackgroundDrawable(t.focused);
                    v.animate().scaleX(1.05f).scaleY(1.05f).setDuration(90).start();
                } else {
                    v.setBackgroundDrawable(t.normal);
                    v.animate().scaleX(1f).scaleY(1f).setDuration(90).start();
                }
            }
        });

        return t;
    }

    private Tile makeNewsTile() {
        final Tile t = new Tile();
        t.action = new Runnable() {
            public void run() {
                startActivity(new Intent(MainActivity.this, NewsActivity.class));
            }
        };
        t.normal = new GradientDrawable();
        t.normal.setColor(0xFF1BA1E2);
        t.focused = new GradientDrawable();
        t.focused.setColor(0xFF1BA1E2);
        t.focused.setStroke(dp(3), isLight ? 0xFF111111 : C_WHITE);

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(12), dp(8), dp(12), dp(7));
        card.setBackgroundDrawable(t.normal);
        card.setFocusable(true);
        card.setClickable(true);

        TextView header = new TextView(this);
        header.setText("NOTICIAS");
        header.setTextSize(8);
        header.setTextColor(0xCCFFFFFF);
        header.setLetterSpacing(0.15f);
        card.addView(header);

        newsLine1 = new TextView(this);
        newsLine1.setTextSize(12);
        newsLine1.setTextColor(C_WHITE);
        newsLine1.setSingleLine(true);
        newsLine1.setEllipsize(TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams firstParams = new LinearLayout.LayoutParams(-1, -2);
        firstParams.topMargin = dp(5);
        card.addView(newsLine1, firstParams);

        newsLine2 = new TextView(this);
        newsLine2.setTextSize(10);
        newsLine2.setTextColor(0xE6FFFFFF);
        newsLine2.setSingleLine(true);
        newsLine2.setEllipsize(TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams secondParams = new LinearLayout.LayoutParams(-1, -2);
        secondParams.topMargin = dp(3);
        card.addView(newsLine2, secondParams);

        t.view = card;
        card.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                t.action.run();
            }
        });
        card.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            public void onFocusChange(View v, boolean f) {
                v.setBackgroundDrawable(f ? t.focused : t.normal);
                v.animate().scaleX(f ? 1.03f : 1f).scaleY(f ? 1.03f : 1f).setDuration(90).start();
            }
        });
        return t;
    }

    private void refreshNewsPreview() {
        if (newsLine1 == null || newsLoading) return;
        applyNewsPreview();
        if (RssService.peek(0) != null && RssService.peek(1) != null) return;

        newsLoading = true;
        newsLine1.setText("Actualizando…");
        newsLine2.setText("");
        final int[] pending = {2};
        RssService.Callback callback = new RssService.Callback() {
            public void done(ArrayList<RssItem> result, String error) {
                pending[0]--;
                if (pending[0] > 0) return;
                newsLoading = false;
                applyNewsPreview();
            }
        };
        RssService.load(this, 0, callback);
        RssService.load(this, 1, callback);
    }

    private void applyNewsPreview() {
        if (newsLine1 == null) return;
        RssItem world = firstItem(0);
        RssItem sports = firstItem(1);
        if (world == null && sports == null) {
            newsLine1.setText("Sin titulares");
            newsLine2.setText("A para reintentar");
            return;
        }
        if (world != null) newsLine1.setText("BBC · " + world.title);
        else newsLine1.setText("BBC · sin titulares");
        if (sports != null) newsLine2.setText("ESPN · " + sports.title);
        else newsLine2.setText("ESPN · sin titulares");
    }

    private RssItem firstItem(int cat) {
        ArrayList<RssItem> list = RssService.peek(cat);
        if (list == null || list.isEmpty()) return null;
        return list.get(0);
    }

    private void buildBtcChip(LinearLayout headerRight) {
        btcChip = new LinearLayout(this);
        btcChip.setOrientation(LinearLayout.VERTICAL);
        btcChip.setGravity(Gravity.CENTER);
        btcChip.setClickable(true);
        btcChip.setPadding(dp(6), dp(2), dp(6), dp(2));

        btcValue = new TextView(this);
        btcValue.setTextSize(13);
        btcValue.setTextColor(textColor());
        btcValue.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));
        btcValue.setSingleLine(true);
        btcChip.addView(btcValue, new LinearLayout.LayoutParams(-2, -2));

        btcStatus = new TextView(this);
        btcStatus.setTextSize(7);
        btcStatus.setTextColor(secondaryColor());
        btcStatus.setSingleLine(true);
        LinearLayout.LayoutParams statusParams = new LinearLayout.LayoutParams(-2, -2);
        statusParams.topMargin = dp(1);
        btcChip.addView(btcStatus, statusParams);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-2, -2);
        params.rightMargin = dp(12);
        headerRight.addView(btcChip, params);
        headerItems.add(btcChip);

        btcChip.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                fetchBtc(true);
            }
        });
        updateBtcViews();
    }

    private void buildSettingsGear(LinearLayout headerRight) {
        settingsGear = new ImageView(this);
        settingsGear.setImageResource(R.mipmap.ic_settings);
        settingsGear.setScaleType(ImageView.ScaleType.FIT_CENTER);
        settingsGear.setClickable(true);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(26), dp(26));
        params.leftMargin = dp(10);
        headerRight.addView(settingsGear, params);
        headerItems.add(settingsGear);

        settingsGear.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                openSettings();
            }
        });
    }

    private void buildSearchButton(LinearLayout headerRight) {
        searchIcon = new ImageView(this);
        searchIcon.setImageResource(android.R.drawable.ic_menu_search);
        searchIcon.setColorFilter(textColor());
        searchIcon.setScaleType(ImageView.ScaleType.FIT_CENTER);
        searchIcon.setClickable(true);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(22), dp(22));
        params.leftMargin = dp(10);
        headerRight.addView(searchIcon, params);
        headerItems.add(searchIcon);
        searchIcon.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                openSearch();
            }
        });
    }

    private void buildCaseMark(LinearLayout headerRight) {
        caseMark = new CaseView(this);
        caseMark.setColors(isLight ? 0xFF0050EF : 0xFF1BA1E2, 0x33FFFFFF);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(16), dp(22));
        params.leftMargin = dp(10);
        headerRight.addView(caseMark, params);
        headerItems.add(caseMark);
        caseMark.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                openCase();
            }
        });
    }

    private void buildUpdateChip(LinearLayout headerRight) {
        updateChip = new TextView(this);
        updateChip.setTextSize(9);
        updateChip.setTextColor(isLight ? 0xFF0050EF : 0xFF7FD3FF);
        updateChip.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        updateChip.setPadding(dp(6), dp(3), dp(6), dp(3));
        updateChip.setVisibility(View.GONE);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-2, -2);
        params.leftMargin = dp(8);
        headerRight.addView(updateChip, params);
        headerItems.add(updateChip);
        updateChip.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                checkUpdates(true);
            }
        });
    }

    private void openSearch() {
        headerFocus = false;
        updateHeaderSelection();
        startActivity(new Intent(this, SearchActivity.class));
    }

    private void openCase() {
        headerFocus = false;
        updateHeaderSelection();
        startActivity(new Intent(this, CaseActivity.class));
    }

    private void enterHeader() {
        headerFocus = true;
        headerPos = -1;
        for (int i = headerItems.size() - 1; i >= 0; i--) {
            if (headerItems.get(i).getVisibility() == View.VISIBLE) {
                headerPos = i;
                break;
            }
        }
        if (headerPos < 0) {
            headerFocus = false;
            return;
        }
        View current = getCurrentFocus();
        if (current != null && current != btcChip && current != settingsGear) {
            current.clearFocus();
        }
        updateHeaderSelection();
        updateBtcViews();
    }

    private void exitHeader() {
        headerFocus = false;
        updateHeaderSelection();
        focusTile(0, focusIndex);
    }

    private void moveHeader(int direction) {
        if (headerItems.isEmpty()) return;
        int next = headerPos;
        for (int step = 0; step < headerItems.size(); step++) {
            next += direction;
            if (next < 0 || next >= headerItems.size()) return;
            if (headerItems.get(next).getVisibility() == View.VISIBLE) {
                headerPos = next;
                updateHeaderSelection();
                return;
            }
        }
    }

    private void openHeaderItem() {
        if (headerPos < 0 || headerPos >= headerItems.size()) return;
        View v = headerItems.get(headerPos);
        if (v == btcChip) {
            fetchBtc(true);
        } else if (v == settingsGear) {
            headerFocus = false;
            updateHeaderSelection();
            openSettings();
        } else if (v == searchIcon) {
            openSearch();
        } else if (v == caseMark) {
            openCase();
        } else if (v == updateChip) {
            checkUpdates(true);
        }
    }

    private void updateHeaderSelection() {
        if (headerItems.isEmpty()) return;
        for (int i = 0; i < headerItems.size(); i++) {
            View v = headerItems.get(i);
            boolean selected = headerFocus && i == headerPos;
            v.animate().scaleX(selected ? 1.12f : 1f)
                    .scaleY(selected ? 1.12f : 1f)
                    .setDuration(90).start();
        }
    }

    private void updateBtcViews() {
        if (btcValue == null) return;
        if (btcLoading) {
            btcValue.setText("BTC …");
            btcStatus.setText("Consultando");
            return;
        }
        BtcData cached = BtcService.loadCached(this);
        if (cached != null) {
            btcValue.setText("BTC " + BtcService.format(cached.usd));
            btcStatus.setText(btcAge(cached.updated));
        } else {
            btcValue.setText("BTC —");
            btcStatus.setText("Sin conexión");
        }
    }

    private String btcAge(long updated) {
        long delta = System.currentTimeMillis() - updated;
        if (delta < 60L * 1000L) return "ahora";
        long minutes = delta / (60L * 1000L);
        if (minutes < 60L) return "hace " + minutes + " min";
        long hours = minutes / 60L;
        long rest = minutes % 60L;
        if (rest > 0) return "hace " + hours + " h " + rest + " m";
        return "hace " + hours + " h";
    }

    private void maybeRefreshBtc() {
        if (!resumed || btcLoading) return;
        if (!BtcService.isStale(BtcService.loadCached(this))) {
            updateBtcViews();
            return;
        }
        fetchBtc(false);
    }

    private void fetchBtc(final boolean force) {
        if (btcLoading) return;
        if (!force && !BtcService.isStale(BtcService.loadCached(this))) return;
        btcLoading = true;
        updateBtcViews();
        BtcService.fetch(this, new BtcService.Callback() {
            public void onResult(BtcData data, String error) {
                btcLoading = false;
                if (data != null) updateBtcViews();
                else {
                    btcError = error;
                    updateBtcViews();
                }
            }
        });
    }

    private void checkUpdates(final boolean manual) {
        if (updateChecking) return;
        updateChecking = true;
        if (manual) toast("Buscando actualización…");
        UpdateService.check(this, manual, new UpdateService.CheckCallback() {
            public void onResult(UpdateService.Release release, String error) {
                updateChecking = false;
                if (release != null) {
                    updateVersion = release.version;
                    if (updateChip != null) {
                        updateChip.setText("↑ " + release.version);
                        updateChip.setVisibility(View.VISIBLE);
                    }
                    if (manual) showUpdateDialog(release);
                } else {
                    updateVersion = null;
                    if (updateChip != null) updateChip.setVisibility(View.GONE);
                    if (manual) {
                        toast(error == null ? "NSP está al día" : "No se pudo comprobar: " + error);
                    }
                }
            }
        });
    }

    private void showUpdateDialog(final UpdateService.Release release) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("NSP " + release.version + " disponible");
        builder.setMessage("Instalada: " + UpdateService.installedVersion(this)
                + "\nNueva: " + release.version
                + (release.size > 0 ? "\nTamaño: " + (release.size / 1024) + " KB" : "")
                + "\n\nAndroid pedirá confirmación para instalar.");
        builder.setPositiveButton("Descargar", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                downloadUpdate(release);
            }
        });
        builder.setNegativeButton("Ahora no", null);

        final AlertDialog dialog = builder.create();
        dialogOpen = true;
        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            public void onDismiss(DialogInterface d) {
                dialogOpen = false;
            }
        });
        dialog.setOnKeyListener(new DialogInterface.OnKeyListener() {
            public boolean onKey(DialogInterface d, int keyCode, KeyEvent event) {
                if (event.getAction() != KeyEvent.ACTION_DOWN) return false;
                if (keyCode == KeyEvent.KEYCODE_BUTTON_B) {
                    d.dismiss();
                    return true;
                }
                if (keyCode == KeyEvent.KEYCODE_BUTTON_A) {
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
                    return true;
                }
                return false;
            }
        });
        dialog.show();
    }

    private void downloadUpdate(final UpdateService.Release release) {
        final AlertDialog progress = new AlertDialog.Builder(this)
                .setTitle("Descargando NSP " + release.version)
                .setMessage("0 %")
                .setCancelable(false)
                .create();
        dialogOpen = true;
        progress.setOnDismissListener(new DialogInterface.OnDismissListener() {
            public void onDismiss(DialogInterface d) {
                dialogOpen = false;
            }
        });
        progress.show();

        UpdateService.download(this, release, new UpdateService.DownloadCallback() {
            public void onProgress(final int percent) {
                ui.post(new Runnable() {
                    public void run() {
                        progress.setMessage(percent + " %");
                    }
                });
            }

            public void onResult(final String path, final String error) {
                ui.post(new Runnable() {
                    public void run() {
                        try {
                            progress.dismiss();
                        } catch (Exception ignored) {
                        }
                        if (path == null) {
                            showUpdateError(release, error);
                        } else if (!UpdateService.install(MainActivity.this, path)) {
                            showUpdateError(release, "No se pudo abrir el instalador");
                        }
                    }
                });
            }
        });
    }

    private void showUpdateError(final UpdateService.Release release, String error) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("No se pudo actualizar");
        builder.setMessage(error == null ? "Error desconocido" : error);
        builder.setPositiveButton("Abrir Release", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                BrowserUtil.open(MainActivity.this, release.pageUrl);
            }
        });
        builder.setNegativeButton("Cerrar", null);
        builder.show();
    }

    private void refreshAgenda() {
        if (agendaLine1 == null) return;
        ArrayList<AgendaService.Event> events = AgendaService.next(this, 2);
        if (events.isEmpty()) {
            agendaLine1.setText("Sin actividades");
            agendaLine2.setText("");
            return;
        }
        agendaLine1.setText(eventLine(events.get(0)));
        agendaLine2.setText(events.size() > 1 ? eventLine(events.get(1)) : "");
    }

    private String eventLine(AgendaService.Event event) {
        Calendar begin = Calendar.getInstance();
        begin.setTimeInMillis(event.begin);
        Calendar now = Calendar.getInstance();
        boolean sameDay = begin.get(Calendar.YEAR) == now.get(Calendar.YEAR)
                && begin.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR);
        String day = "";
        if (!sameDay) {
            day = new SimpleDateFormat("EEE ", new Locale("es", "ES"))
                    .format(new Date(event.begin));
            day = day.substring(0, 1).toUpperCase(new Locale("es", "ES"))
                    + day.substring(1);
        }
        if (event.allDay) {
            return day.isEmpty() ? event.title : day.trim() + " · " + event.title;
        }
        String time = new SimpleDateFormat("HH:mm", new Locale("es", "ES"))
                .format(new Date(event.begin));
        return day + time + " " + event.title;
    }

    private void openCalendarApp() {
        Intent calendar = new Intent(Intent.ACTION_MAIN);
        calendar.setPackage("com.android.calendar");
        calendar.addCategory(Intent.CATEGORY_LAUNCHER);
        try {
            startActivity(calendar);
            return;
        } catch (Exception ignored) {
        }
        try {
            startActivity(getPackageManager()
                    .getLaunchIntentForPackage("com.android.calendar"));
            return;
        } catch (Exception ignored) {
        }
        toast("No hay app de calendario");
    }

    private void focusTile(int sectionIndex, int index) {
        if (sectionIndex < 0 || sectionIndex >= sections.size()) return;
        Section s = sections.get(sectionIndex);
        if (index < 0 || index >= s.tiles.size()) return;

        focusSection = sectionIndex;
        focusIndex = index;
        s.lastIndex = index;

        final View v = s.tiles.get(index).view;
        v.requestFocus();
        v.post(new Runnable() {
            public void run() {
                Rect r = new Rect(0, 0, Math.max(1, v.getWidth()), Math.max(1, v.getHeight()));
                v.requestRectangleOnScreen(r, true);
            }
        });
    }

    private boolean focusByPackage(String pkg) {
        for (int si = 0; si < sections.size(); si++) {
            Section s = sections.get(si);
            for (int ti = 0; ti < s.tiles.size(); ti++) {
                Tile tile = s.tiles.get(ti);
                if (tile.entry != null && tile.entry.pkg.equals(pkg)) {
                    focusTile(si, ti);
                    return true;
                }
            }
        }
        return false;
    }

    private void focusFirstApp() {
        for (int si = 0; si < sections.size(); si++) {
            Section s = sections.get(si);
            if (!s.tiles.isEmpty()) {
                focusTile(si, 0);
                return;
            }
        }
    }

    /** Deja el panel arriba al abrir: SISTEMA visible y sin desplazamiento heredado. */
    private void scrollToTop() {
        final ScrollView scroll = mainScroll;
        if (scroll == null) return;
        scroll.post(new Runnable() {
            public void run() {
                scroll.scrollTo(0, 0);
            }
        });
    }

    private String focusedPackage() {
        if (sections.isEmpty()) return null;
        if (focusSection >= sections.size()) return null;
        Section s = sections.get(focusSection);
        if (focusIndex >= s.tiles.size()) return null;
        Tile tile = s.tiles.get(focusIndex);
        return tile.entry == null ? null : tile.entry.pkg;
    }

    private void showHint() {
        if (hintOverlay == null) return;
        ui.removeCallbacks(hideHintRunnable);
        hintOverlay.animate().cancel();
        hintOverlay.setVisibility(View.VISIBLE);
        hintOverlay.setAlpha(0f);
        hintOverlay.animate().alpha(1f).setDuration(250).start();
        ui.postDelayed(hideHintRunnable, 6000);
    }

    private void hideHint() {
        if (hintOverlay == null) return;
        hintOverlay.animate().cancel();
        hintOverlay.animate().alpha(0f).setDuration(400).start();
        ui.postDelayed(new Runnable() {
            public void run() {
                if (hintOverlay != null && hintOverlay.getAlpha() < 0.05f) {
                    hintOverlay.setVisibility(View.GONE);
                }
            }
        }, 420);
    }

    private final Runnable hideHintRunnable = new Runnable() {
        public void run() {
            hideHint();
        }
    };

    private final Runnable headerTicker = new Runnable() {
        public void run() {
            updateHeaderClock();
            ui.postDelayed(this, 1000);
        }
    };

    private static boolean isInformativeKey(int code) {
        switch (code) {
            case KeyEvent.KEYCODE_VOLUME_UP:
            case KeyEvent.KEYCODE_VOLUME_DOWN:
            case KeyEvent.KEYCODE_MUTE:
            case KeyEvent.KEYCODE_BRIGHTNESS_UP:
            case KeyEvent.KEYCODE_BRIGHTNESS_DOWN:
            case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
            case KeyEvent.KEYCODE_MEDIA_NEXT:
            case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
            case KeyEvent.KEYCODE_MEDIA_STOP:
            case KeyEvent.KEYCODE_BUTTON_MODE:
            case KeyEvent.KEYCODE_BUTTON_C:
            case KeyEvent.KEYCODE_BUTTON_Z:
            case KeyEvent.KEYCODE_SEARCH:
            case KeyEvent.KEYCODE_APP_SWITCH:
            case KeyEvent.KEYCODE_FOCUS:
            case KeyEvent.KEYCODE_CAMERA:
            case KeyEvent.KEYCODE_SETTINGS:
            case KeyEvent.KEYCODE_HELP:
            case KeyEvent.KEYCODE_GUIDE:
            case KeyEvent.KEYCODE_NOTIFICATION:
            case KeyEvent.KEYCODE_ASSIST:
                return true;
            default:
                return false;
        }
    }

    private void updateHeaderClock() {
        Date now = new Date();
        if (headerDate != null) {
            String value = headerDateFormat.format(now).toUpperCase(new Locale("es", "ES"));
            if (!value.contentEquals(headerDate.getText())) headerDate.setText(value);
        }
        if (headerTime != null) {
            boolean seconds = prefs.getBoolean("clock_seconds", false);
            SimpleDateFormat format = seconds ? headerTimeFormatSeconds : headerTimeFormat;
            String value = format.format(now);
            if (!value.contentEquals(headerTime.getText())) headerTime.setText(value);
        }
        if (headerDay != null) {
            Calendar calendar = Calendar.getInstance();
            String weekday = headerWeekdayFormat.format(now).toUpperCase(new Locale("es", "ES"));
            String day = String.valueOf(calendar.get(Calendar.DAY_OF_MONTH));
            String month = headerMonthFormat.format(now).toUpperCase(new Locale("es", "ES"));
            if (!weekday.contentEquals(headerWeekday.getText())) headerWeekday.setText(weekday);
            if (!day.contentEquals(headerDay.getText())) headerDay.setText(day);
            if (!month.contentEquals(headerMonth.getText())) headerMonth.setText(month);
        }
    }

    private void readBattery(Intent intent) {
        if (intent == null) return;
        int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
        int plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0);
        boolean charging = plugged != 0;
        int percent = (level >= 0 && scale > 0)
                ? (int) Math.round(level * 100f / scale) : 0;
        updateBattery(percent, charging);
    }

    private void updateBattery(int percent, boolean charging) {
        String status;
        if (charging) {
            status = "Cargando";
        } else {
            status = estimateBattery(percent);
        }
        batteryPercent = percent;
        batteryCharging = charging;
        batteryStatus = status;
        if (dashboard != null) dashboard.setBattery(percent, charging, status);
    }

    private String estimateBattery(int percent) {
        long now = System.currentTimeMillis();
        int lastLevel = prefs.getInt("batt_level", -1);
        long lastTime = prefs.getLong("batt_time", 0L);
        String result = "";
        if (lastLevel >= 0 && now > lastTime) {
            float minutes = (now - lastTime) / 60000f;
            float drop = lastLevel - percent;
            if (minutes > 1f && drop > 0f) {
                float perMinute = drop / minutes;
                if (perMinute >= 0.25f) {
                    int minutesLeft = Math.round((100 - percent) / perMinute);
                    if (minutesLeft > 0 && minutesLeft < 24 * 60) {
                        int hours = minutesLeft / 60;
                        int mins = minutesLeft % 60;
                        result = hours > 0 ? (hours + " h " + mins + " min") : (mins + " min");
                    }
                }
            }
        }
        prefs.edit().putInt("batt_level", percent).putLong("batt_time", now).apply();
        return result;
    }

    private Tile currentTile() {
        if (sections.isEmpty()) return null;
        if (focusSection < 0 || focusSection >= sections.size()) return null;
        Section s = sections.get(focusSection);
        if (focusIndex < 0 || focusIndex >= s.tiles.size()) return null;
        return s.tiles.get(focusIndex);
    }

    private void moveHorizontal(int direction) {
        if (focusSection == 0) {
            if (focusIndex == 1 || focusIndex == 2) { focusTile(0, direction < 0 ? 1 : 2); return; }
            return;
        }
        if (sections.isEmpty()) return;
        Section current = sections.get(focusSection);
        if (current.tiles.isEmpty()) return;
        int column = focusIndex % COLUMNS;
        if (direction < 0 && column == 0) return;
        if (direction > 0 && column == COLUMNS - 1) return;
        int next = focusIndex + direction;
        if (next < 0 || next >= current.tiles.size()) return;
        focusTile(focusSection, next);
    }

    private void moveVertical(int direction) {
        if (focusSection == 0) {
            if (direction > 0 && focusIndex == 0) { focusTile(0, 1); return; }
            if (direction < 0 && (focusIndex == 1 || focusIndex == 2)) { focusTile(0, 0); return; }
            if (direction > 0 && (focusIndex == 1 || focusIndex == 2)) { moveSection(1, 0); return; }
            return;
        }
        if (sections.isEmpty()) return;
        Section current = sections.get(focusSection);
        if (current.tiles.isEmpty()) return;
        int column = focusIndex % COLUMNS;
        int next = focusIndex + direction * COLUMNS;
        if (next >= 0 && next < current.tiles.size()) {
            focusTile(focusSection, next);
            return;
        }
        moveSection(direction, column);
    }

    private void moveSection(int direction) {
        int column = 0;
        if (!sections.isEmpty() && focusSection < sections.size()) {
            Section current = sections.get(focusSection);
            if (!current.tiles.isEmpty()) column = focusIndex % COLUMNS;
        }
        moveSection(direction, column);
    }

    private void moveSection(int direction, int column) {
        int targetSection = focusSection + direction;
        if (targetSection < 0 || targetSection >= sections.size()) return;
        Section target = sections.get(targetSection);
        if (target.tiles.isEmpty()) return;
        int index;
        if (direction > 0) {
            int firstRow = Math.min(COLUMNS, target.tiles.size()) - 1;
            index = Math.min(column, firstRow);
        } else {
            int lastRowStart = ((target.tiles.size() - 1) / COLUMNS) * COLUMNS;
            index = Math.min(lastRowStart + column, target.tiles.size() - 1);
        }
        focusTile(targetSection, index);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (dialogOpen) return super.dispatchKeyEvent(event);

        int code = event.getKeyCode();
        int action = event.getAction();
        if (action == KeyEvent.ACTION_DOWN) {
            switch (code) {
                case KeyEvent.KEYCODE_DPAD_LEFT:
                    if (headerFocus) moveHeader(-1);
                    else moveHorizontal(-1);
                    return true;
                case KeyEvent.KEYCODE_DPAD_RIGHT:
                    if (headerFocus) moveHeader(1);
                    else moveHorizontal(1);
                    return true;
                case KeyEvent.KEYCODE_DPAD_UP:
                    if (headerFocus) return true;
                    if (focusSection == 0 && !headerItems.isEmpty()) {
                        enterHeader();
                        return true;
                    }
                    moveVertical(-1);
                    return true;
                case KeyEvent.KEYCODE_DPAD_DOWN:
                    if (headerFocus) exitHeader();
                    else moveVertical(1);
                    return true;
                case KeyEvent.KEYCODE_BUTTON_L1:
                    moveSection(-1);
                    return true;
                case KeyEvent.KEYCODE_BUTTON_R1:
                    moveSection(1);
                    return true;
                case KeyEvent.KEYCODE_BUTTON_A:
                case KeyEvent.KEYCODE_DPAD_CENTER:
                case KeyEvent.KEYCODE_ENTER:
                case KeyEvent.KEYCODE_NUMPAD_ENTER:
                    if (event.getRepeatCount() == 0) {
                        if (headerFocus) openHeaderItem();
                        else openFocused();
                    }
                    return true;
                case KeyEvent.KEYCODE_BUTTON_X:
                    if (event.getRepeatCount() == 0) toggleFavoriteFocused();
                    return true;
                case KeyEvent.KEYCODE_BUTTON_Y:
                    if (event.getRepeatCount() == 0) categoryFocused();
                    return true;
                case KeyEvent.KEYCODE_BUTTON_SELECT:
                case KeyEvent.KEYCODE_MENU:
                    if (event.getRepeatCount() == 0) toggleTheme();
                    return true;
                case KeyEvent.KEYCODE_BUTTON_START:
                    if (event.getRepeatCount() == 0) {
                        toast("Recargando aplicaciones");
                        reloadApps();
                    }
                    return true;
                case KeyEvent.KEYCODE_BUTTON_B:
                    if (event.getRepeatCount() == 0) {
                        if (headerFocus) exitHeader();
                        else if (!HomeUtil.isDefaultHome(this)) finish();
                    }
                    return true;
                case KeyEvent.KEYCODE_BUTTON_MODE:
                    if (event.getRepeatCount() == 0) startActivity(new Intent(this, NewsActivity.class));
                    return true;
                default:
                    if (isInformativeKey(code)) showHint();
                    break;
            }
        }
        return super.dispatchKeyEvent(event);
    }

    @Override
    public boolean onGenericMotionEvent(MotionEvent event) {
        if (dialogOpen || sections.isEmpty()) return super.onGenericMotionEvent(event);

        int source = event.getSource();
        if ((source & InputDevice.SOURCE_JOYSTICK) == 0
                && (source & InputDevice.SOURCE_GAMEPAD) == 0
                && (source & InputDevice.SOURCE_DPAD) == 0) {
            return super.onGenericMotionEvent(event);
        }
        if (event.getAction() != MotionEvent.ACTION_MOVE) {
            return super.onGenericMotionEvent(event);
        }

        float hatX = event.getAxisValue(MotionEvent.AXIS_HAT_X);
        float hatY = event.getAxisValue(MotionEvent.AXIS_HAT_Y);
        float x = Math.abs(hatX) > 0.5f ? hatX : event.getAxisValue(MotionEvent.AXIS_X);
        float y = Math.abs(hatY) > 0.5f ? hatY : event.getAxisValue(MotionEvent.AXIS_Y);

        int dir = 0;
        if (Math.abs(x) >= Math.abs(y) && Math.abs(x) > 0.55f) {
            dir = x > 0 ? 1 : 2;
        } else if (Math.abs(y) > 0.55f) {
            dir = y > 0 ? 3 : 4;
        }

        if (dir == 0) {
            stickDir = 0;
            return true;
        }

        long now = SystemClock.uptimeMillis();
        if (dir != stickDir || now - lastStickTime >= 140) {
            lastStickTime = now;
            stickDir = dir;
            if (dir == 1) moveHorizontal(1);
            else if (dir == 2) moveHorizontal(-1);
            else if (dir == 3) moveVertical(1);
            else moveVertical(-1);
        }
        return true;
    }

    private void openFocused() {
        Tile t = currentTile();
        if (t == null) return;
        if (t.entry != null) open(t.entry);
        else if (t.action != null) t.action.run();
    }

    private void open(AppEntry e) {
        repo.recordLaunch(e.pkg);
        recentsDirty = true;
        Intent i = new Intent(Intent.ACTION_MAIN);
        i.addCategory(Intent.CATEGORY_LAUNCHER);
        i.setClassName(e.pkg, e.activity);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            startActivity(i);
        } catch (Exception ex) {
            Intent launch = getPackageManager().getLaunchIntentForPackage(e.pkg);
            if (launch != null) {
                try {
                    startActivity(launch);
                    return;
                } catch (Exception ignored) {
                }
            }
            toast("No se pudo abrir " + e.label);
        }
    }

    private void toggleFavoriteFocused() {
        Tile t = currentTile();
        if (t == null || t.entry == null) return;
        boolean added = repo.toggleFavorite(t.entry.pkg);
        if (added) {
            addFavoriteInPlace(t.entry);
        } else {
            removeFavoriteInPlace(t.entry);
        }
        toast(added ? "Añadido a Favoritos" : "Quitado de Favoritos");
        restoreFocus(t.entry.pkg);
    }

    private void categoryFocused() {
        Tile t = currentTile();
        if (t != null && t.entry != null) showCategoryDialog(t.entry);
    }

    private void updateNotificationBadge() {
        if (notificationTile == null || notificationTile.badge == null) return;
        int total = NotificationStore.total();
        if (total > 0) {
            notificationTile.badge.setText(total > 99 ? "99+" : String.valueOf(total));
            notificationTile.badge.setVisibility(View.VISIBLE);
        } else {
            notificationTile.badge.setVisibility(View.GONE);
        }
    }

    private int selectedPosition(AlertDialog dialog) {
        try {
            android.widget.ListView list = dialog.getListView();
            if (list == null && dialog.getWindow() != null) {
                list = findListView(dialog.getWindow().getDecorView());
            }
            if (list == null) return -1;
            int position = list.getSelectedItemPosition();
            if (position < 0 && list.getCount() > 0) position = 0;
            return position;
        } catch (Exception e) {
            return -1;
        }
    }

    private android.widget.ListView findListView(View root) {
        if (root == null) return null;
        if (root instanceof android.widget.ListView) {
            return (android.widget.ListView) root;
        }
        if (root instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) root;
            for (int i = 0; i < group.getChildCount(); i++) {
                android.widget.ListView found = findListView(group.getChildAt(i));
                if (found != null) return found;
            }
        }
        return null;
    }

    private void showNotificationDialog() {
        final java.util.List<String> packages = NotificationStore.packages();
        if (packages.isEmpty()) {
            toast("No hay notificaciones");
            return;
        }
        final String[] items = new String[packages.size()];
        for (int i = 0; i < packages.size(); i++) {
            AppEntry entry = findApp(apps, packages.get(i));
            String name = entry == null ? packages.get(i) : entry.label;
            items[i] = name + "  (" + NotificationStore.count(packages.get(i)) + ")";
        }

        final AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Notificaciones")
                .setAdapter(new ArrayAdapter<String>(this,
                        android.R.layout.simple_list_item_1, items) {
                    @Override
                    public View getView(int position, View convertView, ViewGroup parent) {
                        TextView tv = (TextView) super.getView(position, convertView, parent);
                        tv.setTextColor(isLight ? 0xFF111111 : 0xFFFFFFFF);
                        tv.setTextSize(14);
                        tv.setPadding(dp(20), dp(6), dp(20), dp(6));
                        tv.setMinHeight(0);
                        tv.setMinimumHeight(0);
                        return tv;
                    }
                }, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface d, int which) {
                        AppEntry entry = findApp(apps, packages.get(which));
                        if (entry != null) open(entry);
                    }
                })
                .create();

        dialog.setOnKeyListener(new DialogInterface.OnKeyListener() {
            public boolean onKey(DialogInterface d, int keyCode, KeyEvent event) {
                if (event.getAction() != KeyEvent.ACTION_DOWN) return false;
                if (keyCode == KeyEvent.KEYCODE_BUTTON_Y) {
                    int position = selectedPosition(dialog);
                    if (position >= 0 && position < packages.size()) {
                        NotificationStore.clear(packages.get(position));
                        toast("Avisos descartados");
                    }
                    d.dismiss();
                    return true;
                }
                if (keyCode == KeyEvent.KEYCODE_BUTTON_X) {
                    NotificationStore.clearAll();
                    toast("Avisos descartados");
                    d.dismiss();
                    return true;
                }
                if (keyCode == KeyEvent.KEYCODE_BUTTON_B) {
                    d.dismiss();
                    return true;
                }
                return false;
            }
        });
        dialog.show();
    }

    private void showAppMenu(final AppEntry entry) {
        final boolean favorite = repo.isFavorite(entry.pkg);
        final String[] items = new String[] {
                "Abrir",
                favorite ? "Quitar de Favoritos" : "Añadir a Favoritos",
                "Mover de categoría",
                "Info de la app",
                "Desinstalar"
        };

        final AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(entry.label)
                .setAdapter(new ArrayAdapter<String>(this,
                        android.R.layout.simple_list_item_1, items) {
                    @Override
                    public View getView(int position, View convertView, ViewGroup parent) {
                        TextView tv = (TextView) super.getView(position, convertView, parent);
                        tv.setTextColor(isLight ? 0xFF111111 : 0xFFFFFFFF);
                        tv.setTextSize(14);
                        tv.setPadding(dp(20), dp(6), dp(20), dp(6));
                        tv.setMinHeight(0);
                        tv.setMinimumHeight(0);
                        return tv;
                    }
                }, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface d, int which) {
                        if (which == 0) {
                            open(entry);
                        } else if (which == 1) {
                            boolean added = repo.toggleFavorite(entry.pkg);
                            if (added) {
                                addFavoriteInPlace(entry);
                            } else {
                                removeFavoriteInPlace(entry);
                            }
                            toast(added ? "Añadido a Favoritos" : "Quitado de Favoritos");
                            restoreFocus(entry.pkg);
                        } else if (which == 2) {
                            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                public void run() {
                                    showCategoryDialog(entry);
                                }
                            }, 150);
                        } else if (which == 3) {
                            try {
                                Intent intent = new Intent(
                                        android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                        android.net.Uri.parse("package:" + entry.pkg));
                                startActivity(intent);
                            } catch (Exception ignored) {
                                toast("No se pudo abrir la info de la app");
                            }
                        } else {
                            confirmUninstall(entry);
                        }
                    }
                })
                .create();

        dialog.setOnKeyListener(new DialogInterface.OnKeyListener() {
            public boolean onKey(DialogInterface d, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_BUTTON_B && event.getAction() == KeyEvent.ACTION_DOWN) {
                    d.dismiss();
                    return true;
                }
                return false;
            }
        });
        dialog.show();
    }

    private void confirmUninstall(final AppEntry entry) {
        new AlertDialog.Builder(this)
                .setTitle("Desinstalar " + entry.label + "?")
                .setMessage("Se abrirá el desinstalador del sistema.")
                .setPositiveButton("Desinstalar", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        try {
                            Intent intent = new Intent(Intent.ACTION_DELETE,
                                    android.net.Uri.parse("package:" + entry.pkg));
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent);
                        } catch (Exception ignored) {
                            toast("No se pudo abrir el desinstalador");
                        }
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void showCategoryDialog(final AppEntry e) {
        final String[] items = new String[AppRepository.CATS.length + 1];
        System.arraycopy(AppRepository.CATS, 0, items, 0, AppRepository.CATS.length);
        items[items.length - 1] = "Automática (detectar)";

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(e.label);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1, items) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                TextView tv = (TextView) super.getView(position, convertView, parent);
                tv.setTextColor(textColor());
                tv.setTextSize(14);
                tv.setPadding(dp(20), dp(6), dp(20), dp(6));
                tv.setMinHeight(0);
                tv.setMinimumHeight(0);
                return tv;
            }
        };
        builder.setAdapter(adapter, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface d, int which) {
                String oldCategory = e.category;
                if (which == AppRepository.CATS.length) {
                    repo.clearCategory(e.pkg);
                } else {
                    repo.setCategory(e.pkg, AppRepository.CATS[which]);
                }
                String newCategory = repo.categoryOf(e);
                e.category = newCategory;
                if (!newCategory.equals(oldCategory)) {
                    moveCategoryInPlace(e, oldCategory, newCategory);
                }
                restoreFocus(e.pkg);
            }
        });

        AlertDialog dialog = builder.create();
        dialog.setOnKeyListener(new DialogInterface.OnKeyListener() {
            public boolean onKey(DialogInterface d, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_BUTTON_B && event.getAction() == KeyEvent.ACTION_DOWN) {
                    d.dismiss();
                    return true;
                }
                return false;
            }
        });
        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            public void onDismiss(DialogInterface d) {
                dialogOpen = false;
                ui.postDelayed(new Runnable() {
                    public void run() {
                        restoreFocus(e.pkg);
                    }
                }, 60);
            }
        });
        dialogOpen = true;
        dialog.show();
    }

    private void toggleTheme() {
        isLight = !isLight;
        prefs.edit().putBoolean("light", isLight).apply();
        if (BackgroundManager.mode(this) == BackgroundManager.MODE_CUSTOM) {
            BackgroundManager.setThemeOverride(this,
                    isLight ? BackgroundManager.THEME_LIGHT : BackgroundManager.THEME_DARK);
        }
        prefs.edit().putBoolean("bg_changed", true).apply();
        recreate();
    }

    private void openSettings() {
        startActivity(new Intent(this, SettingsActivity.class));
    }

    private void maybeRefreshWeather() {
        if (!resumed || weatherLoading) return;
        if (!WeatherService.isStale(weather)) return;
        fetchWeather(false);
    }

    private void fetchWeather(final boolean force) {
        if (weatherLoading) return;
        if (!force && !WeatherService.isStale(weather)) return;
        weatherLoading = true;
        if (weather == null && dashboard != null) dashboard.setWeatherLoading();
        WeatherService.fetch(this, new WeatherService.Callback() {
            public void onResult(WeatherData data, String error) {
                weatherLoading = false;
                if (data != null) {
                    weather = data;
                    if (dashboard != null) dashboard.setWeather(data);
                } else if (weather == null && dashboard != null) {
                    dashboard.setWeatherError();
                }
                if (force) {
                    toast(data != null ? "Clima actualizado" : "No se pudo actualizar el clima");
                }
            }
        });
    }

    private void refreshIfPackagesChanged() {
        if (scanning || dialogOpen || apps.isEmpty()) return;
        Set<String> installed = repo.installedPackages();
        if (installed.size() != apps.size()) {
            reloadApps();
            return;
        }
        for (AppEntry e : apps) {
            if (!installed.contains(e.pkg)) {
                reloadApps();
                return;
            }
        }
    }

    private void registerPackageReceiver() {
        if (packageReceiver != null) return;
        packageReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                if (scanning || dialogOpen) return;
                ui.removeCallbacks(packageReload);
                ui.postDelayed(packageReload, 800);
            }
        };
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_PACKAGE_ADDED);
        filter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        filter.addAction(Intent.ACTION_PACKAGE_CHANGED);
        filter.addAction(Intent.ACTION_PACKAGE_REPLACED);
        filter.addDataScheme("package");
        registerReceiver(packageReceiver, filter);
    }

    private void unregisterPackageReceiver() {
        if (packageReceiver == null) return;
        try {
            unregisterReceiver(packageReceiver);
        } catch (Exception ignored) {
        }
        packageReceiver = null;
    }

    @Override
    protected void onResume() {
        super.onResume();
        resumed = true;

        if (prefs.getBoolean("bg_changed", false)) {
            prefs.edit().putBoolean("bg_changed", false).apply();
            ui.post(new Runnable() {
                public void run() {
                    recreate();
                }
            });
            return;
        }

        boolean prefLight = prefs.getBoolean("light", false);
        if (prefLight != isLight) {
            ui.post(new Runnable() {
                public void run() {
                    recreate();
                }
            });
            return;
        }

        if (prefs.getInt("clock_style", 0) != clockStyle) {
            ui.post(new Runnable() {
                public void run() {
                    recreate();
                }
            });
            return;
        }

        if (dashboard != null) {
            WeatherData cached = WeatherService.loadCached(this);
            if (cached != null && (weather == null || cached.updated > weather.updated)) {
                weather = cached;
                dashboard.setWeather(cached);
            }
            dashboard.start();
            dashboard.refreshNow();
        }

        if (headerClock != null) {
            headerClock.setShowSeconds(prefs.getBoolean("clock_seconds", false));
            headerClock.setRunning(true);
        }
        updateHeaderClock();
        updateNotificationBadge();
        refreshAgenda();
        refreshNewsPreview();
        maybeRefreshBtc();
        checkUpdates(false);
        ui.removeCallbacks(headerTicker);
        ui.postDelayed(headerTicker, 1000);

        if (batteryReceiver == null) {
            batteryReceiver = new BroadcastReceiver() {
                public void onReceive(Context context, Intent intent) {
                    readBattery(intent);
                }
            };
        }
        try {
            registerReceiver(batteryReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        } catch (Exception ignored) {
        }

        if (recentsDirty) refreshRecents();

        NotificationStore.addListener(badgeListener);
        applyAllBadges();

        if (prefs.getBoolean("reload_apps", false)) {
            prefs.edit().putBoolean("reload_apps", false).apply();
            reloadApps();
        } else {
            refreshIfPackagesChanged();
        }

        maybeRefreshWeather();
        registerPackageReceiver();
    }

    @Override
    protected void onPause() {
        resumed = false;
        NotificationStore.removeListener(badgeListener);
        if (headerClock != null) headerClock.setRunning(false);
        if (batteryReceiver != null) {
            try {
                unregisterReceiver(batteryReceiver);
            } catch (Exception ignored) {
            }
        }
        if (dashboard != null) dashboard.stop();
        unregisterPackageReceiver();
        super.onPause();
    }

    @Override
    public void onBackPressed() {
        if (HomeUtil.isDefaultHome(this)) return;
        super.onBackPressed();
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        String pkg = focusedPackage();
        if (pkg != null) outState.putString("focusPkg", pkg);
    }
}


