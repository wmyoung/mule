/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.tooling.internal.artifact.params;

import static java.util.stream.Collectors.joining;
import static org.mule.runtime.api.metadata.DataType.fromType;
import static org.mule.runtime.app.declaration.api.fluent.SimpleValueType.DATETIME;
import static org.mule.runtime.app.declaration.api.fluent.SimpleValueType.STRING;
import static org.mule.runtime.app.declaration.api.fluent.SimpleValueType.TIME;
import static org.mule.runtime.extension.api.util.ExtensionMetadataTypeUtils.isMap;

import org.mule.metadata.api.model.ArrayType;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.api.model.ObjectType;
import org.mule.metadata.java.api.annotation.ClassInformationAnnotation;
import org.mule.metadata.message.api.el.TypeBindings;
import org.mule.runtime.api.metadata.ExpressionLanguageMetadataService;
import org.mule.runtime.app.declaration.api.ParameterValue;
import org.mule.runtime.app.declaration.api.ParameterValueVisitor;
import org.mule.runtime.app.declaration.api.fluent.ParameterListValue;
import org.mule.runtime.app.declaration.api.fluent.ParameterObjectValue;
import org.mule.runtime.app.declaration.api.fluent.ParameterSimpleValue;
import org.mule.runtime.app.declaration.api.fluent.SimpleValueType;
import org.mule.runtime.core.api.el.ExpressionManager;
import org.mule.runtime.extension.api.util.ExtensionMetadataTypeUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

public class ParameterExtractor {

  private ExpressionManager expressionManager;
  private ExpressionLanguageMetadataService expressionLanguageMetadataService;

  public <T> T extractValue(ParameterValue parameterValue, MetadataType metadataType, Class<T> type) {
    expressionLanguageMetadataService.getOutputType(TypeBindings.builder().build(), "{ \"actingField\": 1, \"nonActingField\": \"hi\"}", new ExpressionLanguageMetadataService.MessageCallback() {
      @Override
      public void warning(String message, ExpressionLanguageMetadataService.MessageLocation location) {

      }

      @Override
      public void error(String message, ExpressionLanguageMetadataService.MessageLocation location) {

      }
    });

    return (T) expressionManager
        .evaluate("#[%dw 2.0 output application/java --- " + toWeaveExpression(parameterValue, metadataType) + "]",
                  fromType(type))
        .getValue();
  }

  private String toWeaveExpression(ParameterValue parameterValue, MetadataType metadataType) {
    final DataWeaveParameterValueVisitor visitor = new DataWeaveParameterValueVisitor(metadataType);
    parameterValue.accept(visitor);
    return visitor.get();
  }

  public ParameterExtractor(ExpressionManager expressionManager, ExpressionLanguageMetadataService expressionLanguageMetadataService) {
    this.expressionManager = expressionManager;
    this.expressionLanguageMetadataService = expressionLanguageMetadataService;
  }

  private class DataWeaveParameterValueVisitor implements ParameterValueVisitor {

    private String script;
    private MetadataType metadataType;

    public DataWeaveParameterValueVisitor(MetadataType metadataType) {
      this.metadataType = metadataType;
    }

    @Override
    public void visitSimpleValue(ParameterSimpleValue text) {
      this.script = text.getValue();
      SimpleValueType valueType = text.getType();
      if (valueType.equals(DATETIME)) {
        Optional<ClassInformationAnnotation> classInformationAnnotation =
            metadataType.getAnnotation(ClassInformationAnnotation.class);
        if (classInformationAnnotation.isPresent()) {
          String classname = classInformationAnnotation.get().getClassname();
          if (classname.equals(LocalDate.class.getName())) {
            this.script = "'" + this.script + "' as Date";
          } else if (classname.equals(LocalDateTime.class.getName())) {
            this.script = "'" + this.script + "' as LocalDateTime";
          } else {
            this.script = "'" + this.script + "' as DateTime";
          }
        } else {
          this.script = "'" + this.script + "' as DateTime";
        }
      } else if (valueType.equals(TIME)) {
        this.script = "'" + this.script + "' as Time";
      } else if (valueType.equals(STRING)) {
        this.script = "'" + this.script + "'";
      }
    }

    @Override
    public void visitListValue(ParameterListValue list) {
      StringBuilder scriptBuilder = new StringBuilder();
      scriptBuilder.append("[");
      scriptBuilder.append(list.getValues()
          .stream()
          //TODO Cast!
          .map(value -> toWeaveExpression(value, ((ArrayType) metadataType).getType()))
          .collect(joining(",")));
      scriptBuilder.append("]");
      this.script = scriptBuilder.toString();
    }

    @Override
    public void visitObjectValue(ParameterObjectValue objectValue) {
      //TODO cast
      ObjectType objectType = (ObjectType) metadataType;
      StringBuilder scriptBuilder = new StringBuilder();
      scriptBuilder.append("{");
      scriptBuilder.append(objectValue.getParameters().entrySet()
          .stream()
          .map(entry -> "'" + entry.getKey() + "':" + toWeaveExpression(entry.getValue(), isMap(objectType) ? objectType.getOpenRestriction().orElseThrow(() -> new IllegalArgumentException("TODO: change this!"))
                  : objectType.getFieldByName(entry.getKey()).orElseThrow(() -> new IllegalArgumentException("TODO: change this!")).getValue()))
          .collect(joining(",")));
      scriptBuilder.append("}");
      this.script = scriptBuilder.toString();
    }

    private String get() {
      return script;
    }

  }

}
