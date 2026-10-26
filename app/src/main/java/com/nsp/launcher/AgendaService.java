package com.nsp.launcher;

import android.content.Context;

import java.util.ArrayList;
import java.util.Calendar;

import android.database.Cursor;
import android.provider.CalendarContract;

public final class AgendaService {

    public static class Event {
        public final long begin;
        public final long end;
        public final String title;
        public final boolean allDay;

        Event(long begin, long end, String title, boolean allDay) {
            this.begin = begin;
            this.end = end;
            this.title = title;
            this.allDay = allDay;
        }
    }

    private AgendaService() {
    }

    /**
     * Devuelve las próximas actividades (máximo {@param max}).
     * Devuelve lista vacía si no hay eventos o si el proveedor falla.
     */
    public static ArrayList<Event> next(Context context, int max) {
        ArrayList<Event> events = new ArrayList<Event>();
        Cursor cursor = null;
        try {
            long now = System.currentTimeMillis();
            Calendar window = Calendar.getInstance();
            window.setTimeInMillis(now);
            window.add(Calendar.DAY_OF_MONTH, 30);
            long end = window.getTimeInMillis();

            String[] projection = new String[] {
                    CalendarContract.Instances.BEGIN,
                    CalendarContract.Instances.END,
                    CalendarContract.Instances.TITLE,
                    CalendarContract.Instances.ALL_DAY
            };
            cursor = CalendarContract.Instances.query(
                    context.getContentResolver(), projection, 0L, end);

            int beginIdx = cursor.getColumnIndex(CalendarContract.Instances.BEGIN);
            int endIdx = cursor.getColumnIndex(CalendarContract.Instances.END);
            int titleIdx = cursor.getColumnIndex(CalendarContract.Instances.TITLE);
            int allDayIdx = cursor.getColumnIndex(CalendarContract.Instances.ALL_DAY);

            ArrayList<Event> candidates = new ArrayList<Event>();
            while (cursor.moveToNext()) {
                long begin = cursor.getLong(beginIdx);
                long eventEnd = cursor.getLong(endIdx);
                String title = cursor.getString(titleIdx);
                boolean allDay = cursor.getInt(allDayIdx) != 0;
                if (title == null) title = "(Sin título)";
                boolean relevant = (allDay && eventEnd >= now) || begin >= now;
                if (!relevant) continue;
                candidates.add(new Event(begin, eventEnd, title, allDay));
            }

            java.util.Collections.sort(candidates, new java.util.Comparator<Event>() {
                public int compare(Event a, Event b) {
                    long diff = a.begin - b.begin;
                    return diff < 0 ? -1 : diff > 0 ? 1 : 0;
                }
            });

            for (Event e : candidates) {
                if (events.size() >= max) break;
                events.add(e);
            }
        } catch (Exception ignored) {
            // Sin proveedor de calendario: lista vacía.
        } finally {
            if (cursor != null) {
                try { cursor.close(); } catch (Exception ignored) {}
            }
        }
        return events;
    }
}
