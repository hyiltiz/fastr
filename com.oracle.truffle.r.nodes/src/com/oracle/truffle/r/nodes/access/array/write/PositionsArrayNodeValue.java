/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.access.array.write;

import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.nodes.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.access.array.*;
import com.oracle.truffle.r.nodes.access.array.ArrayPositionCast.OperatorConverterNode;
import com.oracle.truffle.r.runtime.*;

@NodeChildren({@NodeChild(value = "vector", type = RNode.class), @NodeChild(value = "newValue", type = RNode.class)})
public class PositionsArrayNodeValue extends PositionsArrayNodeAdapter {
    @Children protected final MultiDimPosConverterValueNode[] multiDimOperatorConverters;

    public PositionsArrayNodeValue(ArrayPositionCast[] elements, RNode[] positions, OperatorConverterNode[] operatorConverters, MultiDimPosConverterValueNode[] multiDimOperatorConverters) {
        super(elements, positions, operatorConverters);
        this.multiDimOperatorConverters = multiDimOperatorConverters;
    }

    @ExplodeLoop
    public Object executeEval(VirtualFrame frame, Object vector, Object value) {
        Object[] evaluatedElements = new Object[elements.length];
        if (elements.length > 0) {
            for (int i = 0; i < elements.length; i++) {
                Object p = positions[i].execute(frame);
                Object convertedOperator = operatorConverters[i].executeConvert(frame, vector, p, RRuntime.LOGICAL_TRUE);
                evaluatedElements[i] = elements[i].executeArg(frame, vector, convertedOperator);
                if (multiDimOperatorConverters != null && multiDimOperatorConverters.length > 1) {
                    evaluatedElements[i] = multiDimOperatorConverters[i].executeConvert(frame, vector, value, evaluatedElements[i]);
                }
            }
        }
        return elements.length == 1 ? evaluatedElements[0] : evaluatedElements;
    }

}
