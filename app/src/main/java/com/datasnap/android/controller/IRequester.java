
package com.datasnap.android.controller;

import com.datasnap.android.models.EventListContainer;
//import com.datasnap.android.models.SuperOfEventWrapperSuper;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;

public interface IRequester {

  HttpResponse send(HttpPost httpPost);

 // SuperOfEventWrapperSuper fetchSettings();
}
