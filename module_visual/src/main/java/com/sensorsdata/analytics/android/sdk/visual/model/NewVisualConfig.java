package com.sensorsdata.analytics.android.sdk.visual.model;

/**
 * @author xiongwenjie
 * @time 2022/11/14 15:12
 * @des
 * @updateAuthor $
 * @updateDate $
 * @updateDes
 */

public class NewVisualConfig {
    private String screenName;
    private String elementSelector;
    private String event_id;

    public String getScreenName() {
        return screenName;
    }

    public void setScreenName(String screenName) {
        this.screenName = screenName;
    }

    public String getElementSelector() {
        return elementSelector;
    }

    public void setElementSelector(String elementSelector) {
        this.elementSelector = elementSelector;
    }

    public String getEvent_id() {
        return event_id;
    }

    public void setEvent_id(String event_id) {
        this.event_id = event_id;
    }
}
