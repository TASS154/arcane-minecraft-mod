package com.arcane.api.form;

import com.arcane.execution.DeliveryResult;
import com.arcane.execution.DeliverySpec;
import com.arcane.execution.ExecutionContext;

public interface FormHandler {
    DeliveryResult deliver(ExecutionContext context, DeliverySpec spec);
}
