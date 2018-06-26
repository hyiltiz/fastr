/*
 * Copyright (c) 2017, 2018, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 3 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 3 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 3 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.oracle.truffle.r.runtime.data;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.Message;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.profiles.ValueProfile;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.model.RAbstractContainer;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;
import com.oracle.truffle.r.runtime.data.nodes.FastPathVectorAccess.FastPathFromStringAccess;
import com.oracle.truffle.r.runtime.data.nodes.SlowPathVectorAccess.SlowPathFromStringAccess;
import com.oracle.truffle.r.runtime.data.nodes.VectorAccess;

public final class RForeignStringWrapper extends RForeignWrapper implements RAbstractStringVector {

    public RForeignStringWrapper(TruffleObject delegate) {
        super(delegate);
    }

    @Override
    public RStringVector materialize() {
        return (RStringVector) copy();
    }

    @Override
    @TruffleBoundary
    public Object getDataAtAsObject(int index) {
        return getDataAt(index);
    }

    @Override
    @TruffleBoundary
    public String getDataAt(int index) {
        return getString(delegate, index);
    }

    private static String getString(TruffleObject truffleObject, int index) throws RuntimeException {
        try {
            Object value = ForeignAccess.sendRead(READ, truffleObject, index);
            if (value instanceof TruffleObject) {
                if (ForeignAccess.sendIsNull(IS_NULL, (TruffleObject) value)) {
                    return RRuntime.STRING_NA;
                }
            }
            return value.toString();
        } catch (UnsupportedMessageException | UnknownIdentifierException e) {
            throw RInternalError.shouldNotReachHere(e);
        }
    }

    private static final class FastPathAccess extends FastPathFromStringAccess {

        FastPathAccess(RAbstractContainer value) {
            super(value);
        }

        private final ConditionProfile isNullProfile = ConditionProfile.createBinaryProfile();
        private final ValueProfile resultProfile = ValueProfile.createClassProfile();
        @Child private Node getSize = Message.GET_SIZE.createNode();
        @Child private Node read = Message.READ.createNode();
        @Child private Node isNull;

        @Override
        protected int getLength(RAbstractContainer vector) {
            try {
                return (int) ForeignAccess.sendGetSize(getSize, ((RForeignWrapper) vector).delegate);
            } catch (UnsupportedMessageException e) {
                throw RInternalError.shouldNotReachHere(e);
            }
        }

        @Override
        protected String getString(Object internalStore, int index) {
            try {
                Object value = ForeignAccess.sendRead(read, (TruffleObject) internalStore, index);
                if (isNullProfile.profile(value instanceof TruffleObject)) {
                    if (isNull == null) {
                        CompilerDirectives.transferToInterpreterAndInvalidate();
                        isNull = insert(Message.IS_NULL.createNode());
                    }
                    if (ForeignAccess.sendIsNull(isNull, (TruffleObject) value)) {
                        return RRuntime.STRING_NA;
                    }
                }
                return resultProfile.profile(value).toString();
            } catch (UnsupportedMessageException | UnknownIdentifierException e) {
                throw RInternalError.shouldNotReachHere(e);
            }
        }
    }

    @Override
    public VectorAccess access() {
        return new FastPathAccess(this);
    }

    private static final SlowPathFromStringAccess SLOW_PATH_ACCESS = new SlowPathFromStringAccess() {
        @Override
        @TruffleBoundary
        protected int getLength(RAbstractContainer vector) {
            try {
                return (int) ForeignAccess.sendGetSize(GET_SIZE, ((RForeignStringWrapper) vector).delegate);
            } catch (UnsupportedMessageException e) {
                throw RInternalError.shouldNotReachHere(e);
            }
        }

        @Override
        @TruffleBoundary
        protected String getString(Object store, int index) {
            RForeignStringWrapper vector = (RForeignStringWrapper) store;
            return RForeignStringWrapper.getString(vector.delegate, index);
        }
    };

    @Override
    public VectorAccess slowPathAccess() {
        return SLOW_PATH_ACCESS;
    }
}
