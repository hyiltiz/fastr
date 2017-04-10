/*
 * Copyright (c) 2013, 2017, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.oracle.truffle.r.nodes.function;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.nodes.attributes.SpecialAttributesFunctions.GetDimAttributeNode;
import com.oracle.truffle.r.nodes.function.opt.ShareObjectNode;
import com.oracle.truffle.r.nodes.unary.UnaryNode;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.data.RAttributable;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.RTypedValue;

public abstract class ImplicitClassHierarchyNode extends UnaryNode {

    private static final RStringVector implicitArrayClass = ShareObjectNode.sharePermanent(RDataFactory.createStringVector("array"));
    private static final RStringVector implicitMatrixClass = ShareObjectNode.sharePermanent(RDataFactory.createStringVector("matrix"));
    @CompilationFinal(dimensions = 1) private static final RStringVector[] implicitClasses = new RStringVector[RType.values().length];

    public static RStringVector getImplicitClass(RType type) {
        RStringVector result = implicitClasses[type.ordinal()];
        if (result == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            implicitClasses[type.ordinal()] = result = ShareObjectNode.sharePermanent(RDataFactory.createStringVector(type.getClazz()));
        }
        return result;
    }

    public abstract RStringVector execute(Object arg);

    @Specialization
    protected static RStringVector get(@SuppressWarnings("unused") int value) {
        return getImplicitClass(RType.Integer);
    }

    @Specialization
    protected static RStringVector get(@SuppressWarnings("unused") double value) {
        return getImplicitClass(RType.Double);
    }

    @Specialization
    protected static RStringVector get(@SuppressWarnings("unused") String value) {
        return getImplicitClass(RType.Character);
    }

    @Specialization
    protected static RStringVector get(@SuppressWarnings("unused") byte value) {
        return getImplicitClass(RType.Logical);
    }

    @Specialization(limit = "5", guards = "value.getClass() == valueClass")
    protected static RStringVector getCachedClass(RTypedValue value,
                    @Cached("value.getClass()") Class<? extends RTypedValue> valueClass,
                    @Cached("createBinaryProfile()") ConditionProfile isArray,
                    @Cached("createBinaryProfile()") ConditionProfile isMatrix,
                    @Cached("create()") GetDimAttributeNode getDim) {
        return getCachedType(value, valueClass.cast(value).getRType(), isArray, isMatrix, getDim);
    }

    @Specialization(replaces = "getCachedClass", limit = "5", guards = "value.getRType() == type")
    protected static RStringVector getCachedType(RTypedValue value,
                    @Cached("value.getRType()") RType type,
                    @Cached("createBinaryProfile()") ConditionProfile isArray,
                    @Cached("createBinaryProfile()") ConditionProfile isMatrix,
                    @Cached("create()") GetDimAttributeNode getDim) {
        int[] dimensions = getDim.getDimensions(value);
        if (isMatrix.profile(GetDimAttributeNode.isMatrix(dimensions))) {
            return implicitMatrixClass;
        } else if (isArray.profile(GetDimAttributeNode.isArray(dimensions))) {
            return implicitArrayClass;
        } else {
            return getImplicitClass(type);
        }
    }

    @Specialization(replaces = {"getCachedClass", "getCachedType"})
    protected static RStringVector get(RTypedValue value,
                    @Cached("createBinaryProfile()") ConditionProfile isArray,
                    @Cached("createBinaryProfile()") ConditionProfile isMatrix,
                    @Cached("create()") GetDimAttributeNode getDim) {
        return getCachedType(value, value.getRType(), isArray, isMatrix, getDim);
    }

    public static RStringVector getImplicitClass(Object value) {
        CompilerAsserts.neverPartOfCompilation();
        if (value instanceof Integer) {
            return getImplicitClass(RType.Integer);
        } else if (value instanceof Double) {
            return getImplicitClass(RType.Double);
        } else if (value instanceof String) {
            return getImplicitClass(RType.Character);
        } else if (value instanceof Byte) {
            return getImplicitClass(RType.Logical);
        } else if (value instanceof RAttributable) {
            RAttributable attributable = (RAttributable) value;
            RIntVector dim = (RIntVector) attributable.getAttr(RRuntime.DIM_ATTR_KEY);
            if (dim != null) {
                int[] dimArray = dim.getInternalStore();
                if (GetDimAttributeNode.isMatrix(dimArray)) {
                    return implicitMatrixClass;
                } else if (GetDimAttributeNode.isArray(dimArray)) {
                    return implicitArrayClass;
                }
            }
        }
        return getImplicitClass(((RTypedValue) value).getRType());
    }
}
