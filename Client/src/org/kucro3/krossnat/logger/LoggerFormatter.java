package org.kucro3.krossnat.logger;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

public class LoggerFormatter extends Formatter {
    @Override
    public String format(LogRecord record)
    {
        Date date = new Date(record.getMillis());

        return new StringBuilder("[")
                .append(DATE_FORMAT.format(date))
                .append("][")
                .append(record.getLoggerName())
                .append("][")
                .append(record.getLevel().getName())
                .append("] ")
                .append(record.getMessage())
                .append("\n")
                .toString();
    }

    public static LoggerFormatter getInstance()
    {
        return INSTANCE;
    }

    private final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private static final LoggerFormatter INSTANCE = new LoggerFormatter();
}
