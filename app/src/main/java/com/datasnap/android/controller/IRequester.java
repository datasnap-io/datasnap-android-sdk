
package com.datasnap.android.controller;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;

public interface IRequester {

    HttpResponse send(HttpPost httpPost);
}
