package com.wakereality.storyfinding;

/**
 * Created by adminsag on 4/21/17.
 */

public class EventStoryFindingAppError {
    public String errorCategory = "";
    public int errorNumber = 0;
    public int errorLevel = 0;
    public String detail1 = "";
    public String detail2 = "";

    public EventStoryFindingAppError(String errorCategoryPrefix, int errorNumberForCategory, int errorSeverityLevel, String detailString1, String detailString2) {
        errorCategory = errorCategoryPrefix;
        errorNumber = errorNumberForCategory;
        errorLevel = errorSeverityLevel;
        detail1 = detailString1;
        detail2 = detailString2;
    }
}
