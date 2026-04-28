package com.smarttask.service;

import com.smarttask.model.Formation;
import com.smarttask.util.EnvConfig;

import java.awt.Desktop;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class GoogleCalendarService {

    private static final DateTimeFormatter CAL_DATE = DateTimeFormatter.BASIC_ISO_DATE;
    private final String calendarId = EnvConfig.read("GOOGLE_CALENDAR_ID", "");
    private final String timezone = EnvConfig.read("APP_TIMEZONE", "Europe/Paris");

    public String buildEventUrl(Formation formation) {
        if (formation == null || formation.getDateDebut() == null || formation.getDateFin() == null) {
            return "";
        }

        String title = safe(formation.getTitre(), "Formation");
        String details = safe(formation.getDescription(), "Formation SmartTask");
        String dates = toGoogleDates(formation.getDateDebut(), formation.getDateFin());

        return "https://calendar.google.com/calendar/render?action=TEMPLATE"
                + (calendarId.isBlank() ? "" : "&src=" + enc(calendarId))
                + "&text=" + enc(title)
                + "&details=" + enc(details)
                + "&ctz=" + enc(timezone)
                + "&dates=" + enc(dates);
    }

    public void openEventInBrowser(Formation formation) {
        String url = buildEventUrl(formation);
        if (url.isBlank()) {
            return;
        }
        if (!Desktop.isDesktopSupported()) {
            return;
        }
        try {
            Desktop.getDesktop().browse(URI.create(url));
        } catch (Exception ex) {
            System.err.println("Could not open browser for Google Calendar link: " + ex.getMessage());
        }
    }

    private static String toGoogleDates(LocalDate start, LocalDate endInclusive) {
        LocalDate endExclusive = endInclusive.plusDays(1);
        return start.format(CAL_DATE) + "/" + endExclusive.format(CAL_DATE);
    }

    private static String safe(String value, String fallback) {
        return (value == null || value.isBlank()) ? fallback : value.trim();
    }

    private static String enc(String value) {
        try {
            return URLEncoder.encode(value, StandardCharsets.UTF_8.toString());
        } catch (UnsupportedEncodingException e) {
            return value;
        }
    }
}
