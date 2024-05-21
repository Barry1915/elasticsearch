/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.autoscaling.action;

import org.elasticsearch.common.io.stream.Writeable;
import org.elasticsearch.core.TimeValue;
import org.elasticsearch.test.AbstractWireSerializingTestCase;

public class DeleteAutoscalingPolicyActionRequestWireSerializingTests extends AbstractWireSerializingTestCase<
    DeleteAutoscalingPolicyAction.Request> {

    @Override
    protected Writeable.Reader<DeleteAutoscalingPolicyAction.Request> instanceReader() {
        return DeleteAutoscalingPolicyAction.Request::new;
    }

    @Override
    protected DeleteAutoscalingPolicyAction.Request createTestInstance() {
        return new DeleteAutoscalingPolicyAction.Request(TimeValue.THIRTY_SECONDS, TimeValue.THIRTY_SECONDS, randomAlphaOfLength(8));
    }

    @Override
    protected DeleteAutoscalingPolicyAction.Request mutateInstance(DeleteAutoscalingPolicyAction.Request instance) {
        return null;// TODO implement https://github.com/elastic/elasticsearch/issues/25929
    }

}
