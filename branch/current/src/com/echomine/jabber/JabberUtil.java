package com.echomine.jabber;

import com.echomine.common.ParseException;
import org.apache.oro.text.regex.*;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.xml.sax.InputSource;

import java.io.IOException;
import java.io.StringReader;
import java.util.Calendar;
import java.util.TimeZone;

/**
 * This class contains convenience methods to work with the Jabber protocol.  These may
 * involve features such as parsing date and time according to
 * <a href="http://www.jabber.org/jeps/jep-0082.html">JEP-0082</a> (Date and Time
 * Profile).
 */
public class JabberUtil {
    private static Pattern datePat;
    private static Pattern timePat;
    private static Pattern dateTimePat;
    //date pattern will contain the following \1 = year, \2 = month, \3 = day of month
    private static final String DATE_PATTERN = "([0-9]{4})-([0-1][0-9])-([0-3][0-9])";
    //time pattern will contain the following \1 = hour, \2 = minutes, \3 = seconds, \4 = timezone hour prepended with + or - (null if none or UTC)
    private static final String TIME_PATTERN = "([0-2][0-9]):([0-5][0-9]):([0-5][0-9])(?:\\.[0-9]{3})?(?:(?:Z)|([+-][0-9]{2}:[0-9]{2}))?";
    private static final String DATETIME_PATTERN = DATE_PATTERN + "T" + "([0-2][0-9]):([0-5][0-9]):([0-5][0-9])(?:\\.[0-9]{3})?(?:(?:Z)|([+-][0-9]{2}:[0-9]{2}))";

    /**
     * Sets up the initial stuff
     */
    private static void setup() throws MalformedPatternException {
        Perl5Compiler p5compiler = new Perl5Compiler();
        datePat = p5compiler.compile(DATE_PATTERN);
        timePat = p5compiler.compile(TIME_PATTERN);
        dateTimePat = p5compiler.compile(DATETIME_PATTERN);
    }

    /**
     * parses the a date string according to JEP-0082.  The date must be in the format
     * of yyyy-MM-dd.  The parser should be multi-thread safe since the matcher is
     * instantiated per call.  However, the pattern is already compiled so there should
     * be little delay when doing the regex matching.  Also, the time should not be
     * considered in the returned date as this method deals specifically with date, not time.
     * Timezone should not matter in this case.
     * @param date the date in the format specified by the JEP.
     * @return the calendar-based date
     * @throws ParseException when parsing problems occur
     */
    public static Calendar parseDate(String date) throws ParseException {
        try {
            if (datePat == null) setup();
            Perl5Matcher p5matcher = new Perl5Matcher();
            if (p5matcher.matches(date, datePat)) {
                //matches
                Calendar cal = Calendar.getInstance();
                MatchResult result = p5matcher.getMatch();
                cal.set(Calendar.YEAR, Integer.parseInt(result.group(1)));
                cal.set(Calendar.MONTH, Integer.parseInt(result.group(2)) - 1);
                cal.set(Calendar.DAY_OF_MONTH, Integer.parseInt(result.group(3)));
                return cal;
            } else {
                throw new ParseException("The date provided does not conform to the parsing patterns");
            }
        } catch (MalformedPatternException ex) {
            throw new ParseException("The regular expression for parsing date is valid");
        }
    }

    /**
     * parses the time string according to JEP-0082.  The time must be in the format
     * of hh:mm:ss[.sss][TZD].  The fractional seconds are optional and is ignored
     * by the parsing.  The timezone can either be the letter Z to denote UTC or can be
     * an offset from UTC (ie. +-08:00).  The returned date should not use the date
     * value of the object.  Only the time should be used.  The timezone is optional and thus
     * if it does not exist, then the current system timezone is assumed.
     * @param time the time string
     * @return the parsed time
     * @throws ParseException
     */
    public static Calendar parseTime(String time) throws ParseException {
        try {
            if (timePat == null) setup();
            Perl5Matcher p5matcher = new Perl5Matcher();
            if (p5matcher.matches(time, timePat)) {
                //matches
                Calendar cal = Calendar.getInstance();
                MatchResult result = p5matcher.getMatch();
                cal.set(Calendar.HOUR_OF_DAY, Integer.parseInt(result.group(1)));
                cal.set(Calendar.MINUTE, Integer.parseInt(result.group(2)));
                cal.set(Calendar.SECOND, Integer.parseInt(result.group(3)));
                cal.setTimeZone(parseTimeZone(result.group(4)));
                return cal;
            } else {
                throw new ParseException("The time provided does not conform to the parsing patterns");
            }
        } catch (MalformedPatternException ex) {
            throw new ParseException("The regular expression for parsing date is valid");
        }
    }

