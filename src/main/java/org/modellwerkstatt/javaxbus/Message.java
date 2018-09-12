package org.modellwerkstatt.javaxbus;

import mjson.Json;

public class Message {
    private boolean typeError;
    private String errorMessage;
    private String errorCode;
    private String errorType;


    private boolean send;
    private Json payLoad;
    private String replyAddress;
    private String address;




    public Message(String adr, boolean sended, String reply, Json content) {
        typeError =false;
        payLoad= content;
        send = sended;
        address = adr;
        replyAddress = reply;
    }

    public Message(String adr, boolean sended, String reply, String message, String failCode, String failType) {
        typeError = true;
        send = sended;
        address = adr;
        replyAddress = reply;
        errorMessage = message;
        errorCode = failCode;
        errorType =  failType;
    }

    public boolean isPublishedMsg(){
        return !send;
    }

    public Json getBodyAsMJson(){
        if (isErrorMsg()) {
            String exMsg = String.format("This is a error msg '%s' (code: %s, type %s), no body present!",
                    getErrMessage(), getErrFailureCode(), getErrFailureType() );
            throw new IllegalStateException(exMsg);
        }
        return payLoad;
    }

    public String getReplyAddress(){
        // or null
        return replyAddress;
    }
    public String getAddress(){
        // or null
        return address;
    }

    public boolean isErrorMsg(){
        return typeError;
    }

    public String getErrFailureCode() {
        if (!isErrorMsg()){
            throw new IllegalStateException("This is not an error message! Msg body is " + getBodyAsShortString());
        }
        return errorCode;
    }

    public String getErrFailureType() {
        if (!isErrorMsg()){
            throw new IllegalStateException("This is not an error message! Msg body is " + getBodyAsShortString());
        }
        return errorType;
    }

    public String getErrMessage() {
        if (!isErrorMsg()){
            throw new IllegalStateException("This is not an error message! Msg body is " + getBodyAsShortString());
        }
        return errorMessage;
    }


    private String getBodyAsShortString(){
        return payLoad.toString(50);
    }

    @Override
    public String toString() {
        if (isErrorMsg()) {
            return String.format("[ErrorMsg '%s' (code %s, type %s)]", getErrMessage(), getErrFailureCode(), getErrFailureType());
        }
        return "[Message " + payLoad + "]";
    }
}
