/*
 * Copyright (c) 2016, 2018, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.engine.interop;

import com.oracle.truffle.api.interop.CanResolve;
import com.oracle.truffle.api.interop.Message;
import com.oracle.truffle.api.interop.MessageResolution;
import com.oracle.truffle.api.interop.Resolve;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.RLogical;

@MessageResolution(receiverType = RLogical.class)
public class RLogicalMR {
    @Resolve(message = "IS_BOXED")
    public abstract static class RLogicalIsBoxedNode extends Node {
        protected Object access(RLogical receiver) {
            return !RRuntime.isNA(receiver.getValue());
        }
    }

    @Resolve(message = "KEY_INFO")
    public abstract static class RLogicalKeyInfoNode extends Node {
        protected Object access(@SuppressWarnings("unused") RLogical receiver, @SuppressWarnings("unused") Object identifier) {
            return 0;
        }
    }

    @Resolve(message = "UNBOX")
    public abstract static class RLogicalUnboxNode extends Node {
        protected Object access(RLogical receiver) {
            return unboxLogical(receiver.getValue());
        }
    }

    @CanResolve
    public abstract static class RLogicalCheck extends Node {
        protected static boolean test(TruffleObject receiver) {
            return receiver instanceof RLogical;
        }
    }

    public static boolean isUnboxable(byte value) {
        return !RRuntime.isNA(value);
    }

    public static Object unboxLogical(byte value) {
        if (!isUnboxable(value)) {
            throw UnsupportedMessageException.raise(Message.UNBOX);
        }
        return RRuntime.fromLogical(value);
    }
}