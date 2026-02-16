<<<<<<<< HEAD:services/user-service/src/main/java/io/github/joaosimsic/core/exceptions/business/UserNotFoundException.java
package io.github.joaosimsic.core.exceptions.business;
========
package com.example.message.core.exceptions.business;
>>>>>>>> origin/main:src/main/java/com/example/message/core/exceptions/business/UserNotFoundException.java

import io.github.joaosimsic.core.exceptions.abstracts.BusinessException;

public class UserNotFoundException extends BusinessException {
  public UserNotFoundException(String message) {
    super(message);
  }
}
