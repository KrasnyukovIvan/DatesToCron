package com.digdes.school;

import java.text.ParseException;
import java.util.List;

public interface DatesToCronConverter {

    /**
     * Default date format for input dates
     */
    public final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss";

    /**
     * Method to convert list of dates to cron expression
     *
     * @param dates list of dates
     * @return cron expression ("0 * * * * MON")
     */
    String convert(List<String> dates) throws DatesToCronConvertException, ParseException;

    /**
     *
     *  Method to get infofmation about interface implementation,
     *  author, name, package
     *
     * @return implementation info
     */
    String getImplementationInfo();

}
