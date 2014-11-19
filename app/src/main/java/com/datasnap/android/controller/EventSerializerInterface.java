package com.datasnap.android.controller;

import com.datasnap.android.models.EventWrapperSuper;

public interface EventSerializerInterface {
  String serialize(EventWrapperSuper payload);

  com.datasnap.android.models.EventWrapper deserialize(String str);
}
