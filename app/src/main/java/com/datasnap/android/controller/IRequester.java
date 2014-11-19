
package com.datasnap.android.controller;

import com.datasnap.android.models.EventListContainer;
//import com.datasnap.android.models.SuperOfEventWrapperSuper;

import org.apache.http.HttpResponse;

public interface IRequester {

  HttpResponse send(EventListContainer eventListContainer);

 // SuperOfEventWrapperSuper fetchSettings();
}