    /**
     * This method parses the date and the time out of the string.  The format is in the form of
     * yyyy-MM-ddThh:mm:ss[.sss]TZD.  This is essentially the combination of the date and time parsing
     * with the exception that the timezone is a required element rather than optional (as in the time
     * parsing).
     * @param dateTime the datetime string to be parsed
     * @return the calendar object with the date and time set and timezone set
     * @throws ParseException when parsing problems occur
     */
    public static Calendar parseDateTime(String dateTime) throws ParseException {
        try {
            if (dateTimePat == null) setup();
            Perl5Matcher p5matcher = new Perl5Matcher();
            if (p5matcher.matches(dateTime, dateTimePat)) {
                //matches
                Calendar cal = Calendar.getInstance();
                MatchResult result = p5matcher.getMatch();
                cal.set(Calendar.YEAR, Integer.parseInt(result.group(1)));
                cal.set(Calendar.MONTH, Integer.parseInt(result.group(2)) - 1);
                cal.set(Calendar.DAY_OF_MONTH, Integer.parseInt(result.group(3)));
                cal.set(Calendar.HOUR_OF_DAY, Integer.parseInt(result.group(4)));
                cal.set(Calendar.MINUTE, Integer.parseInt(result.group(5)));
                cal.set(Calendar.SECOND, Integer.parseInt(result.group(6)));
                cal.setTimeZone(parseTimeZone(result.group(7)));
                return cal;
            } else {
                throw new ParseException("The time provided does not conform to the parsing patterns");
            }
        } catch (MalformedPatternException ex) {
            throw new ParseException("The regular expression for parsing date is valid");
        }
    }

    /**
     * This formats the date to a string that is compliant with the Jabber dateTime format.
     * The timezone used by the date formatter is the timezone specified in the calendar instance.
     * @param cal the calendar instance to convert into a string
     * @return the datetime string representation
     */
    public static String formatDateTime(Calendar cal) {
        StringBuffer buf = new StringBuffer();
        buf.append(cal.get(Calendar.YEAR)).append("-");
        buf.append(cal.get(Calendar.MONTH) + 1).append("-");
        buf.append(cal.get(Calendar.DAY_OF_MONTH)).append("T");
        buf.append(cal.get(Calendar.HOUR_OF_DAY)).append(":");
        buf.append(cal.get(Calendar.MINUTE)).append(":");
        buf.append(cal.get(Calendar.SECOND));
        //now append the timezone
        String tz = cal.getTimeZone().getID();
        if ("GMT".equals(tz))
            buf.append("Z");
        else
            buf.append(tz.substring(3, tz.length()));
        return buf.toString();
    }

    /**
     * Parses the timezone from a given string.  The timezone can be null, in which case
     * the UTC timezone is assumed.  The timezone can be the string "Z", in which case the UTC timezone
     * will be returned.  Any other timezones should be in the format +-hh:mm.  If the timezone
     * cannot be recognized, UTC will be returned.
     * @param tz the timezone string, or can be null
     * @return the timezone instance, or current timezone if null is passed in
     */
    public static TimeZone parseTimeZone(String tz) {
        if (tz == null) return TimeZone.getTimeZone("GMT");
        return TimeZone.getTimeZone("GMT" + tz);
    }

    /**
     * parses the XML string into a JDOM dom element.  This dom element is reuseable and can be inserted
     * into another dom tree.
     */
    public static Element parseXmlStringToDOM(String xmlStr) throws IOException, JDOMException {
        SAXBuilder builder = new SAXBuilder();
        Document doc = builder.build(new InputSource(new StringReader(xmlStr)));
        return doc.getRootElement().detach();
    }
}
