<<<<<<<< HEAD:services/user-service/src/main/java/io/github/joaosimsic/core/exceptions/business/ConflictException.java
package io.github.joaosimsic.core.exceptions.business;
========
package com.example.message.core.exceptions.business;
>>>>>>>> origin/main:src/main/java/com/example/message/core/exceptions/business/ConflictException.java

import io.github.joaosimsic.core.exceptions.abstracts.BusinessException;

public class ConflictException extends BusinessException {
  public ConflictException(String message) {
    super(message);
  }
}
