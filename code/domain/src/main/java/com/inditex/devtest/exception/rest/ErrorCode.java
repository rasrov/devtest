package com.inditex.devtest.exception.rest;

import java.io.Serializable;

public interface ErrorCode extends Serializable {

  String code();

  String message();
}
