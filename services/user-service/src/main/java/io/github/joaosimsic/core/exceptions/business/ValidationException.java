<<<<<<<< HEAD:services/user-service/src/main/java/io/github/joaosimsic/core/exceptions/business/ValidationException.java
package io.github.joaosimsic.core.exceptions.business;
========
package com.example.message.core.exceptions.business;
>>>>>>>> origin/main:src/main/java/com/example/message/core/exceptions/business/ValidationException.java

import io.github.joaosimsic.core.exceptions.abstracts.BusinessException;

public class ValidationException extends BusinessException {
  public ValidationException(String message) {
    super(message);
  }
}
