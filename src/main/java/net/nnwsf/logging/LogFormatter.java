package net.nnwsf.logging;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

public class LogFormatter extends Formatter {

    @Override
    public String format(LogRecord record) {
        Date date = new Date(record.getMillis());
        DateFormat formatter = new SimpleDateFormat("HH:mm:ss.SSS");
        formatter.setTimeZone(TimeZone.getTimeZone("UTC"));

        String message = record.getMessage();

        if(record.getParameters() != null) {
            for(int i= 0; i < record.getParameters().length; i++) {
                if(record.getParameters()[i] instanceof  Exception) {
                    Exception e = (Exception)record.getParameters()[i];
                    StringWriter sw = new StringWriter();
                    e.printStackTrace(new PrintWriter(sw));
                    message += "\n" + sw.toString();
                }
                if(record.getParameters()[i] != null){
                    message = message.replace("{" + i + "}", record.getParameters()[i].toString());
                }
            }
        }
        if(record.getThrown() != null) {
            StringWriter sw = new StringWriter();
            record.getThrown().printStackTrace(new PrintWriter(sw));
            message += "\n" + sw.toString();
        }
        return new StringBuilder()
                .append(formatter.format(date))
                .append(" - ")
                .append(Thread.currentThread().toString())
                .append( " - ")
                .append(record.getLoggerName())
                .append( " - ")
                .append(record.getLevel())
                .append(": ")
                .append(message)
                .append("\n")
                .toString();
    }

}
