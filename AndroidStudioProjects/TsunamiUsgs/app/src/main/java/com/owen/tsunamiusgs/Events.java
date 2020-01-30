package com.owen.tsunamiusgs;

public class Events {
    public final String title;

    public final long time;

    public final int tsunamiAlert;

    public Events(String eventTitle, long eventTime, int eventTsunamiAlert){

        title=eventTitle;
        time=eventTime;
        tsunamiAlert=eventTsunamiAlert;
    }



}

