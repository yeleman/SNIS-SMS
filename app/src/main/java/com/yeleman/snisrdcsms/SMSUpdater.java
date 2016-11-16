package com.yeleman.snisrdcsms;

interface SMSUpdater
{
    void gotSms(String from, Long timestamp, String body);
    void gotSMSStatusUpdate(int status, String message);
}
